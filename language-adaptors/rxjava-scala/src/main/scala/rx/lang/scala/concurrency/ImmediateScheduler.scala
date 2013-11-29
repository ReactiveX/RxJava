package rx.lang.scala.concurrency

import rx.lang.scala.Scheduler

object ImmediateScheduler {

  /**
   * Returns a [[rx.lang.scala.Scheduler]] that executes work immediately on the current thread.
   */
  def apply(): ImmediateScheduler =  {
    new ImmediateScheduler(rx.concurrency.Schedulers.immediate())
  }
}

class ImmediateScheduler private[scala] (val asJavaScheduler: rx.concurrency.ImmediateScheduler)
  extends Scheduler {}


