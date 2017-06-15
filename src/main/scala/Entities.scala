import java.io.File

import util._

object Entities {
  def main(args: Array[String]): Unit = {
    import scala.collection.JavaConverters._

    val cmd = new Commands
    val md = new Markdown
    val integrations = new Integrations
    val subtitles = new Subtitles
    val urls = new Urls

    cmd.withTempDirectory[Unit](
      (dir) => {
        val url = "http://nautil.us/issue/49/the-absurd/the-impossible-mathematics-of-the-real-world"
        cmd.curl(dir)(url, "nautilus.html")

        val text = cmd.text(dir, "nautilus.html")
        println(text)
      }
    )
  }
}
