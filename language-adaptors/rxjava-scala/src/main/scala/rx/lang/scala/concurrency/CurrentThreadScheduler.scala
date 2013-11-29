package rx.lang.scala.concurrency

import rx.lang.scala.Scheduler

object CurrentThreadScheduler {

  /**
   * Returns a [[rx.lang.scala.Scheduler]] that queues work on the current thread to be executed after the current work completes.
   */
  def apply(): CurrentThreadScheduler =  {
    new ImmediateScheduler(rx.concurrency.Schedulers.currentThread())
  }
}

class CurrentThreadScheduler private[scala] (val asJavaScheduler: rx.concurrencyCurrentThreadScheduler)
  extends Scheduler {}