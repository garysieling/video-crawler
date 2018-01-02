package indexer

import org.deeplearning4j.models.word2vec.Word2Vec
import util.Semantic

/**
  * Created by gary on 10/8/2017.
  */
object Cluster {
  def main(args: Array[String]): Unit = {
    val w2v = new Semantic("D:\\projects\\clones\\pathToSaveModel1.txt")
    w2v.init

    w2v.model.map(
      (w2v: Word2Vec) => {
        import scala.collection.JavaConversions._

        w2v.getVocab.vocabWords.toList.map(
          (word) => {
            word.getWord

            //word
          }
        )
      }
    )
  }
}
