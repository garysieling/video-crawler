sbt one-jar
tar zcf resources.tar.gz src/main/resources

scp target/scala-2.12/scala-indexer_2.12-1.0-one-jar.jar root@$SERVER:/root
scp resources.tar.gz root@$SERVER:/root
