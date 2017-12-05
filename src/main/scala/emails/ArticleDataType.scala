package emails

/**
  * Created by gary on 12/5/2017.
  */
class ArticleDataType extends  DataType {
  val core = "articles2"
  val filter = Some("article_text_s:*")
  val fieldsToRetrieve = List("id", "score", "title_s", "article_text_s")
  val fieldsToQuery = List(("title_s", 2.0), ("article_text_s", 1.0))
}
