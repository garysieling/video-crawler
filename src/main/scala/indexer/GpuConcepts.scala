package indexer


import com.hazelcast.config.Config
import com.hazelcast.core.Hazelcast
import indexer.emails.Concepts
import org.nd4j.linalg.api.ndarray.INDArray
import org.nd4s.Implicits._
import util.NLP

import scala.collection.Iterable

object GpuConcepts {

  val cfg = new Config("concepts")
  val hazelcastInstance = Hazelcast.newHazelcastInstance(cfg)
  // val concepts = new Concepts(hazelcastInstance)
  val widthOfWordVector: Int = 1000 //concepts.model.getLayerSize

  def time[R](name: String, block: => R): R = {
    val t0 = System.nanoTime()
    val result = block    // call-by-name
    val t1 = System.nanoTime()
    println("Elapsed time: " + (t1 - t0) / 1000000000.0 + "s (" + name + ")")
    result
  }

  def main(args: Array[String]): Unit = {
    // TODO: query GPU memory size
    // TODO: split this map up into chunks, to fit the RAM available

    println(
      time("total", exec)
    )
  }

  def exec(): Unit = {
    val solr = new Solr(hazelcastInstance)
    val documentsSolr: List[(String, String, Float)] =
      time(
        "solr",
        solr.list(
          "talks",
          """auto_transcript_txt_en:"machine learning" OR auto_transcript_txt_en:"python"""",
          List("score", "title_s", "auto_transcript_txt_en"),
          100
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
        ).toList
      )

    val sentences: List[Map[String, Int]] =
      time(
        "tokenize",
        documentsSolr.map(
        _._2
      ).map(
        (sentence) => {
          val tokens = new NLP(hazelcastInstance).getTokens(sentence)

          tokens
            .filter(
              (x: String) => true
              //concepts.model.hasWord
            )
            .groupBy(
              (v: String) => v
            )
            .mapValues(_.size)
        }
      ).toList
    )

    val df: Map[String, Int] =
      time(
        "df",
        sentences.reduce(
          (a: Map[String, Int], b: Map[String, Int]) => {
            (a.keySet ++ b.keySet).map(
              (key) => (key -> (a.getOrElse(key, 0) + b.getOrElse(key, 0)))
            ).toMap
          }
        ).par.seq
      )

    val wordVectors =
      time(
        "vectors",
        df.keySet.map(
          (key) =>
            (key ->
              Seq.iterate(1, widthOfWordVector)((idx: Int) => 1).map(
                (i: Int) => Math.random()
              )
              //concepts.model.getWordVector(key)
            )
        ).toMap
      )

    time(
      "average",
      sentences.map(
        (sentence: Map[String, Int]) => score(sentence, df, wordVectors)
      )
    )

    hazelcastInstance.shutdown()
  }

  def score(termFrequencies: Map[String, Int], documentFrequencies: Map[String, Int], wordVectors: Map[String, Iterable[Double]]): INDArray = {
    val numWords: Int = termFrequencies.size
    val modes: Int = 3 // TF, IDF, concept
    val max: Int = numWords * widthOfWordVector * modes

    val words = termFrequencies.keySet

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

    val arr =
      time(
        "data construction",
        data.toNDArray
      )

    time("math",
      {
        val modeVectors = arr.reshape(modes, widthOfWordVector * numWords)
        val scores = modeVectors(0 -> 1)
        val tf = modeVectors(1 -> 2)
        val df = modeVectors(2 -> 3)

        val weighted = scores * tf / df

        val wordVects = weighted.reshape(numWords, widthOfWordVector)
        // this is the weighted everage

        wordVects.sum(0) / numWords
      })

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
