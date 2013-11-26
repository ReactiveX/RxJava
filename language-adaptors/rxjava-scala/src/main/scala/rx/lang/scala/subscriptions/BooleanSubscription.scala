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
package rx.lang.scala.subscriptions

import rx.lang.scala._

object BooleanSubscription {

  /**
   * Creates a [[rx.lang.scala.subscriptions.BooleanSubscription]].
   */
  def apply(): BooleanSubscription =  {
    new BooleanSubscription(new rx.subscriptions.BooleanSubscription())
  }

  /**
   * Creates a [[rx.lang.scala.subscriptions.BooleanSubscription]] that invokes the specified action when unsubscribed.
   */
  def apply(u: => Unit): BooleanSubscription = {
    new BooleanSubscription(new rx.subscriptions.BooleanSubscription {
      override def unsubscribe(): Unit = {
        if(!super.isUnsubscribed()) {
            u
            super.unsubscribe()
        }
      }
    })
  }
}

/**
 * Represents a [[rx.lang.scala.Subscription]] that can be checked for status.
 */
class BooleanSubscription private[scala] (val asJavaSubscription: rx.subscriptions.BooleanSubscription)
  extends Subscription {

  /**
   * Checks whether the subscription has been unsubscribed.
   */
  def isUnsubscribed: Boolean = asJavaSubscription.isUnsubscribed

}
