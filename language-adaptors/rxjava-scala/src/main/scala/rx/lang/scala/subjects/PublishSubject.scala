package rx.lang.scala.subjects

import rx.lang.scala.JavaWrapper

trait PublishSubject[T] extends Subject[T, T] with JavaWrapper[rx.subjects.PublishSubject[T]] {}  

object PublishSubject {
  private[PublishSubject] class PublishSubjectWrapper[T](val asJava: rx.subjects.PublishSubject[T]) extends PublishSubject[T] {}
  
  def apply[T](): PublishSubject[T] = {
    new PublishSubjectWrapper[T](rx.subjects.PublishSubject.create())
  }
}

