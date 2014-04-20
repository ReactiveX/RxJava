Clojure bindings for RxJava.

# Binaries

Binaries and dependency information for Maven, Ivy, Gradle and others can be found at [http://search.maven.org](http://search.maven.org/#search%7Cga%7C1%7Ca%3A%22rxjava-clojure%22).

Example for Leiningen:

```clojure
[com.netflix.rxjava/rxjava-clojure "x.y.z"]
```

and for Gradle:

```groovy
compile 'com.netflix.rxjava:rxjava-clojure:x.y.z'
```

and for Maven:

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

# Clojure Bindings
This library provides convenient, idiomatic Clojure bindings for RxJava.

The bindings try to present an API that will be comfortable and familiar to a Clojure programmer that's familiar with the sequence operations in `clojure.core`. It "fixes" several issues with using RxJava with raw Java interop, for example:

* Argument lists are in the "right" order. So in RxJava, the function applied in `Observable.map` is the second argument, while here it's the first argument with one or more Observables as trailing arguments
* Operators take normal Clojure functions as arguments, bypassing need for the interop described below
* Predicates accomodate Clojure's notion of truth
* Operators are generally names as they would be in `clojure.core` rather than the Rx names

There is no object wrapping going on. That is, all functions return normal `rx.Observable` objects, so you can always drop back to Java interop for anything that's missing in this wrapper.

## Basic Usage
Most functionality resides in the `rx.lang.clojure.core` namespace and for the most part looks like normal Clojure sequence manipulation:

```clojure
(require '[rx.lang.clojure.core :as rx])

(->> my-observable
     (rx/map (comp clojure.string/lower-case :first-name))
     (rx/map clojure.string/lower-case)
     (rx/filter #{"bob"})
     (rx/distinct)
     (rx/into []))
;=> An Observable that emits a single vector of names
```

Blocking operators, which are useful for testing, but should otherwise be avoided, reside in `rx.lang.clojure.blocking`. For example:

```clojure
(require '[rx.lang.clojure.blocking :as rxb])

(rxb/doseq [{:keys [first-name]} users-observable]
  (println "Hey," first-name))
;=> nil
```

## Open Issues

* The missing stuff mentioned below
* `group-by` val-fn variant isn't implemented in RxJava
* There are some functions for defining customer Observables and Operators (`subscriber`, `operator*`, `observable*`). I don't think these are really enough for serious operator implementation, but I'm hesitant to guess at an abstraction at this point. These will probably change dramatically.

## What's Missing
This library is an ongoing work in progress driven primarily by the needs of one team at Netflix. As such some things are currently missing:

* Highly-specific operators that we felt cluttered the API and were easily composed from existing operators, especially since we're in not-Java land. For example, `Observable.sumLong()`.
* Most everything involving schedulers
* Most everything involving time
* `Observable.window` and `Observable.buffer`. Who knows which parts of these beasts to wrap?

Of course, contributions that cover these cases are welcome.

# Low-level Interop
This adaptor provides functions and macros to ease Clojure/RxJava interop. In particular, there are functions and macros for turning Clojure functions and code into RxJava `Func*` and `Action*` interfaces without the tedium of manually reifying the interfaces.

## Basic Usage

### Requiring the interop namespace
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

### Using rx/fn
Once the namespace is required, you can use the `rx/fn` macro anywhere RxJava wants a `rx.functions.Func` object. The syntax is exactly the same as `clojure.core/fn`:

```clojure
(-> my-observable
    (.map (rx/fn [v] (* 2 v))))
```

If you already have a plain old Clojure function you'd like to use, you can pass it to the `rx/fn*` function to get a new object that implements `rx.functions.Func`:

```clojure
(-> my-numbers
    (.reduce (rx/fn* +)))
```

### Using rx/action
The `rx/action` macro is identical to `rx/fn` except that the object returned implements `rx.functions.Action` interfaces. It's used in `subscribe` and other side-effect-y contexts:

```clojure
(-> my-observable
    (.map (rx/fn* transform-data))
    (.finallyDo (rx/action [] (println "Finished transform")))
    (.subscribe (rx/action [v] (println "Got value" v))
                (rx/action [e] (println "Get error" e))
                (rx/action [] (println "Sequence complete"))))
```

### Using Observable/create
As of 0.17, `rx.Observable/create` takes an implementation of `rx.Observable$OnSubscribe` which is basically an alias for `rx.functions.Action1` that takes an `rx.Subscriber` as its argument. Thus, you can just use `rx/action` when creating new observables:

```clojure
; A simple observable that emits 0..9 taking unsubscribe into account
(Observable/create (rx/action [^rx.Subscriber s]
                     (loop [i 0]
                       (when (and (< i 10) (.isUnsubscribed s))
                         (.onNext s i)
                         (recur (inc i))))
                     (.onCompleted s)))
```

## Gotchas
Here are a few things to keep in mind when using this interop:

* Keep in mind the (mostly empty) distinction between `Func` and `Action` and which is used in which contexts
* If there are multiple Java methods overloaded by `Func` arity, you'll need to use a type hint to let the compiler know which one to choose.
* Methods that take a predicate (like filter) expect the predicate to return a boolean value. A function that returns a non-boolean value will result in a `ClassCastException`.

