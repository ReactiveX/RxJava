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
package rx.lang.scala.examples

import java.io.IOException
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

import scala.concurrent.Await
import scala.collection.mutable
import scala.concurrent.duration.Duration
import scala.concurrent.duration.DurationInt
import scala.concurrent.duration.DurationLong
import scala.language.postfixOps
import scala.language.implicitConversions

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assert.assertFalse
import org.junit.Ignore
import org.junit.Test
import org.scalatest.junit.JUnitSuite

import rx.lang.scala._
import rx.lang.scala.schedulers._

/**
 * Demo how the different operators can be used. In Eclipse, you can right-click
 * a test and choose "Run As" > "Scala JUnit Test".
 * 
 * For each operator added to Observable.java, we add a little usage demo here.
 * It does not need to test the functionality (that's already done by the tests in
 * RxJava core), but it should demonstrate how it can be used, to make sure that
 * the method signature makes sense.
 */
@Ignore // Since this doesn't do automatic testing, don't increase build time unnecessarily
class RxScalaDemo extends JUnitSuite {

  @Test def subscribeExample() {
    val o = Observable.just(1, 2, 3)

    // Generally, we have two methods, `subscribe` and `foreach`, to listen to the messages from an Observable.
    // `foreach` is just an alias to `subscribe`.
    o.subscribe(
      n => println(n),
      e => e.printStackTrace(),
      () => println("done")
    )

    o.foreach(
      n => println(n),
      e => e.printStackTrace(),
      () => println("done")
    )

    // For-comprehension is also an alternative, if you are only interested in `onNext`
    for (i <- o) {
      println(i)
    }
  }

  @Test def intervalExample() {
    val o = Observable.interval(200 millis).take(5)
    o.subscribe(n => println("n = " + n))

    // need to wait here because otherwise JUnit kills the thread created by interval()
    waitFor(o)

    println("done")
  }

  def msTicks(start: Long, step: Long): Observable[Long] = {
    // will be easier once we have Observable.generate method
    Observable.interval(step millis) map (_ * step + start)
  }

  def prefixedTicks(start: Long, step: Long, prefix: String): Observable[String] = {
    msTicks(start, step).map(prefix + _)
  }

  @Test def testTicks() {
    val o = prefixedTicks(5000, 500, "t = ").take(5)
    o.subscribe(output(_))
    waitFor(o)
  }

  @Test def testSwitch() {
    // We do not have ultimate precision: Sometimes, 747 gets through, sometimes not
    val o = Observable.interval(1000 millis).map(n => prefixedTicks(0, 249, s"Observable#$n: "))
      .switch.take(16)
    o.subscribe(output(_))
    waitFor(o)
  }

  @Test def testSwitchOnObservableOfInt() {
    // Correctly rejected with error
    // "Cannot prove that Observable[Int] <:< Observable[Observable[U]]"
    // val o = Observable(1, 2).switch
  }

  @Test def testObservableComparison() {
    val first = Observable.from(List(10, 11, 12))
    val second = Observable.from(List(10, 11, 12))

    val b = (first zip second) forall { case (a, b) => a == b }

    assertTrue(b.toBlocking.single)
  }

  @Test def testObservableComparisonWithForComprehension() {
    val first = Observable.from(List(10, 11, 12))
    val second = Observable.from(List(10, 11, 12))

    val booleans = for ((n1, n2) <- (first zip second)) yield (n1 == n2)

    val b1 = booleans.forall(identity)

    assertTrue(b1.toBlocking.single)
  }

  @Test def testStartWithIsUnnecessary() {
    val before = List(-2, -1, 0).toObservable
    val source = List(1, 2, 3).toObservable
    println((before ++ source).toBlocking.toList)
  }

  @Test def mergeTwoExample() {
    val slowNumbers = Observable.interval(400 millis).take(5).map("slow " + _)
    val fastNumbers = Observable.interval(200 millis).take(10).map("fast " + _)
    val o = (slowNumbers merge fastNumbers)
    o.subscribe(output(_))
    waitFor(o)
  }

  def myInterval(period: Long): Observable[String] = {
    Observable.interval(period.millis).map(n => s"Obs-$period emits $n")
  }

  @Test def flattenManyExample() {
    val o = Observable.interval(500 millis).map(n => myInterval((n+1)*100))
    val stopper = Observable.interval(5 seconds)
    o.flatten.takeUntil(stopper).toBlocking.foreach(println(_))
  }

  @Test def flattenSomeExample() {
    // To merge some observables which are all known already:
    List(
      Observable.interval(200 millis),
      Observable.interval(400 millis),
      Observable.interval(800 millis)
    ).toObservable.flatten.take(12).toBlocking.foreach(println(_))
  }

  @Test def flattenExample() {
    List(
      Observable.interval(200 millis).map(_ => 1).take(5),
      Observable.interval(200 millis).map(_ => 2).take(5),
      Observable.interval(200 millis).map(_ => 3).take(5),
      Observable.interval(200 millis).map(_ => 4).take(5)
    ).toObservable.flatten.toBlocking.foreach(println(_))
  }

  @Test def flattenExample2() {
    List(
      Observable.interval(200 millis).map(_ => 1).take(5),
      Observable.interval(200 millis).map(_ => 2).take(5),
      Observable.interval(200 millis).map(_ => 3).take(5),
      Observable.interval(200 millis).map(_ => 4).take(5)
    ).toObservable.flatten(2).toBlocking.foreach(println(_))
  }

  @Test def tumblingBufferExample() {
    val o = Observable.from(1 to 18)
    o.tumblingBuffer(5).subscribe((l: Seq[Int]) => println(l.mkString("[", ", ", "]")))
  }

  @Test def tumblingBufferExample2() {
    val o = Observable.from(1 to 18).zip(Observable.interval(100 millis)).map(_._1)
    val boundary = Observable.interval(500 millis)
    o.tumblingBuffer(boundary).toBlocking.foreach((l: Seq[Int]) => println(l.mkString("[", ", ", "]")))
  }

  @Test def slidingBufferExample() {
    val o = Observable.from(1 to 18).slidingBuffer(4, 2)
    o.subscribe(println(_))
  }

  @Test def slidingBufferExample2() {
    val open = Observable.interval(300 millis)
    val closing = Observable.interval(600 millis)
    val o = Observable.interval(100 millis).take(20).slidingBuffer(open)(_ => closing)
    o.zipWithIndex.toBlocking.foreach {
      case (seq, i) => println(s"Observable#$i emits $seq")
    }
  }

  @Test def slidingBufferExample3() {
    val o = Observable.from(1 to 18).zip(Observable.interval(100 millis)).map(_._1)
    o.slidingBuffer(500 millis, 200 millis).toBlocking.foreach((l: Seq[Int]) => println(l.mkString("[", ", ", "]")))
  }

  @Test def tumblingExample() {
    (for ((o, i) <- Observable.from(1 to 18).tumbling(5).zipWithIndex; n <- o)
      yield s"Observable#$i emits $n"
    ).subscribe(output(_))
  }

