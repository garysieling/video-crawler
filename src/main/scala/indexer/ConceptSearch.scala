package indexer

import java.io._
import java.util.Date

import org.json.JSONObject
import util.{NLP, Semantic}

object ConceptSearch {
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

    // TODO:
    //   Cache word2vec model
    //   Update model from reddit articles eventually
    //   Fill in entity names from DBPedia
    //   Merge these to hadoop/spark for better RAM

    /*val folderPath: String = "C:\\projects\\image-annotation\\data\\talks\\json\\1"

    val folder: File = new File(folderPath)
    val fileList: Array[File] = folder.listFiles(new FilenameFilter() {
      def accept(dir: File, name: String): Boolean = {
        return name.endsWith(".json")
      }
    })*/

    //val work = fileList

    val w2v = new Semantic("D:\\projects\\clones\\pathToSaveModel1.txt")
    w2v.init


    var top = scala.collection.mutable.MutableList[(Double, String)]()

    // Initial Rate:                2.6738253680177455
    // Remove extra adding/sorting: 3.2629016381916616
    // Less printing:               3.24658085277554
    // Even less printing:          3.1875440012363274
    // Try changing caching:        2.867716121856269
    // Don't count word2vec loading 2.963773669253121
    // Move cache a little bit      2.935605281885032
    // Switch to nd4j               2.7937422869698705
    // Another change to caching    2.8428153570498784
    // Slight delay in computations 2.876336421952958

    var toBeat = 1000000000.0

    val solr = new Solr(null)
    val documentsSolr: List[(String, String, Float)] =
        solr.list(
          "talks",
          """auto_transcript_txt_en:"machine learning" OR auto_transcript_txt_en:"python"""",
          List("score", "title_s", "auto_transcript_txt_en"),
          10
        ).filter(
          _ != null
        ).filter(
          doc =>
            doc.get("title_s") != null &&
            doc.get("auto_transcript_txt_en") != null
        ).map(
      (doc) => (
        doc.get("title_s").toString,
        doc.get("auto_transcript_txt_en").toString,
        doc.get("score").asInstanceOf[Float]
      )
    )

    val model = w2v.model.getOrElse(???)

    /*val loadedData =
      fileList.take(10000).map(
        (file) => {
          val fileData = getFile(file)
          fileData
        }).toList*/

    val startTime = new Date
    println(startTime)

    val nlp = new NLP(null)
    val distance = nlp.getDocumentDistance(
      "machine learning, python", model)

    val alpha = 0.20

    val documents = documentsSolr.map(
      (document) =>
        (document._1, nlp.getWords(document._1) ++ nlp.getWords(document._2.substring(0, Math.min(document._2.length, 1000))), document._3)
    ).map(
      (fileData) => {
        //println("Testing: " + fileData)

        val newScore = distance(
          fileData._2
        )

        (
          fileData._1,
          alpha * newScore + (1 - alpha) * fileData._3,
          newScore,
          fileData._3
        )
      }
    ).sortBy(_._2).take(100).map(println)

    val endTime = new Date
    println("")
    //println("Rate: " + fileList.size * 1.0 / (endTime.getTime - startTime.getTime))

  }
}