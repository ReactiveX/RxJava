## Getting Binaries

You can find binaries and dependency information for Maven, Ivy, Gradle, SBT, and others at [http://search.maven.org](http://search.maven.org/#search%7Cga%7C1%7Cg%3A%22io.reactivex%22%20AND%20a%3A%22rxjava%22).

Example for Maven:

```xml
<dependency>
    <groupId>io.reactivex</groupId>
    <artifactId>rxjava</artifactId>
    <version>1.3.4</version>
</dependency>
```
and for Ivy:

```xml
<dependency org="io.reactivex" name="rxjava" rev="1.3.4" />
```

and for SBT:

```scala
libraryDependencies += "io.reactivex" %% "rxscala" % "0.26.5"

libraryDependencies += "io.reactivex" % "rxjava" % "1.3.4"
```

and for Gradle:
```groovy
compile 'io.reactivex:rxjava:1.3.4'
```

If you need to download the jars instead of using a build system, create a Maven `pom` file like this with the desired version:

```xml
<?xml version="1.0"?>
<project xmlns="http://maven.apache.org/POM/4.0.0" 
    xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" 
    xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd">
	<modelVersion>4.0.0</modelVersion>
	<groupId>com.netflix.rxjava.download</groupId>
	<artifactId>rxjava-download</artifactId>
	<version>1.0-SNAPSHOT</version>
	<name>Simple POM to download rxjava and dependencies</name>
	<url>http://github.com/ReactiveX/RxJava</url>
	<dependencies>
		<dependency>
			<groupId>io.reactivex</groupId>
			<artifactId>rxjava</artifactId>
			<version>1.3.4</version>
			<scope/>
		</dependency>
	</dependencies>
</project>
```

Then execute:

```
$ mvn -f download-rxjava-pom.xml dependency:copy-dependencies
```

That command downloads `rxjava-*.jar` and its dependencies into `./target/dependency/`.

You need Java 6 or later.

### Snapshots

Snapshots are available via [JFrog](https://oss.jfrog.org/webapp/search/artifact/?5&q=rxjava):

```groovy
repositories {
    maven { url 'https://oss.jfrog.org/libs-snapshot' }
}

dependencies {
    compile 'io.reactivex:rxjava:1.3.y-SNAPSHOT'
}
```

## Building

To check out and build the RxJava source, issue the following commands:

```
$ git clone git@github.com:ReactiveX/RxJava.git
$ cd RxJava/
$ ./gradlew build
```

To do a clean build, issue the following command:

```
$ ./gradlew clean build
```

A build should look similar to this:

```
$ ./gradlew build
:rxjava:compileJava
:rxjava:processResources UP-TO-DATE
:rxjava:classes
:rxjava:jar
:rxjava:sourcesJar
:rxjava:signArchives SKIPPED
:rxjava:assemble
:rxjava:licenseMain UP-TO-DATE
:rxjava:licenseTest UP-TO-DATE
:rxjava:compileTestJava
:rxjava:processTestResources UP-TO-DATE
:rxjava:testClasses
:rxjava:test
:rxjava:check
:rxjava:build

BUILD SUCCESSFUL

Total time: 30.758 secs
```

On a clean build you will see the unit tests run. They will look something like this:

```
> Building > :rxjava:test > 91 tests completed
```

#### Troubleshooting

One developer reported getting the following error:

> Could not resolve all dependencies for configuration ':language-adaptors:rxjava-scala:provided'

He was able to resolve the problem by removing old versions of `scala-library` from `.gradle/caches` and `.m2/repository/org/scala-lang/` and then doing a clean build. <a href="https://gist.github.com/jaceklaskowski/9496058">(See this page for details.)</a>

You may get the following error during building RxJava:

> Failed to apply plugin [id 'java']
> Could not generate a proxy class for class nebula.core.NamedContainerProperOrder.

It's a JVM issue, see [GROOVY-6951](https://jira.codehaus.org/browse/GROOVY-6951) for details. If so, you can run `export GRADLE_OPTS=-noverify` before building RxJava, or update your JDK.