  @Test def tumblingExample2() {
    val windowObservable = Observable.interval(500 millis)
    val o = Observable.from(1 to 20).zip(Observable.interval(100 millis)).map(_._1)
    (for ((o, i) <- o.tumbling(windowObservable).zipWithIndex; n <- o)
      yield s"Observable#$i emits $n"
    ).toBlocking.foreach(println)
  }

  @Test def slidingExample() {
    val o = Observable.from(1 to 18).sliding(4, 2)
    (for ((o, i) <- o.zipWithIndex;
          n <- o)
      yield s"Observable#$i emits $n"
    ).toBlocking.foreach(println)
  }

  @Test def slidingExample2() {
    val o = Observable.interval(100 millis).take(20).sliding(500 millis, 200 millis)
    (for ((o, i) <- o.zipWithIndex;
          n <- o)
      yield s"Observable#$i emits $n"
    ).toBlocking.foreach(println)
  }

  @Test def slidingExample3() {
    val open = Observable.interval(300 millis)
    val closing = Observable.interval(600 millis)
    val o = Observable.interval(100 millis).take(20).sliding(open)(_ => closing)
    (for ((o, i) <- o.zipWithIndex;
          n <- o)
      yield s"Observable#$i emits $n"
    ).toBlocking.foreach(println)
  }

  @Test def testReduce() {
    assertEquals(10, List(1, 2, 3, 4).toObservable.reduce(_ + _).toBlocking.single)
  }

  @Test def testForeach() {
    val numbers = Observable.interval(200 millis).take(3)

    // foreach is not available on normal Observables:
    // for (n <- numbers) println(n+10)

    // but on BlockingObservable, it is:
    for (n <- numbers.toBlocking) println(n+10)
  }

  @Test def testForComprehension() {
    val observables = List(List(1, 2, 3).toObservable, List(10, 20, 30).toObservable).toObservable
    val squares = (for (o <- observables; i <- o if i % 2 == 0) yield i*i)
    assertEquals(squares.toBlocking.toList, List(4, 100, 400, 900))
  }

  @Test def nextExample() {
    val o = Observable.interval(100 millis).take(20)
    for(i <- o.toBlocking.next) {
      println(i)
      Thread.sleep(200)
    }
  }

  @Test def latestExample() {
    val o = Observable.interval(100 millis).take(20)
    for(i <- o.toBlocking.latest) {
      println(i)
      Thread.sleep(200)
    }
  }

  @Test def toFutureExample() {
    val o = Observable.interval(500 millis).take(1)
    val r = Await.result(o.toBlocking.toFuture, 2 seconds)
    println(r)
  }

  @Test def testTwoSubscriptionsToOneInterval() {
    val o = Observable.interval(100 millis).take(8)
    o.subscribe(
      i => println(s"${i}a (on thread #${Thread.currentThread().getId})")
    )
    o.subscribe(
      i => println(s"${i}b (on thread #${Thread.currentThread().getId})")
    )
    waitFor(o)
  }

  @Test def schedulersExample() {
    val o = Observable.interval(100 millis).take(8)
    o.observeOn(NewThreadScheduler()).subscribe(
      i => println(s"${i}a (on thread #${Thread.currentThread().getId})")
    )
    o.observeOn(NewThreadScheduler()).subscribe(
      i => println(s"${i}b (on thread #${Thread.currentThread().getId})")
    )
    waitFor(o)
  }

  @Test def testGroupByThenFlatMap() {
    val m = List(1, 2, 3, 4).toObservable
    val g = m.groupBy(i => i % 2)
    val t = g.flatMap((p: (Int, Observable[Int])) => p._2)
    assertEquals(List(1, 2, 3, 4), t.toBlocking.toList)
  }

  @Test def testGroupByThenFlatMapByForComprehension() {
    val m = List(1, 2, 3, 4).toObservable
    val g = m.groupBy(i => i % 2)
    val t = for ((i, o) <- g; n <- o) yield n
    assertEquals(List(1, 2, 3, 4), t.toBlocking.toList)
  }

  @Test def testGroupByThenFlatMapByForComprehensionWithTiming() {
    val m = Observable.interval(100 millis).take(4)
    val g = m.groupBy(i => i % 2)
    val t = for ((i, o) <- g; n <- o) yield n
    assertEquals(List(0, 1, 2, 3), t.toBlocking.toList)
  }

  @Test def timingTest() {
    val firstOnly = false
    val numbersByModulo3 = Observable.interval(1000 millis).take(9).groupBy(_ % 3)

    (for ((modulo, numbers) <- numbersByModulo3) yield {
      println("Observable for modulo" + modulo + " started")

      if (firstOnly) numbers.take(1) else numbers
    }).flatten.toBlocking.foreach(println(_))
  }

  @Test def timingTest1() {
    val numbersByModulo3 = Observable.interval(1000 millis).take(9).groupBy(_ % 3)

    val t0 = System.currentTimeMillis

    (for ((modulo, numbers) <- numbersByModulo3) yield {
      println("Observable for modulo" + modulo + " started at t = " + (System.currentTimeMillis - t0))
      numbers.map(n => s"${n} is in the modulo-$modulo group")
    }).flatten.toBlocking.foreach(println(_))
  }
  
  @Test def testOlympicYearTicks() {
    Olympics.yearTicks.subscribe(println(_))
    waitFor(Olympics.yearTicks)
  }

  @Test def groupByExample() {
    val medalsByCountry = Olympics.mountainBikeMedals.groupBy(medal => medal.country)

    val firstMedalOfEachCountry =
      for ((country, medals) <- medalsByCountry; firstMedal <- medals.take(1)) yield firstMedal

    firstMedalOfEachCountry.subscribe(medal => {
      println(s"${medal.country} wins its first medal in ${medal.year}")
    })
    
    Olympics.yearTicks.subscribe(year => println(s"\nYear $year starts."))

    waitFor(Olympics.yearTicks)
  }

  @Test def groupByUntilExample() {
    val numbers = Observable.interval(250 millis).take(14)
    val grouped = numbers.groupByUntil(x => x % 2){ case (key, obs) => obs.filter(x => x == 7) }
    val sequenced = (grouped.map({ case (key, obs) => obs.toSeq })).flatten
    sequenced.subscribe(x => println(s"Emitted group: $x"))
  }

  @Test def groupByUntilExample2() {
    val numbers = Observable.interval(250 millis).take(14)
    val grouped = numbers.groupByUntil(x => x % 2, x => x * 10){ case (key, obs) => Observable.interval(2 seconds) }
    val sequenced = (grouped.map({ case (key, obs) => obs.toSeq })).flatten
    sequenced.toBlocking.foreach(x => println(s"Emitted group: $x"))
  }

