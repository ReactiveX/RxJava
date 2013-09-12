/**
 * Copyright 2013 Netflix, Inc.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
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