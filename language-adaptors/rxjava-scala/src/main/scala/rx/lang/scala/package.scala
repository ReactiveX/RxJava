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
package rx.lang


/*
 * This object contains aliases to all types Scala users need to import.
 * Note that:
 * -  Scala users cannot use Java's type with variance without always using writing
 *    e.g. rx.Notification[_ <: T], so we create aliases fixing the variance
 * -  For consistency, we create aliases for all types
 * -  Type aliases cannot be at top level, they have to be inside an object or class
 */
package object scala {

  type Notification[+T] = rx.Notification[_ <: T]
  object Notification {
    def apply[T](): Notification[T] = new rx.Notification()
    def apply[T](value: T): Notification[T] = new rx.Notification(value)
    def apply[T](t: Throwable): Notification[T] = new rx.Notification(t)
  }
  
  type Observer[-T] = rx.Observer[_ >: T]  
  type Scheduler = rx.Scheduler
  type Subscription = rx.Subscription
  
}

/*

TODO make aliases for these types because:
* those which are covariant or contravariant do need an alias to get variance correct
* the others for consistency

rx.observables.BlockingObservable
rx.observables.ConnectableObservable
rx.observables.GroupedObservable

rx.plugins.RxJavaErrorHandler
rx.plugins.RxJavaObservableExecutionHook
rx.plugins.RxJavaPlugins

rx.subscriptions.BooleanSubscription
rx.subscriptions.CompositeSubscription
rx.subscriptions.Subscriptions

*/