  @Test def combineLatestExample() {
    val firstCounter = Observable.interval(250 millis)
    val secondCounter = Observable.interval(550 millis)
    val combinedCounter = firstCounter.combineLatestWith(secondCounter)(List(_, _)) take 10

    combinedCounter subscribe {x => println(s"Emitted group: $x")}
    waitFor(combinedCounter)
  }

  @Test def combineLatestExample2() {
    val firstCounter = Observable.interval(250 millis)
    val secondCounter = Observable.interval(550 millis)
    val thirdCounter = Observable.interval(850 millis)
    val sources = Seq(firstCounter, secondCounter, thirdCounter)
    val combinedCounter = Observable.combineLatest(sources)(_.toList).take(10)

    combinedCounter subscribe {x => println(s"Emitted group: $x")}
    waitFor(combinedCounter)
  }

  @Test def olympicsExampleWithoutPublish() {
    val medals = Olympics.mountainBikeMedals.doOnEach(_ => println("onNext"))
    medals.subscribe(println(_)) // triggers an execution of medals Observable
    waitFor(medals) // triggers another execution of medals Observable
  }

  @Test def olympicsExampleWithPublish() {
    val medals = Olympics.mountainBikeMedals.doOnEach(_ => println("onNext")).publish
    medals.subscribe(println(_)) // triggers an execution of medals Observable
    medals.connect
    waitFor(medals) // triggers another execution of medals Observable
  }

  @Test def exampleWithoutPublish() {
    val unshared = Observable.from(1 to 4)
    unshared.subscribe(n => println(s"subscriber 1 gets $n"))
    unshared.subscribe(n => println(s"subscriber 2 gets $n"))
  }

  @Test def exampleWithPublish() {
    val unshared = Observable.from(1 to 4)
    val shared = unshared.publish
    shared.subscribe(n => println(s"subscriber 1 gets $n"))
    shared.subscribe(n => println(s"subscriber 2 gets $n"))
    shared.connect
  }

  @Test def exampleWithPublish2() {
    val unshared = Observable.from(1 to 4)
    val shared = unshared.publish(0)
    shared.subscribe(n => println(s"subscriber 1 gets $n"))
    shared.subscribe(n => println(s"subscriber 2 gets $n"))
    shared.connect
  }

  @Test def exampleWithPublish3() {
    val o = Observable.interval(100 millis).take(5).publish((o: Observable[Long]) => o.map(_ * 2))
    o.subscribe(n => println(s"subscriber 1 gets $n"))
    o.subscribe(n => println(s"subscriber 2 gets $n"))
    Thread.sleep(1000)
  }

  @Test def exampleWithPublish4() {
    val o = Observable.interval(100 millis).take(5).publish((o: Observable[Long]) => o.map(_ * 2), -1L)
    o.subscribe(n => println(s"subscriber 1 gets $n"))
    o.subscribe(n => println(s"subscriber 2 gets $n"))
    Thread.sleep(1000)
  }

  def doLater(waitTime: Duration, action: () => Unit): Unit = {
    Observable.interval(waitTime).take(1).subscribe(_ => action())
  }

  @Test def exampleWithoutReplay() {
    val numbers = Observable.interval(1000 millis).take(6)
    val sharedNumbers = numbers.publish
    sharedNumbers.subscribe(n => println(s"subscriber 1 gets $n"))
    sharedNumbers.connect
    // subscriber 2 misses 0, 1, 2!
    doLater(3500 millis, () => { sharedNumbers.subscribe(n => println(s"subscriber 2 gets $n")) })
    waitFor(sharedNumbers)
  }

  @Test def exampleWithReplay() {
    val numbers = Observable.interval(1000 millis).take(6)
    val sharedNumbers = numbers.replay
    sharedNumbers.subscribe(n => println(s"subscriber 1 gets $n"))
    sharedNumbers.connect
    // subscriber 2 subscribes later but still gets all numbers
    doLater(3500 millis, () => { sharedNumbers.subscribe(n => println(s"subscriber 2 gets $n")) })
    waitFor(sharedNumbers)
  }

  @Test def exampleWithReplay2() {
    val numbers = Observable.interval(100 millis).take(10)
    val sharedNumbers = numbers.replay(3)
    sharedNumbers.subscribe(n => println(s"subscriber 1 gets $n"))
    sharedNumbers.connect
    // subscriber 2 subscribes later but only gets the 3 buffered numbers and the following numbers
    Thread.sleep(700)
    sharedNumbers.subscribe(n => println(s"subscriber 2 gets $n"))
    waitFor(sharedNumbers)
  }

  @Test def exampleWithReplay3() {
    val numbers = Observable.interval(100 millis).take(10)
    val sharedNumbers = numbers.replay(300 millis)
    sharedNumbers.subscribe(n => println(s"subscriber 1 gets $n"))
    sharedNumbers.connect
    // subscriber 2 subscribes later but only gets the buffered numbers and the following numbers
    Thread.sleep(700)
    sharedNumbers.subscribe(n => println(s"subscriber 2 gets $n"))
    waitFor(sharedNumbers)
  }

  @Test def exampleWithReplay4() {
    val numbers = Observable.interval(100 millis).take(10)
    val sharedNumbers = numbers.replay(2, 300 millis)
    sharedNumbers.subscribe(n => println(s"subscriber 1 gets $n"))
    sharedNumbers.connect
    // subscriber 2 subscribes later but only gets the buffered numbers and the following numbers
    Thread.sleep(700)
    sharedNumbers.subscribe(n => println(s"subscriber 2 gets $n"))
    waitFor(sharedNumbers)
  }

  @Test def exampleWithReplay5() {
    val numbers = Observable.interval(100 millis).take(10)
    val sharedNumbers = numbers.replay(o => o.map(_ * 2))
    sharedNumbers.subscribe(n => println(s"subscriber gets $n"))
    waitFor(sharedNumbers)
  }

  @Test def testSingleOption() {
    assertEquals(None,    List(1, 2).toObservable.toBlocking.singleOption)
    assertEquals(Some(1), List(1).toObservable.toBlocking.singleOption)
    assertEquals(None,    List().toObservable.toBlocking.singleOption)
  }

  // We can't put a general average method into Observable.scala, because Scala's Numeric
  // does not have scalar multiplication (we would need to calculate (1.0/numberOfElements)*sum)
  def doubleAverage(o: Observable[Double]): Observable[Double] = {
    for ((finalSum, finalCount) <- o.foldLeft((0.0, 0))({case ((sum, count), elem) => (sum+elem, count+1)}))
    yield finalSum / finalCount
  }

  @Test def averageExample() {
    println(doubleAverage(Observable.empty).toBlocking.single)
    println(doubleAverage(List(0.0).toObservable).toBlocking.single)
    println(doubleAverage(List(4.44).toObservable).toBlocking.single)
    println(doubleAverage(List(1, 2, 3.5).toObservable).toBlocking.single)
  }

