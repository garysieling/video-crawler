package util

import java.nio.file.{Files, Paths}
import java.util.Date

import org.deeplearning4j.models.embeddings.inmemory.InMemoryLookupTable
import org.deeplearning4j.models.embeddings.loader.WordVectorSerializer
import org.deeplearning4j.models.word2vec.wordstore.inmemory.InMemoryLookupCache
import org.deeplearning4j.models.word2vec.{VocabWord, Word2Vec}
import org.deeplearning4j.text.sentenceiterator.CollectionSentenceIterator
import org.deeplearning4j.text.tokenization.tokenizer.preprocessor.CommonPreprocessor
import org.deeplearning4j.text.tokenization.tokenizerfactory.{DefaultTokenizerFactory, TokenizerFactory}

/**
  * Created by gary on 8/2/2017.
  */
class Semantic(modelPath: String) {
  var model: Option[Word2Vec] = None

  def init = {
    val token: String = (new Date).getTime + ""

    System.out.println("Trying to load old word2vec file")
    model =
      if (Files.exists(Paths.get(modelPath))) {
        Some(WordVectorSerializer.loadFullModel(modelPath))
      } else {
        val cache = new InMemoryLookupCache()

        val table = new InMemoryLookupTable.Builder[VocabWord]()
          .vectorLength(100)
          .useAdaGrad(false)
          .cache(cache)
          .lr(0.025f).build()

        val t = new DefaultTokenizerFactory()
        t.setTokenPreProcessor(new CommonPreprocessor())

        Some(
          new Word2Vec.Builder()
            .minWordFrequency(1)
            .iterations(5)
            .epochs(5)
            .layerSize(100)
            .seed(42)
            .windowSize(10)
            .tokenizerFactory(t)
            .lookupTable(table)
            .vocabCache(cache)
            .build()
        )
      }
    val tokenizerFactory: TokenizerFactory = new DefaultTokenizerFactory
    tokenizerFactory.setTokenPreProcessor(new CommonPreprocessor)

    model.get.setTokenizerFactory(tokenizerFactory)
  }

  def train(sentences: List[String]) {
    import scala.collection.JavaConversions._

    val iter = new CollectionSentenceIterator(sentences)
    model.get.setSentenceIterator(iter)
    model.get.fit()
  }

  def close(): Unit = {
    WordVectorSerializer.writeFullModel(model.get, modelPath)
  }
}
