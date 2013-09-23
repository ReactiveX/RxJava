package rx.lang.kotlin

import org.mockito.Mock
import rx.Observable
import org.junit.Before
import org.mockito.MockitoAnnotations
import org.junit.Test
import rx.subscriptions.Subscriptions
import org.mockito.Mockito.*
import org.mockito.Matchers.*
import rx.Observer
import org.junit.Assert.*
import rx.Notification

public class BasicKotlinTests {

    [Mock] var a: ScriptAssertion? = null
    [Mock] var w: Observable<Int>? = null

    [Before]
    public fun before() {
        MockitoAnnotations.initMocks(this)
    }

    [Test]
    public fun testCreate() {
        Observable.create<String>{
            it!!.onNext("Hello")
            it.onCompleted()
            Subscriptions.empty()
        }!!.subscribe { result ->
            a!!.received(result)
        }

        verify(a, times(1))!!.received("Hello")
    }
    [Test]
    public fun testCreateEx() {

        {(observer: Observer<in String>) ->
            observer.onNext("Hello")
            observer.onCompleted()
            Subscriptions.empty()!!
        }.asObservable().subscribe { result ->
            a!!.received(result)
        }

        verify(a, times(1))!!.received("Hello")
    }

    [Test]
    public fun testFilter() {
        Observable.from(1, 2, 3)!!.filter { it!! >= 2 }!!.subscribe { result ->
            a!!.received(result)
        }
        verify(a, times(0))!!.received(1);
        verify(a, times(1))!!.received(2);
        verify(a, times(1))!!.received(3);
    }

    [Test]
    public fun testFilterEx() {
        listOf(1, 2, 3).asObservable().filter { it!! >= 2 }!!.subscribe { result ->
            a!!.received(result)
        }
        verify(a, times(0))!!.received(1);
        verify(a, times(1))!!.received(2);
        verify(a, times(1))!!.received(3);
    }

    [Test]
    public fun testLast() {
        assertEquals("three", Observable.from("one", "two", "three")!!.toBlockingObservable()!!.last())
    }

    [Test]
    public fun testLastEx() {
        assertEquals("three", listOf("one", "two", "three").asObservable().toBlockingObservable()!!.last())
    }

    [Test]
    public fun testLastWithPredicate() {
        assertEquals("two", Observable.from("one", "two", "three")!!.toBlockingObservable()!!.last { x -> x!!.length == 3 })
    }

    [Test]
    public fun testLastWithPredicateEx() {
        assertEquals("two", listOf("one", "two", "three").asObservable().toBlockingObservable()!!.last { x -> x!!.length == 3 })
    }

    [Test]
    public fun testMap1() {
        Observable.from(1)!!.map { v -> "hello_$v" }!!.subscribe { result -> a!!.received(result) }
        verify(a, times(1))!!.received("hello_1")
    }

    [Test]
    public fun testMap1Ex() {
        1.asObservable().map { v -> "hello_$v" }!!.subscribe { result -> a!!.received(result) }
        verify(a, times(1))!!.received("hello_1")
    }

    [Test]
    public fun testMap2() {
        Observable.from(1, 2, 3)!!.map { v -> "hello_$v" }!!.subscribe { result -> a!!.received(result) }
        verify(a, times(1))!!.received("hello_1")
        verify(a, times(1))!!.received("hello_2")
        verify(a, times(1))!!.received("hello_3")
    }

    [Test]
    public fun testMap2Ex() {
        listOf(1, 2, 3).asObservable().map { v -> "hello_$v" }!!.subscribe { result -> a!!.received(result) }
        verify(a, times(1))!!.received("hello_1")
        verify(a, times(1))!!.received("hello_2")
        verify(a, times(1))!!.received("hello_3")
    }

    [Test]
    public fun testMaterialize() {
        Observable.from(1, 2, 3)!!.materialize()!!.subscribe { result -> a!!.received(result) }
        verify(a, times(4))!!.received(any(javaClass<Notification<Int>>()))
        verify(a, times(0))!!.error(any(javaClass<Exception>()))
    }

    [Test]
    public fun testMaterializeEx() {
        listOf(1, 2, 3).asObservable().materialize()!!.subscribe { result -> a!!.received(result) }
        verify(a, times(4))!!.received(any(javaClass<Notification<Int>>()))
        verify(a, times(0))!!.error(any(javaClass<Exception>()))
    }

    public trait ScriptAssertion{
        fun error(e: Exception?)

        fun received(e: Any?)
    }
}