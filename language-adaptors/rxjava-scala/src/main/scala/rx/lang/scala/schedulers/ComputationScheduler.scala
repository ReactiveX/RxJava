package rx.lang.scala.schedulers

import rx.lang.scala.Scheduler


object ComputationScheduler {
  /**
   * {@link Scheduler} intended for computational work.
   * <p>
   * This can be used for event-loops, processing callbacks and other computational work.
   * <p>
   * Do not perform IO-bound work on this scheduler. Use {@link IOScheduler()} instead.
   *
   * @return { @link Scheduler} for computation-bound work.
   */
  def apply(): ComputationScheduler = {
    new ComputationScheduler(rx.schedulers.Schedulers.computation())
  }
}

class ComputationScheduler private[scala] (val asJavaScheduler: rx.Scheduler)
  extends Scheduler {}