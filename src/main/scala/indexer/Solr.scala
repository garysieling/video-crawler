package indexer

import java.io._

import com.hazelcast.core.HazelcastInstance
import org.apache.solr.client.solrj.SolrQuery
import org.apache.solr.client.solrj.impl.HttpSolrClient
import org.apache.solr.common.{SolrDocument, SolrInputDocument}
import org.json.JSONObject

import scala.collection.parallel.ForkJoinTaskSupport

class Solr(hazelcastInstance: HazelcastInstance) {
  def listDocuments(dt: DataType, qq: String, rows: Integer, skip: List[String]): List[SolrDocument] = {
    import scala.collection.JavaConversions._

    val solrUrl = "http://40.87.64.225:8983/solr/" + dt.core
    val solr = new HttpSolrClient(solrUrl)

    val query = new SolrQuery()

    // todo remove negative terms

    val userQuery = qq
    query.setQuery( qq )
    query.setFields((dt.fieldsToRetrieve ++ dt.textFields).toArray: _*)
    query.setRequestHandler("tvrh")
    query.setRows(rows)
    skip.map(
      (id) => {
        query.addFilterQuery("-id:" + id)
      }
    )

    dt.filter match {
      case Some(value: String) => query.addFilterQuery(value)
      case None => {}
    }

    val rsp = solr.query( query )

    val result = rsp.getResults().toList

    result
  }

  def list(core: String, qq: String, fl: List[String], rows: Integer): List[SolrDocument] = {
    import scala.collection.JavaConversions._

    //val solrUrl = "http://40.87.64.225:8983/solr/" + core
    val solrUrl = "http://40.87.64.225:8983/solr/" + core

    val solr = new HttpSolrClient(solrUrl)

    val query = new SolrQuery()
    query.setQuery( qq )
    query.setFields(fl.toArray: _*)
    query.setRows(rows)

    val rsp = solr.query( query )

    val result = rsp.getResults().toList

    result
  }

  def first(document: JSONObject, strings: Seq[String]) = {
    strings.filter(
      (key) => document.has(key) && (
          Option(document.get(key)) match {
            case Some("") => false
            case None => false
            case _ => true
          }
        )
    ).headOption map {
      document.get(_)
    }
  }

  def exists(core: String, id: String) = {
    //val solrUrl = "http://40.87.64.225:8983/solr/" + core
    val solrUrl = "http://40.87.64.225:8983/solr/" + core

    val solr = new HttpSolrClient(solrUrl)

    try {
      solr.getById(id)

      true
    } catch {
      case e: Exception => false
    }
  }

  def indexDocument(core: String, doc: SolrInputDocument) = {
    //val solrUrl = "http://40.87.64.225:8983/solr/" + core
    val solrUrl = "http://40.87.64.225:8983/solr/" + core

    val solr = new HttpSolrClient(solrUrl)

    solr.add(doc)
  }

  def commit(core: String) = {
    //val solrUrl = "http://40.87.64.225:8983/solr/" + core
    val solrUrl = "http://40.87.64.225:8983/solr/" + core

    val solr = new HttpSolrClient(solrUrl)

    solr.commit()
  }

