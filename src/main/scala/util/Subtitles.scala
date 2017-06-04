package util

import scala.util.matching.Regex

/**
  * Created by gary on 5/31/2017.
  */
class Entry {
  var startTime: String = ""
  var endTime: String = ""

  var text: String = ""

  def getTime(value: String): Double = {
    val parts = value.trim.split(":")
    if (parts(0) == "") {
      throw new Exception("missing time")
    }

    val nums = List(parts(0).toInt, parts(1).toInt)
    val last = parts(2).split(",").map(_.toInt)

    nums(0) * 3600 + nums(1) * 60 + last(0) + (last(1) / 1000.0)
  }

  def getStartTime(): Double = getTime(startTime)
  def getEndTime(): Double = getTime(endTime)
}

//type Checker = (String) => Boolean

class Subtitles {
  def all(text: Iterator[String]): Iterable[Entry] = {
    val line0 = new Regex("""^\d+$""")
    val line1 = new Regex("""^\d+:\d+:\d+,\d+ --> \d+:\d+:\d+,\d+$""")
    var seen = Map[String, Boolean]()

    val states = Array("line0", "line1", "text")
    val processors = Array(
      (x: String) => null,
      (x: String) => x,
      (x: String) => x
    )

    val nexts = Array(
      (x: String) => line0.findFirstMatchIn(x).isDefined,
      (x: String) => line1.findFirstMatchIn(x).isDefined,
      (x: String) => (x == "")
    )

    val skips = Seq[(String) => Boolean](
      (x: String) => false,
      (x: String) => false,
      (x: String) => {
        val res = seen.get(x)
        seen = seen + (x -> true)
        res.isDefined
      })

    val transitions = Array(1, 2, 0)

    var idx = 0
    var stateIdx = 0

    var results: List[Entry] = List[Entry]()
    var thisRow: Entry = new Entry

    while (text.hasNext) {
      val line = text.next.trim
      val thisLineResult = processors(stateIdx)(line)
      //log(stateIdx);
      //log(thisLineResult);
      if (thisLineResult != null && thisLineResult != "") {
        val lambda: (String) => Boolean = skips.apply(stateIdx)
        val result: Boolean = lambda(thisLineResult)

        if (!result) {
          //thisRow = thisRow ++ thisLineResult
          if (stateIdx == 1) {
            val times = line.split(" --\\> ")

            thisRow.startTime = times(0) // todo can there be multiple lines of text in an interval?
            thisRow.endTime = times(1)
          } else if (stateIdx == 2) {
            thisRow.text = line

          }
        }
      }

      if (nexts(stateIdx)(line)) {
        stateIdx = transitions(stateIdx)
        if (stateIdx == 0) {
          //val allText = thisRow.mkString(" ")

          /*if (!line1.findFirstMatchIn(allText).isDefined) {
            result = result + Entry("", allText)
          }*/
          results = results ++ List(thisRow)
          thisRow = new Entry()
        }
      }
    }

    if (thisRow.text != "") {
      results = results ++ List(thisRow)
    }

    //return result;
    results

    /*(text.map((line) => {
      println(line)
      Entry("", line)
    }).toList,
      Seq(LogEntry("")))*/
  }
}
