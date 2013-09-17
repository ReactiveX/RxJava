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

import org.scalatest.junit.JUnitSuite
import scala.language.postfixOps
import rx.lang.scala._
import scala.concurrent.duration._
import org.junit.{Before, Test, Ignore}
import org.junit.Assert._
import rx.lang.scala.concurrency.NewThreadScheduler

//@Ignore // Since this doesn't do automatic testing, don't increase build time unnecessarily
class RxScalaDemo extends JUnitSuite {

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
    val first = Observable(10, 11, 12)
    val second = Observable(10, 11, 12)
    
    val b1 = (first zip second) map (p => p._1 == p._2) forall (b => b)
    
    val equality = (a: Any, b: Any) => a == b
    val b2 = (first zip second) map (p => equality(p._1, p._2)) forall (b => b)
    
    assertTrue(b1.toBlockingObservable.single)
    assertTrue(b2.toBlockingObservable.single)
  }
  
  @Test def testObservableComparisonWithForComprehension() {
    val first = Observable(10, 11, 12)
    val second = Observable(10, 11, 12)
    
    val booleans = for ((n1, n2) <- (first zip second)) yield (n1 == n2)
    
    val b1 = booleans.forall(_ == true) // without `== true`, b1 is assigned the forall function
    
    assertTrue(b1.toBlockingObservable.single)
  }
  
  @Test def testStartWithIsUnnecessary() {
    val before = Observable(-2, -1, 0)
    val source = Observable(1, 2, 3)
    println((before ++ source).toBlockingObservable.toList)
  }

  @Test def mergeExample() {
    val slowNumbers = Observable.interval(400 millis).take(5).map("slow " + _)
    val fastNumbers = Observable.interval(200 millis).take(10).map("fast " + _)
    val o = (slowNumbers merge fastNumbers)
    o.subscribe(output(_))
    waitFor(o)
  }
  
  @Test def rangeAndBufferExample() {
    val o = Observable(1 to 18)
    o.buffer(5).subscribe((l: Seq[Int]) => println(l.mkString("[", ", ", "]")))
  }
  
  @Test def windowExample() {
    // this will be nicer once we have zipWithIndex
    (for ((o, i) <- Observable(1 to 18).window(5) zip Observable(0 until 4); n <- o) 
      yield s"Observable#$i emits $n")
        .subscribe(output(_))
  }
  
  @Test def testReduce() {
    assertEquals(10, Observable(1, 2, 3, 4).reduce(_ + _).toBlockingObservable.single)
  }
  
  @Test def testForeach() {
    val numbers = Observable.interval(200 millis).take(3)
    
    // foreach is not available on normal Observables:
    // for (n <- numbers) println(n+10)
    
    // but on BlockingObservable, it is:
    for (n <- numbers.toBlockingObservable) println(n+10)
  }
  
  @Test def testForComprehension() {
    val observables = Observable(Observable(1, 2, 3), Observable(10, 20, 30))
    val squares = (for (o <- observables; i <- o if i % 2 == 0) yield i*i)
    assertEquals(squares.toBlockingObservable.toList, List(4, 100, 400, 900))
  }
  
  @Test def testTwoSubscriptionsToOneInterval() {
    val o = Observable.interval(100 millis).take(8)
    o.subscribe(
        i => println(s"${i}a (on thread #${Thread.currentThread().getId()})")
    )
    o.subscribe(
        i => println(s"${i}b (on thread #${Thread.currentThread().getId()})")
    )
    waitFor(o)
  }
  
  @Test def schedulersExample() {
    val o = Observable.interval(100 millis).take(8)
    o.observeOn(NewThreadScheduler).subscribe(
        i => println(s"${i}a (on thread #${Thread.currentThread().getId()})")
    )
    o.observeOn(NewThreadScheduler).subscribe(
        i => println(s"${i}b (on thread #${Thread.currentThread().getId()})")
    )
    waitFor(o)
  }
  
  @Test def testGroupByThenFlatMap() {
    val m = Observable(1, 2, 3, 4)
    val g = m.groupBy(i => i % 2)
    val t = g.flatMap((p: (Int, Observable[Int])) => p._2)
    assertEquals(List(1, 2, 3, 4), t.toBlockingObservable.toList)    
  }
  
  @Test def testGroupByThenFlatMapByForComprehension() {
    val m = Observable(1, 2, 3, 4)
    val g = m.groupBy(i => i % 2)
    val t = for ((i, o) <- g; n <- o) yield n
    assertEquals(List(1, 2, 3, 4), t.toBlockingObservable.toList)    
  }
  
  @Test def testGroupByThenFlatMapByForComprehensionWithTiming() {
    val m = Observable.interval(100 millis).take(4)
    val g = m.groupBy(i => i % 2)
    val t = for ((i, o) <- g; n <- o) yield n
    assertEquals(List(0, 1, 2, 3), t.toBlockingObservable.toList)    
  }

  @Test def groupByExample() {
    val medalsByCountry = Olympics.mountainBikeMedals.groupBy(medal => medal.country)
    
    val firstMedalOfEachCountry = 
      for ((country, medals) <- medalsByCountry; firstMedal <- medals.take(1)) yield firstMedal
      
    firstMedalOfEachCountry.subscribe(medal => {
      println(s"${medal.country} wins its first medal in ${medal.year}")
    })
    
    waitFor(firstMedalOfEachCountry)
  }
  
  def output(s: String): Unit = println(s)
  
  // blocks until obs has completed
  def waitFor[T](obs: Observable[T]): Unit = {
    obs.toBlockingObservable.last
  }
  
}
