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
