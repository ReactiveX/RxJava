package rx.lang.scala.observables

import scala.collection.JavaConverters._
import rx.lang.scala.internal.ImplicitFunctionConversions._

class BlockingObservable[+T](val asJava: rx.observables.BlockingObservable[_ <: T]) 
  extends AnyVal 
{

  def foreach(f: T => Unit): Unit = {
    asJava.forEach(f)
  }

  def last: T = {
    asJava.last() : T // useless ascription because of compiler bug
  }

  // last(Func1<? super T, Boolean>)
  // lastOrDefault(T)
  // lastOrDefault(T, Func1<? super T, Boolean>)
  // mostRecent(T)
  // next()

  def single: T = {
    asJava.single() : T // useless ascription because of compiler bug
  }
  
  // single(Func1<? super T, Boolean>)
  
  // def singleOption: Option[T] = { TODO  }
  // corresponds to Java's
  // singleOrDefault(T)
  
  // singleOrDefault(BlockingObservable<? extends T>, boolean, T)
  // singleOrDefault(T, Func1<? super T, Boolean>)
  // toFuture()
  
  def toIterable: Iterable[T] = {
    asJava.toIterable().asScala : Iterable[T] // useless ascription because of compiler bug
  }
  
  def toList: List[T] = {
    asJava.toIterable().asScala.toList : List[T] // useless ascription because of compiler bug
  }

}