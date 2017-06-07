import java.io.File
import java.util.UUID

import org.json.JSONObject
import util.{Commands, Directory}

object MetadataCrawler {
  def main(args: Array[String]): Unit = {
    val cmd = new Commands
    cmd.withTempDirectory(
      new MetadataCrawler(_).run
    )
  }
}

class MetadataCrawler(directory: Directory) extends Crawler[Map[String, AnyRef]](directory) {
  def getConfig = {
    val cmd = new Commands
    new JSONObject(
      cmd.node("D:\\projects\\scala-indexer\\src\\main\\resources\\", "config.js heavybit")
    )
  }
  override val startPage = List(
    getConfig.getString("start")
  )

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
