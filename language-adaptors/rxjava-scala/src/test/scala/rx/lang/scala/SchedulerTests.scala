package rx.lang.scala

import java.util.concurrent.TimeUnit

import org.junit.Assert.assertTrue
import org.junit.Test
import org.scalatest.junit.JUnitSuite
import rx.lang.scala.schedulers.TestScheduler

class SchedulerTests extends JUnitSuite {

  @Test def testScheduleRecSingleRound() {
    val scheduler = TestScheduler()
    val worker = scheduler.createWorker
    var count = 0
    worker.scheduleRec({ count += 1; worker.unsubscribe() })
    scheduler.advanceTimeBy(1L, TimeUnit.SECONDS)
    assertTrue(count == 1)
  }

  @Test def testScheduleRecMultipleRounds() {
    val scheduler = TestScheduler()
    val worker = scheduler.createWorker
    var count = 0
    worker.scheduleRec({ count += 1; if(count == 100) worker.unsubscribe() })
    scheduler.advanceTimeBy(1L, TimeUnit.SECONDS)
    assertTrue(count == 100)
  }

  @Test def testScheduleRecUnsubscribe() {
    val scheduler = TestScheduler()
    val worker = scheduler.createWorker
    var count = 0
    val subscription = worker.scheduleRec({ count += 1 })
    subscription.unsubscribe()
    scheduler.advanceTimeBy(1L, TimeUnit.SECONDS)
    assertTrue(count == 0)
  }

  @Test(expected = classOf[Exception])
  def testScheduleRecException() {
    val scheduler = TestScheduler()
    scheduler.createWorker.scheduleRec({ throw new Exception() })
    scheduler.advanceTimeBy(1L, TimeUnit.SECONDS)
  }

}
