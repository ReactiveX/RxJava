package rx.lang.scala.subjects

import rx.lang.scala.JavaWrapper

trait AsyncSubject[T] extends Subject[T, T] with JavaWrapper[rx.subjects.AsyncSubject[T]] {}  


object AsyncSubject {
  private[AsyncSubject] class AsyncSubjectWrapper[T](val asJava: rx.subjects.AsyncSubject[T]) extends AsyncSubject[T] {}
  
  def apply[T](): AsyncSubject[T] = {
    new AsyncSubjectWrapper[T](rx.subjects.AsyncSubject.create())
  }
}

