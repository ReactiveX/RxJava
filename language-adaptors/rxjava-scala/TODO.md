
TODOs for Scala Adapter
-----------------------

This is a (probably incomplete) list of what still needs to be done in the Scala adaptor:

*    mirror complete Java package structure in Scala
*    objects for classes with static methods or singletons (e.g. Schedulers, Subscriptions)
*    Notification as a case class
*    integrating Scala Futures, should there be a common base interface for Futures and Observables?
*    Add methods present in Scala collections library, but not in RxJava, e.g. aggregate à la Scala, collect, exists, tails, ...
*    combineLatest with about arities > 2
*    decide where the MovieLib/MovieLibUsage (use Scala code from Java code) example should live and make sure gradle builds it in the right order
*    Avoid text duplication in scaladoc using templates, add examples, distinction between use case signature and full signature
*    other small TODOs


