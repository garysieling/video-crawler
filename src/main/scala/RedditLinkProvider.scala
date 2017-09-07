import java.security.MessageDigest

import net.dean.jraw.RedditClient
import net.dean.jraw.http._
import net.dean.jraw.http.oauth.Credentials
import net.dean.jraw.models.Submission
import net.dean.jraw.paginators.SubredditPaginator
import org.apache.solr.common.SolrInputDocument
import org.joda.time.DateTime
import org.rogach.scallop.ScallopConf
import util.{Commands, Directory, NLP, Semantic}

import scala.util.matching.Regex

/**
  * Created by gary on 8/1/2017.
  */
class RedditConf(arguments: Seq[String]) extends ScallopConf(arguments) {
  val username = opt[String](required = true)
  val password = opt[String](required = true)
  val clientId = opt[String](required = true)
  val secret = opt[String](required = true)
  val platform = opt[String](required = true)
  val modelPath = opt[String](required = true)

  verify()
}

case class Post (
  comments: Integer,
  domain: String,
  removalReason: String,
  title: String,
  permalink: String,
  created: java.util.Date,
  score: Integer,
  url: String,
  author: String
                )

object RedditLinkProvider {
  def main(args: Array[String]): Unit = {
    val conf = new RedditConf(args)

    val cmd = new Commands
    for (i <- 1 until 100)
    {
      cmd.withTempDirectory(
        new RedditLinkProvider(_, conf,
          List(
            "programmming",
            "javascript",
            // Clojurescript // too small
            // datomic // too small
            // functionalprogramming // too small
            "clojure",
            "algorithms",
            "compsci",
            "python",
            "coding",
            "javascript",
            "MachineLearning",
            "crypto",
            "computervision",
            "LanguageTechnology",
            "datascience",
            "statistics",
            "math",
            "matlab",
            "java",
            "cryptography",
            "softwaredevelopment",
            "systems",
            "kernel",
            "php",
            "osdev",
            "cpp",
            "csharp",
            "ruby",
            "golang",
            "haskell",
            "c_programming",
            "rust",
            "swift",
            "sql",
            "scala",
            "lisp",
            "clojure",
            "latex",
            "perl",
            "matlab",
            "rstats",
            "erlang",
            "objectivec",
            "lua",
            "elixir",
            "scheme",
            "asm",
            "mathematica",
            "Rlanguage",
            "ocaml",
            "fsharp",
            "visualbasic",
            "elm",
            "julia",
            "coffeescript",
            "elm",
            "racket",
            "d_language",
            "gpgpu",
            "shell",
            "groovy",
            "dartlang",
            "prolog",
            "kotlin",
            "fortran",
            "Smaller",
            "smalltalk",
            "nim",
            "ada",
            "forth",
            "coldfusion",
            "delphi",
            "coq",
            "idris",
            "tcl",
            "awk",
            "APLJK",
            "cobol",
            "pascal",
            "verilog",
            "sml",
            "octave",
            "brainfuck",
            "IoLanguage",
            "ATS",
            "oberon",
            "ceylon",
            "Ioke",
            "vhdl",
            "Rebol",
            "PostScript",
            "NetLogo",
            "dylanlang",
            "mercury",
            "datalog",
            "Piet",
            "befunge",
            "MUMPS",
            "LogoUnderground",
            "SNOBOL4",
            "writing",
            "beekeeping",
            "userexperience",
            "web_design",
            "Design",
            "usability",
            "architecture",
            "graphic_design",
            "IndustrialDesign",
            "InteriorDesign",
            "logodesign",
            "product_design",
            "typography",
            "Calligraphy",
            "fonts",
            "Lettering",
            "angularjs",
            "augmentedreality",
            "oculus",
            "virtualreality",
            "computergraphics",
            "psychology",
            "arduino",
            "economics"
          )
        ).run
      )
    }
  }
}

class RedditLinkProvider(directory: Directory, conf: RedditConf, subreddits: List[String]) {
  val version = "1.0.0"

