package rx.lang.scala

import rx.lang.scala.Observer

/**
* A Subject is an Observable and an Observer at the same time.
*/
trait Subject[-T, +R] extends Observable[R] with Observer[T] {
  val asJavaSubject: rx.subjects.Subject[_ >: T, _<: R]
  def asJavaObservable: rx.Observable[_ <: R] = asJavaSubject
  def asJavaObserver: rx.Observer[_ >: T] = asJavaSubject
}