  @Test def testSum() {
    assertEquals(10, List(1, 2, 3, 4).toObservable.sum.toBlocking.single)
    assertEquals(6, List(4, 2).toObservable.sum.toBlocking.single)
    assertEquals(0, List[Int]().toObservable.sum.toBlocking.single)
  }

  @Test def testProduct() {
    assertEquals(24, List(1, 2, 3, 4).toObservable.product.toBlocking.single)
    assertEquals(8, List(4, 2).toObservable.product.toBlocking.single)
    assertEquals(1, List[Int]().toObservable.product.toBlocking.single)
  }

  @Test def mapWithIndexExample() {
    // We don't need mapWithIndex because we already have zipWithIndex, which we can easily
    // combine with map:
    List("a", "b", "c").toObservable.zipWithIndex.map(pair => pair._1 + " has index " + pair._2)
      .toBlocking.foreach(println(_))

    // Or even nicer with for-comprehension syntax:
    (for ((letter, index) <- List("a", "b", "c").toObservable.zipWithIndex) yield letter + " has index " + index)
      .toBlocking.foreach(println(_))
  }

  // source Observables are all known:
  @Test def zip3Example() {
    val o = Observable.zip(List(1, 2).toObservable, List(10, 20).toObservable, List(100, 200).toObservable)
    (for ((n1, n2, n3) <- o) yield s"$n1, $n2 and $n3")
      .toBlocking.foreach(println(_))
  }

  // source Observables are in an Observable:
  @Test def zipManyObservableExample() {
    val observables = List(List(1, 2).toObservable, List(10, 20).toObservable, List(100, 200).toObservable).toObservable
    (for (seq <- Observable.zip(observables)) yield seq.mkString("(", ", ", ")"))
      .toBlocking.foreach(println(_))
  }

  /**
   * This is a bad way of using `zip` with an `Iterable`: even if the consumer unsubscribes,
   * some elements may still be pulled from `Iterable`.
   */
  @Test def zipWithIterableBadExample() {
    val o1 = Observable.interval(100 millis, IOScheduler()).map(_ * 100).take(3)
    val o2 = Observable.from(0 until Int.MaxValue).doOnEach(i => println(i + " from o2"))
    o1.zip(o2).toBlocking.foreach(println(_))
  }

  /**
   * This is a good way of using `zip` with an `Iterable`: if the consumer unsubscribes,
   * no more elements will be pulled from `Iterable`.
   */
  @Test def zipWithIterableGoodExample() {
    val o1 = Observable.interval(100 millis, IOScheduler()).map(_ * 100).take(3)
    val iter = (0 until Int.MaxValue).view.map {
      i => {
        println(i + " from iter")
        i
      }
    }
    o1.zip(iter).toBlocking.foreach(println(_))
  }

  @Test def zipWithExample() {
    val xs = Observable.just(1, 3, 5, 7)
    val ys = Observable.just(2, 4, 6, 8)
    xs.zipWith(ys)(_ * _).subscribe(println(_))
  }

  @Test def takeFirstWithCondition() {
    val condition: Int => Boolean = _ >= 3
    assertEquals(3, List(1, 2, 3, 4).toObservable.filter(condition).first.toBlocking.single)
  }

  @Test def firstOrDefaultWithCondition() {
    val condition: Int => Boolean = _ >= 3
    assertEquals(3, List(1, 2, 3, 4).toObservable.filter(condition).firstOrElse(10).toBlocking.single)
    assertEquals(10, List(-1, 0, 1).toObservable.filter(condition).firstOrElse(10).toBlocking.single)
  }

  @Test def firstLastSingleExample() {
    assertEquals(1, List(1, 2, 3, 4).toObservable.head.toBlocking.single)
    assertEquals(1, List(1, 2, 3, 4).toObservable.first.toBlocking.single)
    assertEquals(4, List(1, 2, 3, 4).toObservable.last.toBlocking.single)
    assertEquals(1, List(1).toObservable.single.toBlocking.single)

    assertEquals(1, List(1, 2, 3, 4).toObservable.toBlocking.head)
    assertEquals(1, List(1, 2, 3, 4).toObservable.toBlocking.first)
    assertEquals(4, List(1, 2, 3, 4).toObservable.toBlocking.last)
    assertEquals(1, List(1).toObservable.toBlocking.single)
  }

  @Test def dropExample() {
    val o = List(1, 2, 3, 4).toObservable
    assertEquals(List(3, 4), o.drop(2).toBlocking.toList)
  }

  @Test def dropWithTimeExample() {
    val o = List(1, 2, 3, 4).toObservable.zip(
      Observable.interval(500 millis, IOScheduler())).map(_._1) // emit every 500 millis
    println(
      o.drop(1250 millis, IOScheduler()).toBlocking.toList // output List(3, 4)
    )
  }

  @Test def dropRightExample() {
    val o = List(1, 2, 3, 4).toObservable
    assertEquals(List(1, 2), o.dropRight(2).toBlocking.toList)
  }

  @Test def dropRightWithTimeExample() {
    val o = List(1, 2, 3, 4).toObservable.zip(
      Observable.interval(500 millis, IOScheduler())).map(_._1) // emit every 500 millis
    println(
      o.dropRight(750 millis, IOScheduler()).toBlocking.toList // output List(1, 2)
    )
  }

  @Test def dropUntilExample() {
    val o = List("Alice", "Bob", "Carlos").toObservable.zip(
      Observable.interval(700 millis, IOScheduler())).map(_._1) // emit every 700 millis
    val other = List(1).toObservable.delay(1 seconds)
    println(
      o.dropUntil(other).toBlocking.toList // output List("Bob", "Carlos")
    )
  }

  def square(x: Int): Int = {
    println(s"$x*$x is being calculated on thread ${Thread.currentThread().getId}")
    Thread.sleep(100) // calculating a square is heavy work :)
    x*x
  }

  def work(o1: Observable[Int]): Observable[String] = {
    println(s"map() is being called on thread ${Thread.currentThread().getId}")
    o1.map(i => s"The square of $i is ${square(i)}")
  }

  @Test def parallelExample() {
    val t0 = System.currentTimeMillis()
    Observable.from(1 to 10).parallel(work(_)).toBlocking.foreach(println(_))
    println(s"Work took ${System.currentTimeMillis()-t0} ms")
  }

  @Test def exampleWithoutParallel() {
    val t0 = System.currentTimeMillis()
    work(Observable.from(1 to 10)).toBlocking.foreach(println(_))
    println(s"Work took ${System.currentTimeMillis()-t0} ms")
  }

  @Test def toSortedList() {
    assertEquals(Seq(7, 8, 9, 10), List(10, 7, 8, 9).toObservable.toSeq.map(_.sorted).toBlocking.single)
    val f = (a: Int, b: Int) => b < a
    assertEquals(Seq(10, 9, 8, 7), List(10, 7, 8, 9).toObservable.toSeq.map(_.sortWith(f)).toBlocking.single)
  }

  @Test def timestampExample() {
    val timestamped = Observable.interval(100 millis).take(6).timestamp.toBlocking
    for ((millis, value) <- timestamped if value > 0) {
      println(value + " at t = " + millis)
    }
  }

