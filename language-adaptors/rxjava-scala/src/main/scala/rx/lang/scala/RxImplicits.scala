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

object RxImplicits {
    import java.{ lang => jlang }
    import language.implicitConversions

    import rx.Observable
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

import org.scalatest.junit.JUnitSuite

class UnitTestSuite extends JUnitSuite {
    import rx.lang.scala.RxImplicits._

    import org.junit.{ Before, Test }
    import org.junit.Assert._
    import org.mockito.Matchers.any
    import org.mockito.Mockito._
    import org.mockito.{ MockitoAnnotations, Mock }
    import rx.{ Notification, Observer, Observable, Subscription }
    import rx.observables.GroupedObservable
    import collection.mutable.ArrayBuffer
    import collection.JavaConverters._
            
    @Mock private[this]
    val observer: Observer[Any] = null
    
    @Mock private[this]
    val subscription: Subscription = null
    
    val isOdd = (i: Int) => i % 2 == 1
    val isEven = (i: Int) => i % 2 == 0
    
    class ObservableWithException(s: Subscription, values: String*) extends Observable[String] {
        var t: Thread = null
        
        override def subscribe(observer: Observer[_ >: String]): Subscription = {
            println("ObservableWithException subscribed to ...")
            t = new Thread(new Runnable() {
                override def run() {
                    try {
                        println("running ObservableWithException thread")
                        values.toList.foreach(v => {
                            println("ObservableWithException onNext: " + v)
                            observer.onNext(v)
                        })
                        throw new RuntimeException("Forced Failure")
                    } catch {
                        case ex: Exception => observer.onError(ex)
                    }
                }
            })
            println("starting ObservableWithException thread")
            t.start()
            println("done starting ObservableWithException thread")
            s
        }
    }
    
    @Before def before {
        MockitoAnnotations.initMocks(this)
    }
    
    // tests of static methods
    
    @Test def testSingle {
        assertEquals(1, Observable.from(1).toBlockingObservable.single)
    }
    
    @Test def testSinglePredicate {
        val found = Observable.from(1, 2, 3).toBlockingObservable.single(isEven)
        assertEquals(2, found)
    }
    
    @Test def testSingleOrDefault {
        assertEquals(0, Observable.from[Int]().toBlockingObservable.singleOrDefault(0))
        assertEquals(1, Observable.from(1).toBlockingObservable.singleOrDefault(0))
        try {
            Observable.from(1, 2, 3).toBlockingObservable.singleOrDefault(0)
            fail("Did not catch any exception, expected IllegalStateException")
        } catch {
            case ex: IllegalStateException => println("Caught expected IllegalStateException")
            case ex: Throwable => fail("Caught unexpected exception " + ex.getCause + ", expected IllegalStateException")
        }
    }
    
    @Test def testSingleOrDefaultPredicate {
        assertEquals(2, Observable.from(1, 2, 3).toBlockingObservable.singleOrDefault(0, isEven))
        assertEquals(0, Observable.from(1, 3).toBlockingObservable.singleOrDefault(0, isEven))
        try {
            Observable.from(1, 2, 3).toBlockingObservable.singleOrDefault(0, isOdd)
            fail("Did not catch any exception, expected IllegalStateException")
        } catch {
            case ex: IllegalStateException => println("Caught expected IllegalStateException")
            case ex: Throwable => fail("Caught unexpected exception " + ex.getCause + ", expected IllegalStateException")
        }
    }
    
    @Test def testFromJavaInterop {
        val observable = Observable.from(List(1, 2, 3).asJava)
        assertSubscribeReceives(observable)(1, 2, 3)
    }
    
    @Test def testSubscribe {
        val observable = Observable.from("1", "2", "3")
        assertSubscribeReceives(observable)("1", "2", "3")
    }
    
    //should not compile - adapted from https://gist.github.com/jmhofer/5195589
    /*@Test def testSubscribeOnInt() {
        val observable = Observable.from("1", "2", "3")
        observable.subscribe((arg: Int) => {
            println("testSubscribe: arg = " + arg)
        })
     }*/
    
