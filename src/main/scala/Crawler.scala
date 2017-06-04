import java.io.File

import crawlercommons.robots.SimpleRobotRulesParser
import util.{Commands, Directory}

/**
  * Created by gary on 6/3/2017.
  */
object Crawler {
  def main(args: Array[String]): Unit = {
    ???
  }
}

class Crawler[T](directory: Directory) {
  val startPage: List[String] = List[String]()
  val cmd = new Commands()

  def initRobots = {
    //val txt = ""

    //val output = cmd.curl(startPage(0) + "/robots.txt")
    //val robots = new SimpleRobotRulesParser()
    //robots.parseContent(startPage(0))
  }
  def robotsOk(value: String) = true

  def run = {
    var seen = Set[String]()
    var todo = startPage

    initRobots

    var index = 0
    var results = List[T]()

    while (todo.headOption.isDefined) {
      val url = todo.head
      todo = todo.tail

      if (!seen.contains(url)) {
        seen = seen + url

        if (robotsOk(url)) {
          val filename = index + ".html"
          val pageContents = cmd.curl(directory)(url, filename)

          index = index + 1

          results = onPage(url, new File(directory.value + "\\" + filename)) :: results

          // parse the page for links...
          // add each link to the list...
          // follow link...
        }
      }
    }

    onComplete(results)
  }

  def onPage(url: String, contents: File): T = ???

  def onComplete(data: List[T]): Unit = ???

  def shouldFollowLink(url: String): Boolean = ???
}
