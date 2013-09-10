
/*
 * These function conversions are only used by the ScalaAdapter, users of RxScala don't need them.
 */
package rx.lang.scala.internal


import java.{lang => jlang}
import rx.util.functions.Action0
import rx.util.functions.Action1
import rx.util.functions.Func0
import rx.util.functions.Func1
import rx.util.functions.Func2
import rx.util.functions.Func3
import rx.util.functions.Func4
import java.{lang => jlang}
import rx.Observer
import rx.Subscription

object ImplicitFunctionConversions {
    // code below is copied from
    // https://github.com/Netflix/RxJava/blob/master/language-adaptors/rxjava-scala/src/main/scala/rx/lang/scala/RxImplicits.scala
    
    import java.{ lang => jlang }
    import language.implicitConversions
  
    import rx.observables.BlockingObservable
    import rx.util.functions._
    import rx.{Observer, Subscription}
    
    implicit def scalaFunction1ToOnSubscribeFunc[T](f: rx.lang.scala.Observer[T] => Subscription) =
        new rx.Observable.OnSubscribeFunc[T] {
            def onSubscribe(obs: Observer[_ >: T]): Subscription = {
              f(obs)
            }
        }
    
    /*implicit def scalaFunction1ToOnSubscribeFunc[T](f: Observer[_ >: T] => Subscription) =
        new rx.Observable.OnSubscribeFunc[T] {
            def onSubscribe(obs: Observer[_ >: T]): Subscription = {
              f(obs)
            }
        }*/
    
    /**
     * Converts a by-name parameter to a Rx Func0
     */
    implicit def scalaByNameParamToFunc0[B](param: => B): Func0[B] = 
        new Func0[B]{
            def call(): B = param
        }
    
    
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
     * Converts 2-arg predicate to Rx Func2[A, B, java.lang.Boolean]
     */
    implicit def scalaBooleanFunction2ToRxBooleanFunc1[A, B](f: ((A, B) => Boolean)): Func2[A, B, jlang.Boolean] =
        new Func2[A, B, jlang.Boolean] {
            def call(a: A, b: B): jlang.Boolean = f(a, b).booleanValue
        }
    
    /**
     * Converts a specific function shape (used in takeWhile) to the equivalent Java types with an Rx Func2
     */
    implicit def convertTakeWhileFuncToRxFunc2[A](f: (A, Int) => Boolean): Func2[A, jlang.Integer, jlang.Boolean] =
        new Func2[A, jlang.Integer, jlang.Boolean] {
            def call(a: A, b: jlang.Integer): jlang.Boolean = f(a, b).booleanValue
        }
    
    /**
     * Converts a function shaped ilke compareTo into the equivalent Rx Func2
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
  
}