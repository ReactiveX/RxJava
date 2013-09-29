
TODOs for Scala Adapter
-----------------------

This is a (probably incomplete) list of what still needs to be done in the Scala adaptor:

*    Integrating Scala Futures: Should there be a common base interface for Futures and Observables? And if all subscribers of an Observable wrapping a Future unsubscribe, the Future should be cancelled, but Futures do not support cancellation.
*    Add methods present in Scala collections library, but not in RxJava, e.g. aggregate à la Scala, collect, tails, ...
*    combineLatest with arities > 2
*    Avoid text duplication in scaladoc using templates, add examples, distinction between use case signature and full signature
*    other small TODOs


