import org.json.JSONObject
import org.nd4j.linalg.api.ndarray.INDArray
import util.{NLP, Semantic}
import org.nd4s.Implicits._
import org.nd4j.linalg.factory.Nd4j
import org.nd4j.linalg.ops.transforms.Transforms
import scala.collection.JavaConverters._

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

    val solr = new Solr("talks")

    val model = w2v.model.get
    val queryMean = model.getWordVectorsMean(List("python", "machine", "learning"))

    println(query)
    val solrResults =
      solr.list(
        query,
        List("score", "title_s", "auto_transcript_txt_en"),
        10
      )

    val documentsArray =
      solrResults.map(
        (document) => {
          val text = document.get("auto_transcript_txt_en").toString
          model.getWordVectorsMean(NLP.getWords(text))
        }
      ).map(
        (vec) => Transforms.cosineSim(vec, queryMean)
      )

    //val documentMeans = Nd4j.create(documentsArray, Array(documentsArray.size, 1000))
    println(documentsArray)


    //println(
    //  documentsSolr
    //)
  }
}