  @Test def materializeExample1() {
    def printObservable[T](o: Observable[T]): Unit = {
      import Notification._
      o.materialize.subscribe(n => n match {
        case OnNext(v) => println("Got value " + v)
        case OnCompleted => println("Completed")
        case OnError(err) => println("Error: " + err.getMessage)
      })
    }

    val o1 = Observable.interval(100 millis).take(3)
    val o2 = Observable.error(new IOException("Oops"))
    printObservable(o1)
    printObservable(o2)
    Thread.sleep(500)
  }

  @Test def materializeExample2() {
    import Notification._
    List(1, 2, 3).toObservable.materialize.subscribe(n => n match {
      case OnNext(v) => println("Got value " + v)
      case OnCompleted => println("Completed")
      case OnError(err) => println("Error: " + err.getMessage)
    })
  }

  @Test def notificationSubtyping() {
    import Notification._
    val oc1: Notification[Nothing] = OnCompleted
    val oc2: Notification[Int] = OnCompleted
    val oc3: rx.Notification[_ <: Int] = oc2.asJavaNotification
    val oc4: rx.Notification[_ <: Any] = oc2.asJavaNotification
  }

  @Test def takeWhileWithIndexAlternative {
    val condition = true
    List("a", "b").toObservable.zipWithIndex.takeWhile{case (elem, index) => condition}.map(_._1)
  }
  
  def calculateElement(index: Int): String = {
    println("omg I'm calculating so hard")
    index match {
      case 0 => "a"
      case 1 => "b"
      case _ => throw new IllegalArgumentException
    }
  }
  
  /**
   * This is a bad way of using Observable.create, because even if the consumer unsubscribes,
   * all elements are calculated.
   */
  @Test def createExampleBad() {
    val o = Observable.create[String](observer => {
      observer.onNext(calculateElement(0))
      observer.onNext(calculateElement(1))
      observer.onCompleted()
      Subscription {}
    })
    o.take(1).subscribe(println(_))
  }
  
  /**
   * This is the good way of doing it: If the consumer unsubscribes, no more elements are 
   * calculated.
   */
  @Test def createExampleGood() {
    val o = Observable[String](subscriber => {
      var i = 0
      while (i < 2 && !subscriber.isUnsubscribed) {
        subscriber.onNext(calculateElement(i))
        i += 1
      }
      if (!subscriber.isUnsubscribed) subscriber.onCompleted()
    })
    o.take(1).subscribe(println(_))
  }

  @Test def createExampleGood2() {
    import scala.io.{Codec, Source}

    val rxscala = Observable[String](subscriber => {
      try {
        val input = new java.net.URL("http://rxscala.github.io/").openStream()
        subscriber.add(Subscription {
          input.close()
        })
        Source.fromInputStream(input)(Codec.UTF8).getLines()
          .takeWhile(_ => !subscriber.isUnsubscribed)
          .foreach(subscriber.onNext(_))
        if (!subscriber.isUnsubscribed) {
          subscriber.onCompleted()
        }
      }
      catch {
        case e: Throwable => if (!subscriber.isUnsubscribed) subscriber.onError(e)
      }
    }).subscribeOn(IOScheduler())

    val count = rxscala.flatMap(_.split("\\W+").toSeq.toObservable)
      .map(_.toLowerCase)
      .filter(_ == "rxscala")
      .size
    println(s"RxScala appears ${count.toBlocking.single} times in http://rxscala.github.io/")
  }

  @Test def createExampleWithBackpressure() {
    val o = Observable {
      subscriber: Subscriber[String] => {
        var emitted = 0
        subscriber.setProducer(n => {
            val intN = if (n >= 10) 10 else n.toInt
            (0 until intN)
              .takeWhile(_ => emitted < 10 && !subscriber.isUnsubscribed)
              .foreach {
              i =>
                emitted += 1
                subscriber.onNext(s"item ${emitted}")
            }
            if (emitted == 10 && !subscriber.isUnsubscribed) {
              subscriber.onCompleted()
            }
        })
      }
    }.subscribeOn(IOScheduler()) // Use `subscribeOn` to make sure `Producer` will run in the same Scheduler
    o.observeOn(ComputationScheduler()).subscribe(new Subscriber[String] {
      override def onStart() {
        println("Request a new one at the beginning")
        request(1)
      }

      override def onNext(v: String) {
        println("Received " + v)
        println("Request a new one after receiving " + v)
        request(1)
      }

      override def onError(e: Throwable) {
        e.printStackTrace()
      }

      override def onCompleted() {
        println("Done")
      }
    })
    Thread.sleep(10000)
  }

  def output(s: String): Unit = println(s)

  /** Subscribes to obs and waits until obs has completed. Note that if you subscribe to
   *  obs yourself and also call waitFor(obs), all side-effects of subscribing to obs
   *  will happen twice.
   */
  def waitFor[T](obs: Observable[T]): Unit = {
    obs.toBlocking.toIterable.last
  }

  @Test def doOnCompletedExample(): Unit = {
    val o = List("red", "green", "blue").toObservable.doOnCompleted { println("onCompleted") }
    o.subscribe(v => println(v), e => e.printStackTrace)
    // red
    // green
    // blue
    // onCompleted
  }

  @Test def doOnTerminateExample(): Unit = {
    val o = List("red", "green", "blue").toObservable.doOnTerminate { println("terminate") }
    o.subscribe(v => println(v), e => e.printStackTrace, () => println("onCompleted"))
    // red
    // green
    // blue
    // terminate
    // onCompleted
  }

  @Test def finallyDoExample(): Unit = {
    val o = List("red", "green", "blue").toObservable.finallyDo { println("finally") }
    o.subscribe(v => println(v), e => e.printStackTrace, () => println("onCompleted"))
    // red
    // green
    // blue
    // onCompleted
    // finally
  }

  @Test def timeoutExample(): Unit = {
    val other = List(100L, 200L, 300L).toObservable
    val result = Observable.interval(100 millis).timeout(50 millis, other).toBlocking.toList
    println(result)
  }

  @Test def timeoutExample2(): Unit = {
    val firstTimeoutSelector = () => {
      Observable.timer(10 seconds, 10 seconds, ComputationScheduler()).take(1)
    }
    val timeoutSelector = (t: Long) => {
      Observable.timer(
        (500 - t * 100) max 1 millis,
        (500 - t * 100) max 1 millis,
        ComputationScheduler()).take(1)
    }
    val other = List(100L, 200L, 300L).toObservable
    val result = Observable.interval(100 millis).timeout(firstTimeoutSelector, timeoutSelector, other).toBlocking.toList
    println(result)
  }

  @Test def ambExample(): Unit = {
    val o1 = List(100L, 200L, 300L).toObservable.delay(4 seconds)
    val o2 = List(1000L, 2000L, 3000L).toObservable.delay(2 seconds)
    val result = o1.amb(o2).toBlocking.toList
    println(result)
  }

