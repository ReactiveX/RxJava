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

An example event-stream is from [Hystrix](https://github.com/Netflix/Hystrix/tree/master/hystrix-contrib/hystrix-metrics-event-stream) used for streaming metrics. An [example webapp](https://github.com/Netflix/Hystrix/tree/master/hystrix-examples-webapp) can be used to test.

Output looks like:

```
data: {"type":"HystrixCommand","name":"CreditCardCommand","group":"CreditCard","currentTime":1379823924934,"isCircuitBreakerOpen":false,"errorPercentage":0,"errorCount":0,"requestCount":0,"rollingCountCollapsedRequests":0,"rollingCountExceptionsThrown":0,"rollingCountFailure":0,"rollingCountFallbackFailure":0,"rollingCountFallbackRejection":0,"rollingCountFallbackSuccess":0,"rollingCountResponsesFromCache":0,"rollingCountSemaphoreRejected":0,"rollingCountShortCircuited":0,"rollingCountSuccess":0,"rollingCountThreadPoolRejected":0,"rollingCountTimeout":0,"currentConcurrentExecutionCount":0,"latencyExecute_mean":0,"latencyExecute":{"0":0,"25":0,"50":0,"75":0,"90":0,"95":0,"99":0,"99.5":0,"100":0},"latencyTotal_mean":0,"latencyTotal":{"0":0,"25":0,"50":0,"75":0,"90":0,"95":0,"99":0,"99.5":0,"100":0},"propertyValue_circuitBreakerRequestVolumeThreshold":20,"propertyValue_circuitBreakerSleepWindowInMilliseconds":5000,"propertyValue_circuitBreakerErrorThresholdPercentage":50,"propertyValue_circuitBreakerForceOpen":false,"propertyValue_circuitBreakerForceClosed":false,"propertyValue_circuitBreakerEnabled":true,"propertyValue_executionIsolationStrategy":"THREAD","propertyValue_executionIsolationThreadTimeoutInMilliseconds":3000,"propertyValue_executionIsolationThreadInterruptOnTimeout":true,"propertyValue_executionIsolationThreadPoolKeyOverride":null,"propertyValue_executionIsolationSemaphoreMaxConcurrentRequests":10,"propertyValue_fallbackIsolationSemaphoreMaxConcurrentRequests":10,"propertyValue_metricsRollingStatisticalWindowInMilliseconds":10000,"propertyValue_requestCacheEnabled":true,"propertyValue_requestLogEnabled":true,"reportingHosts":1}
data: {"type":"HystrixCommand","name":"GetPaymentInformationCommand","group":"PaymentInformation","currentTime":1379823924934,"isCircuitBreakerOpen":false,"errorPercentage":0,"errorCount":0,"requestCount":0,"rollingCountCollapsedRequests":0,"rollingCountExceptionsThrown":0,"rollingCountFailure":0,"rollingCountFallbackFailure":0,"rollingCountFallbackRejection":0,"rollingCountFallbackSuccess":0,"rollingCountResponsesFromCache":0,"rollingCountSemaphoreRejected":0,"rollingCountShortCircuited":0,"rollingCountSuccess":0,"rollingCountThreadPoolRejected":0,"rollingCountTimeout":0,"currentConcurrentExecutionCount":0,"latencyExecute_mean":0,"latencyExecute":{"0":0,"25":0,"50":0,"75":0,"90":0,"95":0,"99":0,"99.5":0,"100":0},"latencyTotal_mean":0,"latencyTotal":{"0":0,"25":0,"50":0,"75":0,"90":0,"95":0,"99":0,"99.5":0,"100":0},"propertyValue_circuitBreakerRequestVolumeThreshold":20,"propertyValue_circuitBreakerSleepWindowInMilliseconds":5000,"propertyValue_circuitBreakerErrorThresholdPercentage":50,"propertyValue_circuitBreakerForceOpen":false,"propertyValue_circuitBreakerForceClosed":false,"propertyValue_circuitBreakerEnabled":true,"propertyValue_executionIsolationStrategy":"THREAD","propertyValue_executionIsolationThreadTimeoutInMilliseconds":1000,"propertyValue_executionIsolationThreadInterruptOnTimeout":true,"propertyValue_executionIsolationThreadPoolKeyOverride":null,"propertyValue_executionIsolationSemaphoreMaxConcurrentRequests":10,"propertyValue_fallbackIsolationSemaphoreMaxConcurrentRequests":10,"propertyValue_metricsRollingStatisticalWindowInMilliseconds":10000,"propertyValue_requestCacheEnabled":true,"propertyValue_requestLogEnabled":true,"reportingHosts":1}
```

