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
import rx.Subscription
import kotlin.concurrent.thread

public class BasicKotlinTests {

    [Mock] var a: ScriptAssertion? = null
    [Mock] var w: Observable<Int>? = null

    [Before]
    public fun before() {
        MockitoAnnotations.initMocks(this)
    }

    fun received<T>(): (T?) -> Unit {
        return {(result: T?) -> a!!.received(result) }
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
        Observable.from(1, 2, 3)!!.filter { it!! >= 2 }!!.subscribe(received())
        verify(a, times(0))!!.received(1);
        verify(a, times(1))!!.received(2);
        verify(a, times(1))!!.received(3);
    }

    [Test]
    public fun testFilterEx() {
        listOf(1, 2, 3).asObservable().filter { it!! >= 2 }!!.subscribe(received())
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
        Observable.from(1)!!.map { v -> "hello_$v" }!!.subscribe(received())
        verify(a, times(1))!!.received("hello_1")
    }

    [Test]
    public fun testMap1Ex() {
        1.asObservable().map { v -> "hello_$v" }!!.subscribe((received()))
        verify(a, times(1))!!.received("hello_1")
    }

    [Test]
    public fun testMap2() {
        Observable.from(1, 2, 3)!!.map { v -> "hello_$v" }!!.subscribe((received()))
        verify(a, times(1))!!.received("hello_1")
        verify(a, times(1))!!.received("hello_2")
        verify(a, times(1))!!.received("hello_3")
    }

    [Test]
    public fun testMap2Ex() {
        listOf(1, 2, 3).asObservable().map { v -> "hello_$v" }!!.subscribe((received()))
        verify(a, times(1))!!.received("hello_1")
        verify(a, times(1))!!.received("hello_2")
        verify(a, times(1))!!.received("hello_3")
    }

    [Test]
    public fun testMaterialize() {
        Observable.from(1, 2, 3)!!.materialize()!!.subscribe((received()))
        verify(a, times(4))!!.received(any(javaClass<Notification<Int>>()))
        verify(a, times(0))!!.error(any(javaClass<Exception>()))
    }

    [Test]
    public fun testMaterializeEx() {
        listOf(1, 2, 3).asObservable().materialize()!!.subscribe((received()))
        verify(a, times(4))!!.received(any(javaClass<Notification<Int>>()))
        verify(a, times(0))!!.error(any(javaClass<Exception>()))
    }

