package rx.lang.scala.concurrency

import scala.concurrent.duration.Duration



class TestScheduler {

  private val asJava = new rx.concurrency.TestScheduler

  /*override*/ def now: Long = asJava.now

  def advanceTimeBy(time: Duration) {
    asJava.advanceTimeBy(time.length, time.unit)
  }

  def advanceTimeTo(time: Duration) {
    asJava.advanceTimeTo(time.length, time.unit)
  }

  def triggerActions() {
    asJava.triggerActions()
  }
}


object TestScheduler {
  def apply(): TestScheduler = {
    //rx.lang.scala.ImplicitFunctionConversions.javaSchedulerToScalaScheduler(new rx.concurrency.TestScheduler())
    new TestScheduler
  }
}

