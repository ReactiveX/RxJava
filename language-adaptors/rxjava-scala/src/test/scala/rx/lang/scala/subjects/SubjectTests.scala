import org.junit.Assert._
import org.junit.Test
import rx.lang.scala.subjects._
import org.mockito.InOrder
import rx.lang.scala._
import org.mockito.Matchers._;
import org.mockito.Mockito._;

import org.junit.Assert._
import org.junit.Test
import org.scalatest.junit.JUnitSuite

class SubjectTests extends JUnitSuite {

//  @Test def PublishSubjectIsAChannel() {
//
//    val channel: BehaviorSubject[Integer] = BehaviorSubject(2013)
//    val observerA: Observer[Integer]  = mock(classOf[Observer[Integer]])
//    val observerB: Observer[Integer] = mock(classOf[Observer[Integer]])
//    val observerC: Observer[Integer] = mock(classOf[Observer[Integer]])
//
//    val x = inOrder(observerA, observerB, observerC)
//
//    val a = channel.subscribe(observerA)
//    val b = channel.subscribe(observerB)
//
//    x.verify(observerA).onNext(2013)
//    x.verify(observerB).onNext(2013)
//
//    channel.onNext(42)
//
//    x.verify(observerA).onNext(42)
//    x.verify(observerB).onNext(42)
//
//    a.unsubscribe()
//
//    channel.onNext(4711)
//
//    x.verify(observerA, never()).onNext(any())
//    x.verify(observerB).onNext(4711)
//
//    channel.onCompleted()
//
//    x.verify(observerA, never()).onCompleted()
//    x.verify(observerB).onCompleted()
//
//    val c = channel.subscribe(observerC)
//
//    x.verify(observerC).onCompleted()
//
//    channel.onNext(13)
//
//    x.verifyNoMoreInteractions()
//
//  }

}
