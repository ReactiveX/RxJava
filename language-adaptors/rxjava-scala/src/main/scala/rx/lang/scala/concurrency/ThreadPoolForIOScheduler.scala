package rx.lang.scala.concurrency

import rx.lang.scala.Scheduler

object ThreadPoolForIOScheduler {

  /**
   * [[rx.lang.scala.Scheduler]] intended for IO-bound work.
   *
   * The implementation is backed by an `java.util.concurrent.Executor` thread-pool that will grow as needed.
   *
   * This can be used for asynchronously performing blocking IO.
   *
   * Do not perform computational work on this scheduler. Use [[rx.lang.scala.concurrency.Schedulers.threadPoolForComputation]] instead.
   */
  def apply(): ExecutorScheduler =  {
    new ThreadPoolForIOScheduler(rx.concurrency.Schedulers.threadPoolForIO())
  }
}

class ThreadPoolForIOScheduler private[scala] (val asJavaScheduler: rx.Scheduler)
  extends Scheduler {}
