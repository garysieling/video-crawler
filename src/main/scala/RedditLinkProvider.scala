import net.dean.jraw.RedditClient
import net.dean.jraw.http._
import net.dean.jraw.http.oauth.Credentials
import net.dean.jraw.paginators.SubredditPaginator
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


object RedditLinkProvider {
  def main(args: Array[String]) = {
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
    for (submission <- firstPage) {
      println(submission)
    }
  }

}
