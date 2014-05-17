# RxJava: Reactive Extensions for the JVM

This library is a Java implementation of <a href="https://rx.codeplex.com">Rx Observables</a>.

Some of the goals of RxJava are:

- Stay close to the original Rx.Net implementation while adjusting naming conventions and idioms to Java
- All contracts of Rx should be the same
- Target the JVM not a language. The first languages supported (beyond Java itself) are 
<a href="https://github.com/Netflix/RxJava/tree/master/language-adaptors/rxjava-groovy">Groovy</a>, 
<a href="https://github.com/Netflix/RxJava/tree/master/language-adaptors/rxjava-clojure">Clojure</a>, 
and <a href="https://github.com/Netflix/RxJava/tree/master/language-adaptors/rxjava-scala">Scala</a>. 
New language adapters can be <a href="https://github.com/Netflix/RxJava/wiki/How-to-Contribute">contributed</a>.
- Support Java 6+ (to include Android support) 

Learn more about Rx on the <a href="https://github.com/Netflix/RxJava/wiki">Wiki Home</a> and the <a href="http://techblog.netflix.com/2013/02/rxjava-netflix-api.html">Netflix TechBlog post</a> where RxJava was introduced.

## Master Build Status

<a href='https://netflixoss.ci.cloudbees.com/job/RxJava-master/'><img src='https://netflixoss.ci.cloudbees.com/job/RxJava-master/badge/icon'></a>

## Pull Request Build Status

<a href='https://netflixoss.ci.cloudbees.com/job/RxJava-pull-requests/'><img src='https://netflixoss.ci.cloudbees.com/job/RxJava-pull-requests/badge/icon'></a>

## Communication

- Google Group: [RxJava](http://groups.google.com/d/forum/rxjava)
- Twitter: [@RxJava](http://twitter.com/RxJava)
- [GitHub Issues](https://github.com/Netflix/RxJava/issues)

## Versioning

RxJava is working towards a 1.0 release which will be reached once it "more or less" becomes feature complete with the [Rx.Net version](https://rx.codeplex.com). The backlog of features needed to accomplish this are documented in the [project issues](https://github.com/Netflix/RxJava/issues).

In short, once the current issue list hits 0 open we will bump to version 1.0.

Until that time the "semantic versioning" will be prefixed with the 0.* and breaking changes will be done such as 0.5.x -> 0.6.x All incremental non-breaking changes with additive functionality will be done like 0.5.1 -> 0.5.2.

Once we hit 1.0 it will follow the normal major.minor.patch semantic versioning approach.

## Full Documentation

- [Wiki](https://github.com/Netflix/RxJava/wiki)
- <a href="http://netflix.github.com/RxJava/javadoc/">Javadoc</a>

## Code

- <a href="https://github.com/Netflix/RxJava/tree/master/rxjava-core">Java Core</a>   
  - <a href="https://github.com/Netflix/RxJava/tree/master/rxjava-core/src/main/java/rx/Observable.java">Observable</a>
  - <a href="https://github.com/Netflix/RxJava/tree/master/rxjava-core/src/main/java/rx/Observer.java">Observer</a>
- <a href="https://github.com/Netflix/RxJava/tree/master/language-adaptors/rxjava-groovy">Groovy Adaptor</a> 
- <a href="https://github.com/Netflix/RxJava/tree/master/language-adaptors/rxjava-clojure">Clojure Adaptor</a> 
- <a href="https://github.com/Netflix/RxJava/tree/master/language-adaptors/rxjava-scala">Scala Adaptor</a> 

## Binaries

Binaries and dependency information for Maven, Ivy, Gradle and others can be found at [http://search.maven.org](http://search.maven.org/#search%7Cga%7C1%7Cg%3A%22com.netflix.rxjava%22).

Example for Maven:

```xml
<dependency>
    <groupId>com.netflix.rxjava</groupId>
    <artifactId>rxjava-core</artifactId>
    <version>x.y.z</version>
</dependency>
```
and for Ivy:

```xml
<dependency org="com.netflix.rxjava" name="rxjava-core" rev="x.y.z" />
```

If you need to download the jars instead of using a build system, create a Maven pom file like this with the desired version:

```xml
<?xml version="1.0"?>
<project xmlns="http://maven.apache.org/POM/4.0.0" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd">
  <modelVersion>4.0.0</modelVersion>
	<groupId>com.netflix.rxjava.download</groupId>
	<artifactId>rxjava-download</artifactId>
	<version>1.0-SNAPSHOT</version>
	<name>Simple POM to download rxjava-core and dependencies</name>
	<url>http://github.com/Netflix/RxJava</url>
	<dependencies>
		<dependency>
			<groupId>com.netflix.rxjava</groupId>
			<artifactId>rxjava-core</artifactId>
			<version>x.y.z</version>
			<scope/>
		</dependency>
	</dependencies>
</project>
```

Then execute:

```
mvn -f download-rxjava-pom.xml dependency:copy-dependencies
```

It will download rxjava-core-*.jar and its dependencies into ./target/dependency/.

You need Java 6 or later.

## Build

To build:

```
$ git clone git@github.com:Netflix/RxJava.git
$ cd RxJava/
$ ./gradlew build
```

Futher details on building can be found on the [Getting Started](https://github.com/Netflix/RxJava/wiki/Getting-Started) page of the wiki.

## Bugs and Feedback

For bugs, questions and discussions please use the [Github Issues](https://github.com/Netflix/RxJava/issues).

 
## LICENSE

Copyright 2013 Netflix, Inc.

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

<http://www.apache.org/licenses/LICENSE-2.0>

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