    @Test def testDefer {
        val lazyObservableFactory = () => Observable.from(1, 2)
        val observable = Observable.defer(lazyObservableFactory)
        assertSubscribeReceives(observable)(1, 2)
    }
    
    @Test def testJust {
        val observable = Observable.just("foo")
        assertSubscribeReceives(observable)("foo")
    }
    
    @Test def testMerge {
        val observable1 = Observable.from(1, 2, 3)
        val observable2 = Observable.from(4, 5, 6)
        val observableList = List(observable1, observable2).asJava
        val merged = Observable.merge(observableList)
        assertSubscribeReceives(merged)(1, 2, 3, 4, 5, 6)
    }
    
    @Test def testFlattenMerge {
        val observable = Observable.from(Observable.from(1, 2, 3))
        val merged = Observable.merge(observable)
        assertSubscribeReceives(merged)(1, 2, 3)
    }
    
    @Test def testSequenceMerge {
        val observable1 = Observable.from(1, 2, 3)
        val observable2 = Observable.from(4, 5, 6)
        val merged = Observable.merge(observable1, observable2)
        assertSubscribeReceives(merged)(1, 2, 3, 4, 5, 6)
    }
    
    @Test def testConcat {
        val observable1 = Observable.from(1, 2, 3)
        val observable2 = Observable.from(4, 5, 6)
        val concatenated = Observable.concat(observable1, observable2)
        assertSubscribeReceives(concatenated)(1, 2, 3, 4, 5, 6)
    }
    
    @Test def testSynchronize {
        val observable = Observable.from(1, 2, 3)
        val synchronized = Observable.synchronize(observable)
        assertSubscribeReceives(synchronized)(1, 2, 3)
    }
    
    @Test def testZip2() {
        val colors: Observable[String] = Observable.from("red", "green", "blue")
        val names: Observable[String] = Observable.from("lion-o", "cheetara", "panthro")
        
        case class Character(color: String, name: String)
        
        val cheetara = Character("green", "cheetara")
        val panthro = Character("blue", "panthro")
        val characters = Observable.zip[Character, String, String](colors, names, Character.apply _)
        assertSubscribeReceives(characters)(cheetara, panthro)
    }
    
    @Test def testZip3() {
        val numbers = Observable.from(1, 2, 3)
        val colors = Observable.from("red", "green", "blue")
        val names = Observable.from("lion-o", "cheetara", "panthro")
        
        case class Character(id: Int, color: String, name: String)
        
        val liono = Character(1, "red", "lion-o")
        val cheetara = Character(2, "green", "cheetara")
        val panthro = Character(3, "blue", "panthro")
        
        val characters = Observable.zip[Character, Int, String, String](numbers, colors, names, Character.apply _)
        assertSubscribeReceives(characters)(liono, cheetara, panthro)
    }
    
    @Test def testZip4() {
        val numbers = Observable.from(1, 2, 3)
        val colors = Observable.from("red", "green", "blue")
        val names = Observable.from("lion-o", "cheetara", "panthro")
        val isLeader = Observable.from(true, false, false)
        
        case class Character(id: Int, color: String, name: String, isLeader: Boolean)
        
        val liono = Character(1, "red", "lion-o", true)
        val cheetara = Character(2, "green", "cheetara", false)
        val panthro = Character(3, "blue", "panthro", false)
        
        val characters = Observable.zip[Character, Int, String, String, Boolean](numbers, colors, names, isLeader, Character.apply _)
        assertSubscribeReceives(characters)(liono, cheetara, panthro)
    }
    
    //tests of instance methods
    
    // missing tests for : takeUntil, groupBy, next, mostRecent
    
    @Test def testFilter {
        val numbers = Observable.from(1, 2, 3, 4, 5, 6, 7, 8, 9)
        val observable = numbers.filter(isEven)
        assertSubscribeReceives(observable)(2, 4, 6, 8)
    }
    
