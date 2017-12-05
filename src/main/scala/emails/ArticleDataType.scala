package emails

import org.apache.solr.common.SolrDocument

/**
  * Created by gary on 12/5/2017.
  */
class ArticleDataType extends  DataType {
  val core = "articles2"
  val filter = Some("article_text_s:*")
  val fieldsToRetrieve = List("id", "score")
  val textFields = List("title_s", "article_text_s")
  val fieldsToQuery = List(("title_s", 2.0), ("article_text_s", 1.0))

  // TODO make sure these URLs are still up?
  val postFilter = (doc: SolrDocument) =>
    doc.get("title_s") != null &&
      doc.get("article_text_s") != null &&
      doc.get("article_text_s") != "" &&
      doc.get("id") != null &&
      doc.get("id").toString.startsWith("http")
}
