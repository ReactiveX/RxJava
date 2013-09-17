package rx.lang.scala

package object subjects {

  // in Java: public abstract class Subject<T,R> extends Observable<R> implements Observer<T>
  type Subject[-T, +R] = rx.subjects.Subject[_ >: T, _ <: R]

  // TODO (including static methods of these classes)
  // rx.subjects.AsyncSubject
  // rx.subjects.BehaviorSubject
  // rx.subjects.PublishSubject
  // rx.subjects.ReplaySubject

}