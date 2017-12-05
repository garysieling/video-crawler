package emails

/**
  * Created by gary on 12/5/2017.
  */
abstract class DataType {
  val core: String
  val filter: Option[String]
  val fieldsToRetrieve: List[String]
  val fieldsToQuery: List[(String, Double)]
}