  @Test def ambWithVarargsExample(): Unit = {
    val o1 = List(100L, 200L, 300L).toObservable.delay(4 seconds)
    val o2 = List(1000L, 2000L, 3000L).toObservable.delay(2 seconds)
    val o3 = List(10000L, 20000L, 30000L).toObservable.delay(4 seconds)
    val result = Observable.amb(o1, o2, o3).toBlocking.toList
    println(result)
  }

  @Test def ambWithSeqExample(): Unit = {
    val o1 = List(100L, 200L, 300L).toObservable.delay(4 seconds)
    val o2 = List(1000L, 2000L, 3000L).toObservable.delay(2 seconds)
    val o3 = List(10000L, 20000L, 30000L).toObservable.delay(4 seconds)
    val o = Seq(o1, o2, o3)
    val result = Observable.amb(o: _*).toBlocking.toList
    println(result)
  }

  @Test def delayExample(): Unit = {
    val o = List(100L, 200L, 300L).toObservable.delay(2 seconds)
    val result = o.toBlocking.toList
    println(result)
  }

  @Test def delayExample2(): Unit = {
    val o = List(100L, 200L, 300L).toObservable.delay(2 seconds, IOScheduler())
    val result = o.toBlocking.toList
    println(result)
  }

  @Test def delayExample3(): Unit = {
    val o = List(100, 500, 200).toObservable.delay(
      (i: Int) => Observable.just(i).delay(i millis)
    )
    o.toBlocking.foreach(println(_))
  }

  @Test def delayExample4(): Unit = {
    val o = List(100, 500, 200).toObservable.delay(
      () => Observable.interval(500 millis).take(1),
      (i: Int) => Observable.just(i).delay(i millis)
    )
    o.toBlocking.foreach(println(_))
  }

  @Test def delaySubscriptionExample(): Unit = {
    val o = List(100L, 200L, 300L).toObservable.delaySubscription(2 seconds)
    val result = o.toBlocking.toList
    println(result)
  }

  @Test def delaySubscriptionExample2(): Unit = {
    val o = List(100L, 200L, 300L).toObservable.delaySubscription(2 seconds, IOScheduler())
    val result = o.toBlocking.toList
    println(result)
  }

  @Test def elementAtExample(): Unit = {
    val o = List("red", "green", "blue").toObservable
    println(o.elementAt(2).toBlocking.single)
  }

  @Test def elementAtOrDefaultExample(): Unit = {
    val o : Observable[Seq[Char]] = List("red".toList, "green".toList, "blue".toList).toObservable.elementAtOrDefault(3, "black".toSeq)
    println(o.toBlocking.single)
  }

  @Test def toMapExample1(): Unit = {
    val o : Observable[String] = List("alice", "bob", "carol").toObservable
    val keySelector = (s: String) => s.head
    val m = o.toMap(keySelector)
    println(m.toBlocking.single)
  }

  @Test def toMapExample2(): Unit = {
    val o : Observable[String] = List("alice", "bob", "carol").toObservable
    val keySelector = (s: String) => s.head
    val valueSelector = (s: String) => s.tail
    val m = o.toMap(keySelector, valueSelector)
    println(m.toBlocking.single)
  }

  @Test def toMapExample3(): Unit = {
    val o : Observable[String] = List("alice", "bob", "carol").toObservable
    val keySelector = (s: String) => s.head
    val valueSelector = (s: String) => s.tail
    val mapFactory = () => Map(('s',"tart"))
    val m = o.toMap(keySelector, valueSelector, mapFactory)
    println(m.toBlocking.single)
  }

  @Test def toMultimapExample1(): Unit = {
    val o : Observable[String] = List("alice", "bob", "carol", "allen", "clarke").toObservable
    val keySelector = (s: String) => s.head
    val m = o.toMultimap(keySelector)
    println(m.toBlocking.single)
  }

  @Test def toMultimapExample2(): Unit = {
    val o : Observable[String] = List("alice", "bob", "carol", "allen", "clarke").toObservable
    val keySelector = (s: String) => s.head
    val valueSelector = (s: String) => s.tail
    val m = o.toMultimap(keySelector, valueSelector)
    println(m.toBlocking.single)
  }

  @Test def toMultimapExample3(): Unit = {
    val o: Observable[String] = List("alice", "bob", "carol", "allen", "clarke").toObservable
    val keySelector = (s: String) => s.head
    val valueSelector = (s: String) => s.tail
    val mapFactory = () => mutable.Map('d' -> mutable.Buffer("oug"))
    val m = o.toMultimap(keySelector, valueSelector, mapFactory)
    println(m.toBlocking.single.mapValues(_.toList))
  }

  @Test def toMultimapExample4(): Unit = {
    val o : Observable[String] = List("alice", "bob", "carol", "allen", "clarke").toObservable
    val keySelector = (s: String) => s.head
    val valueSelector = (s: String) => s.tail
    val mapFactory = () => mutable.Map('d' -> mutable.ListBuffer("oug"))
    val bufferFactory = (k: Char) => mutable.ListBuffer[String]()
    val m = o.toMultimap(keySelector, valueSelector, mapFactory, bufferFactory)
    println(m.toBlocking.single)
  }

  @Test def containsExample(): Unit = {
    val o1 = List(1, 2, 3).toObservable.contains(2)
    assertTrue(o1.toBlocking.single)

    val o2 = List(1, 2, 3).toObservable.contains(4)
    assertFalse(o2.toBlocking.single)
  }

  @Test def repeatExample1(): Unit = {
    val o : Observable[String] = List("alice", "bob", "carol").toObservable.repeat.take(6)
    assertEquals(List("alice", "bob", "carol", "alice", "bob", "carol"), o.toBlocking.toList)
  }

  @Test def repeatExample2(): Unit = {
    val o : Observable[String] = List("alice", "bob", "carol").toObservable.repeat(2)
    assertEquals(List("alice", "bob", "carol", "alice", "bob", "carol"), o.toBlocking.toList)
  }

  @Test def retryExample1(): Unit = {
    val o : Observable[String] = List("alice", "bob", "carol").toObservable
    assertEquals(List("alice", "bob", "carol"), o.retry.toBlocking.toList)
  }

  @Test def retryExample2(): Unit = {
    val o : Observable[String] = List("alice", "bob", "carol").toObservable
    assertEquals(List("alice", "bob", "carol"), o.retry(3).toBlocking.toList)
  }

  @Test def retryExample3(): Unit = {
    var isFirst = true
    val o = Observable {
      (subscriber: Subscriber[String]) =>
        if (isFirst) {
          subscriber.onNext("alice")
          subscriber.onError(new IOException("Oops"))
          isFirst = false
        }
        else {
          subscriber.onNext("bob")
          subscriber.onError(new RuntimeException("Oops"))
        }
    }
    o.retry {
      (times, e) => e match {
        case e: IOException => times <= 3
        case _ => false
      }
    }.subscribe(s => println(s), e => e.printStackTrace())
  }

