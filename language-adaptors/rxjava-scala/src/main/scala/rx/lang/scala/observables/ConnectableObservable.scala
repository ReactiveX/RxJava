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

import rx.lang.scala.{Observable, Subscription}
import rx.lang.scala.JavaConversions._

class ConnectableObservable[+T] private[scala](val asJavaObservable: rx.observables.ConnectableObservable[_ <: T])
  extends Observable[T] {

  /**
   * Call a ConnectableObservable's connect method to instruct it to begin emitting the
   * items from its underlying [[rx.lang.scala.Observable]] to its [[rx.lang.scala.Observer]]s.
   */
  def connect: Subscription = toScalaSubscription(asJavaObservable.connect())

  /**
   * Returns an observable sequence that stays connected to the source as long
   * as there is at least one subscription to the observable sequence.
   *
   * @return a [[rx.lang.scala.Observable]]
   */
  def refCount: Observable[T] = toScalaObservable[T](asJavaObservable.refCount())
}
