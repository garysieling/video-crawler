import org.joda.time.DateTime
import org.json.JSONObject
import org.nd4j.linalg.api.ndarray.INDArray
import org.nd4j.linalg.ops.transforms.Transforms
import util.{NLP, Semantic}

import scala.collection.parallel.ForkJoinTaskSupport
import scala.collection.JavaConverters._

object DiversitySearch {
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
    println(new DateTime)

    val w2v = new Semantic("D:\\projects\\clones\\pathToSaveModel1.txt")
    w2v.init
    val terms = List("python", "pandas")

    val model = w2v.model.get

    val queryMean = model.getWordVectorsMean(terms.asJava)
    val solr = new Solr("articles")

    val solrResults =
      solr.list(
      //  "article:* AND (reddit_title:python^10 reddit_title:pandas^10 article:python article:pandas)",
        "article:* AND (title:python^10 title:pandas^10 title:data^0.93 title:sql^0.7 title:databas^0.7 title:scienc^0.6 title:interact^0.65 title:stack^0.60 title:blog^0.5 title:some^0.09 title:commit^0.4 title:quickli^0.4 title:r^0.49 title:then^0.48 title:growth^0.48 title:extract^0.48 title:posit^0.47 title:impress^0.47 title:market^0.47 title:o^0.47 title:anim^0.47 title:valu^0.47 title:second^0.47 title:mine^0.46 title:with^0.4 title:interview^0.45 title:person^0.44 title:even^0.44 title:grow^0.44 title:question^0.4 title:turn^0.4 title:can't^0.4 title:could^0.43 title:survei^0.43 title:theori^0.42 title:result^0.42 title:while^0.42 title:from^0.42 title:product^0.42 title:don't^0.42 title:read^0.41 title:try^0.4 title:commun^0.4 title:structur^0.40 title:fast^0.40 title:so^0.39 title:written^0.39 title:list^0.3 title:me^0.39 title:if^0.38 title:know^0.38 title:x^0.38 title:visual^0.3 title:elixir^0.37 title:help^0.09 title:framework^0.3 title:to^0.36 title:nim^0.3 title:into^0.3 title:get^0.3 title:and^0.35 title:post^0.35 title:on^0.35 title:updat^0.34 title:like^0.34 title:1^0.34 title:in^0.33 title:open^0.33 title:a^0.33 title:why^0.33 title:creat^0.33 title:of^0.30 title:you^0.2 title:my^0.28 title:an^0.27 title:i^0.27 title:how^0.26 title:2017^0.2 title:is^0.2 title:the^0.16 article:sne^0.37 article:dl.gif^0.37 article:regmedia.co.uk^0.37 article:panda^0.31 article:pv^0.29 article:forecast^0.28 article:point2^0.2 article:points1^0.2 article:nim^0.27 article:datafram^0.25 article:cluster^0.24 article:python^0.2 article:plotli^0.24 article:tf^0.2 article:df^0.24 article:traffic^0.23 article:overflow^0.23 article:gimp^0.22 article:lda^0.2 article:plot^0.22 article:token^0.22 article:ap^0.22 article:quantiti^0.21 article:growth^0.21 article:tcl^0.21 article:donald^0.21 article:robinson^0.21 article:visit^0.20 article:minist^0.19 article:dash^0.19 article:04^0.19 article:column^0.19 article:korean^0.1 article:au^0.1 article:r^0.1 article:women^0.18 article:marbl^0.18 article:django^0.1 article:500^0.18 article:dropdown^0.1 article:malaysia^0.1 article:trump^0.18 article:csv^0.18 article:sweden^0.18 article:queri^0.09 article:t^0.18 article:flask^0.1 article:descript^0.1 article:dictionari^0.17 article:db^0.17 article:nfl^0.17 article:numpi^0.17 article:elixir^0.17 article:4000^0.17 article:legend^0.17 article:scienc^0.17 article:score^0.17 article:presid^0.17 article:stack^0.16945 article:won^0.16 article:pend^0.16 article:scikit^0.16 article:leagu^0.16 article:industri^0.16 article:y^0.16 article:nodej^0.16 article:survei^0.16 article:sampl^0.16 article:grow^0.1 article:attornei^0.16 article:declin^0.16 article:chart^0.16 article:pandoc^0.16 article:abc^0.16 article:preprocess^0.16 article:matplotlib^0.16 article:spinner^0.16 article:season^0.16 article:rst^0.16 article:topic^0.16 article:band^0.16 article:respond^0.15 article:dplyr^0.1 article:visual^0.15 article:georg^0.15 article:dataset^0.1 article:condit^0.1 article:fastest^0.1 article:york^0.15 article:php^0.15 article:categori^0.15 article:frequenc^0.15 article:sql^0.15 article:js^0.15 article:ecosystem^0.15 article:islam^0.15 article:2016^0.15 article:mine^0.15 article:jong^0.15 article:diverg^0.15138264)",
        List("title", "article"),
        20
      )

    val coll =
      solrResults.map(
        (document) =>
          (
            document.get("title").toString,
            document.get("article").toString
          )
      ).par

    coll.tasksupport = new ForkJoinTaskSupport(
      new scala.concurrent.forkjoin.ForkJoinPool(16))

    val allDocuments =
      coll
        .map(
          (document) =>
            (
              document._1,
              NLP.getWords(document._2)
            )
        ).map(
          (tuple) => (
            tuple._1,
            tuple._2,
            model.getWordVectorsMean(tuple._2.asJava)
          )
      ).toList

    def pickNext(
      topDocuments: List[(String, List[String], INDArray)],
      remaining: List[(String, List[String], INDArray)]
    ): (Double, (String, List[String], INDArray)) = {
      val next =
        remaining.map(
          (tuple) => {
            val chosenMean =
              model.getWordVectorsMean(
                topDocuments.map(
                  _._2
                ).reduce(
                  (a: List[String], b: List[String]) => a ++ b
                ).asJava
              )

            // compare this document to the stuff we already chose
            // this technique will bring back things the most unlike the rest of the collection

            // could also compute this score and skip things that are two close
            (Transforms.cosineSim(chosenMean, tuple._3), tuple)
          }
        ).sortBy(_._1).head

      next
    }

    def recurse(
                 idx: Integer,
                 topDocuments: List[(Double, (String, List[String], INDArray))],
                 remaining: List[(String, List[String], INDArray)]
               ): List[(Double, (String, List[String], INDArray))] = {
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

    val result =
      (1.0, allDocuments.head) :: recurse(
        10,
        List((1.0, allDocuments.head)),
        allDocuments.tail
      )

    val resultString =
      result.map(
        (vec) => (vec._2._1, vec._1)
      ).toList.sortBy(
        (vec) => vec._2
      ).reverse.map(
        (vec) => vec._1 + ": " + vec._2
      ).mkString("\n")

    //val documentMeans = Nd4j.create(documentsArray, Array(documentsArray.size, 1000))
    println(resultString)
    println(new DateTime)
  }
}