package rx.lang.scala.subjects

import rx.lang.scala.Subject

object BehaviorSubject {
  def apply[T](value: T): BehaviorSubject[T] = {
    new BehaviorSubject[T](rx.subjects.BehaviorSubject.createWithDefaultValue(value))
  }
}

class BehaviorSubject[T] private[scala] (val asJavaSubject: rx.subjects.BehaviorSubject[T]) extends Subject[T,T]  {}




