/**
  * Created by gary on 5/31/2017.
  */
object TalkToMarkdown {
  def main(args: Array[String]): Unit = {
    val cmd = new Commands
    val md = new Markdown
    val integrations = new Integrations
    val subtitles = new Subtitles
    val urls = new Urls


    cmd.withTempDirectory[Iterable[LogEntry]](
      (dir) => {
        val id = urls.ytId("https://www.youtube.com/watch?v=YME2eyde38A&feature=youtu.be").get
        val url = urls.ytUrl(id)

        cmd.youtubeDL(dir)(url) // TODO map over this
        cmd.vttToSrt(dir)(id) // TODO map over this
        subtitles.all(cmd.load(dir, id.getSubtitleFile))._2 // TODO combine logs
      }
    ).map(
      (logEntry: LogEntry) => {
        println(logEntry.value)
      }
    )

  }
}
