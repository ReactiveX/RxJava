package rx.lang.scala.concurrency

import java.util.concurrent.Executor
import rx.lang.scala.Scheduler

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



