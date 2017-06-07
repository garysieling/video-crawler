package util

import java.io.{BufferedReader, File, FileReader}
import java.nio.file.Files

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

  def node(cmd: String): String = {
    println(cmd)
    import sys.process._

    // TODO configurable
    val output = Process(
      "node " + cmd,
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

    command(subtitlecmd)
  }

  def canEmbed(id: YtId): Boolean = {
    ???
  }
  
  def withTempDirectory[T]( cb: (Directory) => T ): T = {
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
    command.!!
  }

  def load(dir: Directory, file: String): Iterator[String] = {
    println(dir.value + "\\" + file)
    Source.fromFile(dir.value + "\\" + file).getLines()
  }
}
