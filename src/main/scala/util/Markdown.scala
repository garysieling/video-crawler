package util

/**
  * Created by gary on 5/31/2017.
  *
  * TODO maybe this should be one of those self returning things
  * so you can do
  *
  * md.title("test").text(123).footnote( md.url("123") )
  */
class Markdown {
  private val sb = new StringBuffer
  private var footnoteIndex = 0
  private var footnotes = List[String]()
  private var metadata = Map[String, AnyRef]()

  def title(value: String): Unit = {
    metadata = metadata + ("title" -> value)
  }

  def description(value: String): Unit = {
    metadata = metadata + ("description" -> value)
  }

  def tags(value: List[String]): Unit = {
    metadata = metadata + ("tags" -> value)
  }

  def h1(value: String) = {
    sb.append(value)
    sb.append("\n")

    for (i <- 1 to value.length) {
      sb.append("=")
    }
  }

  def text(value: String) = {
    sb.append(value)
    sb.append("\n")
  }

  def footnote(value: String) = {
    footnoteIndex = footnoteIndex + 1
    sb.append("[")
    sb.append(footnoteIndex)
    sb.append("]")

    footnotes = value :: footnotes
  }

  def url(value: String) =
    sb.append("<" + value + ">")

  def url(title: String, value: String) =
    sb.append("(" + title + ")" + "[" + value + "]")

  def image(name: String, title: String) = {
    sb.append(
      s"""
        |{% asset_img "${name}" "${title}" %}
      """.stripMargin
    )
  }

  override  def toString = {
    val finalSb = new StringBuffer

    for (prop <- metadata) {
      finalSb.append(prop._1) // TODO does this need quotes
      finalSb.append(": ")
      prop._2 match {
        case value: Seq[String] => {
          finalSb.append(value.mkString(","))
        }
        case value: String => {
          finalSb.append(value)
        }
        case _ => ???
      }

      finalSb.append("\n")
    }

    finalSb.append("---")
    finalSb.append("\n")
    finalSb.append("\n")

    finalSb.append(sb)

    for (i <- 1 to footnoteIndex) {
      finalSb.append("  [" + i + "] " + footnotes(footnotes.length - i))
      finalSb.append("\n")
    }

    finalSb.toString
  }
}
