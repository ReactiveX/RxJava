package rx.lang.scalatests

import org.scalatest.junit.JUnitSuite

import rx.lang.scala._
import scala.concurrent.duration._
import org.junit.{Before, Test, Ignore}

import org.junit.Assert._

class RxScalaDemo extends JUnitSuite {

  @Test def testSwitchNoTiming() {
    val o1: Observable[Observable[Int]] = Observable(Observable(1, 2), Observable(3, 4))
    val o2: Observable[Int] = o1.switch
    
    // Correctly rejected with error 
    // "Cannot prove that Observable[Int] <:< Observable[Observable[U]]"
    // val o3 = Observable(1, 2).switch
    
    println(o2.toBlockingObservable.toIterable.toList)
  }
  
  @Test def testSequenceEqualIsUnnecessary() {
    // the sequenceEqual is unnecessary
    val first = Observable(10, 11, 12)
    val second = Observable(10, 11, 12)
    
    val b1 = (first zip second) map (p => p._1 == p._2)
    
    val equality = (a: Any, b: Any) => a == b
    
    val b2 = (first zip second) map (p => equality(p._1, p._2))
  }
  
  @Test def testStartWithIsUnnecessary() {
    val before = Observable(-2, -1, 0)
    val source = Observable(1, 2, 3)
    before ++ source
  }
  
  @Test def intervalExample() {
    println("hello")
    Observable.interval(200 millis).take(5).subscribe((n: Long) => println("n = " + n))
    // need to sleep here because otherwise JUnit kills the thread created by interval()
    Thread.sleep(1200)
  }
  
  @Test def forComprehensionExample() {
    val slowNumbers = Observable.interval(400 millis).take(5).map("slow " + _)
    val fastNumbers = Observable.interval(200 millis).take(10).map("fast " + _)
    
    // it would actually make more sense to use merge instead of for comprehension...
    val o1 = Observable(slowNumbers, fastNumbers)
    val o2 = {for (o <- o1; str <- o) yield str}
    
    o2.subscribe((s: String) => println(s))
    
    Thread.sleep(2300)
  }
  
}