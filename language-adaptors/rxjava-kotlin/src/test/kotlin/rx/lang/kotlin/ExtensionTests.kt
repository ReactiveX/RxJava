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

package rx.lang.kotlin

import rx.Observable
import org.junit.Test
import org.mockito.Mockito.*
import org.mockito.Matchers.*
import org.junit.Assert.*
import rx.Notification
import kotlin.concurrent.thread
import rx.Subscriber

/**
 * This class contains tests using the extension functions provided by the language adaptor.
 */
public class ExtensionTests : KotlinTests() {


    [Test]
    public fun testCreate() {

        {(subscriber: Subscriber<in String>) ->
            subscriber.onNext("Hello")
            subscriber.onCompleted()
        }.asObservable().subscribe { result ->
            a!!.received(result)
        }

        verify(a, times(1))!!.received("Hello")
    }

    [Test]
    public fun testFilter() {
        listOf(1, 2, 3).asObservable().filter { it!! >= 2 }!!.subscribe(received())
        verify(a, times(0))!!.received(1);
        verify(a, times(1))!!.received(2);
        verify(a, times(1))!!.received(3);
    }


    [Test]
    public fun testLast() {
        assertEquals("three", listOf("one", "two", "three").asObservable().toBlockingObservable()!!.last())
    }

    [Test]
    public fun testLastWithPredicate() {
        assertEquals("two", listOf("one", "two", "three").asObservable().toBlockingObservable()!!.last { x -> x!!.length == 3 })
    }

    [Test]
    public fun testMap1() {
        1.asObservable().map { v -> "hello_$v" }!!.subscribe((received()))
        verify(a, times(1))!!.received("hello_1")
    }

    [Test]
    public fun testMap2() {
        listOf(1, 2, 3).asObservable().map { v -> "hello_$v" }!!.subscribe((received()))
        verify(a, times(1))!!.received("hello_1")
        verify(a, times(1))!!.received("hello_2")
        verify(a, times(1))!!.received("hello_3")
    }

    [Test]
    public fun testMaterialize() {
        listOf(1, 2, 3).asObservable().materialize()!!.subscribe((received()))
        verify(a, times(4))!!.received(any(javaClass<Notification<Int>>()))
        verify(a, times(0))!!.error(any(javaClass<Exception>()))
    }

    [Test]
    public fun testMergeDelayError() {
        Triple(listOf(1, 2, 3).asObservable(),
                Triple(6.asObservable(),
                        NullPointerException().asObservable<Int>(),
                        7.asObservable()
                ).merge(),
                listOf(4, 5).asObservable()
        ).mergeDelayError().subscribe(received(), { e -> a!!.error(e) })
        verify(a, times(1))!!.received(1)
        verify(a, times(1))!!.received(2)
        verify(a, times(1))!!.received(3)
        verify(a, times(1))!!.received(4)
        verify(a, times(1))!!.received(5)
        verify(a, times(1))!!.received(6)
        verify(a, times(0))!!.received(7)
        verify(a, times(1))!!.error(any(javaClass<NullPointerException>()))
    }

    [Test]
    public fun testMerge() {
        Triple(listOf(1, 2, 3).asObservable(),
                Triple(6.asObservable(),
                        NullPointerException().asObservable<Int>(),
                        7.asObservable()
                ).merge(),
                listOf(4, 5).asObservable()
        ).merge().subscribe(received(), { e -> a!!.error(e) })
        verify(a, times(1))!!.received(1)
        verify(a, times(1))!!.received(2)
        verify(a, times(1))!!.received(3)
        verify(a, times(0))!!.received(4)
        verify(a, times(0))!!.received(5)
        verify(a, times(1))!!.received(6)
        verify(a, times(0))!!.received(7)
        verify(a, times(1))!!.error(any(javaClass<NullPointerException>()))
    }

    [Test]
    public fun testScriptWithMaterialize() {
        TestFactory().observable.materialize()!!.subscribe((received()))
        verify(a, times(2))!!.received(any(javaClass<Notification<Int>>()))
    }

    [Test]
    public fun testScriptWithMerge() {
        val factory = TestFactory()
        (factory.observable to factory.observable).merge().subscribe((received()))
        verify(a, times(1))!!.received("hello_1")
        verify(a, times(1))!!.received("hello_2")
    }


    [Test]
    public fun testFromWithIterable() {
        assertEquals(5, listOf(1, 2, 3, 4, 5).asObservable().count()!!.toBlockingObservable()!!.single())
    }

    [Test]
    public fun testStartWith() {
        val list = listOf(10, 11, 12, 13, 14)
        val startList = listOf(1, 2, 3, 4, 5)
        assertEquals(6, list.asObservable().startWith(0)!!.count()!!.toBlockingObservable()!!.single())
        assertEquals(10, list.asObservable().startWith(startList)!!.count()!!.toBlockingObservable()!!.single())
    }

    [Test]
    public fun testScriptWithOnNext() {
        TestFactory().observable.subscribe((received()))
        verify(a, times(1))!!.received("hello_1")
    }

