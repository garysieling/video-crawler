package util

import java.io.{BufferedReader, File, FileReader}
import java.net.URI
import java.nio.file.{FileSystems, Files, Paths}

import scala.io.Source

/**
  * Created by gary on 5/31/2017.
  */
case class Directory(value: String)
case class LogEntry(value: String)

class Commands {
  val QUOTE =
    if (System.getProperty("os.name") == "Windows") {
      "\""
    } else {
      ""
    }

  val FFMPEG =
    sys.env.get("FFMPEG") match {
      case Some(x: String) => x
      case None => "ffmpeg"
    }

  val NODE =
    sys.env.get("NODE") match {
      case Some(x: String) => x
      case None => "node"
    }

  val YOUTUBE_DL =
    sys.env.get("YOUTUBE_DL") match {
      case Some(x: String) => x
      case None => "youtube-dl"
    }

  val postmarkKey =
    sys.env.get("POSTMARK_API_KEY") match {
      case Some(x: String) => x
      case None => ""
    }

  val POSTMARK_BCC =
    sys.env.get("POSTMARK_BCC") match {
      case Some(x: String) => x
      case None => ""
    }

  val POSTMARK_REPLY_TO =
    sys.env.get("POSTMARK_REPLY_TO") match {
      case Some(x: String) => x
      case None => ""
    }

  val CURL =
    sys.env.get("CURL") match {
      case Some(x: String) => x
      case None => "curl"
    }

  val RESOURCES =
    sys.env.get("RESOURCES") match {
      case Some(x: String) => x
      case None => "D:\\projects\\scala-indexer\\src\\main\\resources\\"
    }


  val slash = java.io.File.separator
  val quote = slash match {
    case "/" => ""
    case "\\" => "\""
  }

  def command(cmd: String): Iterable[LogEntry] = {
    import scala.sys.process._

    println(cmd) // TODO return log entry for this

    val output = cmd.!! // TODO errors should not throw here

    output.split("\n").map(LogEntry)
  }

  def retry(n: Int, fn: () => Unit, fn2: () => Unit): Unit = {
    try {
      fn()

      fn2()
    } catch {
      case e =>
        if (n > 1) retry(n - 1, fn, fn2)
        else throw e
    }
  }

  def node(cmd: String, args: List[String] = List()): String = {
    println(cmd)
    import sys.process._

    val entireCommand =
      s"${NODE} " + cmd + " " + args.mkString(" ")

    println(entireCommand)

    // TODO configurable
    try {
      val output = Process(
        entireCommand,
        new File(RESOURCES)
      ).!! // TODO errors should not throw here

      //println(output)

      output
    }  catch {
      case (e: Exception) => {
        e.printStackTrace()

        ""
      }
    }
  }

  def vttToSrt(dir: Directory)(id: YtId): Iterable[LogEntry] = {

    val srt = dir.value + slash + "v" + id.value + ".srt"
    val vtt = dir.value + slash + "v" + id.value + ".en.vtt"

    val subtitlecmd =
      quote + FFMPEG + quote + " -i " + quote + vtt + quote + " " + quote + srt + quote

    if (!Files.exists(Paths.get(srt))) {
      command(subtitlecmd)
    } else {
      List()
    }
  }

  def canEmbed(id: YtId): Boolean = {
    ???
  }

  def withTempDirectory[T](cb: (Directory) => T): T = {
    cb(Directory(Files.createTempDirectory("indexer").toAbsolutePath.toString))
  }

  def parseJson(filename: String): org.json.JSONObject = {
    import org.json.JSONObject

    val br = new BufferedReader(new FileReader(filename))

    val sb = new StringBuilder()
    var line = br.readLine()

    while (line != null) {
      sb.append(line)
      sb.append(System.lineSeparator())
      line = br.readLine()
    }

    val everything = sb.toString()

    new JSONObject(everything)
  }

  def youtubeDL(directory: Directory)(url: YtUrl) = {
    println("Saving to " + directory)

    command(
      YOUTUBE_DL + " --skip-download " + quote + url.value + quote + " " +
        "--sub-format srt --write-sub --write-auto-sub --ignore-errors --youtube-skip-dash-manifest  " +
        " -o " + quote + directory.value + slash + "v%(id)s" + quote + " --write-info-json --write-description " +
        "--write-annotations --sub-lang en --no-call-home"
    )

    parseJson(directory.value + slash + "v" + url.id.value + ".info.json")
  }

  def curl(directory: Directory)(url: String, filename: String, timeout: Integer = 30): String = {
    import scala.sys.process._

    val command =
      CURL +
        " --connect-timeout " + timeout +
        " --max-time " + timeout +
        " -o  " + QUOTE + directory.value + File.separator + filename + QUOTE + " " + QUOTE + url + QUOTE

    println(command)

    try {
      command.!!
    } catch {
      case e: Exception => {
        e.printStackTrace()

        ""
      }
    }
  }

  def load(dir: Directory, file: String): Iterator[String] = {
    println(dir.value + slash + file)
    Source.fromFile(dir.value + slash + file).getLines()
  }

  def email(
             to: String,
             from: String,
             subject: String,
             text: Option[String],
             html: Option[String],
             attachments: List[File]) = {
    import com.github.sebrichards.postmark.Attachment
    import com.github.sebrichards.postmark.PostmarkClient
    import com.github.sebrichards.postmark.PostmarkError
    import com.github.sebrichards.postmark.PostmarkMessage
    import com.github.sebrichards.postmark.PostmarkSuccess

    if (postmarkKey != "") {
      val client = new PostmarkClient(postmarkKey)

      val message = PostmarkMessage(
        To = to,
        From = from,
        Subject = subject,
        TextBody = text,
        HtmlBody = html,

        // Optional mail fields
        //Cc = Some("Another Recipient <another.recipient@domain.com>"),
        Bcc = Some(POSTMARK_BCC),
        ReplyTo = Some(POSTMARK_REPLY_TO),

        // Optional attachments
        // Attachment(new File("picture.jpg"))
        Attachments = attachments.map(Attachment(_)), //List(
        //Attachment("Text File.txt", "text/plain", Base64.encodeBase64String("Hello world".getBytes)),
        //
        //      ),

        // Optional Postmark fields
        //Tag = Some("My Tag"),
        /*Headers = List(
        NameValueMap("key", "value"),
        NameValueMap("key2", "value2")
      ),*/
        TrackOpens = true
      )

      val result: Either[PostmarkError, PostmarkSuccess] = client.send(message)

      println(result)

      client.destroy
    }
  }

  def save(dir: Directory, filename: String, text: String) = {
    println(dir.value + slash + filename)

    val file = new File(dir.value + slash + filename)

    val p = new java.io.PrintWriter(file)
    try {
      p.append(text)
    } finally {
      p.close()
    }
  }

  def zip(dir: Directory, filename: String, include: List[String]) = {
    import java.io.{ BufferedInputStream, FileInputStream, FileOutputStream }
    import java.util.zip.{ ZipEntry, ZipOutputStream }

    println(dir.value + slash + filename)

    val zip = new ZipOutputStream(new FileOutputStream(dir.value + slash + filename))

    include.foreach { name =>
      zip.putNextEntry(new ZipEntry(name))
      val in = new BufferedInputStream(new FileInputStream(dir.value + slash + name))
      var b = in.read()
      while (b > -1) {
        zip.write(b)
        b = in.read()
      }
      in.close()
      zip.closeEntry()
    }
    zip.close()

  }

  def text(dir: Directory, file: String) = {
    // TODO this could also use TIKA, it would be good to compare
    this.node("text.js", List(dir.value + slash + file))
  }
}
