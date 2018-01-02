package emails

import java.nio.charset.StandardCharsets
import java.nio.file.{Files, Paths}
import java.util.Date

import org.json.JSONArray
import java.util

import scala.collection.JavaConverters._
import com.hazelcast.config.Config
import com.hazelcast.core.Hazelcast
import indexer.emails.Concepts

import scala.collection.parallel.ForkJoinTaskSupport

case class Link(title: String, url: String, text: String, id: String, score: Float)

object ConceptSearchEmails {
  def main(args: Array[String]): Unit = {

    val cfg = new Config("concepts")

    /*cfg.setManagementCenterConfig(
      new ManagementCenterConfig(
        "http://localhost:8080/mancenter",
        3
      )
    )*/
    //cfg.setLiteMember(true)

    val hazelcastInstance = Hazelcast.newHazelcastInstance(cfg)

    val startTime = new Date
    println(startTime)
    // TODO port unit tests?
    // TODO templates for Aweber emails
    // And write some assertins on the input and output
    // TODO stuff doc average in solr
    // TODO try reduction with T-SNE to make searching useful (e.g. pick me one per facet, which might be fast in solr)
    //

    //val textTemplate = scala.io.Source.fromInputStream(getClass.getResourceAsStream("/alerts.txt")).mkString
    //val htmlTemplate = scala.io.Source.fromInputStream(getClass.getResourceAsStream("/alerts.html")).mkString
    val request = scala.io.Source.fromFile(args(0)).mkString

    def list(v: Object): List[String] = {
      v match {
        case (vv: util.List[Object]) => {
          val result = vv.asScala.map(_.toString).toList
          result
        }
        case null => {
          List()
        }
        case _ => {
          println(v)

          ???
        }
      }
    }

    val data = new JSONArray(request)
    val concepts = new Concepts(hazelcastInstance)

    val parallel =
      data.toList.asScala.flatMap(
        (e: AnyRef) => {
          e match {
            case (entry: util.HashMap[String, Object]) => {
              val c = entry.get("context")

              c match {
                case (context: util.HashMap[String, Object]) => {

                  val id = context.get("id")
                  val email = context.get("email")
                  val like = list(context.get("like"))
                  val dislike = list(context.get("dislike"))
                  val previouslySent = list(context.get("sent"))

                  if (like.length > 0) {
                    println("Starting like: " + new Date)
                    val links1 = concepts.generate(like, dislike, previouslySent, new VideoDataType)
                    links1.map(println)
                    println("Videos chosen: " + new Date)

                    //println(new Date)
                    //val links2 = concepts.generate(like, dislike, previouslySent, new ArticleDataType)
                    //links2.map(println)
                    //println(new Date)

                    Some(email, id, links1, links1)
                  } else {
                    None
                  }
                }

                case _ => None
              }
            }
            case _ => None
          }
        }
      ).par

    val threads = 16
    parallel.tasksupport = new ForkJoinTaskSupport(
      new scala.concurrent.forkjoin.ForkJoinPool(threads))

    val output = parallel.toList

    val json = scala.util.parsing.json.JSONArray(output).toString()
    Files.write(Paths.get(args(1)), json.getBytes(StandardCharsets.UTF_8))

    println(((new Date).getTime - startTime.getTime) / 1000.0 / 60.0)

    concepts.shutdown()
    //println(textTemplate)
    //println(htmlTemplate)
  }

}