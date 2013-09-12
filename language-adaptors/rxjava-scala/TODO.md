
TODOs for Scala Adapter
-----------------------

This is a (probably incomplete) list of what still needs to be done in the Scala adaptor:

- [ ] ConnectableObservable: Implement adaptor. Note that it cannot extend Scala Observable, since value classes are final.
- [ ] more methods of BlockingObservable
- [ ] multicast, publish, replay once we have ConnectableObservable
- [ ] groupBy and GroupedObservable
- [ ] mirror complete Java package structure in Scala
- [ ] convert Java futures to Scala futures
- [ ] Add methods present in Scala collections library, but not in RxJava, e.g. zipWithIndex, aggregate à la Scala
- [ ] mergeDelayError, combineLatest, merge, concat, zip: decide if instance method or static or both, decide about arities > 2
- [ ] naming: switch() or switchOnNext()?
- [ ] decide where the MovieLib/MovieLibUsage (use Scala code from Java code) example should live and make sure gradle builds it in the right order
- [ ] Avoid text duplication in scaladoc using templates, add examples, distinction between use case signature and full signature
- [ ] other small TODOs


