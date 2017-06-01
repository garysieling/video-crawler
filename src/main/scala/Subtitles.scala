/**
  * Created by gary on 5/31/2017.
  */
case class Entry(time: String, text: String)

class Subtitles {
  def all(text: Iterator[String]): (Iterable[Entry], Iterable[LogEntry]) = {
    (text.map((line) => {
      println(line)
      Entry("", line)
    }).toList,
      Seq(LogEntry("")))
  }
}
