package emails

import java.io._
import java.util.Date

import org.apache.solr.client.solrj.SolrQuery
import org.apache.solr.client.solrj.impl.HttpSolrClient
import org.apache.solr.common.SolrDocument
import org.json.JSONObject
import org.nd4j.linalg.api.ndarray.INDArray
import org.nd4j.linalg.ops.transforms.Transforms
import util.{NLP, Semantic}

import scala.collection.JavaConverters._
import scala.collection.parallel.ForkJoinTaskSupport

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

  def getFile(file: File): (String, String) = {
    // post to Solrs
    //println(file)
    val br: BufferedReader = new BufferedReader(new FileReader(file))
    val sb: StringBuilder = new StringBuilder
    var line: String = br.readLine
    while (line != null) {
      {
        sb.append(line)
        sb.append(System.lineSeparator)
        line = br.readLine
      }
    }
    val everything: String = sb.toString
    val obj: JSONObject = new JSONObject(everything)

    /*val data1: List[String] =
      first(obj, Seq("transcript_txt_en", "transcript_s", "auto_transcript_txt_en", "auto_transcript_s", "auto_transcript_txt_en")) match {
        case Some(x: String) => List(x)
        case None => List()
      }*/

    val data2: List[String] =
      first(obj, Seq("description_txt_en", "description_s")) match {
        case Some(x: String) => List(x)
        case None => List()
      }

    val title: List[String] =
      first(obj, Seq("title_s")) match {
        case Some(x: String) => List(x)
        case None => List("No Title")
      }

    val results: List[String] = /*data1 ++ */ data2 ++ title
    (title.head, results.mkString(" "))
  }

  def main(args: Array[String]): Unit = {

    val dataType = new ArticleDataType
    val query = args(0)
    val modelFile = args(1)

    val w2v = new Semantic(modelFile)
    w2v.init

    val model = w2v.model.getOrElse(???)

    var getWordVectorsMeanCache = Map[List[String], INDArray]()
    def getWordVectorsMean(tokens: List[String]): INDArray = {

      if (!getWordVectorsMeanCache.contains(tokens)) {
        val output: INDArray = model.getWordVectorsMean(tokens.asJavaCollection)
        getWordVectorsMeanCache = getWordVectorsMeanCache + (tokens -> output)
      }

      // TODO: database-ize
      getWordVectorsMeanCache(tokens)
    }

    // TODO cluster by nearness? -> problems here:
    //    distance metric is an angle
    //    distance metric in N dimensions so be careful
    //
    query.split(",").map(
      (term: String) =>
        (term, getWordVectorsMean(term.split(" ").toList))
    )

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
    def listDocuments(dt: DataType, qq: String, fl: List[String], rows: Integer, skip: List[String]): List[SolrDocument] = {
      import scala.collection.JavaConversions._

      val solrUrl = "http://40.87.64.225:8983/solr/" + dt.core

      val solr = new HttpSolrClient(solrUrl)

      val query = new SolrQuery()

      // todo remove negative terms

      val userQuery = qq
      query.setQuery( qq )
      query.setFields(fl.toArray: _*)
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

    var top = scala.collection.mutable.MutableList[(Double, String)]()
    var toBeat = 1000000000.0

    case class Article(title: String, url: String, article: String, score: Float)

    val rowsToPull = 100

    // TODO videos
    val documentsSolr =
      listDocuments(
          dataType,
          query.split(",").map(
            (token) => (
              "article_text_s:\"" + token + "\" OR " +
              "title_s:\"" + token + "\"^2 "
            )
          ).toList.mkString(" OR "),
          List("id", "score", "title_s", "article_text_s"),
          rowsToPull,
          List("1", "2", "3")
        ).filter(
          _ != null
        ).filter(
          doc =>
            doc.get("title_s") != null &&
            doc.get("article_text_s") != null &&
            doc.get("article_text_s") != "" &&
            doc.get("id") != null &&
            doc.get("id").toString.startsWith("http")
        ).map(
          (doc) =>
            Article(
              doc.get("title_s").toString,
              // TODO remove query string
              // TODO add UTM tags
              normalizeUrl(doc.get("id").toString),
              doc.get("article_text_s").toString,
              doc.get("score").asInstanceOf[Float]
            )
      ).groupBy(_.url).map(
        // TODO: get shortest title
        (grp) => grp._2(0)
      ).toList.par

    val startTime = new Date
    println(startTime)

    val queryWords = NLP.getWords(args(0))

    // TODO : caching - in this case each query would potentially duplicate
    val queryMean = getWordVectorsMean(queryWords)

    val threads = 16
    documentsSolr.tasksupport = new ForkJoinTaskSupport(
      new scala.concurrent.forkjoin.ForkJoinPool(threads))

    val mostAbout =
      documentsSolr.map(
        (document: Article) =>
          (
            NLP.getWords(document.article) ++ NLP.getWords(document.title),
            document
          )
      ).map(
        (document) => {

          //println("starting doc " + new DateTime())

          val mean = getWordVectorsMean(document._1)
          //println("finished doc" + new DateTime())

          (document._1, document._2, mean)
        }
      ).map(
        (vec) => (vec._2, vec._1, Transforms.cosineSim(vec._3, queryMean))
      ).toList // removes par
        .sortBy(
          (vec) => vec._3
        ).reverse

    //mostAbout.tasksupport = new ForkJoinTaskSupport(
    //  new scala.concurrent.forkjoin.ForkJoinPool(threads))

    def pickNext(
                  topDocuments: List[(Article, List[String], INDArray)],
                  remaining: List[(Article, List[String], INDArray)]
                ): (Double, (Article, List[String], INDArray)) = {
      val next =
        remaining.par.map(
          (tuple) => {
            val chosenMean =
              topDocuments.map(
                (doc) => doc._3
              ).reduce(
                (a, b) => a.add(b)
              ).div(topDocuments.length)

            // compare this document to the stuff we already chose
            // this technique will bring back things the most unlike the rest of the collection

            // could also compute this score and skip things that are two close
            (Transforms.cosineSim(chosenMean, tuple._3), tuple)
          }
        ).toList.sortBy(_._1)

      next.head
    }

    def recurse(
                 idx: Integer,
                 topDocuments: List[(Double, (Article, List[String], INDArray))],
                 remaining: List[(Article, List[String], INDArray)]
               ): List[(Double, (Article, List[String], INDArray))] = {
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
      (doc) => (doc._1.title, doc._1.url)
    ).map(println)

    val endTime = new Date
    println(endTime)
    //println("Rate: " + fileList.size * 1.0 / (endTime.getTime - startTime.getTime))

  }
}