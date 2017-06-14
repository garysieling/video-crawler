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
      "node " + cmd + " " + args.mkString(" ")

    println(entireCommand)

    // TODO configurable
    val output = Process(
      entireCommand,
      new File("D:\\projects\\scala-indexer\\src\\main\\resources\\")
    ).!! // TODO errors should not throw here

    println(output)

    output
  }

  def vttToSrt(dir: Directory)(id: YtId): Iterable[LogEntry] = {

    val srt = dir.value + "\\v" + id.value + ".srt" // TODO use the right OS type for file strings
    val vtt = dir.value + "\\v" + id.value + ".en.vtt"

    val subtitlecmd = // TODO configurable paths
      "\"d:/Software/ffmpeg-20160619-5f5a97d-win32-static/bin/ffmpeg.exe\" -i \"" + vtt + "\" \"" + srt + "\""

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
    command(
      "d:\\Software\\youtube-dl.exe --skip-download \"" + url.value + "\" " +
        "--sub-format srt --write-sub --write-auto-sub --ignore-errors --youtube-skip-dash-manifest  " +
        " -o \"" + directory.value + "/v%(id)s\" --write-info-json --write-description " +
        "--write-annotations --sub-lang en --no-call-home"
    )

    parseJson(directory.value + "/v" + url.id.value + ".info.json")
  }

  def curl(directory: Directory)(url: String, filename: String): String = {
    import scala.sys.process._

    val command = ("curl -o  \"" + directory.value + File.separator + filename + "\" \"" + url + "\"")
    println(command)

    command.!!
  }

  def load(dir: Directory, file: String): Iterator[String] = {
    println(dir.value + "\\" + file)
    Source.fromFile(dir.value + "\\" + file).getLines()
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

    val client = new PostmarkClient("895b990f-ac62-47a0-a984-a380edd59d54")

    val message = PostmarkMessage(
      To = to,
      From = from,
      Subject = subject,
      TextBody = text,
      HtmlBody = html,

      // Optional mail fields
      //Cc = Some("Another Recipient <another.recipient@domain.com>"),
      Bcc = Some("gary@garysieling.com"),
      ReplyTo = Some("gary@garysieling.com"),

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

  def save(dir: Directory, filename: String, text: String) = {
    println(dir.value + "\\" + filename)

    val file = new File(dir.value + "\\" + filename)

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

    println(dir.value + "\\" + filename)

    val zip = new ZipOutputStream(new FileOutputStream(dir.value + "\\" + filename))

    include.foreach { name =>
      zip.putNextEntry(new ZipEntry(name))
      val in = new BufferedInputStream(new FileInputStream(dir.value + "\\" + name))
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
}
