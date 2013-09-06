package rx.lang.scala

class Observable[+T](val asJava: rx.Observable[_ <: T]) extends AnyVal {

}

object Observable {
  import scala.collection.JavaConverters._
  import rx.{Observable => JObservable}
  
  def apply[T](args: T*): Observable[T] = {     
    new Observable(JObservable.from(args.toIterable.asJava))
  }
}

import org.scalatest.junit.JUnitSuite

class UnitTestSuite extends JUnitSuite {

  import org.junit.{ Before, Test }
  import org.junit.Assert._
  import org.mockito.Matchers.any
  import org.mockito.Mockito._
  import org.mockito.{ MockitoAnnotations, Mock }
  import rx.{ Notification, Observer, Subscription }
  import rx.observables.GroupedObservable
  import collection.JavaConverters._

  @Test def testTest() = {
    println("testTest()")
    assertEquals(4, Observable(1, 2, 3, 4).asJava.toBlockingObservable().last())
  }


}
