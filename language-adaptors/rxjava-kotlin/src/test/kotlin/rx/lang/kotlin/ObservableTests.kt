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
import rx.Observer
import rx.Subscription
import rx.subscriptions.Subscriptions
import org.mockito.Mock
import org.junit.Before
import org.mockito.MockitoAnnotations
import org.junit.Test
import org.junit.Assert.*
import org.mockito.Mockito.*
import org.mockito.Matchers.*
import rx.Notification

public class ObservableTests {


    [Mock] var a: ScriptAssertion? = null

    [Mock] var w: Observer<Int>? = null

    [Before] public fun before() {
        MockitoAnnotations.initMocks(this)
    }

    private val receiveInt = {(result: Int) ->
        a!!.received(result)
    }

    private val receiveListOfInt = {(result: List<Int>) ->
        a!!.received(result)
    }

    private val receiveString = {(result: String) ->
        a!!.received(result)
    }

    private val lengthEqualsTo3 = {(x: String) ->
        x.length() == 3
    }

    [Test] public fun testCreate() {

        Observable.create<String>{(observer: Observer<String>) ->
            observer.onNext("hello")
            observer.onCompleted()
        }!!.subscribe(receiveString)
        verify(a, times(1))!!.received("hello")
    }

    [Test] public fun testFilter() {

        Observable.filter(Observable.toObservable(1, 2, 3)) {(i: Int) ->
            i >= 2
        }!!.subscribe(receiveInt)
        verify(a, times(0))!!.received(1)
        verify(a, times(1))!!.received(2)
        verify(a, times(1))!!.received(3)
    }

    [Test] public fun testLast() {
        assertEquals("three", Observable.toObservable("one", "two", "three")!!.last())
    }

    [Test] public fun testLastWithPredicate() {
        assertEquals("two", Observable.toObservable("one", "two", "three")!!.last{(x: String) ->
            x.length == 3
        })
    }

    [Test] public fun testMap() {
        TestFactory().getObservable().map<String>{(it: String) ->
            "say${it}"
        }!!.subscribe(receiveString)
        verify(a, times(1))!!.received("sayhello_1")

        Observable.map<Int, String>(Observable.toObservable(1, 2, 3)) {(i: Int) ->
            "hello_$i"
        }!!.subscribe(receiveString)
        verify(a, times(1))!!.received("hello_${1}")
        verify(a, times(1))!!.received("hello_${2}")
        verify(a, times(1))!!.received("hello_${3}")
    }

    [Test] public fun testMaterialize() {
        Observable.materialize(Observable.toObservable(1, 2, 3))!!.subscribe{(result: Notification<Int>) ->
            a!!.received(result)
        }
        verify(a, times(4))!!.received(any(javaClass<Notification<Int>>()))
        verify(a, times(0))!!.error(any(javaClass<Exception>()))
    }

    [Test] public fun testMerge() {
        Observable.merge(
                Observable.toObservable(1, 2, 3),
                Observable.merge(
                        Observable.toObservable(6),
                        Observable.error(NullPointerException()),
                        Observable.toObservable(7)
                ),
                Observable.toObservable(4, 5)
        )!!.subscribe(onNext = receiveInt, onError = {(exception: Exception) -> a!!.error(exception) })

        verify(a, times(1))!!.received(1)
        verify(a, times(1))!!.received(2)
        verify(a, times(1))!!.received(3)
        verify(a, times(0))!!.received(4) // the NPE will cause this sequence to be skipped
        verify(a, times(0))!!.received(5) // the NPE will cause this sequence to be skipped
        verify(a, times(1))!!.received(6) // this comes before the NPE so should exist
        verify(a, times(0))!!.received(7)// this comes in the sequence after the NPE
        verify(a, times(1))!!.error(any(javaClass<NullPointerException>()))
    }

    [Test] public fun testMergeDelayError() {
        Observable.mergeDelayError(
                Observable.toObservable(1, 2, 3),
                Observable.merge(
                        Observable.toObservable(6),
                        Observable.error(NullPointerException()),
                        Observable.toObservable(7)
                ),
                Observable.toObservable(4, 5)
        )!!.subscribe(onNext = receiveInt, onError = {(exception: Exception) -> a!!.error(exception) })
        verify(a, times(1))!!.received(1)
        verify(a, times(1))!!.received(2)
        verify(a, times(1))!!.received(3)
        verify(a, times(1))!!.received(4)
        verify(a, times(1))!!.received(5)
        verify(a, times(1))!!.received(6)
        verify(a, times(0))!!.received(7)
        verify(a, times(1))!!.error(any(javaClass<NullPointerException>()))
    }

    [Test] public fun testScriptWithMaterialize() {
        TestFactory().getObservable().materialize()!!.subscribe{(result: Notification<String>) ->
            a!!.received(result)
        }
        verify(a, times(2))!!.received(any(javaClass<Notification<String>>()))
    }

    [Test] public fun testScriptWithMerge() {
        val factory = TestFactory()
        Observable.merge(factory.getObservable(), factory.getObservable())!!.subscribe(receiveString)
        verify(a, times(1))!!.received("hello_1")
        verify(a, times(1))!!.received("hello_2")
    }

    [Test] public fun testScriptWithOnNext() {
        TestFactory().getObservable().subscribe(receiveString)
        verify(a, times(1))!!.received("hello_1")
    }

