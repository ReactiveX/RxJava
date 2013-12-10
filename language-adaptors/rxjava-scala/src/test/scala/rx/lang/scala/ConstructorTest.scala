package rx.lang.scala
import scala.language.postfixOps
import org.junit.Assert._
import org.junit.Test
import org.scalatest.junit.JUnitSuite

class ConstructorTest extends JUnitSuite {

  @Test def toObservable() {
    val xs = List(1,2,3).toObservable.toBlockingObservable.toList
    assertEquals(List(1,2,3), xs)

    val ys = Observable.from(List(1,2,3)).toBlockingObservable.toList
    assertEquals(List(1,2,3), xs)

    val zs = Observable.items(1,2,3).toBlockingObservable.toList
    assertEquals(List(1,2,3), xs)

  }
}