  def run = {
    val startTime = new DateTime()

    val myUserAgent = UserAgent.of(
      conf.platform.getOrElse(???),
      conf.clientId.getOrElse(???),
      version,
      conf.username.getOrElse(???))

    val redditClient = new RedditClient(myUserAgent)

    val credentials = Credentials.script(
      conf.username.getOrElse(???),
      conf.password.getOrElse(???),
      conf.clientId.getOrElse(???),
      conf.secret.getOrElse(???)
    )

    val authData = redditClient.getOAuthHelper().easyAuth(credentials)
    redditClient.authenticate(authData)


    //val w2v = new Semantic(conf.modelPath.toOption.get)
    //w2v.init

    def cleanUrl(url: String): String = {
      val newUrl = new Regex("^(http:|https:)", "i").replaceAllIn(url, "")

      val nohash = newUrl.split("#")(0)
      val firstUrl = nohash.split("[?]")(0) // remove url params

      firstUrl
    }

    var i = 0
    val solrClient = new Solr("articles")

    subreddits.map(
      (subreddit) => {
        val paginator = new SubredditPaginator(redditClient, subreddit)

        val firstPage = paginator.next().toArray()

        // Iterate over the submissions
        val posts: Array[Post] =
          firstPage.flatMap(
            s => {
              s match {
                case (submission: Submission) => {
                  try {
                    val selfPost: Boolean = {
                      try {
                        submission.isSelfPost
                      } catch {
                        case (e: Exception) => {
                          println(submission.getUrl)

                          false
                        }
                      }
                    }

                    if (!selfPost) {
                      Some(
                        Post(
                          comments = submission.getCommentCount,
                          domain = submission.getDomain,
                          removalReason = submission.getRemovalReason,
                          title = submission.getTitle,
                          permalink = submission.getPermalink,
                          created = submission.getCreated,
                          score = submission.getScore,
                          url = submission.getUrl,
                          author = submission.getAuthor
                        )
                      )
                    } else {
                      None
                    }
                  } catch {
                    case (e: Exception) => {
                      println(submission.getDataNode.toString)

                      e.printStackTrace()

                      None
                    }
                  }
                }
                case _ => ???
              }
            }
          )

        posts.filter(
          (post) => {
            try {
              !post.url.toLowerCase.endsWith(".pdf") &&
              !post.url.toLowerCase.endsWith(".png") &&
              !post.url.toLowerCase.endsWith(".gif") &&
              !post.url.toLowerCase.endsWith(".jpg") &&
              !post.url.toLowerCase.endsWith(".jpeg") &&
              !post.url.toLowerCase.endsWith(".wav") &&
              !post.url.toLowerCase.endsWith(".mp3") &&
              !post.url.toLowerCase.endsWith(".flv") &&
              !post.url.toLowerCase.endsWith(".tiff") &&
              !post.url.toLowerCase.endsWith(".mp4")
            } catch {
              case (e: Exception) => {
                false
              }
            }
          }
        ).zipWithIndex.map((data) => {
          // TODO store original + new
          // TODO store when this was retrieved
          // TODO tag dbpedia entities

          val comments = data._1.comments
          val points = data._1.score

          if (comments + points > 5) {
            val cmd = new Commands
            val file = data._2 + ".html"
            cmd.curl(directory)(data._1.url, file, 10)

            val text = cmd.text(directory, file)

            val sid = new SolrInputDocument()

            val cleanText = NLP.cleanText(text)
            println(cleanText)

            val shorterUrl = cleanUrl(data._1.url)

            val messageDigest = MessageDigest.getInstance("SHA-256")
            messageDigest.update(shorterUrl.getBytes())
            val id = new String(messageDigest.digest())

            sid.setField("article", cleanText)
            sid.setField("url", data._1.url)
            sid.setField("cleanUrl", shorterUrl)
            sid.setField("comments", comments)
            sid.setField("domain", data._1.domain)
            sid.setField("removalReason", data._1.removalReason)
            sid.setField("title", data._1.title)
            sid.setField("created", data._1.created)
            sid.setField("url", data._1.url)
            sid.setField("points", points)
            sid.setField("author", data._1.author)
            sid.setField("id", id)
            sid.setField("weekoftime", startTime.weekyear().get())

            solrClient.indexDocument(
              sid
            )

            i = i + 1

            if (i % 10 == 0) {
              solrClient.commit
            }
          }
          //w2v.close()

          //println("getting sentences")
          //val sentences = NLP.getSentences(cleanText)

          //println("training word2vec")
          //w2v.train(sentences)
          //println("word2vec updated")

          // TODO write this to solr
          // TODO add this to word2vec
        })
      }
    )

    //w2v.close()
    solrClient.commit

    print(startTime)
    print(new DateTime())
  }
}
