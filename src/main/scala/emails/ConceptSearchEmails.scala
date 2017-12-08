package emails

import java.nio.file.{Files, Paths}
import java.nio.charset.StandardCharsets
import java.io._
import java.nio.charset.StandardCharsets
import java.nio.file.{Files, Paths}
import java.util.Date

import org.apache.solr.client.solrj.SolrQuery
import org.apache.solr.client.solrj.impl.HttpSolrClient
import org.apache.solr.common.SolrDocument
import org.json.{JSONArray, JSONObject}
import org.nd4j.linalg.api.ndarray.INDArray
import org.nd4j.linalg.ops.transforms.Transforms
import util.{NLP, Semantic}
import java.util

import org.nd4j.linalg.cpu.nativecpu.NDArray

import scala.collection.JavaConverters._
import scala.collection.parallel.ForkJoinTaskSupport
//import scala.util.parsing.json.JSONArray

case class Link(title: String, url: String, text: String, id: String, score: Float)

object ConceptSearchEmails {
  def main(args: Array[String]): Unit = {
    // TODO port unit tests?
    // TODO get data from Google spreadsheet
    // TODO how long does this take if you do 200 emails instead of one
    // TODO templates for Aweber emails
    // TODO just take input as JSon and output as json
    // And write some assertins on the input and output

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
    val concepts = new Concepts

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
                    println(new Date)
                    val links1 = concepts.generate(like, dislike, previouslySent, new VideoDataType)
                    links1.map(println)
                    println(new Date)

                    println(new Date)
                    val links2 = concepts.generate(like, dislike, previouslySent, new ArticleDataType)
                    links2.map(println)
                    println(new Date)

                    Some(email, id, links1, links2)
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

    //println(textTemplate)
    //println(htmlTemplate)
  }

}