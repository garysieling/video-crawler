package util

import java.text.BreakIterator
import java.util.Locale

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
}
