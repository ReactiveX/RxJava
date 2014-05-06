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
 * These functions convert between RxScala types RxJava types.
 * Pure Scala projects won't need them, but they will be useful for polyglot projects.
 * This object only contains conversions between types. For conversions between functions,
 * use [[rx.lang.scala.ImplicitFunctionConversions]].
 */
object JavaConversions {
  import language.implicitConversions
  
  implicit def toJavaNotification[T](s: Notification[T]): rx.Notification[_ <: T] = s.asJavaNotification
  
  implicit def toScalaNotification[T](s: rx.Notification[_ <: T]): Notification[T] = Notification(s)

  implicit def toJavaSubscription(s: Subscription): rx.Subscription = s.asJavaSubscription
  
  implicit def toScalaSubscription(s: rx.Subscription): Subscription = Subscription(s)

  implicit def toJavaSubscriber[T](s: Subscriber[T]): rx.Subscriber[_ >: T] = s.asJavaSubscriber
  
  implicit def toScalaSubscriber[T](s: rx.Subscriber[_ >: T]): Subscriber[T] = Subscriber(s)
  
  implicit def scalaSchedulerToJavaScheduler(s: Scheduler): rx.Scheduler = s.asJavaScheduler
  implicit def javaSchedulerToScalaScheduler(s: rx.Scheduler): Scheduler = Scheduler(s)

  implicit def scalaWorkerToJavaWorker(s: Worker): rx.Scheduler.Worker = s.asJavaWorker
  implicit def javaWorkerToScalaWorker(s: rx.Scheduler.Worker): Worker = Worker(s)


  implicit def toJavaObserver[T](s: Observer[T]): rx.Observer[_ >: T] = s.asJavaObserver
  
  implicit def toScalaObserver[T](s: rx.Observer[_ >: T]): Observer[T] = Observer(s)

  implicit def toJavaObservable[T](s: Observable[T]): rx.Observable[_ <: T] = s.asJavaObservable
  
  implicit def toScalaObservable[T](observable: rx.Observable[_ <: T]): Observable[T] = {
    new Observable[T]{
      val asJavaObservable = observable
    }
  }

  implicit def toJavaOperator[T, R](operator: Subscriber[R] => Subscriber[T]): rx.Observable.Operator[R, T] = {
    new rx.Observable.Operator[R, T] {
      override def call(subscriber: rx.Subscriber[_ >: R]): rx.Subscriber[_ >: T] = {
        toJavaSubscriber[T](operator(toScalaSubscriber[R](subscriber)))
      }
    }
  }
}