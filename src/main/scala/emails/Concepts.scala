package indexer.emails

import java.util.Date

import org.json.JSONObject
import org.nd4j.linalg.api.ndarray.INDArray
import org.nd4j.linalg.ops.transforms.Transforms
import util.{NLP, Semantic}
import com.hazelcast.core.HazelcastInstance
import emails.Link
import indexer.{DataType, Solr}

import scala.collection.JavaConverters._

/**
  * Created by gary on 12/7/2017.
  */
class Concepts(instance: HazelcastInstance) {
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

  def normalizeUrl(originalUrl: String): String = {
    val result = originalUrl.split("[?]")(0)
    val url =
      if (result.endsWith("/")) {
        result.substring(0, result.length - 1)
      } else {
        result
      }

    url + "?utm_source=findlectures"
  }

  val modelFile = "C:\\data\\pathToSaveModel10_10_1000_5_1510799977189.txt"
  lazy val w2v = new Semantic(modelFile)

  // todo can this happen while other stuff is going on?
  lazy val model = {
    w2v.init
    w2v.model.getOrElse(???)
  }

  var getWordVectorsMeanCache =
    instance.getMap[String, Option[INDArray]]("wordVectors2")

  def shutdown(): Unit = {
    instance.shutdown()
  }

  def getWordVectorsMean(tokens: List[String]): Option[INDArray] = {
    val key: String =
      tokens.sorted.mkString(",")

    if (!getWordVectorsMeanCache.containsKey(key)) {
      val words =
        tokens.filter(
          model.getWordVector(_) != null
        ).sorted

      if (words.length > 0) {
        val output: INDArray = model.getWordVectorsMean(words.asJavaCollection)

        getWordVectorsMeanCache.put(key, Some(output))
        Some(output)
      } else {
        getWordVectorsMeanCache.put(key, None)

        None
     }
    } else {
      getWordVectorsMeanCache.get(key)
    }
  }

  // TODO I think there might be a better way to do this, I doubt this will be good on a GPU
  def safeCosine(
                  a: Option[INDArray],
                  b: Option[INDArray]): Double = {
    a match {
      case Some(aa: INDArray) => {
        b match {
          case Some(bb: INDArray) =>
            Transforms.cosineSim(aa, bb)
          case None => 0
        }
      }
      case None => 0
    }
  }

  def safeAdd(
               a: Option[INDArray],
               b: Option[INDArray]): Option[INDArray] = {
    a match {
      case Some(aa: INDArray) => {
        b match {
          case Some(bb: INDArray) =>
            Some(aa.add(bb))
          case _ => Some(aa)
        }
      }
      case None => {
        b match {
          case Some(bb: INDArray) => {
            Some(bb)
          }
          case None => None
        }
      }
    }
  }

  def safeDiv(a: Option[INDArray], b: Double): Option[INDArray] = {
    a match {
      case Some(aa: INDArray) => {
        Some(aa.div(b))
      }
      case None => None
    }
  }

