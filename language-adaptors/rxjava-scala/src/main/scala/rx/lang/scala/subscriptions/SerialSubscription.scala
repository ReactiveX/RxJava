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

object SerialSubscription {

  /**
   * Creates a [[rx.lang.scala.subscriptions.SerialSubscription]].
   */
  def apply(): SerialSubscription =  new SerialSubscription(new rx.subscriptions.SerialSubscription())

  /**
   * Creates a [[rx.lang.scala.subscriptions.SerialSubscription]] that invokes the specified action when unsubscribed.
   */
  def apply(unsubscribe: => Unit): SerialSubscription = {
   SerialSubscription().subscription = Subscription(unsubscribe)
  }
}

/**
 * Represents a [[rx.lang.scala.Subscription]] that can be checked for status.
 */
class SerialSubscription private[scala] (serial: rx.subscriptions.SerialSubscription) extends Subscription {

  /*
  * As long as rx.subscriptions.SerialSubscription has no isUnsubscribed,
  * we need to intercept and do it ourselves.
   */
  val asJavaSubscription: rx.subscriptions.SerialSubscription = new rx.subscriptions.SerialSubscription() {
    override def unsubscribe(): Unit = {
      if(unsubscribed.compareAndSet(false, true)) { serial.unsubscribe() }
    }
    override def setSubscription(subscription: rx.Subscription): Unit = serial.setSubscription(subscription)
    override def getSubscription(): rx.Subscription = serial.getSubscription()
  }

  def subscription_=(value: Subscription): this.type = { asJavaSubscription.setSubscription(value.asJavaSubscription); this }
  def subscription: Subscription = Subscription(asJavaSubscription.getSubscription)

}

