package indexer

import java.io.FileNotFoundException
import java.net.SocketTimeoutException

import net.dean.jraw.RedditClient
import net.dean.jraw.http._
import net.dean.jraw.http.oauth.Credentials
import net.dean.jraw.models.Submission
import net.dean.jraw.paginators.{Sorting, SubredditPaginator, TimePeriod}
import org.apache.solr.common.SolrInputDocument
import org.joda.time.DateTime
import org.rogach.scallop.ScallopConf
import util.{Commands, Directory, NLP}
import java.util

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
      cmd.withTempDirectory(
        new RedditLinkProvider(_, conf,
          List(
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
            "YAwriters",
            "YAlit",
            "fantasywriters",
            "WritersGroup",
            "selfpublish",
            "IndieBookClub",
            "WritingHub",
            "IdeaDeving",
            "Fantasy",
            "scifi",
            "WritersIdeas",
            "freelance",
            "Write2Publish",
            "books",
            "nanowrimo",
            "horrorlit",
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
            "economics",
            "StainedGlass",
            "Luthier",
            "violinist",
            "beekeeeping",
            "musictheory",
            "recipes",
            "devops",
            "kubernetes",
            "hackernews"
          )
        ).run
      )
  }
}

class RedditLinkProvider(directory: Directory, conf: RedditConf, subreddits: List[String]) {
  val version = "1.1.0"
  val startTime = new DateTime()

  def cleanUrl(url: String): String = {
    val newUrl = new Regex("^(http://|https://)", "i").replaceAllIn(url, "")

    val nohash = newUrl.split("#")(0)


    val firstUrl =
      if (newUrl.indexOf("youtube") > 0) {
        newUrl
      } else {
        nohash.split("[?]")(0) // remove url params
      }

    firstUrl
  }

  def getDateRange(start: DateTime, end: DateTime): List[DateTime] = {
    import scala.collection.JavaConversions._

    val ret: util.ArrayList[DateTime] = new util.ArrayList[DateTime]()

    var tmp: DateTime = start

    while(tmp.isBefore(end) || tmp.equals(end)) {
      ret.add(tmp)
      tmp = tmp.plusDays(1)
    }

    ret.toList
  }

  def loadSubreddit(solrClient: Solr, subreddit: String, redditClient: RedditClient) = {
    var i = 0

    val paginator = new SubredditPaginator(redditClient, subreddit)
    paginator.setSorting(Sorting.TOP)
    paginator.setTimePeriod(TimePeriod.ALL)

    var j = 0
    var done = false
    while (j < 5000 && !done) {
      println("j: " + j)

      j = j + 1
      val firstPage = paginator.next().toArray()
      done = paginator.hasNext

      // Iterate over the submissions
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
                  if (
                    !submission.getUrl.contains("imgur.com") &&
                      !submission.getUrl.contains("instsgram.com")
                  ) {
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
      ).toList.filter(
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
              println(e)
              false
            }
          }
        }
      ).filter(
        (data) => {
          //val shorterUrl = cleanUrl(data.url)
          //!solrClient.exists(shorterUrl)
          true
        }
      ).zipWithIndex.par.map((data) => {
        try {
          // TODO store original + new
          // TODO store when this was retrieved
          // TODO tag dbpedia entities

          val comments = data._1.comments
          val points = data._1.score

          if (comments + points > 5 ||
            (subreddit == "hackernews" && data._1.author == "qznc_bot")) {
            val cmd = new Commands
            val file = data._2 + ".html"
            cmd.curl(directory)(data._1.url, file, 10)

            val text = cmd.text(directory, file)

            val sid = new SolrInputDocument()

            //val articleTitleAndText = cmd.title(directory.value + "\\" + file)

            val nlp = new NLP(null)
            val cleanText = nlp.cleanText(text)
            println(cleanText)

            val shorterUrl = cleanUrl(data._1.url)

            val id = new String(shorterUrl)

            sid.setField("article", nlp.replaceEntities(cleanText))
            sid.setField("cleanUrl", shorterUrl)
            sid.setField("subreddit", subreddit)
            sid.setField("comments", comments)

            if (data._1 != null) {
              sid.setField("domain", data._1.domain)
              sid.setField("removalReason", data._1.removalReason)
              sid.setField("reddit_title", data._1.title)
              sid.setField("reddit_title_entities", nlp.replaceEntities(data._1.title))
              sid.setField("created", data._1.created)
              sid.setField("weekoftime", data._1.created.getTime / 1000 / 3600 / 24 / 7)
              sid.setField("dayoftime", data._1.created.getTime / 1000 / 3600 / 24)
              sid.setField("url", data._1.url)
              sid.setField("author", data._1.author)
              sid.setField("url", data._1.url)
            }

            //if (articleTitleAndText._1 != null) {
              //sid.setField("article_title", articleTitleAndText._1)
            //}

            sid.setField("points", points)
            sid.setField("id", id)
            sid.setField("weekoftime", startTime.weekyear().get() * 52 + startTime.weekOfWeekyear().get())

            solrClient.indexDocument(
              "articles",
              sid
            )

            i = i + 1

            println(" ******************** " + i)

            if (i % 100 == 0) {
              solrClient.commit("articles")
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
        } catch {
          case e: FileNotFoundException => {
            println(e)
          }
          case e: Error => {
            println(e)
          }
        }
      })
    }

    solrClient.commit("articles")
  }

  def run = {
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

    var i = 0
    val solrClient = new Solr(null)

    /*getDateRange(new DateTime().minusDays(730), new DateTime()).reverse.map(
      (date: DateTime) => {
        val paginator = new SubredditPaginator(redditClient, subreddit)
        paginator.setSorting(Sorting.TOP)
        paginator.set
      }
    )*/

    println(subreddits)

    subreddits.par.map(
      (subreddit) => {
        println("Subreddit: " + subreddit)
        try {
          loadSubreddit(solrClient, subreddit, redditClient)
        } catch {
          case e: NetworkException => {
            e.printStackTrace();
          }
          case e: SocketTimeoutException => {
            e.printStackTrace();
          }
          case e: NullPointerException => {
            e.printStackTrace()
          }
          case e: Error => {
            println(e)
          }
        }
      }
    )

    //w2v.close()
    solrClient.commit("articles")

    print(startTime)
    print(new DateTime())
  }
}
