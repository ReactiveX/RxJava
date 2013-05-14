# Groovy Adaptor for RxJava


This adaptor allows 'groovy.lang.Closure' functions to be used and RxJava will know how to invoke them.

This enables code such as:

```groovy
  Observable.from("one", "two", "three")
    .take(2) 
    .subscribe({arg -> println(arg)})
```

# Binaries

Binaries and dependency information for Maven, Ivy, Gradle and others can be found at [http://search.maven.org](http://search.maven.org/#search%7Cga%7C1%7Ca%3A%22rxjava-groovy%22).

Example for Maven:

```xml
<dependency>
    <groupId>com.netflix.rxjava</groupId>
    <artifactId>rxjava-groovy</artifactId>
    <version>x.y.z</version>
</dependency>
```

and for Ivy:

```xml
<dependency org="com.netflix.rxjava" name="rxjava-groovy" rev="x.y.z" />
```

and for Gradle:

```groovy
compile 'com.netflix.rxjava:rxjava-groovy:x.y.z'
```