    @Test def testLast {
        val observable = Observable.from(1, 2, 3, 4).toBlockingObservable
        assertEquals(4, observable.toBlockingObservable.last)
    }
    
    @Test def testLastPredicate {
        val observable = Observable.from(1, 2, 3, 4)
        assertEquals(3, observable.toBlockingObservable.last(isOdd))
    }
    
    @Test def testLastOrDefault {
        val observable = Observable.from(1, 2, 3, 4)
        assertEquals(4, observable.toBlockingObservable.lastOrDefault(5))
        assertEquals(5, Observable.from[Int]().toBlockingObservable.lastOrDefault(5))
    }
    
    @Test def testLastOrDefaultPredicate {
        val observable = Observable.from(1, 2, 3, 4)
        assertEquals(3, observable.toBlockingObservable.lastOrDefault(5, isOdd))
        assertEquals(5, Observable.from[Int]().toBlockingObservable.lastOrDefault(5, isOdd))
    }
    
    @Test def testMap {
        val numbers = Observable.from(1, 2, 3, 4, 5, 6, 7, 8, 9)
        val mappedNumbers = ArrayBuffer.empty[Int]
        val mapped: Observable[Int] = numbers map ((x: Int) => x * x)
        mapped.subscribe((squareVal: Int) => {
            mappedNumbers.append(squareVal)
        })
        assertEquals(List(1, 4, 9, 16, 25, 36, 49, 64, 81), mappedNumbers.toList)
    }
    
    @Test def testMapMany {
        val numbers = Observable.from(1, 2, 3, 4)
        val f = (i: Int) => Observable.from(List(i, -i).asJava)
        val mappedNumbers = ArrayBuffer.empty[Int]
        numbers.mapMany(f).subscribe((i: Int) => {
            mappedNumbers.append(i)
        })
        assertEquals(List(1, -1, 2, -2, 3, -3, 4, -4), mappedNumbers.toList)
    }
    
    @Test def testMaterialize {
        val observable = Observable.from(1, 2, 3, 4)
        val expectedNotifications: List[Notification[Int]] =
            ((1.to(4).map(i => new Notification(i))) :+ new Notification()).toList
        val actualNotifications: ArrayBuffer[Notification[Int]] = ArrayBuffer.empty
        observable.materialize.subscribe((n: Notification[Int]) => {
            actualNotifications.append(n)
        })
        assertEquals(expectedNotifications, actualNotifications.toList)
    }
    
    @Test def testDematerialize {
        val notifications: List[Notification[Int]] =
            ((1.to(4).map(i => new Notification(i))) :+ new Notification()).toList
        val observableNotifications: Observable[Notification[Int]] =
            Observable.from(notifications.asJava)
        val observable: Observable[Int] =
            observableNotifications.dematerialize()
        assertSubscribeReceives(observable)(1, 2, 3, 4)
    }
    
    @Test def testOnErrorResumeNextObservableNoError {
        val observable = Observable.from(1, 2, 3, 4)
        val resumeObservable = Observable.from(5, 6, 7, 8)
        val observableWithErrorHandler = observable.onErrorResumeNext(resumeObservable)
        assertSubscribeReceives(observableWithErrorHandler)(1, 2, 3, 4)
    }
    
    @Test def testOnErrorResumeNextObservableErrorOccurs {
        val observable = new ObservableWithException(subscription, "foo", "bar")
        val resumeObservable = Observable.from("a", "b", "c", "d")
        val observableWithErrorHandler = observable.onErrorResumeNext(resumeObservable)
        observableWithErrorHandler.subscribe(observer.asInstanceOf[Observer[String]])
        
        try {
            observable.t.join()
        } catch {
            case ex: InterruptedException => fail(ex.getMessage)
        }
        
        List("foo", "bar", "a", "b", "c", "d").foreach(t => verify(observer, times(1)).onNext(t))
        verify(observer, never()).onError(any(classOf[Exception]))
        verify(observer, times(1)).onCompleted()
    }
    
