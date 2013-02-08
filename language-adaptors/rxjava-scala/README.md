# Scala Adaptor for RxJava


This adaptor allows 'fn' functions to be used and RxJava will know how to invoke them.

This enables code such as:

```scala
Observable.toObservable("1", "2", "3")
  .take(2)
  .subscribe((callback: String) => {
      println(callback)
  })
```

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
