package rx.lang.scalaimpl

import scala.collection.JavaConverters._

class BlockingObservable[+T](val asJava: rx.observables.BlockingObservable[_ <: T]) 
  // extends AnyVal 
{
    
  // forEach(Action1<? super T>)
  // getIterator()
  
  def last: T = {
    asJava.last()
  }
  
  // last(Func1<? super T, Boolean>)
  // lastOrDefault(T)
  // lastOrDefault(T, Func1<? super T, Boolean>)
  // mostRecent(T)
  // next()
  
  def single: T = {
    asJava.single()
  }
  
  // single(Func1<? super T, Boolean>)
  
  // def singleOption: Option[T] = { TODO  }
  // corresponds to Java's
  // singleOrDefault(T)
  
  // singleOrDefault(BlockingObservable<? extends T>, boolean, T)
  // singleOrDefault(T, Func1<? super T, Boolean>)
  // toFuture()
  
  def toIterable: Iterable[T] = {
    asJava.toIterable().asScala
  }

}