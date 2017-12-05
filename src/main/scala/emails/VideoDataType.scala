package emails

/**
  * Created by gary on 12/5/2017.
  */
class VideoDataType extends  DataType {
  val core = "talks"
  val filter = None
  val fieldsToRetrieve = List("id", "score", "title_s", "url_s")
  val fieldsToQuery = List(("title_s", 2.0), ("auto_transcript_en_txt", 1.0))
}
