package emails

import org.apache.solr.common.SolrDocument

/**
  * Created by gary on 12/5/2017.
  */
class VideoDataType extends  DataType {
  val core = "talks"
  val filter = None
  val fieldsToRetrieve = List("id", "score", "url_s")
  val textFields = List("title_s", "auto_transcript_en_txt")
  val fieldsToQuery = List(("title_s", 2.0), ("auto_transcript_en_txt", 1.0))
  val postFilter = (doc: SolrDocument) => true
}