    [Test]
    public fun testSkipTake() {
        Triple(1, 2, 3).asObservable().skip(1)!!.take(1)!!.subscribe(received())
        verify(a, times(0))!!.received(1)
        verify(a, times(1))!!.received(2)
        verify(a, times(0))!!.received(3)
    }

    [Test]
    public fun testSkip() {
        Triple(1, 2, 3).asObservable().skip(2)!!.subscribe(received())
        verify(a, times(0))!!.received(1)
        verify(a, times(0))!!.received(2)
        verify(a, times(1))!!.received(3)
    }

    [Test]
    public fun testTake() {
        Triple(1, 2, 3).asObservable().take(2)!!.subscribe(received())
        verify(a, times(1))!!.received(1)
        verify(a, times(1))!!.received(2)
        verify(a, times(0))!!.received(3)
    }

    [Test]
    public fun testTakeLast() {
        TestFactory().observable.takeLast(1)!!.subscribe((received()))
        verify(a, times(1))!!.received("hello_1")
    }

    [Test]
    public fun testTakeWhile() {
        Triple(1, 2, 3).asObservable().takeWhile { x -> x!! < 3 }!!.subscribe(received())
        verify(a, times(1))!!.received(1)
        verify(a, times(1))!!.received(2)
        verify(a, times(0))!!.received(3)
    }

    [Test]
    public fun testTakeWhileWithIndex() {
        Triple(1, 2, 3).asObservable().takeWhileWithIndex { x, i -> i!! < 2 }!!.subscribe(received())
        verify(a, times(1))!!.received(1)
        verify(a, times(1))!!.received(2)
        verify(a, times(0))!!.received(3)
    }

    [Test]
    public fun testToSortedList() {
        TestFactory().numbers.toSortedList()!!.subscribe(received())
        verify(a, times(1))!!.received(listOf(1, 2, 3, 4, 5))
    }

    [Test]
    public fun testForEach() {
        asyncObservable.asObservable().toBlockingObservable()!!.forEach(received())
        verify(a, times(1))!!.received(1)
        verify(a, times(1))!!.received(2)
        verify(a, times(1))!!.received(3)
    }

    [Test(expected = javaClass<RuntimeException>())]
    public fun testForEachWithError() {
        asyncObservable.asObservable().toBlockingObservable()!!.forEach { throw RuntimeException("err") }
        fail("we expect an exception to be thrown")
    }

    [Test]
    public fun testLastOrDefault() {
        assertEquals("two", ("one" to"two").asObservable().toBlockingObservable()!!.lastOrDefault("default") { x -> x!!.length == 3 })
        assertEquals("default", ("one" to"two").asObservable().toBlockingObservable()!!.lastOrDefault("default") { x -> x!!.length > 3 })
    }

    [Test]
    public fun testDefer() {
        { (1 to 2).asObservable() }.defer().subscribe(received())
        verify(a, times(1))!!.received(1)
        verify(a, times(1))!!.received(2)
    }

    [Test]
    public fun testAll() {
        Triple(1, 2, 3).asObservable().all { x -> x!! > 0 }!!.subscribe(received())
        verify(a, times(1))!!.received(true)
    }

    [Test]
    public fun testZip() {
        val o1 = Triple(1, 2, 3).asObservable()
        val o2 = Triple(4, 5, 6).asObservable()
        val o3 = Triple(7, 8, 9).asObservable()

        val values = Observable.zip(o1, o2, o3) { a, b, c -> listOf(a, b, c) }!!.toList()!!.toBlockingObservable()!!.single()!!
        assertEquals(listOf(1, 4, 7), values[0])
        assertEquals(listOf(2, 5, 8), values[1])
        assertEquals(listOf(3, 6, 9), values[2])
    }

    val funOnSubscribe: (Int, Subscriber<in String>) -> Unit = { counter, subscriber ->
        subscriber.onNext("hello_$counter")
        subscriber.onCompleted()
    }

    val asyncObservable: (Subscriber<in Int>) -> Unit = { subscriber ->
        thread {
            Thread.sleep(50)
            subscriber.onNext(1)
            subscriber.onNext(2)
            subscriber.onNext(3)
            subscriber.onCompleted()
        }
    }

    /**
     * Copied from (funKTionale)[https://github.com/MarioAriasC/funKTionale/blob/master/src/main/kotlin/org/funktionale/partials/namespace.kt]
     */
    public fun <P1, P2, R> Function2<P1, P2, R>.partially1(p1: P1): (P2) -> R {
        return {(p2: P2) -> this(p1, p2) }
    }

    inner public class TestFactory() {
        var counter = 1

        val numbers: Observable<Int>
            get(){
                return listOf(1, 3, 2, 5, 4).asObservable()
            }

        val onSubscribe: (Subscriber<in String>) -> Unit
            get(){
                return funOnSubscribe.partially1(counter++)
            }

        val observable: Observable<String>
            get(){
                return onSubscribe.asObservable()
            }

    }
}