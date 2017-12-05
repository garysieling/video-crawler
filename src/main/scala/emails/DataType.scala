package emails

import org.apache.solr.common.SolrDocument

/**
  * Created by gary on 12/5/2017.
  */
abstract class DataType {
  val core: String
  val filter: Option[String]
  val fieldsToRetrieve: List[String]
  val fieldsToQuery: List[(String, Double)]
  val postFilter: (SolrDocument) => Boolean
  val textFields: List[String]
  val urlField: (SolrDocument) => String
  val idField: String
  val titleField: String
}
