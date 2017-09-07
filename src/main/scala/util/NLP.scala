package util

import java.text.BreakIterator
import java.util.Locale

import com.telmomenezes.jfastemd._
import org.deeplearning4j.models.word2vec.Word2Vec
import org.nd4j.linalg.api.ndarray.INDArray
import org.nd4j.linalg.factory.Nd4j

/**
  * Created by gary on 8/3/2017.
  */
object NLP {
  def cleanText(text: String): String = {
    text.split("\n").map(
      (line) => line.trim
      // TODO remove links
    ).filter(
      (line) => {
        line.length > 50 &&
        line.split(" ").size > 5
      }
    ).mkString("\n")
  }

  // TODO option to resolve DBPedia entities here
  def getSentences(text: String): List[String] = {
    var sentences: List[String] = List()

    val iterator: BreakIterator = BreakIterator.getSentenceInstance(Locale.US)
    val source: String = text
    iterator.setText(source)

    var start: Int = iterator.first

    val sb: StringBuffer = new StringBuffer

    var end: Int = iterator.next
    while (end != BreakIterator.DONE) {
      val sentence: String = source.substring(start, end)
      sentences = sentences ++ List(sentence)

      start = end
      end = iterator.next
    }

    sentences
  }

  def getWords(text: String): List[String] = {
    var words: List[String] = List()

    val iterator: BreakIterator = BreakIterator.getWordInstance(Locale.US)
    val source: String = text
    iterator.setText(source)

    var start: Int = iterator.first

    val sb: StringBuffer = new StringBuffer

    var end: Int = iterator.next
    while (end != BreakIterator.DONE) {
      val sentence: String = source.substring(start, end)
      words = words ++ List(sentence)

      start = end
      end = iterator.next
    }

    words
  }

  var queryCache1 = Map[String, List[FeatureND]]()
  def cache1(query: String, ds: () => List[FeatureND]): List[Feature] = {
    if (queryCache1.keySet.contains(query)) {
      queryCache1(query)
    } else {
      val result = ds()
      queryCache1 = queryCache1 + (query -> result)

      result
    }
  }

  var queryCache2 = Map[String, List[String]]()
  def cache2(query: String, ds: () => List[String]): List[String]  = {
    if (queryCache2.keySet.contains(query)) {
      queryCache2(query)
    } else {
      val result = ds()
      queryCache2 = queryCache2 + (query -> result)

      result
    }
  }

  def getWeight(term: String): Double = {
    term match {
        // ideally tfidf here
      case "artificial" => 5
      case "intelligence" => 5
      case "machine" => 3
      case "learning" => 5
      case "python" => 2
      case _ => 1.0
    }
  }

  def getDistance(query: String, title: String, document: String, model: Word2Vec): Option[Double] = {
    // TODO cache document

    //val queryWords =  //cache2(query, () => getWords(query))
    // doing nothing - rate is 10000
    // doing just this - 19.6

    // NEXT STEP: load lucene directly
    val allWords = getWords(document)

    val queryMatrix: List[Feature] =
      cache1(
        query,
        () => getWords(query).map(
          model.getWordVector(_)
        ).filter(
          _ != null
        ).map(
          (w: Array[Double]) =>
            new FeatureND(Nd4j.create(w))
        )
      )

    val documentMatrix =
      allWords.map(
        model.getWordVector(_)
      ).filter(
        _ != null
      )

    if (documentMatrix.length > queryMatrix.size) {
      val documentWeights = allWords.map(getWeight).toArray

      // TODO
      val queryWeights = queryMatrix.map( _ => 1.0 ).toArray
      //queryMatrix.map(getWeight(_.getWord)).toArray

      import scala.collection.JavaConversions._

      val s1 = new Signature()
      s1.setNumberOfFeatures(queryMatrix.size)
      s1.setFeatures(queryMatrix.toArray)
      s1.setWeights(queryWeights)

      val s2 = new Signature()
      s2.setNumberOfFeatures(documentMatrix.size)
      s2.setFeatures(documentMatrix.map(
        (w: Array[Double]) => {
          new FeatureND(Nd4j.create(w))
        }
      ).toArray)
      s2.setWeights(documentWeights)

      Some(
        JFastEMD.distance(
          s1,
          s2,
          0 // TODo
        )
      )

    } else {
      None
    }

    //println(title)
    //println(distance)

    // delete training data?
    // normalize vector lengths
    // get word2vec output for all words
    // convert word2vec vectors into "Signature"
    // run jFastEmD
    // print output
    //println("completed distance work")
  }
}
