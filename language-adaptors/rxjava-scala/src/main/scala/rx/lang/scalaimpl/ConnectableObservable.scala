package rx.lang.scalaimpl


class ConnectableObservable[+T](val asJava: rx.observables.ConnectableObservable[_ <: T]) extends AnyVal {
  import rx.lang.scala._
  import rx.lang.scala.util._
  import rx.{Observable => JObservable}
  import rx.lang.scalaimpl.ImplicitFunctionConversions._
  
}