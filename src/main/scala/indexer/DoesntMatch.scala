package indexer

import org.json.JSONObject
import util.Semantic

object DoesntMatch {
  def first(document: JSONObject, strings: Seq[String]): Option[String] = {
    strings.filter(
      (key) => document.has(key) && (
        Option(document.get(key)) match {
          case Some("") => false
          case None => false
          case _ => true
        }
        )
    ).headOption map {
      document.get(_).toString
    }
  }

  def truncate(distanceTitle: Double): String =
    (distanceTitle.toString.substring(0, 4))

  def main(args: Array[String]): Unit = {
    val w2v = new Semantic("D:\\projects\\clones\\pathToSaveModel1.txt")
    w2v.init
    val terms = List("hiking", "art", "violin")

    val sim =
      terms.map(
        (term1) => {
          terms.map(
            (term2) => (term1, term2)
          )
        }
      ).flatten.filter(
        (tuple) => tuple._1 < tuple._2
      ).map(
        (tuple) => (tuple._1, tuple._2, w2v.model.get.similarity(tuple._1, tuple._2))
      ).map(
        (tuple) => tuple._1 + "<-->" + tuple._2 + ": " + tuple._3
      ).mkString("\n")

    println(sim)

  }
}