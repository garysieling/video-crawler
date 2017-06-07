import java.io.File
import java.util.UUID

import org.json.JSONObject
import util.{Commands, Directory}

object MetadataCrawler {
  def main(args: Array[String]): Unit = {
    val cmd = new Commands
    cmd.withTempDirectory(
      new MetadataCrawler(_, "heavybit").run
    )
  }
}

class MetadataCrawler(directory: Directory, crawler: String) extends Crawler[Map[String, AnyRef]](directory) {
  def getConfig = {
    val cmd = new Commands
    new JSONObject(
      cmd.node("config.js " + crawler)
    )
  }
  override val startPage = List(
    getConfig.getString("start")
  )

  override def onPage(url: String, value: File): Map[String, AnyRef] = {
    val cmd = new Commands
    cmd.node("metadata.js " + crawler + " " + value.toPath)

    // todo youtube
    ???
  }

  override def onComplete(values: List[Map[String, AnyRef]]): Unit = {
    // TODO save JSON?
    // TODO upsert?

    ???
  }
}
