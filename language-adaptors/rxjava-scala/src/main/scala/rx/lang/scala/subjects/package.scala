package rx.lang.scala

/**
 * Provides the type `Subject`.
 */
package object subjects {

  /**
   * A Subject is an Observable and an Observer at the same time. 
   * 
   * The Java Subject looks like this: 
   * {{{
   * public abstract class Subject<T,R> extends Observable<R> implements Observer<T>
   * }}}
   */
  type Subject[-T, +R] = rx.subjects.Subject[_ >: T, _ <: R]

  // For nicer scaladoc, we would like to present something like this:
  /*
  trait Observable[+R] {}
  trait Observer[-T] {}
  trait Subject[-T, +R] extends Observable[R] with Observer[T] { }
  */
  
  // We don't make aliases to these types, because they are considered internal/not needed by users:
  // rx.subjects.AsyncSubject
  // rx.subjects.BehaviorSubject
  // rx.subjects.PublishSubject
  // rx.subjects.ReplaySubject

}