<html>
<a href='http://www.pledgie.com/campaigns/4750'><img alt='Click here to lend your support to: scala-maven-plugin and make a donation at www.pledgie.com !' src='http://www.pledgie.com/campaigns/4750.png?skin_name=chrome' border='0' /></a>
</html>
[![Flattr this git repo](http://api.flattr.com/button/flattr-badge-large.png)](https://flattr.com/submit/auto?user_id=davidB&url=https://github.com/davidB/scala-maven-plugin&title=scala-maven-plugin&language=&tags=github&category=software)
scala-maven-plugin
====================

The **scala-maven-plugin** (previously maven-scala-plugin) is used for compiling/testing/running/documenting Scala code in Maven.

* [Documentation](http://davidb.github.com/scala-maven-plugin/index.html)
* [Wiki](https://github.com/davidB/scala-maven-plugin/wiki)
* [Issues](https://github.com/davidB/scala-maven-plugin/issues/)
* [Full changelog](http://davidb.github.com/scala-maven-plugin/changes-report.html)

## Donate



## Build

Currently, you need Maven 3.x to build the plugin, create the site, and run `integration-test`.

An existing installation of Scala is also tested against.
Supposing the scala-library.jar of your existing scala library lives
at `/usr/local/Cellar/scala/2.10.2/libexec` then you will need to set a symbolic link up as
follows (on Unix):

`ln -s /usr/local/Cellar/scala/2.10.2/libexec ~/bin/soft-jvm/scala

## commands

* `mvn package` : generate jar
* `mvn site` : generate the plugin website
* `mvn integration-test` : `mvn package` + run all integration test
  * note: to run _test\_scalaHome_: you have to set `scala.home` property in `src/it/test_scalaHome/pom.xml` to correspond to your environment
* `mvn invoker:run -Dinvoker.test=test1` : run integration test 'test1' useful for tuning/debug
* `mvn install` :  `mvn integration-test` + publish on local maven repository
* `mvn install -Dmaven.test.skip=true` : `mvn install` without run of unit test and run of integration test
* release :
  * `mvn release:prepare && mvn release:perform` : to publish on staging repository via plugin
  * `mvn site source:jar javadoc:jar install:install gpg:sign deploy:deploy changes:announcement-generate`
    `-Dmaven.test.skip=true -DperformRelease=true` : manual
  * connect to http://oss.sonatype.org/ close and release the request(about scala-maven-plugin) in staging repositories
  * browse the updated [mvnsite](http://davidb.github.com/scala-maven-plugin/) (check version into samples, ...)
  * email the content of `target/checkout/target/announcement/announcement.md.vm` to post@implicitly.posterous.com, and to maven-and-scala@googlegroups.com (same subject but without tag part)
  
# TODO

* close issues from https://github.com/davidB/scala-maven-plugin/issues/
* try to integrate the "dependency builder" of SBT 0.10
* try to use aether to manage dependencies  
* refactor :
  * reduce copy-paste
  * file path management can be improve (a lot) 
  * clean the code
  
