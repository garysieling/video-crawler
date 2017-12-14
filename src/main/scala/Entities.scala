import java.io.File

import org.apache.solr.client.solrj.SolrQuery
import org.apache.solr.client.solrj.impl.HttpSolrClient
import org.apache.solr.common.SolrDocument
import util._

object Entities {
  def main(args: Array[String]): Unit = {
    import scala.collection.JavaConverters._

    val cmd = new Commands
    val md = new Markdown
    val integrations = new Integrations
    val subtitles = new Subtitles
    val urls = new Urls

    // http://40.87.64.225:8983/solr/talks/select?fl=id&hl.fl=auto_transcript_txt_en&hl.mergeContiguous=true&hl.simple.post=]&hl.simple.pre=[&hl=on&q=auto_transcript_txt_en:%22machine%20learning%22&rows=100
    val solrUrl = "http://40.87.64.225:8983/solr/talks"
    val solr = new HttpSolrClient(solrUrl)

    val entities = List(
      "machine learning",
      "computer vision",
      "artificial intelligence",
      "natural language processing",
      "social justice",
      "liberation theology",
      "dignity",
      "agency", //<- word sense here
      "agency",
      "racism",
      "racial justice",
      "reparations",
      "aparteid",
      "palestinian",
      "black lives matter",
      "gentrification",
      "economic justice",
      "colonialism",
      "functional programming",
      "cryptocurrency",
      "bitcoin",
      "encryption",
      "african diaspora",
      "ethereum",
      "dogecoin",
      "litecoin"
    )

    def list(qq: String): List[SolrDocument] = {
      import scala.collection.JavaConversions._

      val query = new SolrQuery()
      query.setQuery( qq )
      query.setFields(List("id").toArray: _*)
      query.setRows(1000000)

      val rsp = solr.query( query )

      val result = rsp.getResults().toList

      result
    }

    def printToFile(f: java.io.File)(op: java.io.PrintWriter => Unit) {
      val p = new java.io.PrintWriter(f)
      try { op(p) } finally { p.close() }
    }

    printToFile(new File("entities.txt")) { p =>
      entities.map(
        (e) => list(
          "title_s:\"" + e + "\" OR " + "auto_transcript_txt_en:\"" + e + "\""
        ).map(
          (doc) => {
            p.println(e + "," + doc.get("id"))
          }
        )
      )
    }

    //cmd.withTempDirectory[Unit](
      //(dir) => {
        //val url = "http://nautil.us/issue/49/the-absurd/the-impossible-mathematics-of-the-real-world"
        //cmd.curl(dir)(url, "nautilus.html")

        //val text = cmd.text(dir, "nautilus.html")

        // One option is to do this with



        //println(text)
      //}
    //)
  }
}