  @Test def retryWhenExample(): Unit = {
    Observable[String]({ subscriber =>
      println("subscribing")
      subscriber.onError(new RuntimeException("always fails"))
    }).retryWhen(attempts => {
      attempts.zipWith(Observable.from(1 to 3))((n, i) => i).flatMap(i => {
        println("delay retry by " + i + " second(s)")
        Observable.timer(Duration(i, TimeUnit.SECONDS))
      })
    }).toBlocking.foreach(s => println(s))
  }

  @Test def repeatWhenExample(): Unit = {
    Observable[String]({ subscriber =>
      println("subscribing")
      subscriber.onCompleted()
    }).repeatWhen(attempts => {
      attempts.zipWith(Observable.from(1 to 3))((n, i) => i).flatMap(i => {
        println("delay repeat by " + i + " second(s)")
        Observable.timer(Duration(i, TimeUnit.SECONDS)).materialize
      })
    }, NewThreadScheduler()).toBlocking.foreach(s => println(s))
  }

  @Test def liftExample1(): Unit = {
    // Add "No. " in front of each item
    val o = List(1, 2, 3).toObservable.lift {
      subscriber: Subscriber[String] =>
        Subscriber[Int](
          subscriber,
          (v: Int)  => subscriber.onNext("No. " + v),
          e => subscriber.onError(e),
          () => subscriber.onCompleted
        )
    }.toBlocking.toList
    println(o)
  }

  @Test def liftExample2(): Unit = {
    // Split the input Strings with " "
    val splitStringsWithSpace = (subscriber: Subscriber[String]) => {
      Subscriber[String](
        subscriber,
        (v: String) => v.split(" ").foreach(subscriber.onNext(_)),
        e => subscriber.onError(e),
        () => subscriber.onCompleted
      )
    }

    // Convert the input Strings to Chars
    val stringsToChars = (subscriber: Subscriber[Char]) => {
      Subscriber[String](
        subscriber,
        (v: String) => v.foreach(subscriber.onNext(_)),
        e => subscriber.onError(e),
        () => subscriber.onCompleted
      )
    }

    // Skip the first n items. If the length of source is less than n, throw an IllegalArgumentException
    def skipWithException[T](n: Int) = (subscriber: Subscriber[T]) => {
      var count = 0
      Subscriber[T](
        subscriber,
        (v: T) => {
          if (count >= n) subscriber.onNext(v)
          count += 1
        },
        e => subscriber.onError(e),
        () => if (count < n) subscriber.onError(new IllegalArgumentException("There is no enough items")) else subscriber.onCompleted
      )
    }

    val o = List("RxJava – Reactive Extensions for the JVM").toObservable
      .lift(splitStringsWithSpace)
      .map(_.toLowerCase)
      .lift(stringsToChars)
      .filter(_.isLetter)
      .lift(skipWithException(100))
    try {
      o.toBlocking.toList
    }
    catch {
      case e: IllegalArgumentException => println("IllegalArgumentException from skipWithException")
    }
  }

  @Test def multicastExample1(): Unit = {
    val unshared = Observable.from(1 to 4)
    val shared = unshared.multicast(Subject[Int]())
    shared.subscribe(n => println(s"subscriber 1 gets $n"))
    shared.subscribe(n => println(s"subscriber 2 gets $n"))
    shared.connect
  }

  @Test def multicastExample2(): Unit = {
    val unshared = Observable.from(1 to 4)
    val shared = unshared.multicast(() => Subject[Int]())(o => o.map("No. " + _))
    shared.subscribe(n => println(s"subscriber 1 gets $n"))
    shared.subscribe(n => println(s"subscriber 2 gets $n"))
  }

  @Test def startWithExample(): Unit = {
    val o1 = List(3, 4).toObservable
    val o2 = 1 +: 2 +: o1
    assertEquals(List(1, 2, 3, 4), o2.toBlocking.toList)
  }

  @Test def appendExample(): Unit = {
    val o = List(1, 2).toObservable :+ 3 :+ 4
    assertEquals(List(1, 2, 3, 4), o.toBlocking.toList)
  }

  @Test def sequenceEqualExampe(): Unit = {
    val o1 = List(1, 2, 3).toObservable
    val o2 = List(1, 2, 3).toObservable
    val o3 = List(1, 2).toObservable
    val o4 = List(1.0, 2.0, 3.0).toObservable
    assertTrue(o1.sequenceEqual(o2).toBlocking.single)
    assertFalse(o1.sequenceEqual(o3).toBlocking.single)
    assertTrue(o1.sequenceEqual(o4).toBlocking.single)
  }

  @Test def takeExample(): Unit = {
    val o = (1 to 20).toObservable
      .zip(Observable.interval(300 millis))
      .map(_._1)
      .take(2 seconds)
    println(o.toBlocking.toList)
  }

  @Test def takeRightExample(): Unit = {
    val o = (1 to 6).toObservable.takeRight(3)
    assertEquals(List(4, 5, 6), o.toBlocking.toList)
  }

  @Test def takeRightExample2(): Unit = {
    val o = (1 to 10).toObservable
      .zip(Observable.interval(100 millis))
      .map(_._1)
      .takeRight(300 millis)
    println(o.toBlocking.toList)
  }

  @Test def takeRightExample3(): Unit = {
    val o = (1 to 10).toObservable
      .zip(Observable.interval(100 millis))
      .map(_._1)
      .takeRight(2, 300 millis)
    println(o.toBlocking.toList)
  }

  @Test def timeIntervalExample(): Unit = {
    val o = (1 to 10).toObservable
      .zip(Observable.interval(100 millis))
      .map(_._1)
      .timeInterval
    println(o.toBlocking.toList)
  }

  @Test def schedulerExample1(): Unit = {
    val latch = new CountDownLatch(1)
    val worker = IOScheduler().createWorker
    worker.schedule {
      println("Hello from Scheduler")
      latch.countDown()
    }
    latch.await(5, TimeUnit.SECONDS)
  }

  @Test def schedulerExample2(): Unit = {
    val latch = new CountDownLatch(1)
    val worker = IOScheduler().createWorker
    worker.schedule(1 seconds) {
      println("Hello from Scheduler after 1 second")
      latch.countDown()
    }
    latch.await(5, TimeUnit.SECONDS)
  }

  @Test def schedulerExample3(): Unit = {
    val worker = IOScheduler().createWorker
    var no = 1
    val subscription = worker.schedulePeriodically(initialDelay = 1 seconds, period = 100 millis) {
      println(s"Hello(${no}) from Scheduler")
      no += 1
    }
    TimeUnit.SECONDS.sleep(2)
    subscription.unsubscribe()
  }

