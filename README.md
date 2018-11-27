# Scala Maven Plugin

[![Build Status](https://travis-ci.org/davidB/scala-maven-plugin.svg?branch=master)](https://travis-ci.org/davidB/scala-maven-plugin)
[![Code Quality: Java](https://img.shields.io/lgtm/grade/java/g/davidB/scala-maven-plugin.svg?logo=lgtm&logoWidth=18)](https://lgtm.com/projects/g/davidB/scala-maven-plugin/context:java)
[![Total Alerts](https://img.shields.io/lgtm/alerts/g/davidB/scala-maven-plugin.svg?logo=lgtm&logoWidth=18)](https://lgtm.com/projects/g/davidB/scala-maven-plugin/alerts)

The **scala-maven-plugin** (previously maven-scala-plugin) is used for compiling/testing/running/documenting Scala code in Maven.

* [Documentation](https://davidb.github.io/scala-maven-plugin/index.html)
* [Wiki](https://github.com/davidB/scala-maven-plugin/wiki)
* [Issues](https://github.com/davidB/scala-maven-plugin/issues/)

## Similar plugins

* [scalor-maven-plugin](https://github.com/random-maven/scalor-maven-plugin)
* [sbt-compiler-maven-plugin](https://github.com/sbt-compiler-maven-plugin/sbt-compiler-maven-plugin)

## Build

Currently, you need Maven 3.x & JDK 8 to build the plugin, create the site, and run `integration-test`.

## Commands

* `./mvnw package` : generate jar
* `./mvnw site` : generate the plugin website
* `./mvnw integration-test` : `./mvnw package` + run all integration test
  * note: to run _test\_scalaHome_: you have to set `scala.home` property in `src/it/test_scalaHome/pom.xml` to correspond to your environment.  See Build section above for a simple setup.
* `./mvnw invoker:run -Dinvoker.test=test1` : run integration test 'test1' useful for tuning/debug
* `./mvnw install` :  ./mvnw integration-test` + publish on local maven repository
* `./mvnw install -Dmaven.test.skip=true` : ./mvnw install` without run of unit test and run of integration test
* release :
  * `gpg --use-agent --armor --detach-sign --output $(mktemp) pom.xml` to avoid issue on macosx with gpg signature see [[MGPG-59] GPG Plugin: "gpg: signing failed: Inappropriate ioctl for device" - ASF JIRA](https://issues.apache.org/jira/browse/MGPG-59)
  * `./mvnw release:prepare && ./mvnw release:perform` : to publish on staging repository via plugin
  * `./mvnw site package source:jar javadoc:jar install:install gpg:sign deploy:deploy changes:announcement-generate -Dmaven.test.skip=true -DperformRelease=true` : manual
  * connect to http://oss.sonatype.org/ close and release the request(about scala-maven-plugin) in staging repositories
  * browse the updated [mvnsite](https://davidb.github.io/scala-maven-plugin/) (check version into samples, ...)
  * email the content of `target/checkout/target/announcement/announcement.md.vm` to post@implicitly.posterous.com, and to maven-and-scala@googlegroups.com (same subject but without tag part)

## TODO

* close issues from https://github.com/davidB/scala-maven-plugin/issues/
* try to integrate the "dependency builder" of SBT 0.10
* try to use aether to manage dependencies  
* refactor :
  * reduce copy-paste
  * file path management can be improve (a lot) 
  * clean the code