    [Test] public fun testScriptWithOnNextUsingMao() {
        TestFactory().getObservable().subscribe(hashMapOf("onNext" to receiveString))
        verify(a, times(1))!!.received("hello_1")
    }

    [Test] public fun testSkipTake() {
        Observable.skip(Observable.toObservable(1, 2, 3), 1)!!.take(1)!!.subscribe(receiveInt)
        verify(a, times(0))!!.received(1)
        verify(a, times(1))!!.received(2)
        verify(a, times(0))!!.received(3)
    }

    [Test] public fun testTakeWhile() {
        Observable.takeWhile(Observable.toObservable(1, 2, 3)) {(x: Int) ->
            x < 3
        }!!.subscribe(receiveInt)
        verify(a, times(1))!!.received(1)
        verify(a, times(1))!!.received(2)
        verify(a, times(0))!!.received(3)
    }

    [Test] public fun testTakeWhileWithIndex() {
        Observable.takeWhileWithIndex(Observable.toObservable(1, 2, 3)) {(x: Int, i: Int) ->
            i < 2
        }!!.subscribe(receiveInt)
        verify(a, times(1))!!.received(1)
        verify(a, times(1))!!.received(2)
        verify(a, times(0))!!.received(3)
    }


    [Test]
    public fun testToSortedList() {
        TestFactory().getNumbers().toSortedList()!!.subscribe(receiveListOfInt)
        verify(a, times(1))!!.received(arrayListOf(1, 2, 3, 4, 5))
    }

    [Test]
    public fun testToSortedListStatic() {
        Observable.toSortedList(Observable.toObservable(1, 3, 2, 5, 4))!!.subscribe(receiveListOfInt)
        verify(a, times(1))!!.received(arrayListOf(1, 2, 3, 4, 5))
    }

    [Test]
    public fun testToSortedListWithFunction() {
        TestFactory().getNumbers().toSortedList{(a: Int, b: Int) -> a - b }!!.subscribe(receiveListOfInt)
        verify(a, times(1))!!.received(arrayListOf(1, 2, 3, 4, 5))
    }

    [Test]
    public fun testToSortedListWithFunctionStatic() {
        Observable.toSortedList(Observable.toObservable(1, 3, 2, 5, 4)) {(a: Int, b: Int) -> a - b }!!.subscribe(receiveListOfInt)
        verify(a, times(1))!!.received(arrayListOf(1, 2, 3, 4, 5))
    }

    [Test]
    public fun testForEach() {
        asyncObservable.forEach(receiveInt)
        verify(a, times(1))!!.received(1)
        verify(a, times(1))!!.received(2)
        verify(a, times(1))!!.received(3)
    }

    [Test]
    public fun testForEachWithError() {
        try {
            asyncObservable.forEach{(result: Any) -> throw RuntimeException("err") }
            fail("we expect an exception to be thrown")
        }catch(e: Exception) {
            // do nothing as we expect this
        }
    }

    [Test]
    public fun testLastOrDefault() {

        assertEquals("two", Observable.toObservable("one", "two")!!.lastOrDefault("default", lengthEqualsTo3))
    }

    [Test]
    public fun testLastOrDefault2() {

        assertEquals("default", Observable.toObservable("one", "two")!!.lastOrDefault("default") {(x: String) ->
            x.length() > 3
        })
    }

    public fun testSingle1() {

        assertEquals("one", Observable.toObservable("one")!!.single(lengthEqualsTo3))
    }

    [Test(expected = javaClass<IllegalStateException>())]
    public fun testSingle2() {
        Observable.toObservable("one", "two")!!.single(lengthEqualsTo3)
    }

    [Test]
    public fun testDefer() {
        val obs = Observable.toObservable(1, 2)!!
        Observable.defer<Int>{ obs }!!.subscribe(receiveInt)
        verify(a, times(1))!!.received(1)
        verify(a, times(1))!!.received(2)

    }

    [Test]
    public fun testAll() {
        Observable.toObservable(1, 2, 3)!!.all{(x: Int) -> x > 0 }!!.subscribe{(result: Boolean) ->
            a!!.received(result)
        }
        verify(a, times(1))!!.received(true)
    }


    val asyncObservable = {(observer: Observer<Int>) ->
        Thread(Runnable{
            try{
                Thread.sleep(50)
            }catch(e: Exception) {
                //Do nothing
            }
            observer.onNext(1)
            observer.onNext(2)
            observer.onNext(3)
            observer.onCompleted()
        }).start()
        Subscriptions.empty()!!
    }.asObservable()

    trait ScriptAssertion{
        public fun error(e: Exception?)
        public fun received(a: Any?)
    }

    class TestObservable(val count: Int): Observable<String>(){

        public override fun subscribe(observer: Observer<String>?): Subscription? {
            observer!!.onNext("hello_$count")
            observer.onCompleted()
            return Subscription{ }
        }
    }

    class TestFactory {
        var counter = 1

        public fun getNumbers(): Observable<Int> {
            return Observable.toObservable(1, 2, 3, 4, 5)!!
        }

        public fun getObservable(): TestObservable {
            return TestObservable(counter++)
        }
    }

    fun<T> Function1<Observer<T>, Subscription>.asObservable(): Observable<T> {
        return Observable.create(rx.util.functions.Func1<Observer<T>, Subscription>{
            this(it!!)
        })!!
    }
}