  @Test def schedulerExample4(): Unit = {
    val worker = IOScheduler().createWorker
    var no = 1
    def hello: Unit = {
      println(s"Hello(${no}) from Scheduler")
      no += 1
      worker.schedule(100 millis)(hello)
    }
    val subscription = worker.schedule(1 seconds)(hello)
    TimeUnit.SECONDS.sleep(2)
    subscription.unsubscribe()
  }

  def createAHotObservable: Observable[String] = {
    var first = true
    Observable[String] {
      subscriber =>
        if (first) {
          subscriber.onNext("1st: First")
          subscriber.onNext("1st: Last")
          first = false
        }
        else {
          subscriber.onNext("2nd: First")
          subscriber.onNext("2nd: Last")
        }
        subscriber.onCompleted()
    }
  }

  @Test def withoutPublishLastExample() {
    val hot = createAHotObservable
    hot.takeRight(1).subscribe(n => println(s"subscriber 1 gets $n"))
    hot.takeRight(1).subscribe(n => println(s"subscriber 2 gets $n"))
  }

  @Test def publishLastExample() {
    val hot = createAHotObservable
    val o = hot.publishLast
    o.subscribe(n => println(s"subscriber 1 gets $n"))
    o.subscribe(n => println(s"subscriber 2 gets $n"))
    o.connect
  }

  @Test def publishLastExample2() {
    val hot = createAHotObservable
    val o = hot.publishLast(co => co ++ co) // "++" subscribes "co" twice
    o.subscribe(n => println(s"subscriber gets $n"))
  }

  @Test def unsubscribeOnExample() {
    val o = Observable[String] {
      subscriber =>
        subscriber.add(Subscription {
          println("unsubscribe on " + Thread.currentThread().getName())
        })
        subscriber.onNext("RxScala")
        subscriber.onCompleted()
    }
    o.unsubscribeOn(NewThreadScheduler()).subscribe(println(_))
  }

  @Test def parallelMergeExample() {
    val o: Observable[Observable[Int]] = (1 to 100).toObservable.map(_ => (1 to 10).toObservable)
    assertEquals(100, o.size.toBlocking.single)
    assertEquals(1000, o.flatten.size.toBlocking.single)

    val o2: Observable[Observable[Int]] = o.parallelMerge(10, ComputationScheduler())
    assertEquals(10, o2.size.toBlocking.single)
    assertEquals(1000, o2.flatten.size.toBlocking.single)
  }

  @Test def debounceExample() {
    val o = Observable.interval(100 millis).take(20).debounce {
      n =>
        if (n % 2 == 0) {
          Observable.interval(50 millis)
        }
        else {
          Observable.interval(150 millis)
        }
    }
    o.toBlocking.foreach(println(_))
  }

  @Test def flatMapExample() {
    val o = Observable.just(10, 100)
    o.flatMap(n => Observable.interval(200 millis).map(_ * n))
      .take(20)
      .toBlocking.foreach(println)
  }

  @Test def flatMapExample2() {
    val o = Observable.just(10, 100)
    val o1 = for (n <- o;
                  i <- Observable.interval(200 millis)) yield i * n
    o1.take(20).toBlocking.foreach(println)
  }

  @Test def flatMapExample3() {
    val o = Observable[Int] {
      subscriber =>
        subscriber.onNext(10)
        subscriber.onNext(100)
        subscriber.onError(new IOException("Oops"))
    }
    o.flatMap(
      (n: Int) => Observable.interval(200 millis).map(_ * n),
      e => Observable.interval(200 millis).map(_ * -1),
      () => Observable.interval(200 millis).map(_ * 1000)
    ).take(20)
      .toBlocking.foreach(println)
  }

  @Test def flatMapExample4() {
    val o = Observable.just(10, 100)
    o.flatMap(
      (n: Int) => Observable.interval(200 millis).map(_ * n),
      e => Observable.interval(200 millis).map(_ * -1),
      () => Observable.interval(200 millis).map(_ * 1000)
    ).take(20)
      .toBlocking.foreach(println)
  }

  @Test def flatMapExample5() {
    val o = Observable.just(1, 10, 100, 1000)
    o.flatMapWith(_ => Observable.interval(200 millis).take(5))(_ * _).toBlocking.foreach(println)
  }

  @Test def flatMapIterableExample() {
    val o = Observable.just(10, 100)
    o.flatMapIterable(n => (1 to 20).map(_ * n))
      .toBlocking.foreach(println)
  }

  @Test def flatMapIterableExample2() {
    val o = Observable.just(1, 10, 100, 1000)
    o.flatMapIterableWith(_=> (1 to 5))(_ * _).toBlocking.foreach(println)
  }

  @Test def concatMapExample() {
    val o = Observable.just(10, 100)
    o.concatMap(n => Observable.interval(200 millis).map(_ * n).take(10))
      .take(20)
      .toBlocking.foreach(println)
  }

  @Test def onErrorResumeNextExample() {
    val o = Observable {
      (subscriber: Subscriber[Int]) =>
        subscriber.onNext(1)
        subscriber.onNext(2)
        subscriber.onError(new IOException("Oops"))
        subscriber.onNext(3)
        subscriber.onNext(4)
    }
    o.onErrorResumeNext(_ => Observable.just(10, 11, 12)).subscribe(println(_))
  }

  @Test def onErrorFlatMapExample() {
    val o = Observable {
      (subscriber: Subscriber[Int]) =>
        subscriber.onNext(1)
        subscriber.onNext(2)
        subscriber.onError(new IOException("Oops"))
        subscriber.onNext(3)
        subscriber.onNext(4)
    }
    o.onErrorFlatMap((_, _) => Observable.just(10, 11, 12)).subscribe(println(_))
  }

  @Test def onErrorFlatMapExample2() {
    val o = Observable.just(4, 2, 0).map(16 / _).onErrorFlatMap {
      (e, op) => op match {
        case Some(v) if v == 0 => Observable.just(Int.MinValue)
        case _ => Observable.empty
      }
    }
    o.subscribe(println(_))
  }

  @Test def switchMapExample() {
    val o = Observable.interval(300 millis).take(5).switchMap[String] {
      n => Observable.interval(50 millis).take(10).map(i => s"Seq ${n}: ${i}")
    }
    o.toBlocking.foreach(println)
  }

  @Test def joinExample() {
    val o1 = Observable.interval(500 millis).map(n => "1: " + n)
    val o2 = Observable.interval(100 millis).map(n => "2: " + n)
    val o = o1.join(o2)(_ => Observable.timer(300 millis), _ => Observable.timer(200 millis), (_, _))
    o.take(10).toBlocking.foreach(println)
  }

  @Test def groupJoinExample() {
    val o1 = Observable.interval(500 millis).map(n => "1: " + n)
    val o2 = Observable.interval(100 millis).map(n => "2: " + n)
    val o = o1.groupJoin(o2)(_ => Observable.timer(300 millis), _ => Observable.timer(200 millis), (t1, t2) => (t1, t2.toSeq.toBlocking.single))
    o.take(3).toBlocking.foreach(println)
  }

}
