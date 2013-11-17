package rx.lang.scala.subjects

import rx.lang.scala.JavaWrapper

trait BehaviorSubject[T] extends Subject[T, T] with JavaWrapper[rx.subjects.BehaviorSubject[T]] {}  

object BehaviorSubject {
  private[BehaviorSubject] class BehaviorSubjectWrapper[T](val asJava: rx.subjects.BehaviorSubject[T]) extends BehaviorSubject[T] {}
  
  def apply[T](value: T): BehaviorSubject[T] = {
    new BehaviorSubjectWrapper[T](rx.subjects.BehaviorSubject.createWithDefaultValue(value))
  }
}



