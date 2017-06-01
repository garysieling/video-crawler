/**
  * Created by gary on 5/31/2017.
  */
case class YtId(value: String)
case class YtUrl(value: String)

class Urls {
  def ytId(requestedUrl: String): Option[YtId] = {
    val youtubePrefixes = List("//", "http://", "https://")

    val youtubeUrls = List(
      "mobile-youtube.com",
      "www.mobile-youtube.com",
      "m.youtube.com",
      "youtu.be",
      "www.youtu.be",
      "youtube.com",
      "www.youtube.com"
    )

    val suffixes = List(
      "/embed/",
      "/watch?v="
    )

    val possibleUrls =
      youtubePrefixes.map(
        pre => youtubeUrls.map(
          url =>
            suffixes.map(
              suffix => pre + url + suffix
            )
        )
      ).flatten.flatten

    // TODO this returns no errors if it doesn't match
    val found = possibleUrls.filter(
      (possibleUrlFormat) =>
        requestedUrl.toLowerCase.startsWith(possibleUrlFormat.toLowerCase)
    ).headOption

    found.map(
      (value) =>
        requestedUrl.substring(value.length)
    ).filter(
      (v) => !v.contentEquals("/")
    ) match {
      case Some(value) => {
        value.split("[&]").headOption match {
          case Some(value: String) => Some(YtId(value))
          case None => None
        }
      }
      case None => None
    }
  }

  def ytUrl(valueOpt: Option[YtId]): Option[YtUrl] =
    valueOpt.map(
      (v) => YtUrl("https://www.youtube.com/watch?v=" + v.value)
    )

  def ytUrl(valueOpt: Option[YtId], time: Int): Option[YtUrl] =
    valueOpt.map(
      (v) => YtUrl("https://www.youtube.com/watch?v=" + v.value + "&t=" + time)
    )
}
