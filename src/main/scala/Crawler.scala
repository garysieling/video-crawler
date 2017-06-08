import java.io.File

import crawlercommons.robots.SimpleRobotRulesParser
import org.json.JSONArray
import sun.misc.Regexp
import util.{Commands, Directory}

import scala.util.matching.Regex

/**
  * Created by gary on 6/3/2017.
  */
object Crawler {
  def main(args: Array[String]): Unit = {
    ???
  }
}

class Crawler[T](directory: Directory) {
  lazy val startPage: List[String] = List[String]()
  lazy val nextPage: String = ???
  lazy val dataPage: String = ???
  lazy val domain: String = ???
  lazy val maxPage: Integer = 10000

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

    val isPaged = this.nextPage.indexOf("{page}") >= 0
    var page = 1 // haven't seen 0 yet; also no one starts with page=1

    while (todo.headOption.isDefined) {
      val url = todo.head
      todo = todo.tail

      if (!seen.contains(url)) {
        seen = seen + url

        if (robotsOk(url)) {
          val filename = index + ".html"
          try {
            val pageContents = cmd.curl(directory)(url, filename)

            index = index + 1

            if (url.indexOf(dataPage) >= 0) {
              results = onPage(url, new File(directory.value + "\\" + filename)) :: results
            } else {
              // get all the links from the page and add to the lists
              import scala.collection.JavaConversions._

              val linksJava =
                new JSONArray(
                  cmd.node("links", List(directory.value + "\\" + filename, url))
                ).toList

              val linksScala =
                linksJava.map(
                  (linkVal) => {
                    println(linkVal.toString)
                    linkVal.toString
                  }
                ).distinct.filter(
                  (link) => (link.indexOf(domain) >= 0)
                ).filter(
                  (link) =>
                    (link.indexOf(dataPage) >= 0) ||
                    (link.indexOf(nextPage) >= 0)
                )

              todo = todo ++ linksScala

              if (isPaged) {
                if (page <= maxPage) {
                  page = page + 1
                  todo = (this.nextPage.replace("{page}", page + "")) :: todo
                }
              } else {
                ???
              }
            }

            // parse the page for links...

            // add each link to the list...
            // follow link...
          } catch {
            case e: Exception => {
              e.printStackTrace()
              // paging errors are ok, just keep going
              // what to do if no error?
            }
          }
        }
      }
    }

    onComplete(results)
  }

  def onPage(url: String, contents: File): T = ???

  def onComplete(data: List[T]): Unit = ???

  def shouldFollowLink(url: String): Boolean = ???
}
