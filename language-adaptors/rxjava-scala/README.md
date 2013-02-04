# Scala Adaptor for RxJava


This adaptor allows 'fn' functions to be used and RxJava will know how to invoke them.

This enables code such as:

```scala
Observable.toObservable("1", "2", "3")
   .take(2)
   .subscribe(Map(
      "onNext" -> ((callback: String) => {
          print(callback)
       })
   ))
```
