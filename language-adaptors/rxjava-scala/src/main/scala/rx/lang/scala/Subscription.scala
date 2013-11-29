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

package rx.lang.scala

/**
 * Subscriptions are returned from all `Observable.subscribe` methods to allow unsubscribing.
 *
 * This interface is the equivalent of `IDisposable` in the .NET Rx implementation.
 */
trait Subscription {
  val asJavaSubscription: rx.Subscription

  /**
   * Call this method to stop receiving notifications on the Observer that was registered when
   * this Subscription was received.
   */
  def unsubscribe(): Unit = asJavaSubscription.unsubscribe()

  /**
   * Checks if the subscription is unsubscribed.
   */
  def isUnsubscribed: Boolean
}







