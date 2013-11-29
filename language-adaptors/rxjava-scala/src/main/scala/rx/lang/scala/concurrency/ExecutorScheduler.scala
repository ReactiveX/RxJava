package rx.lang.scala.concurrency

import rx.lang.scala.Scheduler
import java.util.concurrent.{ScheduledExecutorService, Executor}

object ExecutorScheduler {

  /**
  * Returns a [[rx.lang.scala.Scheduler]] that queues work on an `java.util.concurrent.Executor`.
  *
  * Note that this does not support scheduled actions with a delay.
  */
  def apply(executor: Executor): ExecutorScheduler =  {
    new ExecutorScheduler(rx.concurrency.Schedulers.executor(executor))
  }
}


class ExecutorScheduler private[scala] (val asJavaScheduler: rx.concurrency.ExecutorScheduler)
  extends Scheduler {}



