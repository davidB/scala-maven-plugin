scala-maven-plugin
====================

The **scala-maven-plugin** (previously maven-scala-plugin) is used for compiling/testing/running/documenting Scala code in Maven.

* [home](http://alchim31.free.fr/mvnsites/maven-scala-plugin/index.html)

* [wiki](https://github.com/davidB/maven-scala-plugin/wiki)

* [issues](https://github.com/davidB/maven-scala-plugin/issues/)

## Build

Currently, you need Maven 3.x to build the plugin, create the site, and run `integration-test`.

## commands

* `mvn package` : generate jar
* `mvn site` : generate the plugin website
* `mvn integration-test` : `mvn package` + run all integration test
* `mvn invoker:run -Dinvoker.test=test1` : run integration test 'test1' useful for tuning/debug
* `mvn install` :  `mvn integration-test` + publish on local maven repository
* `mvn install -Dmave.test.skip=true` : `mvn install` without run of unit test or run of integration test
* release :
  * `mvn release:prepare && mvn release:perform` : to publish on staging repository via plugin
  * `mvn site source:jar javadoc:jar install:install gpg:sign deploy:deploy changes:announcement-generate`
    `-Dmaven.test.skip=true -DperformRelease=true` : manual
  
# TODO

* close issues from https://github.com/davidB/maven-scala-plugin/issues/
* try to integrate the "dependency builder" of SBT 0.10
* try to use aether to manage dependencies  
* refactor :
  * reduce copy-paste
  * file path management can be improve (a lot) 
  * clean the code
  
