package rx.lang.scala.concurrency

import rx.lang.scala.Scheduler

object ThreadPoolForComputationScheduler {

  /**
   * Returns a [[rx.lang.scala.Scheduler]] intended for computational work.
   *
   * The implementation is backed by a `java.util.concurrent.ScheduledExecutorService` thread-pool sized to the number of CPU cores.
   *
   * This can be used for event-loops, processing callbacks and other computational work.
   *
   * Do not perform IO-bound work on this scheduler. Use [[rx.lang.scala.concurrency.Schedulers.threadPoolForIO]] instead.
   */
  def apply(): ExecutorScheduler =  {
    new ThreadPoolForComputationScheduler(rx.concurrency.Schedulers.threadPoolForComputation())
  }
}

class ThreadPoolForComputationScheduler private[scala] (val asJavaScheduler: rx.Scheduler)
  extends Scheduler {}
