package rx.lang.scala


class ConnectableObservable[+T](val asJava: rx.observables.ConnectableObservable[_ <: T]) {
  import All._
  import All.util._
  import rx.{Observable => JObservable}
  import ImplicitFunctionConversions._
  
}