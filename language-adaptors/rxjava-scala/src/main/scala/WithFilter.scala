package rx.lang.scala

// Cannot yet have inner class because of this error message:
// "implementation restriction: nested class is not allowed in value class.
// This restriction is planned to be removed in subsequent releases."
private[scala] class WithFilter[+T] (p: T => Boolean, asJava: rx.Observable[_ <: T]) {

  import ImplicitFunctionConversions._

  def map[B](f: T => B): Observable[B] = {
    Observable[B](asJava.filter(p).map[B](f))
  }

  def flatMap[B](f: T => Observable[B]): Observable[B] = {
    Observable[B](asJava.filter(p).flatMap[B]((x: T) => f(x).asJavaObservable))
  }

  def withFilter(q: T => Boolean): Observable[T] = {
    Observable[T](asJava.filter((x: T) => p(x) && q(x)))
  }

  // there is no foreach here, that's only available on BlockingObservable
}
