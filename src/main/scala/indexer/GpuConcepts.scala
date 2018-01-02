package indexer


import com.hazelcast.config.Config
import com.hazelcast.core.Hazelcast
import indexer.emails.Concepts
import org.nd4s.Implicits._
import util.NLP

object GpuConcepts {

  val cfg = new Config("concepts")
  val hazelcastInstance = Hazelcast.newHazelcastInstance(cfg)
  val concepts = new Concepts(hazelcastInstance)
  val widthOfWordVector: Int = concepts.model.getLayerSize

  def main(args: Array[String]): Unit = {
    val sentences: List[Map[String, Int]] = List(
      "See the spot run up the hill spot fetched a pail of the water. test test test.",
      "a pig ran the test.",
      "the cat is really the fastest hill test"
    ).map(
      (sentence) => {
        val tokens = new NLP(hazelcastInstance).getTokens(sentence)

        tokens
          .filter(
            concepts.model.hasWord
          )
          .groupBy(
            (v: String) => v
          )
          .mapValues(_.size)
      }
    )

    val df: Map[String, Int] =
      sentences.reduce(
        (a: Map[String, Int], b: Map[String, Int]) => {
          (a.keySet ++ b.keySet).map(
            (key) => (key -> (a.getOrElse(key, 0) + b.getOrElse(key, 0)))
          ).toMap
        }
      )

    val wordVectors =
      df.keySet.map(
        (key) =>
          (key -> concepts.model.getWordVector(key))
      ).toMap

    println(
      sentences.map(
        (sentence: Map[String, Int]) => score(sentence, df, wordVectors)
      )
    )

    hazelcastInstance.shutdown()
  }

  def score(termFrequencies: Map[String, Int], documentFrequencies: Map[String, Int], wordVectors: Map[String, Array[Double]]) {
    println(termFrequencies)
    println(documentFrequencies)
    println(wordVectors)

    val numWords: Int = termFrequencies.size
    val modes: Int = 3 // TF, IDF, concept
    val max: Int = numWords * widthOfWordVector * modes

    val words = termFrequencies.keySet.toList
    val word0 = words.head

    val data: Seq[Double] =
      Seq(
        words.flatMap(
          (w) => wordVectors(w)
        ),
        words.flatMap(
          (w) => Seq.iterate(1, widthOfWordVector)((idx: Int) => termFrequencies(w)).map(
            (vv: Int) => vv.toDouble
          )
        ),
        words.flatMap(
          (w) => Seq.iterate(1, widthOfWordVector)((idx: Int) => documentFrequencies(w)).map(
            (vv: Int) => vv.toDouble
          )
        )
      ).flatten

    val arr = data.toNDArray

    val wordIdx = 0

    println(arr)
    val modeVectors = arr.reshape(modes, widthOfWordVector * numWords)
    println(arr)
    val scores = modeVectors(0->1)
    val tf = modeVectors(1->2)
    val df = modeVectors(2->3)
    println("scores: " + scores)
    println("tf: " + tf)
    println("df: " + df)

    val weighted = scores * tf / df
    println("weighted: " + weighted)

    val wordVects = weighted.reshape(numWords, widthOfWordVector)
    println(wordVects)

    // this is the weighted everage
    println(wordVects.sum(0) / numWords)

    // TODO:
    //  - transform words -> these vector things
    //  - work on a actual document with repeat words
    //  - bigger dataset
    //      how big does this have to be to crash?
    //      how many cores does the card make (or warps or whatever)
    //      performance CPU vs GPU


    //println(arr(0->1))
    //println(arr(0->1, 0->widthOfWordVector, 0->1))
    /*[wordvec] * [number of terms] / [df]
    channel1  * channel2          / channel3
    split this into widths, add all, div by N

    val sub1 = arr(
      wordIdx->(wordIdx + 1),
      0->1,
      0->widthOfWordVector
    )

    val sub2 = arr(
      wordIdx->(wordIdx + 1),
      1->2,
      0->widthOfWordVector
    )

    val sub3 = arr(
      wordIdx->(wordIdx + 1),
      2->3,
      0->widthOfWordVector
    )

    println(sub1, sub2, sub3)

    val sub4 = sub1 * sub2 / sub3
    println(sub4)*/

    // TODO can you just average vectors this way???


    // what is this test
    // create a map of words to 1000d vectors
    // create a list of words in text
    // create a list of word counts (df)
    // create weights (1000d * weight)
    // sum(weight * vector) / number(vectors)
    // TODO: timing of this with TF/IDF
    // TODO: timing of this without TF/IDF
    // TODO: timing with options as nulls
    // TODO: timing of this with some other approach(0s?)
    // TODO: timings on my GPU vs CPU, maybe Greg's or one on some Amazon server
  }
}
