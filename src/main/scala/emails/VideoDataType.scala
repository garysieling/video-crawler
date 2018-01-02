package emails

import indexer.DataType
import org.apache.solr.common.SolrDocument

/**
  * Created by gary on 12/5/2017.
  */
class VideoDataType extends  DataType {
  val core = "talks"
  val filter = None
  val fieldsToRetrieve = List("id", "score", "url_s")
  val textFields = List("title_s", "auto_transcript_txt_en")
  val fieldsToQuery = List(("title_s", 2.0), ("auto_transcript_txt_en", 1.0))
  val titleField = "title_s"
  val idField = "id"
  val urlField = (doc: SolrDocument) =>
    if (doc.get("url_s").toString.indexOf("youtube.com") > 0) {
      "https://www.findlectures.com/view/" + doc.get("id")
    } else {
      doc.get("url_s").toString
    }

  val postFilter = (doc: SolrDocument) => true
}