  def indexFile(core: String, file: File): Unit = {
    //val solrUrl = "http://40.87.64.225:8983/solr/" + core
    val solrUrl = "http://40.87.64.225:8983/solr/" + core

    val solr = new HttpSolrClient(solrUrl)

    // post to Solrs
    //println(file)
    val br: BufferedReader = new BufferedReader(new FileReader(file))
    val sb: StringBuilder = new StringBuilder
    var line: String = br.readLine
    while (line != null) {
      {
        sb.append(line)
        sb.append(System.lineSeparator)
        line = br.readLine
      }
    }
    val everything: String = sb.toString
    val obj: JSONObject = new JSONObject(everything)

    val document = new SolrInputDocument()

   // obj.keys().toList.map(
     // (key) => {
      //  val value = obj.get(key)

/*        //console.log(data.id);
        data.features_ss = (data.features_ss || []).filter(
          (x) => ["Video", "Audio", "Closed Captions", "Suitable for Work"].includes(x)
        );

        if (data.url_s && data.url_s.indexOf('youtube.com') >= 0) {
          data.features_ss.push('Video');
        }

        if (data.video_url_s && data.video_url_s.indexOf('youtube.com') >= 0) {
          data.features_ss.push('Video');
        }

        if (data.video_url_s && data.video_url_s.indexOf('vimeo.com') >= 0) {
          data.features_ss.push('Video');
        }

        if (!!data.audio_url_s) {
          data.features_ss.push('Audio');
        }

        if (!!data.download_s) {
          data.features_ss.push('Audio');
        }

        data.features_ss = uniq(data.features_ss);*/
     // }
      //)

    first(obj, Seq("speakerName_ss")) map (
      document.addField("speakerName_ss", _)
      )


    first(obj, Seq("transcript_txt_en", "transcript_s", "auto_transcript_txt_en", "auto_transcript_s", "auto_transcript_txt_en")) map (
      document.addField("transcript_txt_en", _)
    )

    first(obj, Seq("url_s", "video_url_s", "audio_url_s")) map (
      document.addField("url_s", _)
    )

    first(obj, Seq("description_txt_en", "description_s")) map (
      document.addField("description_txt_en", _)
    )

    first(obj, Seq("speakerBio_s", "speakerBio_s", "speakerBio_ss_src")) map (
      document.addField("speakerBio_s", _)
    )

    first(obj, Seq("id_i")) map (
      (value) => document.addField("id", value.toString)
    )

    first(obj, Seq("title_s")) map (
      document.addField("title_s", _)
    )

    solr.add(document)

    if (document.hashCode() % 777 == 0) {
      solr.commit()
    }
  }

  def main(args:Array[String] ):Unit = {
    //val solrUrl = "http://40.87.64.225:8983/solr/" + core
    val solrUrl = "http://40.87.64.225:8983/solr/talks"

    val solr = new HttpSolrClient(solrUrl)

    val folderPath: String = "C:\\projects\\image-annotation\\data\\talks\\json\\1"

    val folder: File = new File(folderPath)
    val fileList: Array[File] = folder.listFiles(new FilenameFilter() {
      def accept(dir: File, name: String): Boolean = {
        return name.endsWith(".json")
      }
    })

    val work = fileList.par
    work.tasksupport = new ForkJoinTaskSupport(
      new scala.concurrent.forkjoin.ForkJoinPool(32))

    work.map((file) => indexFile("talks", file))
    solr.commit()
  }

  /*@throws[IOException]
  def getTerms: util.List[String] = {
    val folderPath: String = "C:\\projects\\image-annotation\\data\\talks\\json\\1"
    val folder: File = new File(folderPath)
    val fileList: Array[File] = folder.listFiles(new FilenameFilter() {
      def accept(dir: File, name: String): Boolean = {
        return name.endsWith(".json")
      }
    })
    val fileName: File = fileList(0)
    val results: util.List[String] = new util.ArrayList[String]
    var i: Int = 0
    while (i < fileList.length) {
      {
        try {
          val br: BufferedReader = new BufferedReader(new FileReader(fileName))
          try {
            val sb: StringBuilder = new StringBuilder
            var line: String = br.readLine
            while (line != null) {
              {
                sb.append(line)
                sb.append(System.lineSeparator)
                line = br.readLine
              }
            }
            val everything: String = sb.toString
            val obj: JSONObject = new JSONObject(everything)
            if (obj.has("title_s")) {
              results.add(obj.getString("title_s"))
            }
            if (obj.has("description_s")) {
              results.add(obj.getString("description_s"))
            }
            if (obj.has("transcript_s")) {
              results.add(obj.getString("transcript_s"))
            }
            if (obj.has("auto_transcript_txt_en")) {
              results.add(obj.getString("auto_transcript_txt_en"))
            }
            if (obj.has("auto_transcript_s")) {
              results.add(obj.getString("auto_transcript_s"))
            }
            if (obj.has("transcript_txt_en")) {
              results.add(obj.getString("transcript_txt_en"))
            }
            if (obj.has("speakerBio_s")) {
              results.add(obj.getString("speakerBio_s"))
            }
          } finally {
            if (br != null) br.close()
          }
        }
      }
      ({
        i += 1; i - 1
      })
    }
    return results
  }*/
}