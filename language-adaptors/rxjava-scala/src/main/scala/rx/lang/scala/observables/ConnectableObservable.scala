package rx.lang.scala.observables


class ConnectableObservable[+T](val asJava: rx.observables.ConnectableObservable[_ <: T]) extends AnyVal {
  import rx.lang.scala.All._
  import rx.lang.scala.All.util._
  import rx.{Observable => JObservable}
  import rx.lang.scala.ImplicitFunctionConversions._
  
}