
TODOs for Scala Adapter
-----------------------

This is a (probably incomplete) list of what still needs to be done in the Scala adaptor.

TODOs which came up at the meeting with Erik Meijer on 2013-10-11:

*    Rename the factory methods in `object Observable`, considering that the most important is the one taking an `Observer => Subscription` function (the "king" according to Erik). Thunk to Subscription conversion (?), also consider Jason's [comments](https://github.com/Netflix/RxJava/commit/c1596253fc5567b7cc37d20128374d189471ff79). A useful trick might also be to have `apply(T, T, T*)` instead of just `apply(T*)`.
*    Factory methods for observables and instance methods should take implicit scheduler, default is different per method (Isn't this a contradiction? In other words: If I call a method without providing a scheduler, should the default scheduler be used or the implicit that I provided?) Find in .NET source the list of which schedulers goes with which operators by default. If no other default, use NewThreadScheduler. Note that there are methods in Scala Observable which should have an overload taking a Scheduler, but do not yet have it! Also remember Erik saying that he would like to "minimize magically injected concurrency".
*    Bring `BooleanSubscription`, `CompositeSubscription`, `MultipleAssignmentSubscription` to Scala, `compositeSubscription.subscription = ...`instead of setter method, add on `CompositeSubscription` should be `+=`
*    Convert executor to scheduler
*    Java Scheduler methods take `state` arguments (they were needed to schedule work on a different machine, but are now considered a bad idea). Remove these `state` arguments from all Scala schedulers.
*    Check if TestScheduler added in 0.14.3 is sufficient
*    Infinite Iterables: the java Observable.from version unfolds an iterable, even it is infinite. Should be fixed in java.
*    subscribe methods: There are many overloads, but still not all combinations one might need. Make this nicer and complete, maybe using default arguments. Also try to make sure that `subscribe(println)` works, not only `subscribe(println(_))`. `foreach(println)` works on collections, but not on `subscribe(println)`, because subscribe is overloaded.
*    Currently all Swing examples use Swing directly, without using the Scala wrappers around Swing. Make sure that everything is nice if the Scala wrappers around Swing are used, eg `Reaction { case clicked: ButtonClicked => … }` -- avoid default case, check out the hooks for partial function applyOrElse to avoid double pattern matching
*    There are no examples yet using `async`, but `async` will be used in the course. Write examples and check if everything works as expected when combined with `async`.
*    Futures: For the moment, just add a Future->Observable converter method to `object Observable`. Later, think if `Future[T] extends Observable[T]`.
*    Operator `delay`: Once Erik has commented on [this](https://github.com/Netflix/RxJava/pull/384), make sure this operator is added accordingly to RxJava and then to RxScala
*    add wrappers or aliases for `AsyncSubject<T>`, `BehaviorSubject<T>`, `PublishSubject<T>`, and `ReplaySubject<T>`
*    go through Erik's code that he showed at the meeting and check if everything can now be done nicely
*    get Erik's slides from the course and check if they are compatible with the library

Some more TODOs:

*    Integrating Scala Futures: Should there be a common base interface for Futures and Observables? And if all subscribers of an Observable wrapping a Future unsubscribe, the Future should be cancelled, but Futures do not support cancellation.
*    Add methods present in Scala collections library, but not in RxJava, e.g. aggregate à la Scala, collect, tails, ...
*    combineLatest with arities > 2
*    Avoid text duplication in scaladoc using templates, add examples, distinction between use case signature and full signature
*    other small TODOs in source code
*    always: keep Scala Observable in sync with Java Observable


### Appendix:

`println` example:

    List(1, 2).foreach(println)
    Observable(1, 2).toBlockingObservable.foreach(println)

    Observable(1, 2).subscribe(println) // does not work
    
    class Ob[+T] {
      def subscribe(onNext: T => Unit) = ???
    }
    val o2 = new Ob[Int]
    o2.subscribe(println) // works



