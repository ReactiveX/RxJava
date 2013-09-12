# Scala Adaptor for RxJava

There's an old Scala adaptor ( `rx.lang.scala.RxImplicits` with test `rx.lang.scala.RxImplicitsTest` ), which is deprecated. All other classes in `rx.lang.scala` belong to the new adaptor.

# Binaries

Binaries and dependency information for Maven, Ivy, Gradle and others can be found at [http://search.maven.org](http://search.maven.org/#search%7Cga%7C1%7Ca%3A%22rxjava-scala%22).

Example for Maven:

```xml
<dependency>
    <groupId>com.netflix.rxjava</groupId>
    <artifactId>rxjava-scala</artifactId>
    <version>x.y.z</version>
</dependency>
```

and for Ivy:

```xml
<dependency org="com.netflix.rxjava" name="rxjava-scala" rev="x.y.z" />
```

and for sbt:

```scala
libraryDependencies ++= Seq(
  "com.netflix.rxjava" % "rxjava-scala" % "x.y.z"
)
```
