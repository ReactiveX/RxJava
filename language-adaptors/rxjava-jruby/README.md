# JRuby Adaptor for RxJava

This adaptor improves the success and performance of RxJava when Ruby `Proc` is passed to an RxJava method.

This enables correct and efficient execution of code such as:

```ruby
  Observable.from("one", "two", "three").
    take(2).
    subscribe {|val| puts val}
```

# Binaries

Binaries and dependency information for Maven, Ivy, Gradle and others can be found at [http://search.maven.org](http://search.maven.org/#search%7Cga%7C1%7Ca%3A%22rxjava-jruby%22).

Example for Maven:

```xml
<dependency>
    <groupId>com.netflix.rxjava</groupId>
    <artifactId>rxjava-jruby</artifactId>
    <version>x.y.z</version>
</dependency>
```

and for Ivy:

```xml
<dependency org="com.netflix.rxjava" name="rxjava-jruby" rev="x.y.z" />
```

and for Gradle:

```groovy
compile 'com.netflix.rxjava:rxjava-jruby:x.y.z'
```
