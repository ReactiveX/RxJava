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

import ImplicitFunctionConversions.scalaBooleanFunction1ToRxBooleanFunc1
import ImplicitFunctionConversions.scalaFunction1ToRxFunc1

// Cannot yet have inner class because of this error message:
// "implementation restriction: nested class is not allowed in value class.
// This restriction is planned to be removed in subsequent releases."
private[scala] class WithFilter[+T] (p: T => Boolean, asJava: rx.Observable[_ <: T]) {

  import ImplicitFunctionConversions._
  import JavaConversions._

  def map[B](f: T => B): Observable[B] = {
    toScalaObservable[B](asJava.filter(p).map[B](f))
  }

  def flatMap[B](f: T => Observable[B]): Observable[B] = {
    toScalaObservable[B](asJava.filter(p).flatMap[B]((x: T) => f(x).asJavaObservable))
  }

  def withFilter(q: T => Boolean): Observable[T] = {
    toScalaObservable[T](asJava.filter((x: T) => p(x) && q(x)))
  }

  def foreach(onNext: T => Unit): Unit = {
    toScalaObservable[T](asJava.filter(p)).foreach(onNext)
  }
}
