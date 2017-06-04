import java.io.File
import java.util.UUID

import util.Directory

object EntityCrawler {
  def main(args: Array[String]): Unit = {
    ???
  }
}

class EntityCrawler(directory: Directory) extends Crawler[List[String]](directory) {
  override def onPage(url: String, pageContents: File): List[String] = {
    // todo extract all the text
    // todo extract the entities
    // todo verify against openlibrary

    ???
  }

  override def onComplete(values: List[List[String]]): Unit = {
    // TODO save JSON?
    // TODO upsert?

    ???
  }
}
