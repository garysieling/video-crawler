package util

import java.lang.Math._
import java.text.BreakIterator
import java.util.Locale

import com.telmomenezes.jfastemd._
import org.deeplearning4j.models.word2vec.Word2Vec
import org.nd4j.linalg.api.ndarray.INDArray
import org.nd4j.linalg.factory.Nd4j
import org.nd4j.linalg.api.ops.impl.accum.distances.EuclideanDistance;

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

    words.map(
      _.toLowerCase
    )
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

  def getDistance(query: String, words: List[String], model: Word2Vec): Double = {
    // TODO cache document

    //val queryWords =  //cache2(query, () => getWords(query))
    // doing nothing - rate is 10000
    // doing just this - 19.6
  //  val wordVectorMatrix = model.getWordVectorMatrix("artificial")
    //  val wordVector = model.getWordVector("intelligence")


   // cosineSim(x[i], y[i])

        // NEXT STEP: load lucene directly
    val queryVectorsOut =
       //cache1(
       // query,
        getWords(query).map(
          (word) => (word, model.getWordVector(word))
        ).filter(
          _._2 != null
        ).map(
          (w: (String, Array[Double])) =>
            (w._1, Nd4j.create(w._2))
        )
          //)

    val weightLookupTable = model.lookupTable()

      val queryVectorsIn =
        getWords(query).map(
          (word) => (word, model.getWordVectorMatrix(word))
        ).filter(
          _._2 != null
        )

        val documentVectorsOut =
          words.map(
            (word) => (word, model.getWordVector(word))
          ).filter(
            _._2 != null
          ).map(
          (w: (String, Array[Double])) =>
            (w._1, Nd4j.create(w._2))
        )

    val documentVectorsIn =
      words.map(
        (word) => (word, model.getWordVectorMatrix(word))
      ).filter(
        _._2 != null
      )

    // these appear to already be normalized
    val queryNormIn = normalizeList(queryVectorsIn)
    val queryNormOut = normalizeList(queryVectorsOut)

    // fairly certain this is the "out" vector
    val documentNormOut = normalizeList(documentVectorsOut)
    val documentNormIn = normalizeList(documentVectorsIn)

    val documentCenterOut = centroid(documentNormOut)
    val documentCenterIn = centroid(documentNormIn)


    val DESMinoutSCORE = queryNormIn.map(
      (vec: (String, INDArray)) => vec._2
    ).map(
      (vec: INDArray) => {
        vec.mul(documentCenterOut)
      }
    ).map(
      (vec: INDArray) => vec.sumNumber()
    ).map(
      (a: Number) => a.doubleValue()
    ).reduce(
      (a: Double, b: Double) => a + b
    ) / (1.0 * queryNormIn.length)


    val DESMininSCORE = queryNormIn.map(
      (vec: (String, INDArray)) => vec._2
    ).map(
      (vec: INDArray) => {
        vec.mul(documentCenterIn)
      }
    ).map(
      (vec: INDArray) => vec.sumNumber()
    ).map(
      (a: Number) => a.doubleValue()
    ).reduce(
      (a: Double, b: Double) => a + b
    ) / (1.0 * queryNormIn.length)

    DESMinoutSCORE

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

    // group words in doc based on which term they most closely match
    /*  val distances =
        documentMatrix.map(
          (documentWord) =>
            queryMatrix.map(
              (queryWord) => (
                documentWord._1,
                queryWord._1,
                queryWord._2.distance1(documentWord._2) / df(queryWord._1)
              )
            ).sortBy(_._3).head
        )

      val result = distances.map(_._3).reduce(_ + _) / distances.size

      result*/
    /*if (documentMatrix.length > queryMatrix.size) {
      val documentWeights = words.map(getWeight).toArray

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
    }*/

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
