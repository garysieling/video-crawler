package indexer

import org.joda.time.DateTime
import org.json.JSONObject
import org.nd4j.linalg.ops.transforms.Transforms
import util.{NLP, Semantic}

import scala.collection.JavaConverters._
import scala.collection.parallel.ForkJoinTaskSupport

object AboutnessConceptSearch {
  def first(document: JSONObject, strings: Seq[String]): Option[String] = {
    strings.filter(
      (key) => document.has(key) && (
        Option(document.get(key)) match {
          case Some("") => false
          case None => false
          case _ => true
        }
        )
    ).headOption map {
      document.get(_).toString
    }
  }

  def truncate(distanceTitle: Double): String =
    (distanceTitle.toString.substring(0, 4))

  def main(args: Array[String]): Unit = {
    val w2v = new Semantic("D:\\projects\\clones\\pathToSaveModel1.txt")
    w2v.init

    import scala.collection.JavaConversions._
    val query =
      "auto_transcript_txt_en:* AND (" +
      List("writing").map(
        (term) =>
          List(
            "auto_transcript_txt_en:\"" + term + "\"^10",
            "title_s:" + term + "^10"
          )
      ).flatten.mkString(" OR ") + " " +
      List("writing").map(
        (term) => {
          val pTerms: java.util.List[String] = List[String](term).asJava
          val nTerms: java.util.List[String] = List[String]().asJava
          val near =
            w2v.model.get.wordsNearest(pTerms, nTerms, 25)

          "(" +
            near.map(
              (term2: String) => {
                val distanceTranscript = Math.pow(0.5 + w2v.model.get.similarity(term, term2), 2)
                val distanceTitle = Math.pow(0.5 + w2v.model.get.similarity(term, term2), 2)
                "auto_transcript_txt_en:" + term2 + "^" + truncate(distanceTitle) +
                  " OR title_s:" + term2 + "^" + truncate(distanceTitle)
              }
            ).mkString(" OR ") + ")"
        }
      ).mkString(" AND ") + ")"

    val solr = new Solr(null)

    val model = w2v.model.get
    val queryMean = model.getWordVectorsMean(List("python", "machine", "learning"))

    println(query)
    val solrResults =
      solr.list(
        "talks",
        query,
        List("score", "title_s", "auto_transcript_txt_en"),
        100
      )

    val coll =
      solrResults.map(
        (document) =>
          (
            document.get("auto_transcript_txt_en").toString,
            document.get("title_s").toString
          )
      ).par

    coll.tasksupport = new ForkJoinTaskSupport(
      new scala.concurrent.forkjoin.ForkJoinPool(16))

    val nlp = new NLP(null)
    val documents =
      coll.map(
        (document) => {
          println("starting doc " + new DateTime())

          val mean = model.getWordVectorsMean(nlp.getWords(document._1))
          println("finished doc" + new DateTime())

          (document._2, mean)
        }
      ).map(
        (vec) => (vec._1, Transforms.cosineSim(vec._2, queryMean))
      ).toList.sortBy(
        (vec) => vec._2
      ).reverse.map(
        (vec) => vec._1 + ": " + vec._2
      ).mkString("\n")

    //val documentMeans = Nd4j.create(documentsArray, Array(documentsArray.size, 1000))
    println(documents)


    //println(
    //  documentsSolr
    //)
  }
}