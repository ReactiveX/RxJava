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

object MultipleAssignmentSubscription {

  /**
   * Creates a [[rx.lang.scala.subscriptions.MultipleAssignmentSubscription]] that invokes the specified action when unsubscribed.
   */
  def apply(subscription: => Unit): MultipleAssignmentSubscription = {
    val m = MultipleAssignmentSubscription()
    m.subscription = Subscription{ subscription }
    m
  }

  /**
   * Creates a [[rx.lang.scala.subscriptions.MultipleAssignmentSubscription]].
   */
  def apply(): MultipleAssignmentSubscription = {
    new MultipleAssignmentSubscription(new rx.subscriptions.MultipleAssignmentSubscription())
  }
}



/**
 * Represents a [[rx.lang.scala.Subscription]] whose underlying subscription can be swapped for another subscription.
 */
class MultipleAssignmentSubscription private[scala] (override val asJavaSubscription: rx.subscriptions.MultipleAssignmentSubscription)
  extends Subscription {

  /**
   * Gets the underlying subscription.
   */
  def subscription: Subscription = Subscription(asJavaSubscription.getSubscription)

  /**
   * Gets the underlying subscription
   * @param that the new subscription
   * @return the [[rx.lang.scala.subscriptions.MultipleAssignmentSubscription]] itself.
   */
  def subscription_=(that: Subscription): this.type = {
    asJavaSubscription.setSubscription(that.asJavaSubscription)
    this
  }

  def unsubscribe(): Unit =  asJavaSubscription.unsubscribe()
  override def isUnsubscribed: Boolean = asJavaSubscription.isUnsubscribed

}


