import net.dean.jraw.RedditClient
import net.dean.jraw.http._
import net.dean.jraw.http.oauth.Credentials
import net.dean.jraw.models.Submission
import net.dean.jraw.paginators.SubredditPaginator
import org.apache.solr.common.SolrInputDocument
import org.rogach.scallop.ScallopConf
import util.{Commands, Directory}

/**
  * Created by gary on 8/1/2017.
  */
class RedditConf(arguments: Seq[String]) extends ScallopConf(arguments) {
  val username = opt[String](required = true)
  val password = opt[String](required = true)
  val clientId = opt[String](required = true)
  val secret = opt[String](required = true)
  val platform = opt[String](required = true)

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
      new RedditLinkProvider(_, conf, List(
        "programmming"
      )).run
    )

  }
}

class RedditLinkProvider(directory: Directory, conf: RedditConf, subreddits: List[String]) {
  val version = "1.0.0"

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

    val paginator = new SubredditPaginator(redditClient, "programming")

    val firstPage = paginator.next().toArray()

    // Iterate over the submissions
    val posts: Array[Post] =
      firstPage.flatMap(
        s => {
          s match {
            case (submission: Submission) => {
              if (!submission.isSelfPost) {
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
            }
            case _ => ???
          }
        }
      )

      val solrClient = new Solr("articles")
      posts.zipWithIndex.map( ( data ) => {
        val cmd = new Commands
        val file = data._2 + ".html"
        cmd.curl(directory)(data._1.url, file, 10)

        val text = cmd.text(directory, file)

        val sid = new SolrInputDocument()
        sid.setField("article", text)
        sid.setField("url", data._1.url)
        sid.setField("comments", data._1.comments)
        sid.setField("domain", data._1.domain)
        sid.setField("removalReason", data._1.removalReason)
        sid.setField("title", data._1.title)
        sid.setField("created", data._1.created)
        sid.setField("url", data._1.url)
        sid.setField("score", data._1.score)
        sid.setField("author", data._1.author)
        sid.setField("id", data._1.permalink)

        solrClient.indexDocument(
          sid
        )

        solrClient.commit
        // TODO write this to solr
        // TODO add this to word2vec
      })
  }
}
