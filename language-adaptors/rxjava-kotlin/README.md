# Kotlin Adaptor for RxJava

Kotlin has support for SAM (Single Abstract Method) Interfaces as Functions (i.e. Java 8 Lambdas). So you could use Kotlin in RxJava whitout this adaptor

```kotlin
Observable.create(OnSubscribeFunc<String> { observer ->
    observer!!.onNext("Hello")
    observer.onCompleted()
    Subscriptions.empty()
})!!.subscribe { result ->
    a!!.received(result)
}
```

In RxJava [0.17.0](https://github.com/Netflix/RxJava/releases/tag/0.17.0) version a new Subscriber type was included

```kotlin
Observable.create(object:OnSubscribe<String> {
    override fun call(subscriber: Subscriber<in String>?) {
        subscriber!!.onNext("Hello")
        subscriber.onCompleted()
    }
})!!.subscribe { result ->
    a!!.received(result)
}
```

(Due to a [bug in Kotlin's compiler](http://youtrack.jetbrains.com/issue/KT-4753) you can't use SAM with OnSubscribe)

This adaptor exposes a set of Extension functions that allow a more idiomatic Kotlin usage

```kotlin
{(subscriber: Subscriber<in String>) ->
    subscriber.onNext("Hello")
    subscriber.onCompleted()
}.asObservable().subscribe { result ->
    a!!.received(result)
}
```

## Binaries

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

and for Gradle:

```groovy
compile 'com.netflix.rxjava:rxjava-kotlin:x.y.z'
```