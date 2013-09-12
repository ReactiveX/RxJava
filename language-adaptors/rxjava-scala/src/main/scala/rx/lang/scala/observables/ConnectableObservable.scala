package rx.lang.scala.observables


class ConnectableObservable[+T](val asJava: rx.observables.ConnectableObservable[_ <: T]) extends AnyVal {
  import rx.lang.scala._
  import rx.lang.scala.util._
  import rx.{Observable => JObservable}
  import rx.lang.scala.internal.ImplicitFunctionConversions._
  
}