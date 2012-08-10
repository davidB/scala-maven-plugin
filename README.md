scala-maven-plugin
====================

The **scala-maven-plugin** (previously maven-scala-plugin) is used for compiling/testing/running/documenting Scala code in Maven.

* [Documentation](http://davidb.github.com/scala-maven-plugin/index.html)
* [Wiki](https://github.com/davidB/scala-maven-plugin/wiki)
* [Issues](https://github.com/davidB/scala-maven-plugin/issues/)
* [Full changelog](http://davidb.github.com/scala-maven-plugin/changes-report.html)

## Build

Currently, you need Maven 3.x to build the plugin, create the site, and run `integration-test`.

## commands

* `mvn package` : generate jar
* `mvn site` : generate the plugin website
* `mvn integration-test` : `mvn package` + run all integration test
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
  
