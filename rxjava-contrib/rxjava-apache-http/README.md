# rxjava-apache-http

Observable API for Apache [HttpAsyncClient](http://hc.apache.org/httpcomponents-asyncclient-dev/)

It is aware of Content-Type `text/event-stream` and will stream each event via `Observer.onNext`.

Other Content-Types will be returned as a single call to `Observer.onNext`.

Main Classes:

- (ObservableHttp)[https://github.com/Netflix/RxJava/blob/master/rxjava-contrib/rxjava-apache-http/src/main/java/rx/apache/http/ObservableHttp.java]
- (ObservableHttpResponse)[https://github.com/Netflix/RxJava/blob/master/rxjava-contrib/rxjava-apache-http/src/main/java/rx/apache/http/ObservableHttpResponse.java]


# Binaries

Binaries and dependency information for Maven, Ivy, Gradle and others can be found at [http://search.maven.org](http://search.maven.org/#search%7Cga%7C1%7Ccom.netflix.rxjava).

Example for [Maven](http://search.maven.org/#search%7Cga%7C1%7Ca%3A%22rxjava-apache-http%22):

```xml
<dependency>
    <groupId>com.netflix.rxjava</groupId>
    <artifactId>rxjava-apache-http</artifactId>
    <version>x.y.z</version>
</dependency>
```

and for Ivy:

```xml
<dependency org="com.netflix.rxjava" name="rxjava-apache-http" rev="x.y.z" />
```

# Sample usage