    @Test def testOnErrorResumeNextFuncNoError {
        val observable = Observable.from(1, 2, 3, 4)
        val resumeFunc = (ex: Throwable) => Observable.from(5, 6, 7, 8)
        val observableWithErrorHandler = observable.onErrorResumeNext(resumeFunc)
        assertSubscribeReceives(observableWithErrorHandler)(1, 2, 3, 4)
    }
    
    @Test def testOnErrorResumeNextFuncErrorOccurs {
        val observable = new ObservableWithException(subscription, "foo", "bar")
        val resumeFunc = (ex: Throwable) => Observable.from("a", "b", "c", "d")
        val observableWithErrorHandler = observable.onErrorResumeNext(resumeFunc)
        observableWithErrorHandler.subscribe(observer.asInstanceOf[Observer[String]])
        
        try {
            observable.t.join()
        } catch {
            case ex: InterruptedException => fail(ex.getMessage)
        }
        
        List("foo", "bar", "a", "b", "c", "d").foreach(t => verify(observer, times(1)).onNext(t))
        verify(observer, never()).onError(any(classOf[Exception]))
        verify(observer, times(1)).onCompleted()
    }
    
    @Test def testOnErrorReturnFuncNoError {
        val observable = Observable.from(1, 2, 3, 4)
        val returnFunc = (ex: Throwable) => 87
        val observableWithErrorHandler = observable.onErrorReturn(returnFunc)
        assertSubscribeReceives(observableWithErrorHandler)(1, 2, 3, 4)
    }
    
    @Test def testOnErrorReturnFuncErrorOccurs {
        val observable = new ObservableWithException(subscription, "foo", "bar")
        val returnFunc = (ex: Throwable) => "baz"
        val observableWithErrorHandler = observable.onErrorReturn(returnFunc)
        observableWithErrorHandler.subscribe(observer.asInstanceOf[Observer[String]])
        
        try {
            observable.t.join()
        } catch {
            case ex: InterruptedException => fail(ex.getMessage)
        }
        
        List("foo", "bar", "baz").foreach(t => verify(observer, times(1)).onNext(t))
        verify(observer, never()).onError(any(classOf[Exception]))
        verify(observer, times(1)).onCompleted()
    }
    
    @Test def testReduce {
        val observable = Observable.from(1, 2, 3, 4)
        assertEquals(10, observable.reduce((a: Int, b: Int) => a + b).toBlockingObservable.single)
    }
    
    @Test def testSkip {
        val observable = Observable.from(1, 2, 3, 4)
        val skipped = observable.skip(2)
        assertSubscribeReceives(skipped)(3, 4)
    }
    
    @Test def testTake {
        val observable = Observable.from(1, 2, 3, 4, 5)
        val took = observable.take(2)
        assertSubscribeReceives(took)(1, 2)
    }
    
    @Test def testTakeWhile {
        val observable = Observable.from(1, 3, 5, 6, 7, 9, 11)
        val took = observable.takeWhile(isOdd)
        assertSubscribeReceives(took)(1, 3, 5)
    }
    
    @Test def testTakeWhileWithIndex {
        val observable = Observable.from(1, 3, 5, 7, 9, 11, 12, 13, 15, 17)
        val took = observable.takeWhileWithIndex((i: Int, idx: Int) => isOdd(i) && idx < 8)
        assertSubscribeReceives(took)(1, 3, 5, 7, 9, 11)
    }
    
    @Test def testTakeLast {
        val observable = Observable.from(1, 2, 3, 4, 5, 6, 7, 8, 9)
        val tookLast = observable.takeLast(3)
        assertSubscribeReceives(tookLast)(7, 8, 9)
    }
    
    @Test def testToList {
        val observable = Observable.from(1, 2, 3, 4)
        val toList = observable.toList
        assertSubscribeReceives(toList)(List(1, 2, 3, 4).asJava)
    }
    
    @Test def testToSortedList {
        val observable = Observable.from(1, 3, 4, 2)
        val toSortedList = observable.toSortedList
        assertSubscribeReceives(toSortedList)(List(1, 2, 3, 4).asJava)
    }
    
