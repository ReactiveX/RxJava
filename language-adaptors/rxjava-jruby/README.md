# JRuby Adaptor for RxJava


This adaptor allows `org.jruby.RubyProc` lambda functions to be used and RxJava will know how to invoke them.

This enables code such as:

```ruby
  Observable.from("one", "two", "three")
    .take(2) 
    .subscribe(lambda { |arg| puts arg })
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

and for JBundler:

```ruby
jar 'com.netflix.rxjava:rxjava-ruby', 'x.y.z'
```
