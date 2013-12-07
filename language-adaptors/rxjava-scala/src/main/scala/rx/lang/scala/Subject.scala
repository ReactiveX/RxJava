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
* A Subject is an Observable and an Observer at the same time.
*/
trait Subject[T] extends Observable[T] with Observer[T] {
  private [scala] val asJavaSubject: rx.subjects.Subject[_ >: T, _<: T]

  val asJavaObservable: rx.Observable[_ <: T] = asJavaSubject

  val asJavaObserver: rx.Observer[_ >: T] = asJavaSubject
  def onNext(value: T): Unit = { asJavaObserver.onNext(value)}
  def onError(error: Throwable): Unit = { asJavaObserver.onError(error)  }
  def onCompleted() { asJavaObserver.onCompleted() }
}

object Subject {
  def apply[T](): Subject[T] = new rx.lang.scala.subjects.PublishSubject[T](rx.subjects.PublishSubject.create())
}





