/**
 * Copyright 2013 Netflix, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package rx.lang.scala

/**
 Provides a mechanism for receiving push-based notifications.
*
* After an Observer calls an [[rx.lang.scala.Observable]]'s `subscribe` method, the Observable
* calls the Observer's `onNext` method to provide notifications. A well-behaved Observable will
* call an Observer's `onCompleted` method exactly once or the Observer's `onError` method exactly once.
*/
trait Observer[-T] {

  private [scala] val asJavaObserver: rx.Observer[_ >: T]

 /**
 * Provides the Observer with new data.
 *
 * The [[rx.lang.scala.Observable]] calls this closure 0 or more times.
 *
 * The [[rx.lang.scala.Observable]] will not call this method again after it calls either `onCompleted` or `onError`.
 */
  def onNext(value: T): Unit

  /**
  * Notifies the Observer that the [[rx.lang.scala.Observable]] has experienced an error condition.
  *
  * If the [[rx.lang.scala.Observable]] calls this method, it will not thereafter call `onNext` or `onCompleted`.
  */
  def onError(error: Throwable): Unit

  /**
   * Notifies the Observer that the [[rx.lang.scala.Observable]] has finished sending push-based notifications.
   *
   * The [[rx.lang.scala.Observable]] will not call this method if it calls `onError`.
   */
  def onCompleted(): Unit

}

object Observer {

  /**
   * Scala calls XXX; Java receives XXX.
   */
  private [scala] def apply[T](observer: rx.Observer[T]) : Observer[T] = {
     new Observer[T] {

       val asJavaObserver = observer

       def onNext(value: T): Unit = asJavaObserver.onNext(value)
       def onError(error: Throwable): Unit = asJavaObserver.onError(error)
       def onCompleted(): Unit = asJavaObserver.onCompleted()
     }

   }

  def apply[T](                                                                ): Observer[T] = apply[T]((v:T)=>(), (e: Throwable)=>(), ()=>())
  def apply[T](onNext: T=>Unit                                                 ): Observer[T] = apply[T](onNext, (e: Throwable)=>(), ()=>())
  def apply[T](onNext: T=>Unit, onError: Throwable=>Unit                       ): Observer[T] = apply[T](onNext, onError, ()=>())
  def apply[T](onNext: T=>Unit,                           onCompleted: ()=>Unit): Observer[T] = apply[T](onNext, (e: Throwable)=>(), onCompleted)
  def apply[T](n: T=>Unit, e: Throwable=>Unit, c: ()=>Unit): Observer[T] = {
      // Java calls XXX; Scala receives XXX.
      Observer(new rx.Observer[T]{
         def onNext(value: T): Unit = n(value)
         def onError(error: Throwable): Unit = e(error)
         def onCompleted(): Unit = c()
       })
  }
}