    @Test def testToArbitrarySortedList {
        val observable = Observable.from("a", "aaa", "aaaa", "aa")
        val sortByLength = (s1: String, s2: String) => s1.length.compareTo(s2.length)
        val toSortedList = observable.toSortedList(sortByLength)
        assertSubscribeReceives(toSortedList)(List("a", "aa", "aaa", "aaaa").asJava)
    }
    
    @Test def testToIterable {
        val observable = Observable.from(1, 2)
        val it = observable.toBlockingObservable.toIterable.iterator
        assertTrue(it.hasNext)
        assertEquals(1, it.next)
        assertTrue(it.hasNext)
        assertEquals(2, it.next)
        assertFalse(it.hasNext)
    }
    
    @Test def testStartWith {
        val observable = Observable.from(1, 2, 3, 4)
        val newStart = observable.startWith(-1, 0)
        assertSubscribeReceives(newStart)(-1, 0, 1, 2, 3, 4)
    }
    
    @Test def testOneLineForComprehension {
        val mappedObservable = for {
            i: Int <- Observable.from(1, 2, 3, 4)
        } yield i + 1
        assertSubscribeReceives(mappedObservable)(2, 3, 4, 5)
        assertFalse(mappedObservable.isInstanceOf[ScalaObservable[_]])
    }
    
    @Test def testSimpleMultiLineForComprehension {
        val flatMappedObservable = for {
            i: Int <- Observable.from(1, 2, 3, 4)
            j: Int <- Observable.from(1, 10, 100, 1000)
        } yield i + j
        assertSubscribeReceives(flatMappedObservable)(2, 12, 103, 1004)
        assertFalse(flatMappedObservable.isInstanceOf[ScalaObservable[_]])
    }
    
    @Test def testMultiLineForComprehension {
        val doubler = (i: Int) => Observable.from(i, i)
        val flatMappedObservable = for {
            i: Int <- Observable.from(1, 2, 3, 4)
            j: Int <- doubler(i)
        } yield j
        //can't use assertSubscribeReceives since each number comes in 2x
        flatMappedObservable.subscribe(observer.asInstanceOf[Observer[Int]])
        List(1, 2, 3, 4).foreach(i => verify(observer, times(2)).onNext(i))
        verify(observer, never()).onError(any(classOf[Exception]))
        verify(observer, times(1)).onCompleted()
        assertFalse(flatMappedObservable.isInstanceOf[ScalaObservable[_]])
    }
    
    @Test def testFilterInForComprehension {
        val doubler = (i: Int) => Observable.from(i, i)
        val filteredObservable: Observable[Int] = for {
            i: Int <- Observable.from(1, 2, 3, 4)
            j: Int <- doubler(i) if isOdd(i)
        } yield j
        //can't use assertSubscribeReceives since each number comes in 2x
        filteredObservable.subscribe(observer.asInstanceOf[Observer[Int]])
        List(1, 3).foreach(i => verify(observer, times(2)).onNext(i))
        verify(observer, never()).onError(any(classOf[Exception]))
        verify(observer, times(1)).onCompleted()
        assertFalse(filteredObservable.isInstanceOf[ScalaObservable[_]])
    }
    
    @Test def testForEachForComprehension {
        val doubler = (i: Int) => Observable.from(i, i)
        val intBuffer = ArrayBuffer.empty[Int]
        val forEachComprehension = for {
            i: Int <- Observable.from(1, 2, 3, 4)
            j: Int <- doubler(i) if isEven(i)
        } {
            intBuffer.append(j)
        }
        assertEquals(List(2, 2, 4, 4), intBuffer.toList)
    }
    
    private def assertSubscribeReceives[T](o: Observable[T])(values: T*) = {
        o.subscribe(observer.asInstanceOf[Observer[T]])
        values.toList.foreach(t => verify(observer, times(1)).onNext(t))
        verify(observer, never()).onError(any(classOf[Exception]))
        verify(observer, times(1)).onCompleted()
    }
}