    [Test]
    public fun testMergeDelayError() {
        Observable.mergeDelayError(
                Observable.from(1, 2, 3),
                Observable.merge(
                        Observable.from(6),
                        Observable.error(NullPointerException()),
                        Observable.from(7)
                ),
                Observable.from(4, 5)
        )!!.subscribe(received(), { e -> a!!.error(e) })
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
    public fun testMergeDelayErrorEx() {

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
        Observable.merge(
                Observable.from(1, 2, 3),
                Observable.merge(
                        Observable.from(6),
                        Observable.error(NullPointerException()),
                        Observable.from(7)
                ),
                Observable.from(4, 5)
        )!!.subscribe(received(), { e -> a!!.error(e) })
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
    public fun testMergeEx() {
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
        Observable.merge(factory.observable, factory.observable)!!.subscribe((received()))
        verify(a, times(1))!!.received("hello_1")
        verify(a, times(1))!!.received("hello_2")
    }

    [Test]
    public fun testScriptWithMergeEx() {
        val factory = TestFactory()
        (factory.observable to factory.observable).merge().subscribe((received()))
        verify(a, times(1))!!.received("hello_1")
        verify(a, times(1))!!.received("hello_2")
    }

    [Test]
    public fun testFromWithIterable() {
        val list = listOf(1, 2, 3, 4, 5)
        assertEquals(5, Observable.from(list)!!.count()!!.toBlockingObservable()!!.single())
    }

    [Test]
    public fun testFromWithIterableEx() {
        assertEquals(5, listOf(1, 2, 3, 4, 5).asObservable().count()!!.toBlockingObservable()!!.single())
    }

    [Test]
    public fun testFromWithObjects() {
        val list = listOf(1, 2, 3, 4, 5)
        assertEquals(2, Observable.from(list, 6)!!.count()!!.toBlockingObservable()!!.single())
    }

    [Test]
    public fun testStartWith() {
        val list = listOf(10, 11, 12, 13, 14)
        val startList = listOf(1, 2, 3, 4, 5)
        assertEquals(6, Observable.from(list)!!.startWith(0)!!.count()!!.toBlockingObservable()!!.single())
        assertEquals(10, Observable.from(list)!!.startWith(startList)!!.count()!!.toBlockingObservable()!!.single())
    }

    [Test]
    public fun testStartWithEx() {
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
        Observable.from(1, 2, 3)!!.skip(1)!!.take(1)!!.subscribe(received())
        verify(a, times(0))!!.received(1)
        verify(a, times(1))!!.received(2)
        verify(a, times(0))!!.received(3)
    }

    [Test]
    public fun testSkipTakeEx() {
        Triple(1, 2, 3).asObservable().skip(1)!!.take(1)!!.subscribe(received())
        verify(a, times(0))!!.received(1)
        verify(a, times(1))!!.received(2)
        verify(a, times(0))!!.received(3)
    }

    [Test]
    public fun testSkip() {
        Observable.from(1, 2, 3)!!.skip(2)!!.subscribe(received())
        verify(a, times(0))!!.received(1)
        verify(a, times(0))!!.received(2)
        verify(a, times(1))!!.received(3)
    }

    [Test]
    public fun testSkipEx() {
        Triple(1, 2, 3).asObservable().skip(2)!!.subscribe(received())
        verify(a, times(0))!!.received(1)
        verify(a, times(0))!!.received(2)
        verify(a, times(1))!!.received(3)
    }

    [Test]
    public fun testTake() {
        Observable.from(1, 2, 3)!!.take(2)!!.subscribe(received())
        verify(a, times(1))!!.received(1)
        verify(a, times(1))!!.received(2)
        verify(a, times(0))!!.received(3)
    }

    [Test]
    public fun testTakeEx() {
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
        Observable.from(1, 2, 3)!!.takeWhile { x -> x!! < 3 }!!.subscribe(received())
        verify(a, times(1))!!.received(1)
        verify(a, times(1))!!.received(2)
        verify(a, times(0))!!.received(3)
    }

    [Test]
    public fun testTakeWhileEx() {
        Triple(1, 2, 3).asObservable().takeWhile { x -> x!! < 3 }!!.subscribe(received())
        verify(a, times(1))!!.received(1)
        verify(a, times(1))!!.received(2)
        verify(a, times(0))!!.received(3)
    }

    [Test]
    public fun testTakeWhileWithIndex() {
        Observable.from(1, 2, 3)!!.takeWhileWithIndex { x, i -> i!! < 2 }!!.subscribe(received())
        verify(a, times(1))!!.received(1)
        verify(a, times(1))!!.received(2)
        verify(a, times(0))!!.received(3)
    }

    [Test]
    public fun testTakeWhileWithIndexEx() {
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
        assertEquals("two", Observable.from("one", "two")!!.toBlockingObservable()!!.lastOrDefault("default") { x -> x!!.length == 3 })
        assertEquals("default", Observable.from("one", "two")!!.toBlockingObservable()!!.lastOrDefault("default") { x -> x!!.length > 3 })
    }

    [Test]
    public fun testLastOrDefaultEx() {
        assertEquals("two", ("one" to"two").asObservable().toBlockingObservable()!!.lastOrDefault("default") { x -> x!!.length == 3 })
        assertEquals("default", ("one" to"two").asObservable().toBlockingObservable()!!.lastOrDefault("default") { x -> x!!.length > 3 })
    }

    [Test(expected = javaClass<IllegalStateException>())]
    public fun testSingle() {
        assertEquals("one", Observable.from("one")!!.toBlockingObservable()!!.single { x -> x!!.length == 3 })
        Observable.from("one", "two")!!.toBlockingObservable()!!.single { x -> x!!.length == 3 }
        fail()
    }

    [Test]
    public fun testDefer() {
        Observable.defer { Observable.from(1, 2) }!!.subscribe(received())
        verify(a, times(1))!!.received(1)
        verify(a, times(1))!!.received(2)
    }

    [Test]
    public fun testDeferEx() {
        { (1 to 2).asObservable() }.defer().subscribe(received())
        verify(a, times(1))!!.received(1)
        verify(a, times(1))!!.received(2)
    }

    [Test]
    public fun testAll() {
        Observable.from(1, 2, 3)!!.all { x -> x!! > 0 }!!.subscribe(received())
        verify(a, times(1))!!.received(true)
    }

    [Test]
    public fun testAllEx() {
        Triple(1, 2, 3).asObservable().all { x -> x!! > 0 }!!.subscribe(received())
        verify(a, times(1))!!.received(true)
    }

    [Test]
    public fun testZip() {
        val o1 = Observable.from(1, 2, 3)!!
        val o2 = Observable.from(4, 5, 6)!!
        val o3 = Observable.from(7, 8, 9)!!

        val values = Observable.zip(o1, o2, o3) { a, b, c -> listOf(a, b, c) }!!.toList()!!.toBlockingObservable()!!.single()!!
        assertEquals(listOf(1, 4, 7), values[0])
        assertEquals(listOf(2, 5, 8), values[1])
        assertEquals(listOf(3, 6, 9), values[2])
    }

    [Test]
    public fun testZipWithIterable() {
        val o1 = Observable.from(1, 2, 3)!!
        val o2 = Observable.from(4, 5, 6)!!
        val o3 = Observable.from(7, 8, 9)!!

        val values = Observable.zip(listOf(o1, o2, o3)) { args -> listOf(*args) }!!.toList()!!.toBlockingObservable()!!.single()!!
        assertEquals(listOf(1, 4, 7), values[0])
        assertEquals(listOf(2, 5, 8), values[1])
        assertEquals(listOf(3, 6, 9), values[2])
    }

    [Test]
    public fun testGroupBy() {
        var count = 0

        Observable.from("one", "two", "three", "four", "five", "six")!!
                .groupBy { s -> s!!.length }!!
                .mapMany { groupObervable ->
            groupObervable!!.map { s ->
                "Value: $s Group ${groupObervable.getKey()}"
            }
        }!!
                .toBlockingObservable()!!.forEach { s ->
            println(s)
            count++
        }

        assertEquals(6, count)
    }

    public trait ScriptAssertion{
        fun error(e: Throwable?)

        fun received(e: Any?)
    }

    val funOnSubscribe: (Int, Observer<in String>) -> Subscription = { counter, observer ->
        observer.onNext("hello_$counter")
        observer.onCompleted()
        Subscription { }
    }

    val asyncObservable: (Observer<in Int>) -> Subscription = { observer ->
        thread {
            Thread.sleep(50)
            observer.onNext(1)
            observer.onNext(2)
            observer.onNext(3)
            observer.onCompleted()
        }
        Subscriptions.empty()!!
    }

    /**
     * Copied from (funKTionale)[https://github.com/MarioAriasC/funKTionale/blob/master/src/main/kotlin/org/funktionale/partials/namespace.kt]
     */
    public fun <P1, P2, R> Function2<P1, P2, R>.partially1(p1: P1): (P2) -> R {
        return {(p2: P2) -> this(p1, p2) }
    }

    inner public class TestFactory(){
        var counter = 1

        val numbers: Observable<Int>
            get(){
                return listOf(1, 3, 2, 5, 4).asObservable()
            }

        val onSubscribe: (Observer<in String>) -> Subscription
            get(){
                return funOnSubscribe.partially1(counter++)
            }

        val observable: Observable<String>
            get(){
                return onSubscribe.asObservable()
            }

    }
}