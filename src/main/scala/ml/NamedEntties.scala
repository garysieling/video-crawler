package ml

import java.io.FileInputStream

import opennlp.tools.namefind.{NameFinderME, TokenNameFinderModel}
import util.Commands
import opennlp.tools.tokenize.{TokenizerME, TokenizerModel}

object NamedEntities {
  def main(args: Array[String]): Unit = {
    val dir = "D:\\Software\\apache-opennlp-1.8.0\\models\\"
    val inputStreamTokenizer = new
        FileInputStream(dir + "en-token.bin")
    val tokenModel = new TokenizerModel(inputStreamTokenizer)
    val tokenizer = new TokenizerME(tokenModel)
    val sentence = "Gary Sieling is senior programming manager and Rama is a clerk both are working at Tutorialspoint"
    val tokens = tokenizer.tokenize(sentence)

    val inputStreamNameFinder = new
        FileInputStream(dir + "en-ner-person.bin")
    val model = new TokenNameFinderModel(inputStreamNameFinder)
    val nameFinder = new NameFinderME(model)
    val nameSpans = nameFinder.find(tokens)

    for (
      s <- nameSpans;
      //x <- Option(println(s.getType))
      //y <- Option(println(s.toString))
      y <-
        (s.getStart to s.getEnd - 1).map(
          (i) => {
            println(
              tokens(i)
            )
          })
    ) yield s

    /*
      //Loading the tokenizer model

         //Instantiating the TokenizerME class

         //Tokenizing the sentence in to a string array

         //Loading the NER-person model

         //Instantiating the NameFinderME class

         //Finding the names in the sentence

         //Printing the names and their spans in a sentence
     */
  }
}
/**
  * Created by gary on 6/15/2017.
  */
class NamedEntties {

}
