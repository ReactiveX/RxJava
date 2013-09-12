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
 * This is the old Scala adaptor. It is kept here for backwards compatibility.
 * The new adaptor is {@code rx.lang.scala.Observable}.
 */
@deprecated("use rx.lang.scala.Observable instead", "0.14")
object RxImplicits {
    import java.{ lang => jlang }
    import language.implicitConversions

    import rx.{ Observable, Observer, Subscription }
    import rx.Observable.OnSubscribeFunc
    import rx.observables.BlockingObservable
    import rx.util.functions._
    
    /**
     * Converts 0-arg function to Rx Action0
     */
    implicit def scalaFunction0ProducingUnitToAction0(f: (() => Unit)): Action0 =
        new Action0 {
            def call(): Unit = f()
        }
    
    /**
     * Converts 1-arg function to Rx Action1
     */
    implicit def scalaFunction1ProducingUnitToAction1[A](f: (A => Unit)): Action1[A] =
        new Action1[A] {
            def call(a: A): Unit = f(a)
        }
    
    /**
     * Converts 1-arg predicate to Rx Func1[A, java.lang.Boolean]
     */
    implicit def scalaBooleanFunction1ToRxBooleanFunc1[A](f: (A => Boolean)): Func1[A, jlang.Boolean] =
        new Func1[A, jlang.Boolean] {
            def call(a: A): jlang.Boolean = f(a).booleanValue
        }
    
    /**
     * Converts a specific function shape (used in takeWhile) to the equivalent Java types with an Rx Func2
     */
    implicit def convertTakeWhileFuncToRxFunc2[A](f: (A, Int) => Boolean): Func2[A, jlang.Integer, jlang.Boolean] =
        new Func2[A, jlang.Integer, jlang.Boolean] {
            def call(a: A, b: jlang.Integer): jlang.Boolean = f(a, b).booleanValue
        }
    
    /**
     * Converts a function shaped like compareTo into the equivalent Rx Func2
     */
    implicit def convertComparisonFuncToRxFunc2[A](f: (A, A) => Int): Func2[A, A, jlang.Integer] =
        new Func2[A, A, jlang.Integer] {
            def call(a1: A, a2: A): jlang.Integer = f(a1, a2).intValue
        }
    
    /*
     * This implicit allows Scala code to use any exception type and still work
     * with invariant Func1 interface
     */
    implicit def exceptionFunction1ToRxExceptionFunc1[A <: Exception, B](f: (A => B)): Func1[Exception, B] =
        new Func1[Exception, B] {
            def call(ex: Exception): B = f(ex.asInstanceOf[A])
        }
    
    /**
     * The following implicits convert functions of different arities into the Rx equivalents
     */
    implicit def scalaFunction0ToRxFunc0[A](f: () => A): Func0[A] =
        new Func0[A] {
            def call(): A = f()
        }

    implicit def scalaFunction1ToRxFunc1[A, B](f: (A => B)): Func1[A, B] =
        new Func1[A, B] {
            def call(a: A): B = f(a)
        }

    implicit def scalaFunction2ToRxFunc2[A, B, C](f: (A, B) => C): Func2[A, B, C] =
        new Func2[A, B, C] {
            def call(a: A, b: B) = f(a, b)
        }

    implicit def scalaFunction3ToRxFunc3[A, B, C, D](f: (A, B, C) => D): Func3[A, B, C, D] =
        new Func3[A, B, C, D] {
            def call(a: A, b: B, c: C) = f(a, b, c)
        }

    implicit def scalaFunction4ToRxFunc4[A, B, C, D, E](f: (A, B, C, D) => E): Func4[A, B, C, D, E] =
        new Func4[A, B, C, D, E] {
            def call(a: A, b: B, c: C, d: D) = f(a, b, c, d)
        }

    implicit def onSubscribeFunc[A](f: (Observer[_ >: A]) => Subscription): OnSubscribeFunc[A] =
        new OnSubscribeFunc[A] {
            override def onSubscribe(a: Observer[_ >: A]) = f(a)
        }

    /**
     * This implicit class implements all of the methods necessary for including Observables in a
     * for-comprehension.  Note that return type is always Observable, so that the ScalaObservable
     * type never escapes the for-comprehension
     */
    implicit class ScalaObservable[A](wrapped: Observable[A]) {
        def map[B](f: A => B): Observable[B] = wrapped.map[B](f)
        def flatMap[B](f: A => Observable[B]): Observable[B] = wrapped.mapMany(f)
        def foreach(f: A => Unit): Unit = wrapped.toBlockingObservable.forEach(f)
        def withFilter(p: A => Boolean): WithFilter = new WithFilter(p)

        class WithFilter(p: A => Boolean) {
            def map[B](f: A => B): Observable[B] = wrapped.filter(p).map(f)
            def flatMap[B](f: A => Observable[B]): Observable[B] = wrapped.filter(p).flatMap(f)
            def foreach(f: A => Unit): Unit = wrapped.filter(p).toBlockingObservable.forEach(f)
            def withFilter(p: A => Boolean): Observable[A] = wrapped.filter(p)
        }
    }
}