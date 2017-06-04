import java.io.File
import java.util.UUID

import util.Directory

object MetadataCrawler {
  def main(args: Array[String]): Unit = {

  }
}

class MetadataCrawler(directory: Directory) extends Crawler[Map[String, AnyRef]](directory) {
  override def onPage(url: String, value: File): Map[String, AnyRef] = {
    // todo cheerio
    // todo youtube
    ???
  }

  override def onComplete(values: List[Map[String, AnyRef]]): Unit = {
    // TODO save JSON?
    // TODO upsert?

    ???
  }
}
