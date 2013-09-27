package rx.lang.scala.concurrency

import scala.concurrent.duration.Duration
import rx.lang.scala.Subscription
import rx.lang.scala.Scheduler
import rx.lang.scala.ImplicitFunctionConversions._
import rx.util.functions.Func2
import java.util.concurrent.TimeUnit

// TODO make a Scheduler interface in Java, and a DefaultScheduler Java and one for Scala 

class TestScheduler extends Scheduler {

  private val asJava = new rx.concurrency.TestScheduler

  override def now: Long = asJava.now

  def advanceTimeBy(time: Duration) {
    asJava.advanceTimeBy(time.length, time.unit)
  }

  def advanceTimeTo(time: Duration) {
    asJava.advanceTimeTo(time.length, time.unit)
  }

  def triggerActions() {
    asJava.triggerActions()
  }

  def schedule[T](state: T, action: (Scheduler, T) => Subscription): Subscription = {
    asJava.schedule(state, action)
  }

  def schedule[T](state: T, action: (Scheduler, T) => Subscription, delay: Duration): Subscription = {
    asJava.schedule(state, action, delay.length, delay.unit)
  }

  override def schedule[T](state: T, action: Func2[_ >: Scheduler, _ >: T, _ <: Subscription]): Subscription = {
    asJava.schedule(state, action)
  }

  override def schedule[T](state: T, action: Func2[_ >: Scheduler, _ >: T, _ <: Subscription], delayTime: Long, unit: TimeUnit): Subscription = {
    asJava.schedule(state, action, delayTime, unit)
  }

}
