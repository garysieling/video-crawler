package emails

import java.nio.file.{Files, Paths}
import java.nio.charset.StandardCharsets
import java.io._
import java.nio.charset.StandardCharsets
import java.nio.file.{Files, Paths}
import java.util.Date

import org.apache.solr.client.solrj.SolrQuery
import org.apache.solr.client.solrj.impl.HttpSolrClient
import org.apache.solr.common.SolrDocument
import org.json.{JSONArray, JSONObject}
import org.nd4j.linalg.api.ndarray.INDArray
import org.nd4j.linalg.ops.transforms.Transforms
import util.{NLP, Semantic}
import java.util

import org.nd4j.linalg.cpu.nativecpu.NDArray

import scala.collection.JavaConverters._
import scala.collection.parallel.ForkJoinTaskSupport
//import scala.util.parsing.json.JSONArray

case class Link(title: String, url: String, text: String, id: String, score: Float)

object ConceptSearchEmails {
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

  val modelFile = "D:\\projects\\clones\\pathToSaveModel10_10_1000_5_1510799977189.txt"
  val w2v = new Semantic(modelFile)
  w2v.init

  // todo can this happen while other stuff is going on?
  val model = w2v.model.getOrElse(???)

  var getWordVectorsMeanCache = Map[List[String], INDArray]()
  def getWordVectorsMean(tokens: List[String]): Option[INDArray] = {
    val key =
      tokens.filter(
        model.getWordVector(_) != null
      )

    if (key.length == 0) {
      None
    } else {
      // TODO: database-ize
      if (!getWordVectorsMeanCache.contains(key)) {
        val output: INDArray = model.getWordVectorsMean(key.asJavaCollection)
        getWordVectorsMeanCache = getWordVectorsMeanCache + (key -> output)

        Some(output)
      } else {
        Some(getWordVectorsMeanCache(key))
      }
    }
  }

  def main(args: Array[String]): Unit = {
    val dataType = new VideoDataType

    // TODO port unit tests?
    // TODO get data from Google spreadsheet
    // TODO how long does this take if you do 200 emails instead of one
    // TODO templates for Aweber emails
    // TODO just take input as JSon and output as json
    // And write some assertins on the input and output

    //val textTemplate = scala.io.Source.fromInputStream(getClass.getResourceAsStream("/alerts.txt")).mkString
    //val htmlTemplate = scala.io.Source.fromInputStream(getClass.getResourceAsStream("/alerts.html")).mkString
    val request = scala.io.Source.fromFile(args(0)).mkString

    def list(v: Object): List[String] = {
      v match {
        case (vv: util.List[Object]) => {
          vv.asScala.map(_.toString).toList
        }
        case _ => ???
      }
    }

    val data = new JSONArray(request)

    val parallel =
      data.toList.asScala.flatMap(
        (e: AnyRef) => {
          e match {
            case (entry: util.HashMap[String, Object]) => {
              val c = entry.get("context")

              c match {
                case (context: util.HashMap[String, Object]) => {

                  val id = context.get("id")
                  val email = context.get("email")
                  val like = list(context.get("like"))
                  val dislike = list(context.get("dislike"))
                  val previouslySent = list(context.get("sent"))

                  if (like.length > 0) {
                    println(new Date)
                    val links1 = generate(like, dislike, previouslySent, new VideoDataType)
                    links1.map(println)
                    println(new Date)

                    println(new Date)
                    val links2 = generate(like, dislike, previouslySent, new ArticleDataType)
                    links2.map(println)
                    println(new Date)

                    Some(email, id, links1, links2)
                  } else {
                    None
                  }
                }
                case _ => None
              }
            }
            case _ => None
          }
        }
      ).par

    val threads = 16
    parallel.tasksupport = new ForkJoinTaskSupport(
      new scala.concurrent.forkjoin.ForkJoinPool(threads))

    val output = parallel.toList

    val json = scala.util.parsing.json.JSONArray(output).toString()
    Files.write(Paths.get(args(1)), json.getBytes(StandardCharsets.UTF_8))

    //println(textTemplate)
    //println(htmlTemplate)
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

  val zero1000 = new NDArray(Array.tabulate[Int](1000)( (a) => 0))

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
    def listDocuments(dt: DataType, qq: String, rows: Integer, skip: List[String]): List[SolrDocument] = {
      import scala.collection.JavaConversions._

      val solrUrl = "http://40.87.64.225:8983/solr/" + dt.core
      val solr = new HttpSolrClient(solrUrl)

      val query = new SolrQuery()

      // todo remove negative terms

      val userQuery = qq
      query.setQuery( qq )
      query.setFields((dt.fieldsToRetrieve ++ dt.textFields).toArray: _*)
      query.setRequestHandler("tvrh")
      query.setRows(rows)
      skip.map(
        (id) => {
          query.addFilterQuery("-id:" + id)
        }
      )

      dt.filter match {
        case Some(value: String) => query.addFilterQuery(value)
        case None => {}
      }

      val rsp = solr.query( query )

      val result = rsp.getResults().toList

      result
    }

    val rowsToPull = 100

    val documentsSolr =
      listDocuments(
          dataType,
          queryWords.map(
            (token) => (
              // TODO: ANDs vs ORs
              dataType.fieldsToQuery.map(
                (f) => f._1 + "\"" + token + "\"^" + f._2
              )
            )
          ).toList.mkString(" OR "),
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

    // TODO : caching - in this case each query would potentially duplicate
    val queryMean = getWordVectorsMean(queryWords)

    val mostAbout =
      documentsSolr.map(
        (document: Link) =>
          (
            NLP.getWords(document.text),
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
}