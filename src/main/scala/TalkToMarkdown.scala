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

    urls.ytUrl(
      urls.ytId("https://www.youtube.com/watch?v=YME2eyde38A&feature=youtu.be")
    ) map (
      cmd.youtubeDL
    )

  }
}
