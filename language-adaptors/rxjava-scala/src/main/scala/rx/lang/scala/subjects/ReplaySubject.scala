package rx.lang.scala.subjects

import rx.lang.scala.JavaWrapper

trait ReplaySubject[T] extends Subject[T, T] with JavaWrapper[rx.subjects.ReplaySubject[T]] {}  

object ReplaySubject {
  private[ReplaySubject] class ReplaySubjectWrapper[T](val asJava: rx.subjects.ReplaySubject[T]) extends ReplaySubject[T] {}
  
  def apply[T](): ReplaySubject[T] = {
    new ReplaySubjectWrapper[T](rx.subjects.ReplaySubject.create())
  }
}



