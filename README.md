# Scala Maven Plugin

[![ci](https://github.com/davidB/scala-maven-plugin/actions/workflows/ci.yaml/badge.svg)](https://github.com/davidB/scala-maven-plugin/actions/workflows/ci.yaml)
[![Maven Central](https://maven-badges.herokuapp.com/maven-central/net.alchim31.maven/scala-maven-plugin/badge.svg)](https://maven-badges.herokuapp.com/maven-central/net.alchim31.maven/scala-maven-plugin)

The **scala-maven-plugin** (previously maven-scala-plugin) is used for compiling/testing/running/documenting Scala code in Maven.

* [Documentation](https://davidb.github.io/scala-maven-plugin/index.html)
* [Wiki](https://github.com/davidB/scala-maven-plugin/wiki)
* [Issues](https://github.com/davidB/scala-maven-plugin/issues/)

## Similar plugins

* [scalor-maven-plugin](https://github.com/random-maven/scalor-maven-plugin)
* [sbt-compiler-maven-plugin](https://github.com/sbt-compiler-maven-plugin/sbt-compiler-maven-plugin)
* [sbt-delegate-maven-plugin](https://github.com/AugustNagro/sbt-delegate-maven-plugin)

## Build

Currently, you need Maven 3.x & JDK 8 to build the plugin, create the site, and run `integration-test`.

## Commands

* `./mvnw package` : generate jar
* `./mvnw site` : generate the plugin website
* `./mvnw integration-test` : `./mvnw package` + run all integration test
  * note: to run `test_scalaHome`: you have to set `scala.home` property in `src/it/test_scalaHome/pom.xml` to correspond to your environment.  See Build section above for a simple setup.
* `./mvnw integration-test -Dinvoker.test=test1` : run integration test 'test1' (against all configuration) useful for tuning/debug
* `./mvnw install` :  ./mvnw integration-test` + publish on local maven repository
* `./mvnw install -Dmaven.test.skip=true` : ./mvnw install` without run of unit test and run of integration test
* release :
  * `gpg --use-agent --armor --detach-sign --output $(mktemp) pom.xml` to avoid issue on macosx with gpg signature see [[MGPG-59] GPG Plugin: "gpg: signing failed: Inappropriate ioctl for device" - ASF JIRA](https://issues.apache.org/jira/browse/MGPG-59)
  * `./mvnw release:clean && ./mvnw release:prepare && ./mvnw release:perform` : to publish on staging repository via plugin
  * `./mvnw release:clean && ./mvnw release:prepare -Darguments="-DskipTests -Dmaven.test.skip=true" && ./mvnw release:perform -Darguments="-DskipTests -Dmaven.test.skip=true"` to publish without tests (integration test require 30min on CI)
  * `./mvnw site package source:jar javadoc:jar install:install gpg:sign deploy:deploy changes:announcement-generate -Dmaven.test.skip=true -DperformRelease=true` : manual
  * connect to <https://oss.sonatype.org/> close and release the request(about scala-maven-plugin) in staging repositories
  * browse the updated [mvnsite](https://davidb.github.io/scala-maven-plugin/) (check version into samples, ...)

## TODO

* close issues from <https://github.com/davidB/scala-maven-plugin/issues/>
* refactor :
  * reduce copy-paste
  * file path management can be improved (a lot)
  * clean the code
