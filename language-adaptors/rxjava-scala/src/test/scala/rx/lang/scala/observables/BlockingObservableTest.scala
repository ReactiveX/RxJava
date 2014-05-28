package rx.lang.scala.observables

import org.junit.Assert._
import org.junit.Test
import org.scalatest.junit.JUnitSuite
import scala.language.postfixOps
import rx.lang.scala.Observable

class BlockingObservableTest extends JUnitSuite {

  @Test
  def testSingleOption() {
    val o = Observable.items(1)
    assertEquals(Some(1), o.toBlocking.singleOption)
  }

  @Test
  def testSingleOptionWithEmpty() {
    val o = Observable.empty
    assertEquals(None, o.toBlocking.singleOption)
  }

  @Test(expected = classOf[IllegalArgumentException])
  def testSingleOptionWithMultipleItems() {
    Observable.items(1, 2).toBlocking.singleOption
  }

  @Test
  def testSingleOrElse() {
    val o = Observable.items(1)
    assertEquals(1, o.toBlocking.singleOrElse(2))
  }

  @Test
  def testSingleOrElseWithEmpty() {
    val o = Observable.empty
    assertEquals(2, o.toBlocking.singleOrElse(2))
  }

  @Test(expected = classOf[IllegalArgumentException])
  def testSingleOrElseWithMultipleItems() {
    Observable.items(1, 2).toBlocking.singleOrElse(2)
  }

  @Test
  def testHeadOption() {
    val o = Observable.items(1)
    assertEquals(Some(1), o.toBlocking.headOption)
  }

  @Test
  def testHeadOptionWithEmpty() {
    val o = Observable.empty
    assertEquals(None, o.toBlocking.headOption)
  }

  @Test
  def testHeadOptionWithMultipleItems() {
    val o = Observable.items(1, 2)
    assertEquals(Some(1), o.toBlocking.headOption)
  }

  @Test
  def testHeadOrElse() {
    val o = Observable.items(1)
    assertEquals(1, o.toBlocking.headOrElse(2))
  }

  @Test
  def testHeadOrElseWithEmpty() {
    val o = Observable.empty
    assertEquals(2, o.toBlocking.headOrElse(2))
  }

  @Test
  def testHeadOrElseWithMultipleItems() {
    val o = Observable.items(1, 2)
    assertEquals(1, o.toBlocking.headOrElse(2))
  }

  @Test
  def testLastOption() {
    val o = Observable.items(1)
    assertEquals(Some(1), o.toBlocking.lastOption)
  }

  @Test
  def testLastOptionWithEmpty() {
    val o = Observable.empty
    assertEquals(None, o.toBlocking.lastOption)
  }

  @Test
  def testLastOptionWithMultipleItems() {
    val o = Observable.items(1, 2)
    assertEquals(Some(2), o.toBlocking.lastOption)
  }

  @Test
  def testLastOrElse() {
    val o = Observable.items(1)
    assertEquals(1, o.toBlocking.lastOrElse(2))
  }

  @Test
  def testLastOrElseWithEmpty() {
    val o = Observable.empty
    assertEquals(2, o.toBlocking.lastOrElse(2))
  }

  @Test
  def testLastOrElseWithMultipleItems() {
    val o = Observable.items(1, 2)
    assertEquals(2, o.toBlocking.lastOrElse(3))
  }
}
