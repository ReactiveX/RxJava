package rx.lang.scala.concurrency

import rx.Subscription
import java.util.Date
import scala.concurrent.duration.Duration
import rx.lang.scala.ImplicitFunctionConversions._
import rx.util.functions.Func2
import rx.lang.scala.Scheduler
import rx.lang.scala.Observer
import org.scalatest.junit.JUnitSuite
import org.junit.Before
import rx.lang.scala.Observable


class GenericScheduler[+S <: rx.Scheduler](val asJava: S) extends AnyVal {
    /**
     * Schedules a cancelable action to be executed.
     *
     * @param state
     *            State to pass into the action.
     * @param action
     *            Action to schedule.
     * @return a subscription to be able to unsubscribe from action.
     */
    def schedule[T](state: T, action: (Scheduler, T) => Subscription): Subscription = {
      asJava.schedule(state, action)
    }

    /**
     * Schedules a cancelable action to be executed in delayTime.
     *
     * @param state
     *            State to pass into the action.
     * @param action
     *            Action to schedule.
     * @param delayTime
     *            Time the action is to be delayed before executing.
     * @param unit
     *            Time unit of the delay time.
     * @return a subscription to be able to unsubscribe from action.
     */
    def schedule[T](state: T, action: (Scheduler, T) => Subscription, delayTime: Duration): Subscription = {
      asJava.schedule(state, action, delayTime.length, delayTime.unit)
    }

    /**
     * Schedules a cancelable action to be executed periodically.
     * This default implementation schedules recursively and waits for actions to complete (instead of potentially executing
     * long-running actions concurrently). Each scheduler that can do periodic scheduling in a better way should override this.
     *
     * @param state
     *            State to pass into the action.
     * @param action
     *            The action to execute periodically.
     * @param initialDelay
     *            Time to wait before executing the action for the first time.
     * @param period
     *            The time interval to wait each time in between executing the action.
     * @return A subscription to be able to unsubscribe from action.
     */
    def schedulePeriodically[T](state: T, action: (Scheduler, T) => Subscription, initialDelay: Duration, period: Duration): Subscription = {
      asJava.schedulePeriodically(state, action, initialDelay.length, initialDelay.unit.convert(period.length, period.unit), initialDelay.unit)
    }

    /**
     * Schedules a cancelable action to be executed at dueTime.
     *
     * @param state
     *            State to pass into the action.
     * @param action
     *            Action to schedule.
     * @param dueTime
     *            Time the action is to be executed. If in the past it will be executed immediately.
     * @return a subscription to be able to unsubscribe from action.
     */
    def schedule[T](state: T, action: (Scheduler, T) => Subscription, dueTime: Date): Subscription = {
      asJava.schedule(state, action, dueTime)
    }

    /**
     * Schedules an action to be executed.
     *
     * @param action
     *            action
     * @return a subscription to be able to unsubscribe from action.
     */
    def schedule(action: () => Unit): Subscription = {
      asJava.schedule(action)
    }

    /**
     * Schedules an action to be executed in delayTime.
     *
     * @param action
     *            action
     * @return a subscription to be able to unsubscribe from action.
     */
    def schedule(action: () => Unit, delayTime: Duration): Subscription = {
      asJava.schedule(action, delayTime.length, delayTime.unit)
    }

    /**
     * Schedules an action to be executed periodically.
     *
     * @param action
     *            The action to execute periodically.
     * @param initialDelay
     *            Time to wait before executing the action for the first time.
     * @param period
     *            The time interval to wait each time in between executing the action.
     * @return A subscription to be able to unsubscribe from action.
     */
    def schedulePeriodically(action: () => Unit, initialDelay: Duration, period: Duration): Subscription = {
      asJava.schedulePeriodically(action, initialDelay.length, initialDelay.unit.convert(period.length, period.unit), initialDelay.unit)
    }

    /**
     * @return the scheduler's notion of current absolute time in milliseconds.
     */
    def now: Long = {
      asJava.now
    }

    /**
     * Parallelism available to a Scheduler.
     * 
     * This defaults to {@code Runtime.getRuntime().availableProcessors()} but can be overridden for use cases such as scheduling work on a computer cluster.
     *
     * @return the scheduler's available degree of parallelism.
     */
    def degreeOfParallelism: Int = {
      asJava.degreeOfParallelism
    }

}

class UnitTest extends JUnitSuite {
  import org.mockito.Matchers._
  import org.mockito.Mockito._
  import org.junit.Test
  import org.junit.Before
  import scala.concurrent.duration._
  import scala.language.postfixOps

  var scheduler: TestScheduler = null
  var observer: Observer[Long] = null
  var observer2: Observer[Long] = null

  @Before def before() {
    scheduler = TestScheduler()
    observer = mock(classOf[rx.Observer[Long]])
    observer2 = mock(classOf[rx.Observer[Long]])
  }

  @Test def testInterval() {
    val w = Observable.interval(1 seconds)
    val sub = w.subscribe(observer)

    verify(observer, never()).onNext(0L)
    verify(observer, never()).onCompleted()
    verify(observer, never()).onError(any(classOf[Throwable]))

    scheduler.advanceTimeTo(2 seconds)

    val inOrdr = inOrder(observer);
    inOrdr.verify(observer, times(1)).onNext(0L)
    inOrdr.verify(observer, times(1)).onNext(1L)
    inOrdr.verify(observer, never()).onNext(2L)
    verify(observer, never()).onCompleted();
    verify(observer, never()).onError(any(classOf[Throwable]))

    sub.unsubscribe();
    scheduler.advanceTimeTo(4 seconds)
    verify(observer, never()).onNext(2L)
    verify(observer, times(1)).onCompleted()
    verify(observer, never()).onError(any(classOf[Throwable]))
  }
}

