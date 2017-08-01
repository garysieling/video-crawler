name := "scala-indexer"

version := "1.0"

libraryDependencies += "org.json" % "json" % "20170516"
libraryDependencies += "com.hynnet" % "solr-solrj" % "5.3.1"
libraryDependencies += "commons-logging" % "commons-logging-api" % "1.1"
libraryDependencies += "org.mozilla" % "rhino" % "1.7.7.1"
libraryDependencies += "com.github.crawler-commons" % "crawler-commons" % "0.7"
libraryDependencies += "com.github.sebrichards" %% "postmark-scala" % "1.3"
libraryDependencies += "org.apache.opennlp" % "opennlp-tools" % "1.8.0"

resolvers += "jCenter" at "https://jcenter.bintray.com"

libraryDependencies += "net.dean.jraw" % "JRAW" % "0.9.0"
libraryDependencies += "org.rogach" %% "scallop" % "3.0.3"

scalaVersion := "2.12.2"
