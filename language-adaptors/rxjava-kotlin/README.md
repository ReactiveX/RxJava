# Kotlin Adaptor for RxJava

This adaptor allows Kotlin Functions to be used and RxJava will know how to invoke them

This enable code such as:

```kotlin
Observable.toObservable("one", "two", "three")
    .take(2)
    .subscribe{ (arg:String) ->
        println(arg)
    }
```

In the future this module will expose a more idiomatic way to use RxJava inside Kotlin

# Binaries

Binaries and dependency information for Maven, Ivy, Gradle and others can be found at [http://search.maven.org](http://search.maven.org/#search%7Cga%7C1%7Ca%3A%22rxjava-kotlin%22).

Example for Maven:

```xml
<dependency>
    <groupId>com.netflix.rxjava</groupId>
    <artifactId>rxjava-kotlin</artifactId>
    <version>x.y.z</version>
</dependency>
```

and for Ivy:

```xml
<dependency org="com.netflix.rxjava" name="rxjava-kotlin" rev="x.y.z" />
```

