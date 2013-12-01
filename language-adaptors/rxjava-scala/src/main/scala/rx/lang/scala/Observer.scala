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

import rx.joins.ObserverBase

/**
 Provides a mechanism for receiving push-based notifications.
*
* After an Observer calls an [[rx.lang.scala.Observable]]'s `subscribe` method, the Observable
* calls the Observer's `onNext` method to provide notifications. A well-behaved Observable will
* call an Observer's `onCompleted` method exactly once or the Observer's `onError` method exactly once.
*/
trait Observer[-T] {

  private [scala] def asJavaObserver: rx.Observer[_ >: T] = new ObserverBase[T] {
    protected def onCompletedCore(): Unit = onCompleted()
    protected def onErrorCore(error: Throwable): Unit = onError(error)
    protected def onNextCore(value: T): Unit = onNext(value)
  }

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
   * Assume that the underlying rx.Observer does not need to be wrapped
   */
  private [scala] def apply[T](observer: rx.Observer[T]) : Observer[T] = {
     new Observer[T]() {

       override def asJavaObserver = observer

       def onCompleted(): Unit = asJavaObserver.onCompleted()
       def onError(error: Throwable): Unit = asJavaObserver.onError(error)
       def onNext(value: T): Unit = asJavaObserver.onNext(value)
     }
   }
}