  def generate(
                queryWords: List[String],
                dislike: List[String],
                previouslySent: List[String],
                dataType: DataType): List[(String, String, String)] = {
    // TODO cluster by nearness? -> problems here:
    //    distance metric is an angle
    //    distance metric in N dimensions so be careful
    //
    /*list.split(",").map(
      (term: String) =>
        (term, getWordVectorsMean(term.split(" ").toList))
    )*/

    //val queryWords = NLP.getWords(query)

    // STEPS:
    //   stay running, take queries (server?)
    //   get user's query
    //      split into related & unrelated
    //   get usere's things to remove
    //   POST to Solr
    //   get [talks, articles] from Solr (many)
    //      can this be Rocchio?
    //   re-sort by 'aboutness', top N
    //   re-sort by diversity
    //  get reddit updater to work against the same core as new stuff
    val rowsToPull = 100

    val solrQuery =
      "(" +
        queryWords.map(
          (token) => (
            // TODO: ANDs vs ORs
            dataType.fieldsToQuery.map(
              (f) => f._1 + "\"" + token + "\"^" + f._2
            )
          )
        ).mkString(" OR ") +
      ") AND (" +
        dislike.map(
          (token) => (
            // TODO: ANDs vs ORs
            dataType.fieldsToQuery.map(
              (f) => "-" + f._1 + "\"" + token + "\"^" + f._2
            )
          )
        ).mkString(" AND ") +
    ")"


    val solr = new Solr(instance)

    val documentsSolr =
      solr.listDocuments(
        dataType,
        solrQuery,
        rowsToPull,
        previouslySent
      ).filter(
        _ != null
      ).filter(
        dataType.postFilter
      ).map(
        (doc) =>
          Link(
            doc.get(dataType.titleField).toString,
            normalizeUrl(dataType.urlField(doc)),
            dataType.textFields.map(
              (field) => doc.get(field)
            ).mkString("\n"),
            doc.get("id").toString,
            doc.get("score").asInstanceOf[Float]
          )
      ).groupBy(_.url).map(
        // TODO: get shortest title
        (grp) => grp._2(0)
      ).toList

    val startTime = new Date
    println(startTime)

    val queryMean = getWordVectorsMean(queryWords)

    val nlp = new NLP(instance)

    val mostAbout =
      documentsSolr.map(
        (document: Link) =>
          (
            nlp.getTokensCached(document.text),
            document
            )
      ).map(
        (document) => {
          val mean = getWordVectorsMean(document._1)

          (document._1, document._2, mean)
        }
      ).map(
        (vec) => (
          vec._2,
          vec._1,
          safeCosine(queryMean, vec._3)
          )
      ).toList // removes par
        .sortBy(
        (vec) => vec._3
      ).reverse

    //mostAbout.tasksupport = new ForkJoinTaskSupport(
    //  new scala.concurrent.forkjoin.ForkJoinPool(threads))

    def pickNext(
                  topDocuments: List[(Link, List[String], Option[INDArray])],
                  remaining: List[(Link, List[String], Option[INDArray])]
                ): (Double, (Link, List[String], Option[INDArray])) = {
      val next =
        remaining.map(
          (tuple) => {
            val chosenMean =
              safeDiv(
                topDocuments.map(
                  (doc) => doc._3
                ).reduce(
                  (a, b) => safeAdd(a, b)
                ),
                topDocuments.length)

            // compare this document to the stuff we already chose
            // this technique will bring back things the most unlike the rest of the collection

            // could also compute this score and skip things that are two close
            (safeCosine(chosenMean, tuple._3), tuple)
          }
        ).toList.sortBy(_._1)

      next.head
    }

    def recurse(
                 idx: Integer,
                 topDocuments: List[(Double, (Link, List[String], Option[INDArray]))],
                 remaining: List[(Link, List[String], Option[INDArray])]
               ): List[(Double, (Link, List[String], Option[INDArray]))] = {
      val nextDocument = pickNext(topDocuments.map((vec) => vec._2), remaining)

      if (idx == 1) {
        List(nextDocument)
      } else {
        val nextRemaining = remaining.filter(
          (doc) => doc._1 != nextDocument._2._1
        )

        val nextTopDocumentList = nextDocument :: topDocuments
        nextDocument :: recurse(idx - 1, nextTopDocumentList, nextRemaining)
      }
    }

    val mostAboutMeans =
      mostAbout.map(
        (tuple) => (
          tuple._1,
          tuple._2,
          getWordVectorsMean(tuple._2)
          )
      )

    if (mostAboutMeans.size > 0) {
      val diverse =
        (1.0, mostAboutMeans.head) :: recurse(
          10,
          List((1.0, mostAboutMeans.head)),
          mostAboutMeans.tail
        )

      diverse.map(
        (doc) => (doc._2)
      ).take(
        10
      ).map(
        (doc) => (doc._1.title, doc._1.url, doc._1.id)
      )
    }
    else {
      List()
    }
  }
}
