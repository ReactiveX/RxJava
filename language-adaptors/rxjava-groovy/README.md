# Groovy Adaptor for RxJava


This adaptor allows 'groovy.lang.Closure' functions to be used and RxJava will know how to invoke them.

This enables code such as:

```groovy
  Observable.toObservable("one", "two", "three")
    .take(2) 
    .subscribe({arg -> println(arg)})
```
