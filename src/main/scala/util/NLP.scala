package util

import java.io.StringReader
import java.lang.Math._
import java.text.BreakIterator
import java.util
import java.util.Locale

import com.hazelcast.core.HazelcastInstance
import com.telmomenezes.jfastemd._
import org.deeplearning4j.models.word2vec.Word2Vec
import org.nd4j.linalg.api.ndarray.INDArray
import org.nd4j.linalg.factory.Nd4j
import org.apache.lucene.analysis.Analyzer
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute
import org.apache.lucene.analysis.standard._
import org.apache.lucene.analysis.TokenStream

import scala.util.matching.Regex

/**
  * Created by gary on 8/3/2017.
  */
class NLP(instance: HazelcastInstance) {
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
  def getTokens(text: String): List[String] = {
    val result = new util.ArrayList[String]()
    val analyzer: Analyzer = new StandardAnalyzer()

    val stream: TokenStream = analyzer.tokenStream(null, new StringReader(text))
    stream.reset()

    while (stream.incrementToken) {
      result.add(stream.getAttribute(classOf[CharTermAttribute]).toString())
    }

    import scala.collection.JavaConversions._
    result.toList
  }

  def replaceEntities(text: String): String = {
    val entities = List(
      List("functional_programming", "Functional programming"),
      List("orm", "Object Relational Mapping"),
      List("ar","augmented reality"),
      List("vr","virtual reality"),
      List("ml","machine learning"),
      List("iot","internet of things"),
      List("ai","artificial intelligence"),
      List("computer_science", "computer science"),
      List("recommender_systems", "recommender systems"),
      List("recommendation_engines", "recommendation engines"),
      List("distributed_systems", "distributed systems"),
      List("autonomous_vehicles", "autonomous vehicles"),
      List("graphic_design", "graphic design"),
      List("distributed_systems", "distributed systems"),
      List("nlp","natural language processing"),
      List("deep_learning", "deep learning"),
      List("data_science", "data science"),
      List("reverse_engineering", "reverse engineering"),
      List("operating_systems", "operating systems","os"),
      List("nodejs", "node js", "node.js"),
      List("crm", "customer relationship management"),
      List("natural_language", "natural language")
    )

    entities.map(
      (entityMapper) => {
        val preferred = entityMapper.head
        val rest = entityMapper.tail

        rest.map(
          (v: String) =>
            (target: String) => new Regex("\\b" + v + "\\b", "i").replaceAllIn(target, preferred)
        )
      }
    ).flatten.reduce(
      (a, b) =>
        (target: String) => a(b(target))
    )(text)
  }

  var getWordsCache =
    instance.getMap[String, List[String]]("getWordsCache")


