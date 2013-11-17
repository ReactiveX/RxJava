package rx.lang.scala.subjects

import rx.lang.scala.{Observable, Observer, JavaWrapper}

/**
 * A Subject is an Observable and an Observer at the same time.
 */
trait Subject[-T, +R] extends Observer[T] with Observable[R] with JavaWrapper[rx.subjects.Subject[_ >: T, _ <: R]] {}

