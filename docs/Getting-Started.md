## Getting Binaries

You can find binaries and dependency information for Maven, Ivy, Gradle, SBT, and others at [http://search.maven.org](https://search.maven.org/search?q=g:io.reactivex.rxjava4%20AND%20rxjava).

Example for Maven:

```xml
<dependency>
    <groupId>io.reactivex.rxjava4</groupId>
    <artifactId>rxjava</artifactId>
    <version>x.y.z</version>
</dependency>
```
and for Ivy:

```xml
<dependency org="io.reactivex.rxjava4" name="rxjava" rev="x.y.z" />
```

and for SBT:

```scala
libraryDependencies += "io.reactivex.rxjava4" % "rxjava" % "x.y.z"
```

and for Gradle:
```groovy
implementation 'io.reactivex.rxjava4:rxjava:x.y.z'
```

Replace `x.y.z` with a released version from Maven Central.

If you need to download the jars instead of using a build system, create a Maven `pom` file like this with the desired version:

```xml
<?xml version="1.0"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
    xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
    xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd">
      <modelVersion>4.0.0</modelVersion>
      <groupId>com.example</groupId>
      <artifactId>download-rxjava</artifactId>
      <version>1.0.0</version>
      <dependencies>
          <dependency>
              <groupId>io.reactivex.rxjava4</groupId>
              <artifactId>rxjava</artifactId>
              <version>x.y.z</version>
          </dependency>
      </dependencies>
</project>
```

Then execute:

```
$ mvn -f download-rxjava-pom.xml dependency:copy-dependencies
```

That command downloads `rxjava-*.jar` and its dependencies into `./target/dependency/`.

You need Java 26 or later.

### Snapshots

Snapshots after May 19th, 2025 are available via https://central.sonatype.com/repository/maven-snapshots/io/reactivex/rxjava4/rxjava/

```groovy
repositories {
  maven { url 'https://central.sonatype.com/repository/maven-snapshots' }
}

dependencies {
  implementation 'io.reactivex.rxjava4:rxjava:4.0.0-SNAPSHOT'
}
```

Javadoc snapshots are available at https://reactivex.io/RxJava/4.x/javadoc/snapshot


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