  def getWords(text: String): List[String] = {
    if (getWordsCache.containsKey(text)) {
      getWordsCache.get(text)
    } else {
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

      val result = words.map(
        _.toLowerCase
      )

      getWordsCache.put(text, result)

      result
    }
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

  def normalizeList(vectors: List[(String, INDArray)]) = {
    vectors.map(
      (value: (String, INDArray)) => {
        (value._1, normalize(value._2))
      }
    )
  }



  def normalize(vector: INDArray) = {
    val vec2: INDArray = vector.mul(vector)
    val total = vec2.sumNumber()

    vector.div(sqrt(total.doubleValue()))
  }

  def centroid(vectors: List[(String, INDArray)]) = {
    val vectorsOnly =
      vectors.map(
        (v: (String, INDArray)) => v._2
      )

    val length: Double = vectorsOnly.length
    vectorsOnly.reduce(
      (a: INDArray, b: INDArray) => a.add(b)
    ).div(
      length
    )
  }

  def consineSimilarity(a: INDArray, b: INDArray): Double = {
    a.mul(b).sumNumber().doubleValue()
  }

  def getDocumentDistance(query: String, model: Word2Vec): (List[String]) => Double = {

    val queryMatrix: List[(String, Feature)] =
      getWords(query).map(
        (word) => (word, model.getWordVector(word))
      ).filter(
        _._2 != null
      ).map(
        (w: (String, Array[Double])) =>
          (w._1, new FeatureND(Nd4j.create(w._2)))
      )
    /*(List[String]) => Double = {
    // todo is this the right one?
    val weightLookupTable = model.lookupTable ()

    val queryVectorsOut =
      getWords (query).map (
        (word) => (word, model.getWordVector (word) )
      ).filter (
        _._2 != null
      ).map (
        (w: (String, Array[Double] ) ) =>
          (w._1, Nd4j.create (w._2) )
      )

    val queryVectorsIn =
      getWords (query).map (
        (word) => (word, model.getWordVectorMatrix (word) )
      ).filter (
        _._2 != null
      )

    // these appear to already be normalized
    val queryNormIn = normalizeList (queryVectorsIn)
    val queryNormOut = normalizeList (queryVectorsOut)*/

    def getDistance(words: List[String]): Double = {
      val documentMatrix: List[(String, Feature)] =
        words.map(
          (word) => (word, model.getWordVector(word))
        ).filter(
          _._2 != null
        ).map(
          (w: (String, Array[Double])) =>
            (w._1, new FeatureND(Nd4j.create(w._2)))
        )

      // group words in doc based on which term they most closely match
      val distances =
        documentMatrix.map(
          (documentWord) =>
            queryMatrix.map(
              (queryWord) => (
                documentWord._1,
                queryWord._1,
                queryWord._2.groundDist(documentWord._2) // df(queryWord._1)//
                )
            ).sortBy(_._3).head
        )

      if (distances.length == 0) {
        -1
      } else {
        val result = distances.map(_._3).reduce(_ + _) / distances.size

        if (documentMatrix.length > queryMatrix.size) {
          val documentWeights = words.map(getWeight).toArray

          // TODO
          val queryWeights = queryMatrix.map(_ => 1.0).toArray
          //queryMatrix.map(getWeight(_.getWord)).toArray

          import scala.collection.JavaConversions._

          val s1 = new Signature()
          s1.setNumberOfFeatures(queryMatrix.size)
          s1.setFeatures(queryMatrix.map(_._2).toArray)
          s1.setWeights(queryWeights)

          val s2 = new Signature()
          s2.setNumberOfFeatures(documentMatrix.size)
          s2.setFeatures(
            documentMatrix.map(
              _._2
            ).toArray)

          s2.setWeights(documentWeights)

          10 / (
            1 + JFastEMD.distance(
              s1,
              s2,
              0 // TODo
            )
            )
        } else {
          0
        }
      }
    }

    getDistance
  }
}



// TODO cache document

//val queryWords =  //cache2(query, () => getWords(query))
// doing nothing - rate is 10000
// doing just this - 19.6
//  val wordVectorMatrix = model.getWordVectorMatrix("artificial")
//  val wordVector = model.getWordVector("intelligence")


// cosineSim(x[i], y[i])

// NEXT STEP: load lucene directly
//)

/* val documentVectorsOut =
   words.map (
     (word) => (word, model.getWordVector (word) )
   ).filter (
     _._2 != null
   ).map (
     (w: (String, Array[Double] ) ) =>
       (w._1, Nd4j.create (w._2) )
   )

 val documentVectorsIn =
   words.map (
     (word) => (word, model.getWordVectorMatrix (word) )
   ).filter (
     _._2 != null
   )


 // fairly certain this is the "out" vector
 val documentNormOut = normalizeList (documentVectorsOut)
 val documentNormIn = normalizeList (documentVectorsIn)

 val documentCenterOut = centroid (documentNormOut)
 val documentCenterIn = centroid (documentNormIn)


 val DESMinoutSCORE =
 queryNormIn.map (
   (document) => consineSimilarity (document._2, documentCenterOut)
 ).reduce (
   (a, b) => a + b
 ) / (1.0 * queryNormIn.size)

 val DESMininSCORE =
   queryNormIn.map (
     (document) => consineSimilarity (document._2, documentCenterIn)
   ).reduce (
     (a, b) => a + b
   ) / (1.0 * queryNormIn.size)


 DESMinoutSCORE
}

getDistance*/
// TESTs
// I must not be getting the right "in" vectors, because the two measuers are the same

/*

DESM ( Q, D ) =
   1 / | Q | *
     sum(
      qi in Q
      qiT * D / || qi || || D ||
     )

D = 1 / | D | * sum (
  d in D
  dj / || dj ||

)

Final Rank

MM(Q, D) = αDESM(Q, D) + (1 − α)BM25(Q, D)
α ∈ R, 0 ≤ α ≤ 1
(
 */

//0

/*   val df = Map(
     "intelligence" -> 110,
     "machine" -> 316,
     "python" -> 197,
     "scala" -> 21,
     "artificial" -> 60,
     "learning" -> 575
   )

     0*/

//println(title)
//println(distance)

// delete training data?
// normalize vector lengths
// get word2vec output for all words
// convert word2vec vectors into "Signature"
// run jFastEmD
// print output
//println("completed distance work")