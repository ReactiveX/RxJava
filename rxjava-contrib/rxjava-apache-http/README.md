# rxjava-apache-http

Observable API for Apache [HttpAsyncClient](http://hc.apache.org/httpcomponents-asyncclient-dev/)

It is aware of Content-Type `text/event-stream` and will stream each event via `Observer.onNext`.

Other Content-Types will be returned as a single call to `Observer.onNext`.

Main Classes:

- [ObservableHttp](https://github.com/Netflix/RxJava/blob/master/rxjava-contrib/rxjava-apache-http/src/main/java/rx/apache/http/ObservableHttp.java)
- [ObservableHttpResponse](https://github.com/Netflix/RxJava/blob/master/rxjava-contrib/rxjava-apache-http/src/main/java/rx/apache/http/ObservableHttpResponse.java)


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

# Sample Usage

### Create a Request

```java
ObservableHttp.createGet("http://www.wikipedia.com", httpClient).toObservable();
ObservableHttp.createRequest(HttpAsyncMethods.createGet("http://www.wikipedia.com"), httpClient).toObservable();
```

### Http Client

A basic default client:

```java
CloseableHttpAsyncClient httpClient = HttpAsyncClients.createDefault();
```

or a custom client with configuration options:

```java
final RequestConfig requestConfig = RequestConfig.custom()
        .setSocketTimeout(3000)
        .setConnectTimeout(3000).build();
final CloseableHttpAsyncClient httpclient = HttpAsyncClients.custom()
        .setDefaultRequestConfig(requestConfig)
        .setMaxConnPerRoute(20)
        .setMaxConnTotal(50)
        .build();
```

### Normal Http GET

Execute a request and transform the `byte[]` reponse to a `String`:

```groovy
        ObservableHttp.createRequest(HttpAsyncMethods.createGet("http://www.wikipedia.com"), client)
        .toObservable()
        .flatMap({ ObservableHttpResponse response ->
                return response.getContent().map({ byte[] bb ->
                        return new String(bb);
                });
        })
        .toBlockingObservable()
        .forEach({ String resp -> 
                // this will be invoked once with the response
                println(resp);
        });
```

### Streaming Http GET with [Server-Sent Events (text/event-stream)](http://www.w3.org/TR/eventsource/) Response

Execute a request and transform the `byte[]` response of each event to a `String`:

```groovy
        ObservableHttp.createRequest(HttpAsyncMethods.createGet("http://hostname/event.stream"), client)
        .toObservable()
        .flatMap({ ObservableHttpResponse response ->
                return response.getContent().map({ byte[] bb ->
                        return new String(bb);
                });
        })
        .toBlockingObservable()
        .forEach({ String resp -> 
                // this will be invoked for each event
                println(resp);
        });
```




