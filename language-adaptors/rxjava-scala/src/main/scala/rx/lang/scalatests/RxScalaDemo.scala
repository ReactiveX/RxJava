package rx.lang.scalatests

import org.scalatest.junit.JUnitSuite

import scala.language.postfixOps
import rx.lang.scala._
import scala.concurrent.duration._
import org.junit.{Before, Test, Ignore}

import org.junit.Assert._

@Ignore // Since this doesn't do automatic testing.
class RxScalaDemo extends JUnitSuite {
    
  def output(s: String): Unit = println(s)
  
  def sleep(ms: Long): Unit = Thread.sleep(ms)
   
  @Test def intervalExample() {
    println("hello")
    Observable.interval(200 millis).take(5).subscribe((n: Long) => println("n = " + n))
    // need to sleep here because otherwise JUnit kills the thread created by interval()
    sleep(1200)
  }
  
  def msTicks(start: Long, step: Long): Observable[Long] = {
    // will be easier once we have Observable.generate method
    Observable.interval(step millis) map (_ * step + start)
  }
  
  def prefixedTicks(start: Long, step: Long, prefix: String): Observable[String] = {
    msTicks(start, step).map(prefix + _)
  }
  
  @Test def testTicks() {
    prefixedTicks(5000, 500, "t = ").take(5).subscribe(output(_))
    sleep(3000)
  }
  
  @Test def testSwitch() {
    // We do not have ultimate precision: Sometimes, 747 gets through, sometimes not
    val o = Observable.interval(1000 millis).map(n => prefixedTicks(0, 249, s"Observable#$n: "))
    o.switch.take(16).subscribe(output(_))
    sleep(5000)
  }
  
  @Test def testSwitchOnObservableOfInt() {
    // Correctly rejected with error 
    // "Cannot prove that Observable[Int] <:< Observable[Observable[U]]"
    // val o = Observable(1, 2).switch
  }

  @Test def testMyOwnSequenceEqual() {
    // the sequenceEqual operation can be obtained like this:
    
    val first = Observable(10, 11, 12)
    val second = Observable(10, 11, 12)
    
    val b1 = (first zip second) map (p => p._1 == p._2) forall (b => b)
    
    val equality = (a: Any, b: Any) => a == b
    val b2 = (first zip second) map (p => equality(p._1, p._2)) forall (b => b)
    
    assertTrue(b1.toBlockingObservable.single)
    assertTrue(b2.toBlockingObservable.single)
  }
  
  @Test def testMyOwnSequenceEqualWithForComprehension() {
    // the sequenceEqual operation can be obtained like this:
    
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
    
    // TODO
  }
  
}
