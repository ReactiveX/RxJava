# Clojure Adaptor for RxJava

This adaptor provides functions and macros to ease Clojure/RxJava interop. In particular, there are functions and macros for turning Clojure functions and code into RxJava `Func*` and `Action*` interfaces without the tedium of manually reifying the interfaces.

# Basic Usage

## Requiring the interop namespace
The first thing to do is to require the namespace:

```clojure
(ns my.namespace
  (:require [rx.lang.clojure.interop :as rx])
  (:import [rx Observable]))
```

or, at the REPL:

```clojure
(require '[rx.lang.clojure.interop :as rx])
```

## Using rx/fn
Once the namespace is required, you can use the `rx/fn` macro anywhere RxJava wants a `rx.util.functions.Func` object. The syntax is exactly the same as `clojure.core/fn`:

```clojure
(-> my-observable
    (.map (rx/fn [v] (* 2 v))))
```

If you already have a plain old Clojure function you'd like to use, you can pass it to the `rx/fn*` function to get a new object that implements `rx.util.functions.Func`:

```clojure
(-> my-numbers
    (.reduce (rx/fn* +)))
```

## Using rx/action
The `rx/action` macro is identical to `rx/fn` except that the object returned implements `rx.util.functions.Action` interfaces. It's used in `subscribe` and other side-effect-y contexts:

```clojure
(-> my-observable
    (.map (rx/fn* transform-data))
    (.finallyDo (rx/action [] (println "Finished transform")))
    (.subscribe (rx/action [v] (println "Got value" v))
                (rx/action [e] (println "Get error" e))
                (rx/action [] (println "Sequence complete"))))
```

## Using Observable/create
As of 0.17, `rx.Observable/create` takes an implementation of `rx.Observable$OnSubscribe` which is basically an alias for `rx.util.functions.Action1` that takes an `rx.Subscriber` as its argument. Thus, you can just use `rx/action` when creating new observables:

```clojure
; A simple observable that emits 0..9 taking unsubscribe into account
(Observable/create (rx/action [^rx.Subscriber s]
                     (loop [i 0]
                       (when (and (< i 10) (.isUnsubscribed s))
                         (.onNext s i)
                         (recur (inc i))))
                     (.onCompleted s)))
```

# Gotchas
Here are a few things to keep in mind when using this interop:

* Keep in mind the (mostly empty) distinction between `Func` and `Action` and which is used in which contexts
* If there are multiple Java methods overloaded by `Func` arity, you'll need to use a type hint to let the compiler know which one to choose.
* Methods that take a predicate (like filter) expect the predicate to return a boolean value. A function that returns a non-boolean value will result in a `ClassCastException`.

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
