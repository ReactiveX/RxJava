# Clojure Adaptor for RxJava


This adaptor allows 'fn' functions to be used and RxJava will know how to invoke them.

This enables code such as:

```clojure
(-> 
  (Observable/toObservable ["one" "two" "three"])
  (.take 2) 
  (.subscribe (fn [arg] (println arg))))
```

This still dependes on Clojure using Java interop against the Java API. 

A future goal is a Clojure wrapper to expose the functions in a more idiomatic way.


# Binaries

Binaries and dependency information for Maven, Ivy, Gradle and others can be found at [http://search.maven.org](http://search.maven.org/#search%7Cga%7C1%7Ca%3A%22rxjava-clojure%22).

Example for Maven:

```xml
<dependency>
    <groupId>com.netflix.rxjava</groupId>
    <artifactId>rxjava-clojure</artifactId>
    <version>x.y.z</version>
</dependency>
```

and for Ivy:

```xml
<dependency org="com.netflix.rxjava" name="rxjava-clojure" rev="x.y.z" />
```

and for Leiningen:

```clojure
[com.netflix.rxjava/rxjava-clojure "x.y.z"]
```
