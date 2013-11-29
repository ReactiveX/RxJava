package rx.lang.scala.concurrency

import rx.lang.scala.Scheduler
import java.util.concurrent.ScheduledExecutorService
import scala.concurrent.duration.Duration

object ScheduledExecutorServiceScheduler {

  /**
   * Returns a [[rx.lang.scala.Scheduler]] that queues work on an `java.util.concurrent.ScheduledExecutorService`.
   */
  def apply(executor: ScheduledExecutorService): ExecutorScheduler =  {
    new ScheduledExecutorServiceScheduler(rx.concurrency.Schedulers.executor(executor))
  }
}

class ScheduledExecutorServiceScheduler private[scala] (val asJavaScheduler: rx.Scheduler)
  extends Scheduler {}

