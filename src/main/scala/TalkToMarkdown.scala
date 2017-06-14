import java.util.UUID
import java.io.File

import util._

object TalkToMarkdown {
  def main(args: Array[String]): Unit = {
    import scala.collection.JavaConverters._

    val cmd = new Commands
    val md = new Markdown
    val integrations = new Integrations
    val subtitles = new Subtitles
    val urls = new Urls

    cmd.withTempDirectory[Unit](
      (dir) => {
        val id = urls.ytId("https://www.youtube.com/watch?v=YME2eyde38A&feature=youtu.be").get
        val url = urls.ytUrl(id)

        // TODO add a concept of a cacheable operation
        // TODO way to send me data (Google spreadsheets?)

        // TODO call youtube-dl -U periodically
        val ytData = cmd.youtubeDL(dir)(url) // TODO map over this

        md.title(ytData.get("fulltitle").toString)
        md.description(ytData.get("description").toString)

        val tags =
          (
            ytData.getJSONArray("categories").toList.asScala ++
            ytData.getJSONArray("tags").toList.asScala
          ).toList.map(_.toString)

        md.tags(tags)

        // duration?
        // todo: some form of caching

        // TODO sometimes the youtube download doesn't work

        cmd.retry(5, () => {
          cmd.vttToSrt(dir)(id)
        }, () => {
          val st = subtitles.all(cmd.load(dir, id.getSubtitleFile)) // TODO combine logs

          // TODO retry logic
          // on this message "WARNING: Couldn't find automatic captions for"

          // TODO:
          //   Logger that listens
          //   Logger should update the web service with progress
          //     That means there needs to be a request context
          //     You should be able to refresh the page and still get status
          //   Logger should know based on tokens how much time is left (and how much has passed)
          val firstBatch = st.take(st.size - 1).toList
          val secondBatch = st.toList.takeRight(st.size - 1).toList
          firstBatch.zip(
            secondBatch
          ).map(
            (values) => {
              val gap = values._1.getEndTime() - values._2.getStartTime()
              (gap, values._1.text, values._1.getStartTime())
            }
          ).map(
            (v: (Double, String, Double)) => {
              if (v._1 >= 2.7) {
                md.url(urls.ytUrl(id, v._3.toInt).value)
                md.text("\n")
              }
              md.text(v._2)
            }
          ).toList

          md.text(st.last.text)

          cmd.save(dir, "text.md", md.toString)
          cmd.zip(dir, id.value + ".zip", List("text.md"))

          cmd.email(
            "gary@garysieling.com",
            "gary@garysieling.com",
            "Markdown file for your talk",
            Option(
              """
                |Please find your transcript attached.
                |
                |Please also check out https://www.FindLectures.com!
              """.stripMargin),
              Option(
                """
                  |Please find your transcript attached.
                  |
                  |Please also check out <a href="https://www.findlectures.com">https://www.FindLectures.com</a>.
                """.stripMargin),
              List(new File(dir.value + "\\" + id.value + ".zip"))
          )
        })
      }
    )
  }
}
