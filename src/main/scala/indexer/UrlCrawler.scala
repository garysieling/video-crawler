package indexer

import java.io.File

import util.{Commands, Directory}

/**
  * Created by gary on 6/4/2017.
  */
object UrlCrawler {
  def main(args: Array[String]) = {
    val cmd = new Commands
    cmd.withTempDirectory(
      new UrlCrawler(_).run
    )

  }
}

class UrlCrawler(directory: Directory) extends Crawler[Map[String, AnyRef]](directory) {
  override lazy val startPage = List("http://www.greatblackspeakers.com/author/dlhughley/")

  override def onPage(url: String, contents: File): Map[String, AnyRef] = {
    println(url)
    println(contents)

    Map[String, AnyRef]()
  }

  override def onComplete(data: List[Map[String, AnyRef]]) = {
    println("Complete!")
  }
}
