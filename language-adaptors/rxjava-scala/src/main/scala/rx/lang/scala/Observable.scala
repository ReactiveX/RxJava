/**
 * Copyright 2013 Netflix, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package rx.lang.scala

import rx.functions.FuncN
import rx.Observable.OnSubscribeFunc
import rx.lang.scala.observables.ConnectableObservable
import scala.concurrent.duration
import java.util
import collection.JavaConversions._


/**
 * The Observable interface that implements the Reactive Pattern.
 *
 * @define subscribeObserverMain
 * Call this method to subscribe an [[rx.lang.scala.Observer]] for receiving 
 * items and notifications from the Observable.
 *
 * A typical implementation of `subscribe` does the following:
 *
 * It stores a reference to the Observer in a collection object, such as a `List[T]` object.
 *
 * It returns a reference to the [[rx.lang.scala.Subscription]] interface. This enables Observers to
 * unsubscribe, that is, to stop receiving items and notifications before the Observable stops
 * sending them, which also invokes the Observer's [[rx.lang.scala.Observer.onCompleted onCompleted]] method.
 *
 * An `Observable[T]` instance is responsible for accepting all subscriptions
 * and notifying all Observers. Unless the documentation for a particular
 * `Observable[T]` implementation indicates otherwise, Observers should make no
 * assumptions about the order in which multiple Observers will receive their notifications.
 *
 * @define subscribeObserverParamObserver 
 *         the observer
 * @define subscribeObserverParamScheduler 
 *         the [[rx.lang.scala.Scheduler]] on which Observers subscribe to the Observable
 *
 * @define subscribeSubscriberMain
 * Call this method to subscribe an [[Subscriber]] for receiving items and notifications from the [[Observable]].
 *
 * A typical implementation of `subscribe` does the following:
 *
 * It stores a reference to the Observer in a collection object, such as a `List[T]` object.
 *
 * It returns a reference to the [[rx.lang.scala.Subscription]] interface. This enables [[Subscriber]]s to
 * unsubscribe, that is, to stop receiving items and notifications before the Observable stops
 * sending them, which also invokes the Subscriber's [[rx.lang.scala.Observer.onCompleted onCompleted]] method.
 *
 * An [[Observable]] instance is responsible for accepting all subscriptions
 * and notifying all [[Subscriber]]s. Unless the documentation for a particular
 * [[Observable]] implementation indicates otherwise, [[Subscriber]]s should make no
 * assumptions about the order in which multiple [[Subscriber]]s will receive their notifications.
 *
 * @define subscribeSubscriberParamObserver
 *         the [[Subscriber]]
 * @define subscribeSubscriberParamScheduler
 *         the [[rx.lang.scala.Scheduler]] on which [[Subscriber]]s subscribe to the Observable
 *
 * @define subscribeAllReturn 
 *         a [[rx.lang.scala.Subscription]] reference whose `unsubscribe` method can be called to  stop receiving items
 *         before the Observable has finished sending them
 *
 * @define subscribeCallbacksMainWithNotifications
 * Call this method to receive items and notifications from this observable.
 *
 * @define subscribeCallbacksMainNoNotifications
 * Call this method to receive items from this observable.
 *
 * @define subscribeCallbacksParamOnNext 
 *         this function will be called whenever the Observable emits an item
 * @define subscribeCallbacksParamOnError
 *         this function will be called if an error occurs
 * @define subscribeCallbacksParamOnComplete
 *         this function will be called when this Observable has finished emitting items
 * @define subscribeCallbacksParamScheduler
 *         the scheduler to use
 *
 * @define debounceVsThrottle
 * Information on debounce vs throttle:
 *  - [[http://drupalmotion.com/article/debounce-and-throttle-visual-explanation]]
 *  - [[http://unscriptable.com/2009/03/20/debouncing-javascript-methods/]]
 *  - [[http://www.illyriad.co.uk/blog/index.php/2011/09/javascript-dont-spam-your-server-debounce-and-throttle/]]
 *
 *
 */
trait Observable[+T]
{
  import scala.collection.JavaConverters._
  import scala.collection.Seq
  import scala.concurrent.duration.{Duration, TimeUnit, MILLISECONDS}
  import rx.functions._
  import rx.lang.scala.observables.BlockingObservable
  import ImplicitFunctionConversions._
  import JavaConversions._

  private [scala] val asJavaObservable: rx.Observable[_ <: T]

  /**
   * $subscribeObserverMain
   *
   * @return $subscribeAllReturn
   */
  def subscribe(): Subscription = {
    asJavaObservable.subscribe()
  }

  /**
   * $subscribeObserverMain
   *
   * @param observer $subscribeObserverParamObserver
   * @return $subscribeAllReturn
   */
  def subscribe(observer: Observer[T]): Subscription = {
    asJavaObservable.subscribe(observer.asJavaObserver)
  }

  /**
   * $subscribeObserverMain
   *
   * @param observer $subscribeObserverParamObserver
   * @return $subscribeAllReturn
   */
  def apply(observer: Observer[T]): Subscription = subscribe(observer)

  /**
   * $subscribeSubscriberMain
   *
   * @param subscriber $subscribeSubscriberParamObserver
   * @return $subscribeAllReturn
   */
  def subscribe(subscriber: Subscriber[T]): Subscription = {
    // Add the casting to avoid compile error "ambiguous reference to overloaded definition"
    val thisJava = asJavaObservable.asInstanceOf[rx.Observable[T]]
    thisJava.subscribe(subscriber.asJavaSubscriber)
  }

  /**
   * $subscribeSubscriberMain
   *
   * @param subscriber $subscribeSubscriberParamObserver
   * @return $subscribeAllReturn
   */
  def apply(subscriber: Subscriber[T]): Subscription = subscribe(subscriber)

  /**
   * $subscribeCallbacksMainNoNotifications
   *
   * @param onNext $subscribeCallbacksParamOnNext
   * @return $subscribeAllReturn
   */
  def subscribe(onNext: T => Unit): Subscription = {
   asJavaObservable.subscribe(scalaFunction1ProducingUnitToAction1(onNext))
  }

  /**
   * $subscribeCallbacksMainWithNotifications
   *
   * @param onNext $subscribeCallbacksParamOnNext
   * @param onError $subscribeCallbacksParamOnError
   * @return $subscribeAllReturn
   */
  def subscribe(onNext: T => Unit, onError: Throwable => Unit): Subscription = {
    asJavaObservable.subscribe(
      scalaFunction1ProducingUnitToAction1(onNext),
      scalaFunction1ProducingUnitToAction1(onError)
    )
  }

  /**
   * $subscribeCallbacksMainWithNotifications
   *
   * @param onNext $subscribeCallbacksParamOnNext
   * @param onError $subscribeCallbacksParamOnError
   * @param onCompleted $subscribeCallbacksParamOnComplete
   * @return $subscribeAllReturn
   */
  def subscribe(onNext: T => Unit, onError: Throwable => Unit, onCompleted: () => Unit): Subscription = {
    asJavaObservable.subscribe(
      scalaFunction1ProducingUnitToAction1(onNext),
      scalaFunction1ProducingUnitToAction1(onError), 
      scalaFunction0ProducingUnitToAction0(onCompleted)
    )
  }

  /**
   * Returns a pair of a start function and an [[rx.lang.scala.Observable]] that upon calling the start function causes the source Observable to
   * push results into the specified subject.
   *
   * @param subject
   *            the `rx.lang.scala.subjects.Subject` to push source items into
   * @return a pair of a start function and an [[rx.lang.scala.Observable]] such that when the start function
   *         is called, the Observable starts to push results into the specified Subject
   */
  def multicast[R >: T](subject: rx.lang.scala.Subject[R]): ConnectableObservable[R] = {
    val s: rx.subjects.Subject[_ >: T, _<: R] = subject.asJavaSubject
    new ConnectableObservable[R](asJavaObservable.multicast(s))
  }

  /**
   * Returns an Observable that emits items produced by multicasting the source Observable within a selector function.
   *
   * @param subjectFactory the `Subject` factory
   * @param selector the selector function, which can use the multicasted source Observable subject to the policies
   *                 enforced by the created `Subject`
   * @return an Observable that emits the items produced by multicasting the source Observable within a selector function
   */
  def multicast[R >: T, U](subjectFactory: () => rx.lang.scala.Subject[R], selector: Observable[R] => Observable[U]): Observable[U] = {
    val subjectFactoryJava: Func0[rx.subjects.Subject[_ >: T, _ <: R]] = () => subjectFactory().asJavaSubject
    val selectorJava: Func1[rx.Observable[R], rx.Observable[U]] =
      (jo: rx.Observable[R]) => selector(toScalaObservable[R](jo)).asJavaObservable.asInstanceOf[rx.Observable[U]]
    toScalaObservable[U](asJavaObservable.multicast[R, U](subjectFactoryJava, selectorJava))
  }

  /**
   * Returns an Observable that first emits the items emitted by `this`, and then `elem`.
   *
   * @param elem the item to be appended
   * @return  an Observable that first emits the items emitted by `this`, and then `elem`.
   */
  def :+[U >: T](elem: U): Observable[U] = {
    this ++ Observable.items(elem)
  }

  /**
   * Returns an Observable that first emits the items emitted by `this`, and then the items emitted
   * by `that`.
   *
   * <img width="640" src="https://raw.github.com/wiki/Netflix/RxJava/images/rx-operators/concat.png">
   *
   * @param that
   *            an Observable to be appended
   * @return an Observable that emits items that are the result of combining the items emitted by
   *         this and that, one after the other
   */
  def ++[U >: T](that: Observable[U]): Observable[U] = {
    val o1: rx.Observable[_ <: U] = this.asJavaObservable
    val o2: rx.Observable[_ <: U] = that.asJavaObservable
    toScalaObservable(rx.Observable.concat(o1, o2))
  }

  /**
   * Returns an Observable that emits a specified item before it begins to emit items emitted by the source Observable.
   * <p>
   * <img width="640" src="https://raw.github.com/wiki/Netflix/RxJava/images/rx-operators/startWith.png">
   *
   * @param elem the item to emit
   * @return an Observable that emits the specified item before it begins to emit items emitted by the source Observable
   */
  def +:[U >: T](elem: U): Observable[U] = {
    val thisJava = this.asJavaObservable.asInstanceOf[rx.Observable[U]]
    toScalaObservable(thisJava.startWith(elem))
  }

  /**
   * Returns an Observable that emits the items emitted by several Observables, one after the
   * other.
   *
   * This operation is only available if `this` is of type `Observable[Observable[U]]` for some `U`,
   * otherwise you'll get a compilation error.
   *
   * @usecase def concat[U]: Observable[U]
   */
  def concat[U](implicit evidence: Observable[T] <:< Observable[Observable[U]]): Observable[U] = {
    val o2: Observable[Observable[U]] = this
    val o3: Observable[rx.Observable[_ <: U]] = o2.map(_.asJavaObservable)
    val o4: rx.Observable[_ <: rx.Observable[_ <: U]] = o3.asJavaObservable
    val o5 = rx.Observable.concat[U](o4)
    toScalaObservable[U](o5)
  }

  /**
   * Wraps this Observable in another Observable that ensures that the resulting
   * Observable is chronologically well-behaved.
   *
   * <img width="640" src="https://github.com/Netflix/RxJava/wiki/images/rx-operators/synchronize.png">
   *
   * A well-behaved Observable does not interleave its invocations of the [[rx.lang.scala.Observer.onNext onNext]], [[rx.lang.scala.Observer.onCompleted onCompleted]], and [[rx.lang.scala.Observer.onError onError]] methods of
   * its [[rx.lang.scala.Observer]]s; it invokes `onCompleted` or `onError` only once; and it never invokes `onNext` after invoking either `onCompleted` or `onError`.
   * [[Observable.serialize serialize]] enforces this, and the Observable it returns invokes `onNext` and `onCompleted` or `onError` synchronously.
   *
   * @return an Observable that is a chronologically well-behaved version of the source
   *         Observable, and that synchronously notifies its [[rx.lang.scala.Observer]]s
   */
  def serialize: Observable[T] = {
    toScalaObservable[T](asJavaObservable.serialize)
  }

  /**
   * Wraps each item emitted by a source Observable in a timestamped tuple.
   *
   * <img width="640" src="https://github.com/Netflix/RxJava/wiki/images/rx-operators/timestamp.png">
   *
   * @return an Observable that emits timestamped items from the source Observable
   */
  def timestamp: Observable[(Long, T)] = {
    toScalaObservable[rx.schedulers.Timestamped[_ <: T]](asJavaObservable.timestamp())
      .map((t: rx.schedulers.Timestamped[_ <: T]) => (t.getTimestampMillis, t.getValue))
  }

  /**
   * Wraps each item emitted by a source Observable in a timestamped tuple
   * with timestamps provided by the given Scheduler.
   * <p>
   * <img width="640" src="https://raw.github.com/wiki/Netflix/RxJava/images/rx-operators/timestamp.s.png">
   * 
   * @param scheduler [[rx.lang.scala.Scheduler]] to use as a time source.
   * @return an Observable that emits timestamped items from the source
   *         Observable with timestamps provided by the given Scheduler
   */
  def timestamp(scheduler: Scheduler): Observable[(Long, T)] = {
    toScalaObservable[rx.schedulers.Timestamped[_ <: T]](asJavaObservable.timestamp(scheduler))
      .map((t: rx.schedulers.Timestamped[_ <: T]) => (t.getTimestampMillis, t.getValue))
  }

  /**
   * Returns an Observable formed from this Observable and another Observable by combining 
   * corresponding elements in pairs. 
   * The number of `onNext` invocations of the resulting `Observable[(T, U)]`
   * is the minumum of the number of `onNext` invocations of `this` and `that`. 
   */
  def zip[U](that: Observable[U]): Observable[(T, U)] = {
    zipWith(that, (t: T, u: U) => (t, u))
  }

  /**
   * Returns an Observable formed from this Observable and another Observable by combining
   * corresponding elements using the selector function.
   * The number of `onNext` invocations of the resulting `Observable[(T, U)]`
   * is the minumum of the number of `onNext` invocations of `this` and `that`.
   */
  def zipWith[U, R](that: Observable[U], selector: (T,U) => R): Observable[R] = {
    toScalaObservable[R](rx.Observable.zip[T, U, R](this.asJavaObservable, that.asJavaObservable, selector))
  }

  /**
   * Zips this Observable with its indices.
   *
   * @return An Observable emitting pairs consisting of all elements of this Observable paired with 
   *         their index. Indices start at 0.
   */
  def zipWithIndex: Observable[(T, Int)] = {
    zip((0 until Int.MaxValue).toObservable)
  }

  /**
   * Creates an Observable which produces buffers of collected values.
   *
   * This Observable produces connected non-overlapping buffers. The current buffer is
   * emitted and replaced with a new buffer when the Observable produced by the specified function produces an object. The function will then
   * be used to create a new Observable to listen for the end of the next buffer.
   *
   * @param closings
   *            The function which is used to produce an [[rx.lang.scala.Observable]] for every buffer created.
   *            When this [[rx.lang.scala.Observable]] produces an object, the associated buffer
   *            is emitted and replaced with a new one.
   * @return
   *         An [[rx.lang.scala.Observable]] which produces connected non-overlapping buffers, which are emitted
   *         when the current [[rx.lang.scala.Observable]] created with the function argument produces an object.
   */
  def buffer(closings: () => Observable[Any]) : Observable[Seq[T]] = {
    val f: Func0[_ <: rx.Observable[_ <: Any]] = closings().asJavaObservable
    val jObs: rx.Observable[_ <: java.util.List[_]] = asJavaObservable.buffer[Any](f)
    Observable.jObsOfListToScObsOfSeq(jObs.asInstanceOf[rx.Observable[_ <: java.util.List[T]]])
  }
  /**
   * Creates an Observable which produces buffers of collected values.
   *
   * This Observable produces buffers. Buffers are created when the specified `openings`
   * Observable produces an object. That object is used to construct an Observable to emit buffers, feeding it into `closings` function.
   * Buffers are emitted when the created Observable produces an object.
   *
   * @param openings
   *            The [[rx.lang.scala.Observable]] which, when it produces an object, will cause
   *            another buffer to be created.
   * @param closings
   *            The function which is used to produce an [[rx.lang.scala.Observable]] for every buffer created.
   *            When this [[rx.lang.scala.Observable]] produces an object, the associated buffer
   *            is emitted.
   * @return
   *         An [[rx.lang.scala.Observable]] which produces buffers which are created and emitted when the specified [[rx.lang.scala.Observable]]s publish certain objects.
   */
  def buffer[Opening](openings: Observable[Opening], closings: Opening => Observable[Any]): Observable[Seq[T]] = {
    val opening: rx.Observable[_ <: Opening] = openings.asJavaObservable
    val closing: Func1[_ >: Opening, _ <: rx.Observable[_ <: Any]] = (o: Opening) => closings(o).asJavaObservable
    val jObs: rx.Observable[_ <: java.util.List[_]] = asJavaObservable.buffer[Opening, Any](opening, closing)
    Observable.jObsOfListToScObsOfSeq(jObs.asInstanceOf[rx.Observable[_ <: java.util.List[T]]])
  }

  /**
   * Creates an Observable which produces buffers of collected values.
   *
   * This Observable produces connected non-overlapping buffers, each containing `count`
   * elements. When the source Observable completes or encounters an error, the current
   * buffer is emitted, and the event is propagated.
   *
   * @param count
   *            The maximum size of each buffer before it should be emitted.
   * @return
   *         An [[rx.lang.scala.Observable]] which produces connected non-overlapping buffers containing at most
   *         `count` produced values.
   */
  def buffer(count: Int): Observable[Seq[T]] = {
    val oJava: rx.Observable[_ <: java.util.List[_]] = asJavaObservable.buffer(count)
    Observable.jObsOfListToScObsOfSeq(oJava.asInstanceOf[rx.Observable[_ <: java.util.List[T]]])
  }

  /**
   * Creates an Observable which produces buffers of collected values.
   *
   * This Observable produces buffers every `skip` values, each containing `count`
   * elements. When the source Observable completes or encounters an error, the current
   * buffer is emitted, and the event is propagated.
   *
   * @param count
   *            The maximum size of each buffer before it should be emitted.
   * @param skip
   *            How many produced values need to be skipped before starting a new buffer. Note that when `skip` and
   *            `count` are equals that this is the same operation as `buffer(int)`.
   * @return
   *         An [[rx.lang.scala.Observable]] which produces buffers every `skip` values containing at most
   *         `count` produced values.
   */
  def buffer(count: Int, skip: Int): Observable[Seq[T]] = {
    val oJava: rx.Observable[_ <: java.util.List[_]] = asJavaObservable.buffer(count, skip)
    Observable.jObsOfListToScObsOfSeq(oJava.asInstanceOf[rx.Observable[_ <: java.util.List[T]]])
  }

  /**
   * Creates an Observable which produces buffers of collected values.
   *
   * This Observable produces connected non-overlapping buffers, each of a fixed duration
   * specified by the `timespan` argument. When the source Observable completes or encounters
   * an error, the current buffer is emitted and the event is propagated.
   *
   * @param timespan
   *            The period of time each buffer is collecting values before it should be emitted, and
   *            replaced with a new buffer.
   * @return
   *         An [[rx.lang.scala.Observable]] which produces connected non-overlapping buffers with a fixed duration.
   */
  def buffer(timespan: Duration): Observable[Seq[T]] = {
    val oJava: rx.Observable[_ <: java.util.List[_]] = asJavaObservable.buffer(timespan.length, timespan.unit)
    Observable.jObsOfListToScObsOfSeq(oJava.asInstanceOf[rx.Observable[_ <: java.util.List[T]]])
  }

  /**
   * Creates an Observable which produces buffers of collected values.
   *
   * This Observable produces connected non-overlapping buffers, each of a fixed duration
   * specified by the `timespan` argument. When the source Observable completes or encounters
   * an error, the current buffer is emitted and the event is propagated.
   *
   * @param timespan
   *            The period of time each buffer is collecting values before it should be emitted, and
   *            replaced with a new buffer.
   * @param scheduler
   *            The [[rx.lang.scala.Scheduler]] to use when determining the end and start of a buffer.
   * @return
   *         An [[rx.lang.scala.Observable]] which produces connected non-overlapping buffers with a fixed duration.
   */
  def buffer(timespan: Duration, scheduler: Scheduler): Observable[Seq[T]] = {
    val oJava: rx.Observable[_ <: java.util.List[_]] = asJavaObservable.buffer(timespan.length, timespan.unit, scheduler)
    Observable.jObsOfListToScObsOfSeq(oJava.asInstanceOf[rx.Observable[_ <: java.util.List[T]]])
  }

  /**
   * Creates an Observable which produces buffers of collected values. This Observable produces connected
   * non-overlapping buffers, each of a fixed duration specified by the `timespan` argument or a maximum size
   * specified by the `count` argument (which ever is reached first). When the source Observable completes
   * or encounters an error, the current buffer is emitted and the event is propagated.
   *
   * @param timespan
   *            The period of time each buffer is collecting values before it should be emitted, and
   *            replaced with a new buffer.
   * @param count
   *            The maximum size of each buffer before it should be emitted.
   * @return
   *         An [[rx.lang.scala.Observable]] which produces connected non-overlapping buffers which are emitted after
   *         a fixed duration or when the buffer has reached maximum capacity (which ever occurs first).
   */
  def buffer(timespan: Duration, count: Int): Observable[Seq[T]] = {
    val oJava: rx.Observable[_ <: java.util.List[_]] = asJavaObservable.buffer(timespan.length, timespan.unit, count)
    Observable.jObsOfListToScObsOfSeq(oJava.asInstanceOf[rx.Observable[_ <: java.util.List[T]]])
  }

  /**
   * Creates an Observable which produces buffers of collected values. This Observable produces connected
   * non-overlapping buffers, each of a fixed duration specified by the `timespan` argument or a maximum size
   * specified by the `count` argument (which ever is reached first). When the source Observable completes
   * or encounters an error, the current buffer is emitted and the event is propagated.
   *
   * @param timespan
   *            The period of time each buffer is collecting values before it should be emitted, and
   *            replaced with a new buffer.
   * @param count
   *            The maximum size of each buffer before it should be emitted.
   * @param scheduler
   *            The [[rx.lang.scala.Scheduler]] to use when determining the end and start of a buffer.
   * @return
   *         An [[rx.lang.scala.Observable]] which produces connected non-overlapping buffers which are emitted after
   *         a fixed duration or when the buffer has reached maximum capacity (which ever occurs first).
   */
  def buffer(timespan: Duration, count: Int, scheduler: Scheduler): Observable[Seq[T]] = {
    val oJava: rx.Observable[_ <: java.util.List[_]] = asJavaObservable.buffer(timespan.length, timespan.unit, count, scheduler)
    Observable.jObsOfListToScObsOfSeq(oJava.asInstanceOf[rx.Observable[_ <: java.util.List[T]]])
  }

  /**
   * Creates an Observable which produces buffers of collected values. This Observable starts a new buffer
   * periodically, which is determined by the `timeshift` argument. Each buffer is emitted after a fixed timespan
   * specified by the `timespan` argument. When the source Observable completes or encounters an error, the
   * current buffer is emitted and the event is propagated.
   *
   * @param timespan
   *            The period of time each buffer is collecting values before it should be emitted.
   * @param timeshift
   *            The period of time after which a new buffer will be created.
   * @return
   *         An [[rx.lang.scala.Observable]] which produces new buffers periodically, and these are emitted after
   *         a fixed timespan has elapsed.
   */
  def buffer(timespan: Duration, timeshift: Duration): Observable[Seq[T]] = {
    val span: Long = timespan.length
    val shift: Long = timespan.unit.convert(timeshift.length, timeshift.unit)
    val unit: TimeUnit = timespan.unit
    val oJava: rx.Observable[_ <: java.util.List[_]] = asJavaObservable.buffer(span, shift, unit)
    Observable.jObsOfListToScObsOfSeq(oJava.asInstanceOf[rx.Observable[_ <: java.util.List[T]]])
  }

  /**
   * Creates an Observable which produces buffers of collected values. This Observable starts a new buffer
   * periodically, which is determined by the `timeshift` argument. Each buffer is emitted after a fixed timespan
   * specified by the `timespan` argument. When the source Observable completes or encounters an error, the
   * current buffer is emitted and the event is propagated.
   *
   * @param timespan
   *            The period of time each buffer is collecting values before it should be emitted.
   * @param timeshift
   *            The period of time after which a new buffer will be created.
   * @param scheduler
   *            The [[rx.lang.scala.Scheduler]] to use when determining the end and start of a buffer.
   * @return
   *         An [[rx.lang.scala.Observable]] which produces new buffers periodically, and these are emitted after
   *         a fixed timespan has elapsed.
   */
  def buffer(timespan: Duration, timeshift: Duration, scheduler: Scheduler): Observable[Seq[T]] = {
    val span: Long = timespan.length
    val shift: Long = timespan.unit.convert(timeshift.length, timeshift.unit)
    val unit: TimeUnit = timespan.unit
    val oJava: rx.Observable[_ <: java.util.List[_]] = asJavaObservable.buffer(span, shift, unit, scheduler)
    Observable.jObsOfListToScObsOfSeq(oJava.asInstanceOf[rx.Observable[_ <: java.util.List[T]]])
  }

  /**
   * Returns an Observable that emits non-overlapping buffered items from the source Observable each time the
   * specified boundary Observable emits an item.
   * <p>
   * <img width="640" src="https://raw.github.com/wiki/Netflix/RxJava/images/rx-operators/buffer8.png">
   * <p>
   * Completion of either the source or the boundary Observable causes the returned Observable to emit the
   * latest buffer and complete.
   *
   * @param boundary the boundary Observable
   * @return an Observable that emits buffered items from the source Observable when the boundary Observable
   *         emits an item
   */
  def buffer(boundary: Observable[Any]): Observable[Seq[T]] = {
    val thisJava = this.asJavaObservable.asInstanceOf[rx.Observable[T]]
    toScalaObservable(thisJava.buffer(boundary.asJavaObservable)).map(_.asScala)
  }

  /**
   * Returns an Observable that emits non-overlapping buffered items from the source Observable each time the
   * specified boundary Observable emits an item.
   * <p>
   * <img width="640" src="https://raw.github.com/wiki/Netflix/RxJava/images/rx-operators/buffer8.png">
   * <p>
   * Completion of either the source or the boundary Observable causes the returned Observable to emit the
   * latest buffer and complete.
   *
   * @param boundary the boundary Observable
   * @param initialCapacity the initial capacity of each buffer chunk
   * @return an Observable that emits buffered items from the source Observable when the boundary Observable
   *         emits an item
   */
  def buffer(boundary: Observable[Any], initialCapacity: Int): Observable[Seq[T]] = {
    val thisJava = this.asJavaObservable.asInstanceOf[rx.Observable[T]]
    toScalaObservable(thisJava.buffer(boundary.asJavaObservable, initialCapacity)).map(_.asScala)
  }

  /**
   * Creates an Observable which produces windows of collected values. This Observable produces connected
   * non-overlapping windows. The current window is emitted and replaced with a new window when the
   * Observable produced by the specified function produces an object. 
   * The function will then be used to create a new Observable to listen for the end of the next
   * window.
   *
   * @param closings
   *            The function which is used to produce an [[rx.lang.scala.Observable]] for every window created.
   *            When this [[rx.lang.scala.Observable]] produces an object, the associated window
   *            is emitted and replaced with a new one.
   * @return
   *         An [[rx.lang.scala.Observable]] which produces connected non-overlapping windows, which are emitted
   *         when the current [[rx.lang.scala.Observable]] created with the function argument produces an object.
   */
  def window(closings: () => Observable[Any]): Observable[Observable[T]] = {
    val func : Func0[_ <: rx.Observable[_ <: Any]] = closings().asJavaObservable
    val o1: rx.Observable[_ <: rx.Observable[_]] = asJavaObservable.window[Any](func)
    val o2 = Observable.items(o1).map((x: rx.Observable[_]) => {
      val x2 = x.asInstanceOf[rx.Observable[_ <: T]]
      toScalaObservable[T](x2)
    })
    o2
  }

  /**
   * Creates an Observable which produces windows of collected values. Chunks are created when the specified `openings`
   * Observable produces an object. That object is used to construct an Observable to emit windows, feeding it into `closings` function.
   * Windows are emitted when the created Observable produces an object.
   *
   * @param openings
   *            The [[rx.lang.scala.Observable]] which when it produces an object, will cause
   *            another window to be created.
   * @param closings
   *            The function which is used to produce an [[rx.lang.scala.Observable]] for every window created.
   *            When this [[rx.lang.scala.Observable]] produces an object, the associated window
   *            is emitted.
   * @return
   *         An [[rx.lang.scala.Observable]] which produces windows which are created and emitted when the specified [[rx.lang.scala.Observable]]s publish certain objects.
   */
  def window[Opening](openings: Observable[Opening], closings: Opening => Observable[Any]) = {
    Observable.jObsOfJObsToScObsOfScObs(
      asJavaObservable.window[Opening, Any](openings.asJavaObservable, (op: Opening) => closings(op).asJavaObservable))
      : Observable[Observable[T]] // SI-7818
  }

  /**
   * Creates an Observable which produces windows of collected values. This Observable produces connected
   * non-overlapping windows, each containing `count` elements. When the source Observable completes or
   * encounters an error, the current window is emitted, and the event is propagated.
   *
   * @param count
   *            The maximum size of each window before it should be emitted.
   * @return
   *         An [[rx.lang.scala.Observable]] which produces connected non-overlapping windows containing at most
   *         `count` produced values.
   */
  def window(count: Int): Observable[Observable[T]] = {
    // this unnecessary ascription is needed because of this bug (without, compiler crashes):
    // https://issues.scala-lang.org/browse/SI-7818
    Observable.jObsOfJObsToScObsOfScObs(asJavaObservable.window(count)) : Observable[Observable[T]]
  }

  /**
   * Creates an Observable which produces windows of collected values. This Observable produces windows every
   * `skip` values, each containing `count` elements. When the source Observable completes or encounters an error,
   * the current window is emitted and the event is propagated.
   *
   * @param count
   *            The maximum size of each window before it should be emitted.
   * @param skip
   *            How many produced values need to be skipped before starting a new window. Note that when `skip` and
   *            `count` are equal that this is the same operation as `window(int)`.
   * @return
   *         An [[rx.lang.scala.Observable]] which produces windows every `skip` values containing at most
   *         `count` produced values.
   */
  def window(count: Int, skip: Int): Observable[Observable[T]] = {
    Observable.jObsOfJObsToScObsOfScObs(asJavaObservable.window(count, skip))
      : Observable[Observable[T]] // SI-7818
  }

  /**
   * Creates an Observable which produces windows of collected values. This Observable produces connected
   * non-overlapping windows, each of a fixed duration specified by the `timespan` argument. When the source
   * Observable completes or encounters an error, the current window is emitted and the event is propagated.
   *
   * @param timespan
   *            The period of time each window is collecting values before it should be emitted, and
   *            replaced with a new window.
   * @return
   *         An [[rx.lang.scala.Observable]] which produces connected non-overlapping windows with a fixed duration.
   */
  def window(timespan: Duration): Observable[Observable[T]] = {
    Observable.jObsOfJObsToScObsOfScObs(asJavaObservable.window(timespan.length, timespan.unit))
      : Observable[Observable[T]] // SI-7818
  }

  /**
   * Creates an Observable which produces windows of collected values. This Observable produces connected
   * non-overlapping windows, each of a fixed duration specified by the `timespan` argument. When the source
   * Observable completes or encounters an error, the current window is emitted and the event is propagated.
   *
   * @param timespan
   *            The period of time each window is collecting values before it should be emitted, and
   *            replaced with a new window.
   * @param scheduler
   *            The [[rx.lang.scala.Scheduler]] to use when determining the end and start of a window.
   * @return
   *         An [[rx.lang.scala.Observable]] which produces connected non-overlapping windows with a fixed duration.
   */
  def window(timespan: Duration, scheduler: Scheduler): Observable[Observable[T]] = {
    Observable.jObsOfJObsToScObsOfScObs(asJavaObservable.window(timespan.length, timespan.unit, scheduler))
      : Observable[Observable[T]] // SI-7818
  }

  /**
   * Creates an Observable which produces windows of collected values. This Observable produces connected
   * non-overlapping windows, each of a fixed duration specified by the `timespan` argument or a maximum size
   * specified by the `count` argument (which ever is reached first). When the source Observable completes
   * or encounters an error, the current window is emitted and the event is propagated.
   *
   * @param timespan
   *            The period of time each window is collecting values before it should be emitted, and
   *            replaced with a new window.
   * @param count
   *            The maximum size of each window before it should be emitted.
   * @return
   *         An [[rx.lang.scala.Observable]] which produces connected non-overlapping windows which are emitted after
   *         a fixed duration or when the window has reached maximum capacity (which ever occurs first).
   */
  def window(timespan: Duration, count: Int): Observable[Observable[T]] = {
    Observable.jObsOfJObsToScObsOfScObs(asJavaObservable.window(timespan.length, timespan.unit, count))
      : Observable[Observable[T]] // SI-7818
  }

  /**
   * Creates an Observable which produces windows of collected values. This Observable produces connected
   * non-overlapping windows, each of a fixed duration specified by the `timespan` argument or a maximum size
   * specified by the `count` argument (which ever is reached first). When the source Observable completes
   * or encounters an error, the current window is emitted and the event is propagated.
   *
   * @param timespan
   *            The period of time each window is collecting values before it should be emitted, and
   *            replaced with a new window.
   * @param count
   *            The maximum size of each window before it should be emitted.
   * @param scheduler
   *            The [[rx.lang.scala.Scheduler]] to use when determining the end and start of a window.
   * @return
   *         An [[rx.lang.scala.Observable]] which produces connected non-overlapping windows which are emitted after
   *         a fixed duration or when the window has reached maximum capacity (which ever occurs first).
   */
  def window(timespan: Duration, count: Int, scheduler: Scheduler): Observable[Observable[T]] = {
    Observable.jObsOfJObsToScObsOfScObs(asJavaObservable.window(timespan.length, timespan.unit, count, scheduler))
      : Observable[Observable[T]] // SI-7818
  }

  /**
   * Creates an Observable which produces windows of collected values. This Observable starts a new window
   * periodically, which is determined by the `timeshift` argument. Each window is emitted after a fixed timespan
   * specified by the `timespan` argument. When the source Observable completes or encounters an error, the
   * current window is emitted and the event is propagated.
   *
   * @param timespan
   *            The period of time each window is collecting values before it should be emitted.
   * @param timeshift
   *            The period of time after which a new window will be created.
   * @return
   *         An [[rx.lang.scala.Observable]] which produces new windows periodically, and these are emitted after
   *         a fixed timespan has elapsed.
   */
  def window(timespan: Duration, timeshift: Duration): Observable[Observable[T]] = {
    val span: Long = timespan.length
    val shift: Long = timespan.unit.convert(timeshift.length, timeshift.unit)
    val unit: TimeUnit = timespan.unit
    Observable.jObsOfJObsToScObsOfScObs(asJavaObservable.window(span, shift, unit))
      : Observable[Observable[T]] // SI-7818
  }

  /**
   * Creates an Observable which produces windows of collected values. This Observable starts a new window
   * periodically, which is determined by the `timeshift` argument. Each window is emitted after a fixed timespan
   * specified by the `timespan` argument. When the source Observable completes or encounters an error, the
   * current window is emitted and the event is propagated.
   *
   * @param timespan
   *            The period of time each window is collecting values before it should be emitted.
   * @param timeshift
   *            The period of time after which a new window will be created.
   * @param scheduler
   *            The [[rx.lang.scala.Scheduler]] to use when determining the end and start of a window.
   * @return
   *         An [[rx.lang.scala.Observable]] which produces new windows periodically, and these are emitted after
   *         a fixed timespan has elapsed.
   */
  def window(timespan: Duration, timeshift: Duration, scheduler: Scheduler): Observable[Observable[T]] = {
    val span: Long = timespan.length
    val shift: Long = timespan.unit.convert(timeshift.length, timeshift.unit)
    val unit: TimeUnit = timespan.unit
    Observable.jObsOfJObsToScObsOfScObs(asJavaObservable.window(span, shift, unit, scheduler))
      : Observable[Observable[T]] // SI-7818
  }

  /**
   * Returns an Observable which only emits those items for which a given predicate holds.
   *
   * <img width="640" src="https://raw.github.com/wiki/Netflix/RxJava/images/rx-operators/filter.png">
   *
   * @param predicate
   *            a function that evaluates the items emitted by the source Observable, returning `true` if they pass the filter
   * @return an Observable that emits only those items in the original Observable that the filter
   *         evaluates as `true`
   */
  def filter(predicate: T => Boolean): Observable[T] = {
    toScalaObservable[T](asJavaObservable.filter(predicate))
  }

  /**
   * Registers an function to be called when this Observable invokes [[rx.lang.scala.Observer.onCompleted onCompleted]] or [[rx.lang.scala.Observer.onError onError]].
   *
   * <img width="640" src="https://github.com/Netflix/RxJava/wiki/images/rx-operators/finallyDo.png">
   *
   * @param action
   *            an function to be invoked when the source Observable finishes
   * @return an Observable that emits the same items as the source Observable, then invokes the function
   */
  def finallyDo(action: () => Unit): Observable[T] = {
    toScalaObservable[T](asJavaObservable.finallyDo(action))
  }

  /**
   * Creates a new Observable by applying a function that you supply to each item emitted by
   * the source Observable, where that function returns an Observable, and then merging those
   * resulting Observables and emitting the results of this merger.
   *
   * <img width="640" src="https://raw.github.com/wiki/Netflix/RxJava/images/rx-operators/flatMap.png">
   *
   * @param f
   *            a function that, when applied to an item emitted by the source Observable, returns
   *            an Observable
   * @return an Observable that emits the result of applying the transformation function to each
   *         item emitted by the source Observable and merging the results of the Observables
   *         obtained from this transformation.
   */
  def flatMap[R](f: T => Observable[R]): Observable[R] = {
    toScalaObservable[R](asJavaObservable.flatMap[R](new Func1[T, rx.Observable[_ <: R]]{
      def call(t1: T): rx.Observable[_ <: R] = { f(t1).asJavaObservable }
    }))
  }

  /**
   * Returns an Observable that applies the given function to each item emitted by an
   * Observable and emits the result.
   *
   * <img width="640" src="https://raw.github.com/wiki/Netflix/RxJava/images/rx-operators/map.png">
   *
   * @param func
   *            a function to apply to each item emitted by the Observable
   * @return an Observable that emits the items from the source Observable, transformed by the
   *         given function
   */
  def map[R](func: T => R): Observable[R] = {
    toScalaObservable[R](asJavaObservable.map[R](new Func1[T,R] {
      def call(t1: T): R = func(t1)
    }))
  }

  /**
   * Turns all of the notifications from a source Observable into [[rx.lang.scala.Observer.onNext onNext]] emissions,
   * and marks them with their original notification types within [[rx.lang.scala.Notification]] objects.
   *
   * <img width="640" src="https://raw.github.com/wiki/Netflix/RxJava/images/rx-operators/materialize.png">
   *
   * @return an Observable whose items are the result of materializing the items and
   *         notifications of the source Observable
   */
  def materialize: Observable[Notification[T]] = {
    toScalaObservable[rx.Notification[_ <: T]](asJavaObservable.materialize()).map(Notification(_))
  }

  /**
   * Asynchronously subscribes and unsubscribes Observers on the specified [[rx.lang.scala.Scheduler]].
   *
   * <img width="640" src="https://github.com/Netflix/RxJava/wiki/images/rx-operators/subscribeOn.png">
   *
   * @param scheduler
   *            the [[rx.lang.scala.Scheduler]] to perform subscription and unsubscription actions on
   * @return the source Observable modified so that its subscriptions and unsubscriptions happen
   *         on the specified [[rx.lang.scala.Scheduler]]
   */
  def subscribeOn(scheduler: Scheduler): Observable[T] = {
    toScalaObservable[T](asJavaObservable.subscribeOn(scheduler))
  }

  /**
   * Asynchronously notify [[rx.lang.scala.Observer]]s on the specified [[rx.lang.scala.Scheduler]].
   *
   * <img width="640" src="https://github.com/Netflix/RxJava/wiki/images/rx-operators/observeOn.png">
   *
   * @param scheduler
   *            the [[rx.lang.scala.Scheduler]] to notify [[rx.lang.scala.Observer]]s on
   * @return the source Observable modified so that its [[rx.lang.scala.Observer]]s are notified on the
   *         specified [[rx.lang.scala.Scheduler]]
   */
  def observeOn(scheduler: Scheduler): Observable[T] = {
    toScalaObservable[T](asJavaObservable.observeOn(scheduler))
  }

  /**
   * Returns an Observable that reverses the effect of [[rx.lang.scala.Observable.materialize]] by
   * transforming the [[rx.lang.scala.Notification]] objects emitted by the source Observable into the items
   * or notifications they represent.
   *
   * This operation is only available if `this` is of type `Observable[Notification[U]]` for some `U`, 
   * otherwise you will get a compilation error.
   *
   * <img width="640" src="https://github.com/Netflix/RxJava/wiki/images/rx-operators/dematerialize.png">
   *
   * @return an Observable that emits the items and notifications embedded in the [[rx.lang.scala.Notification]] objects emitted by the source Observable
   *
   * @usecase def dematerialize[U]: Observable[U]
   *   @inheritdoc
   *
   */
  // with =:= it does not work, why?
  def dematerialize[U](implicit evidence: Observable[T] <:< Observable[Notification[U]]): Observable[U] = {
    val o1: Observable[Notification[U]] = this
    val o2: Observable[rx.Notification[_ <: U]] = o1.map(_.asJavaNotification)
    val o3 = o2.asJavaObservable.dematerialize[U]()
    toScalaObservable[U](o3)
  }

  /**
   * Instruct an Observable to pass control to another Observable rather than invoking [[rx.lang.scala.Observer.onError onError]] if it encounters an error.
   *
   * <img width="640" src="https://raw.github.com/wiki/Netflix/RxJava/images/rx-operators/onErrorResumeNext.png">
   *
   * By default, when an Observable encounters an error that prevents it from emitting the
   * expected item to its [[rx.lang.scala.Observer]], the Observable invokes its Observer's
   * `onError` method, and then quits without invoking any more of its Observer's
   * methods. The `onErrorResumeNext` method changes this behavior. If you pass a
   * function that returns an Observable (`resumeFunction`) to
   * `onErrorResumeNext`, if the original Observable encounters an error, instead of
   * invoking its Observer's `onError` method, it will instead relinquish control to
   * the Observable returned from `resumeFunction`, which will invoke the Observer's 
   * [[rx.lang.scala.Observer.onNext onNext]] method if it is able to do so. In such a case, because no
   * Observable necessarily invokes `onError`, the Observer may never know that an
   * error happened.
   *
   * You can use this to prevent errors from propagating or to supply fallback data should errors
   * be encountered.
   *
   * @param resumeFunction
   *            a function that returns an Observable that will take over if the source Observable
   *            encounters an error
   * @return the original Observable, with appropriately modified behavior
   */
  def onErrorResumeNext[U >: T](resumeFunction: Throwable => Observable[U]): Observable[U] = {
    val f: Func1[Throwable, rx.Observable[_ <: U]] = (t: Throwable) => resumeFunction(t).asJavaObservable
    val f2 = f.asInstanceOf[Func1[Throwable, rx.Observable[Nothing]]]
    toScalaObservable[U](asJavaObservable.onErrorResumeNext(f2))
  }

  /**
   * Instruct an Observable to pass control to another Observable rather than invoking [[rx.lang.scala.Observer.onError onError]] if it encounters an error.
   *
   * <img width="640" src="https://raw.github.com/wiki/Netflix/RxJava/images/rx-operators/onErrorResumeNext.png">
   *
   * By default, when an Observable encounters an error that prevents it from emitting the
   * expected item to its [[rx.lang.scala.Observer]], the Observable invokes its Observer's
   * `onError` method, and then quits without invoking any more of its Observer's
   * methods. The `onErrorResumeNext` method changes this behavior. If you pass
   * another Observable (`resumeSequence`) to an Observable's
   * `onErrorResumeNext` method, if the original Observable encounters an error,
   * instead of invoking its Observer's `onError` method, it will instead relinquish
   * control to `resumeSequence` which will invoke the Observer's [[rx.lang.scala.Observer.onNext onNext]]
   * method if it is able to do so. In such a case, because no
   * Observable necessarily invokes `onError`, the Observer may never know that an
   * error happened.
   *
   * You can use this to prevent errors from propagating or to supply fallback data should errors
   * be encountered.
   *
   * @param resumeSequence
   *            a function that returns an Observable that will take over if the source Observable
   *            encounters an error
   * @return the original Observable, with appropriately modified behavior
   */
  def onErrorResumeNext[U >: T](resumeSequence: Observable[U]): Observable[U] = {
    val rSeq1: rx.Observable[_ <: U] = resumeSequence.asJavaObservable
    val rSeq2: rx.Observable[Nothing] = rSeq1.asInstanceOf[rx.Observable[Nothing]]
    toScalaObservable[U](asJavaObservable.onErrorResumeNext(rSeq2))
  }

  /**
   * Instruct an Observable to pass control to another Observable rather than invoking [[rx.lang.scala.Observer.onError onError]] if it encounters an error of type `java.lang.Exception`.
   *
   * This differs from `Observable.onErrorResumeNext` in that this one does not handle `java.lang.Throwable` or `java.lang.Error` but lets those continue through.
   *
   * <img width="640" src="https://raw.github.com/wiki/Netflix/RxJava/images/rx-operators/onErrorResumeNext.png">
   *
   * By default, when an Observable encounters an error that prevents it from emitting the
   * expected item to its [[rx.lang.scala.Observer]], the Observable invokes its Observer's
   * `onError` method, and then quits without invoking any more of its Observer's
   * methods. The `onErrorResumeNext` method changes this behavior. If you pass
   * another Observable (`resumeSequence`) to an Observable's
   * `onErrorResumeNext` method, if the original Observable encounters an error,
   * instead of invoking its Observer's `onError` method, it will instead relinquish
   * control to `resumeSequence` which will invoke the Observer's [[rx.lang.scala.Observer.onNext onNext]]
   * method if it is able to do so. In such a case, because no
   * Observable necessarily invokes `onError`, the Observer may never know that an
   * error happened.
   *
   * You can use this to prevent errors from propagating or to supply fallback data should errors
   * be encountered.
   *
   * @param resumeSequence
   *            a function that returns an Observable that will take over if the source Observable
   *            encounters an error
   * @return the original Observable, with appropriately modified behavior
   */
  def onExceptionResumeNext[U >: T](resumeSequence: Observable[U]): Observable[U] = {
    val rSeq1: rx.Observable[_ <: U] = resumeSequence.asJavaObservable
    val rSeq2: rx.Observable[Nothing] = rSeq1.asInstanceOf[rx.Observable[Nothing]]
    toScalaObservable[U](asJavaObservable.onExceptionResumeNext(rSeq2))
  }

  /**
   * Instruct an Observable to emit an item (returned by a specified function) rather than
   * invoking [[rx.lang.scala.Observer.onError onError]] if it encounters an error.
   *
   * <img width="640" src="https://github.com/Netflix/RxJava/wiki/images/rx-operators/onErrorReturn.png">
   *
   * By default, when an Observable encounters an error that prevents it from emitting the
   * expected item to its [[rx.lang.scala.Observer]], the Observable invokes its Observer's
   * `onError` method, and then quits without invoking any more of its Observer's
   * methods. The `onErrorReturn` method changes this behavior. If you pass a function
   * (`resumeFunction`) to an Observable's `onErrorReturn` method, if the
   * original Observable encounters an error, instead of invoking its Observer's
   * `onError` method, it will instead pass the return value of
   * `resumeFunction` to the Observer's [[rx.lang.scala.Observer.onNext onNext]] method.
   *
   * You can use this to prevent errors from propagating or to supply fallback data should errors
   * be encountered.
   *
   * @param resumeFunction
   *            a function that returns an item that the new Observable will emit if the source
   *            Observable encounters an error
   * @return the original Observable with appropriately modified behavior
   */
  def onErrorReturn[U >: T](resumeFunction: Throwable => U): Observable[U] = {
    val f1: Func1[Throwable, _ <: U] = resumeFunction
    val f2 = f1.asInstanceOf[Func1[Throwable, Nothing]]
    toScalaObservable[U](asJavaObservable.onErrorReturn(f2))
  }

  /**
   * Returns an Observable that applies a function of your choosing to the first item emitted by a
   * source Observable, then feeds the result of that function along with the second item emitted
   * by the source Observable into the same function, and so on until all items have been emitted
   * by the source Observable, and emits the final result from the final call to your function as
   * its sole item.
   *
   * <img width="640" src="https://raw.github.com/wiki/Netflix/RxJava/images/rx-operators/reduce.png">
   *
   * This technique, which is called "reduce" or "aggregate" here, is sometimes called "fold,"
   * "accumulate," "compress," or "inject" in other programming contexts. Groovy, for instance,
   * has an `inject` method that does a similar operation on lists.
   *
   * @param accumulator
   *            An accumulator function to be invoked on each item emitted by the source
   *            Observable, whose result will be used in the next accumulator call
   * @return an Observable that emits a single item that is the result of accumulating the
   *         output from the source Observable
   */
  def reduce[U >: T](accumulator: (U, U) => U): Observable[U] = {
    val func: Func2[_ >: U, _ >: U, _ <: U] = accumulator
    val func2 = func.asInstanceOf[Func2[T, T, T]]
    toScalaObservable[U](asJavaObservable.asInstanceOf[rx.Observable[T]].reduce(func2))
  }

  /**
   * Returns a pair of a start function and an [[rx.lang.scala.Observable]] that shares a single subscription to the underlying
   * Observable that will replay all of its items and notifications to any future [[rx.lang.scala.Observer]].
   *
   * <img width="640" src="https://raw.github.com/wiki/Netflix/RxJava/images/rx-operators/replay.png">
   *
   * @return a pair of a start function and an [[rx.lang.scala.Observable]] such that when the start function
   *         is called, the Observable starts to emit items to its [[rx.lang.scala.Observer]]s
   */
  def replay: ConnectableObservable[T] = {
    new ConnectableObservable[T](asJavaObservable.replay())
  }

  /**
   * Returns an Observable that emits items that are the results of invoking a specified selector on the items
   * emitted by a `ConnectableObservable` that shares a single subscription to the source Observable.
   * <p>
   * <img width="640" src="https://raw.github.com/wiki/Netflix/RxJava/images/rx-operators/replay.f.png">
   *
   * @param selector the selector function, which can use the multicasted sequence as many times as needed, without
   *                 causing multiple subscriptions to the Observable
   * @return an Observable that emits items that are the results of invoking the selector on a `ConnectableObservable`
   *         that shares a single subscription to the source Observable
   */
  def replay[U >: T, R](selector: Observable[U] => Observable[R]): Observable[R] = {
    val thisJava = this.asJavaObservable.asInstanceOf[rx.Observable[U]]
    val fJava: Func1[rx.Observable[U], rx.Observable[R]] =
      (jo: rx.Observable[U]) => selector(toScalaObservable[U](jo)).asJavaObservable.asInstanceOf[rx.Observable[R]]
    toScalaObservable[R](thisJava.replay(fJava))
  }

  /**
   * Returns an Observable that emits items that are the results of invoking a specified selector on items
   * emitted by a `ConnectableObservable` that shares a single subscription to the source Observable,
   * replaying `bufferSize` notifications.
   * <p>
   * <img width="640" src="https://raw.github.com/wiki/Netflix/RxJava/images/rx-operators/replay.fn.png">
   *
   * @param selector the selector function, which can use the multicasted sequence as many times as needed, without
   *                 causing multiple subscriptions to the Observable
   * @param bufferSize  the buffer size that limits the number of items the connectable observable can replay
   * @return an Observable that emits items that are the results of invoking the selector on items emitted by
   *         a `ConnectableObservable` that shares a single subscription to the source Observable  replaying
   *         no more than `bufferSize` items
   */
  def replay[U >: T, R](selector: Observable[U] => Observable[R], bufferSize: Int): Observable[R] = {
    val thisJava = this.asJavaObservable.asInstanceOf[rx.Observable[U]]
    val fJava: Func1[rx.Observable[U], rx.Observable[R]] =
      (jo: rx.Observable[U]) => selector(toScalaObservable[U](jo)).asJavaObservable.asInstanceOf[rx.Observable[R]]
    toScalaObservable[R](thisJava.replay(fJava, bufferSize))
  }

  /**
   * Returns an Observable that emits items that are the results of invoking a specified selector on items
   * emitted by a `ConnectableObservable` that shares a single subscription to the source Observable,
   * replaying no more than `bufferSize` items that were emitted within a specified time window.
   * <p>
   * <img width="640" src="https://raw.github.com/wiki/Netflix/RxJava/images/rx-operators/replay.fnt.png">
   *
   * @param selector  a selector function, which can use the multicasted sequence as many times as needed, without
   *                  causing multiple subscriptions to the Observable
   * @param bufferSize the buffer size that limits the number of items the connectable observable can replay
   * @param time the duration of the window in which the replayed items must have been emitted
   * @return an Observable that emits items that are the results of invoking the selector on items emitted by
   *         a `ConnectableObservable` that shares a single subscription to the source Observable, and
   *         replays no more than `bufferSize` items that were emitted within the window defined by `time`
   */
  def replay[U >: T, R](selector: Observable[U] => Observable[R], bufferSize: Int, time: Duration): Observable[R] = {
    val thisJava = this.asJavaObservable.asInstanceOf[rx.Observable[U]]
    val fJava: Func1[rx.Observable[U], rx.Observable[R]] =
      (jo: rx.Observable[U]) => selector(toScalaObservable[U](jo)).asJavaObservable.asInstanceOf[rx.Observable[R]]
    toScalaObservable[R](thisJava.replay(fJava, bufferSize, time.length, time.unit))
  }

  /**
   * Returns an Observable that emits items that are the results of invoking a specified selector on items
   * emitted by a `ConnectableObservable` that shares a single subscription to the source Observable,
   * replaying no more than `bufferSize` items that were emitted within a specified time window.
   * <p>
   * <img width="640" src="https://raw.github.com/wiki/Netflix/RxJava/images/rx-operators/replay.fnts.png">
   *
   * @param selector  a selector function, which can use the multicasted sequence as many times as needed, without
   *                  causing multiple subscriptions to the Observable
   * @param bufferSize the buffer size that limits the number of items the connectable observable can replay
   * @param time  the duration of the window in which the replayed items must have been emitted
   * @param scheduler  the Scheduler that is the time source for the window
   * @return an Observable that emits items that are the results of invoking the selector on items emitted by
   *         a `ConnectableObservable` that shares a single subscription to the source Observable, and
   *         replays no more than `bufferSize` items that were emitted within the window defined by `time`
   * @throws IllegalArgumentException if `bufferSize` is less than zero
   */
  def replay[U >: T, R](selector: Observable[U] => Observable[R], bufferSize: Int, time: Duration, scheduler: Scheduler): Observable[R] = {
    val thisJava = this.asJavaObservable.asInstanceOf[rx.Observable[U]]
    val fJava: Func1[rx.Observable[U], rx.Observable[R]] =
      (jo: rx.Observable[U]) => selector(toScalaObservable[U](jo)).asJavaObservable.asInstanceOf[rx.Observable[R]]
    toScalaObservable[R](thisJava.replay(fJava, bufferSize, time.length, time.unit, scheduler))
  }

  /**
   * Returns an Observable that emits items that are the results of invoking a specified selector on items
   * emitted by a `ConnectableObservable` that shares a single subscription to the source Observable,
   * replaying a maximum of `bufferSize` items.
   * <p>
   * <img width="640" src="https://raw.github.com/wiki/Netflix/RxJava/images/rx-operators/replay.fns.png">
   *
   * @param selector  a selector function, which can use the multicasted sequence as many times as needed, without
   *                  causing multiple subscriptions to the Observable
   * @param bufferSize  the buffer size that limits the number of items the connectable observable can replay
   * @param scheduler  the Scheduler on which the replay is observed
   * @return an Observable that emits items that are the results of invoking the selector on items emitted by
   *         a `ConnectableObservable` that shares a single subscription to the source Observable,
   *         replaying no more than `bufferSize` notifications
   */
  def replay[U >: T, R](selector: Observable[U] => Observable[R], bufferSize: Int, scheduler: Scheduler): Observable[R] = {
    val thisJava = this.asJavaObservable.asInstanceOf[rx.Observable[U]]
    val fJava: Func1[rx.Observable[U], rx.Observable[R]] =
      (jo: rx.Observable[U]) => selector(toScalaObservable[U](jo)).asJavaObservable.asInstanceOf[rx.Observable[R]]
    toScalaObservable[R](thisJava.replay(fJava, bufferSize, scheduler))
  }

  /**
   * Returns an Observable that emits items that are the results of invoking a specified selector on items
   * emitted by a `ConnectableObservable` that shares a single subscription to the source Observable,
   * replaying all items that were emitted within a specified time window.
   * <p>
   * <img width="640" src="https://raw.github.com/wiki/Netflix/RxJava/images/rx-operators/replay.ft.png">
   *
   * @param selector   a selector function, which can use the multicasted sequence as many times as needed, without
   *                   causing multiple subscriptions to the Observable
   * @param time  the duration of the window in which the replayed items must have been emitted
   * @return an Observable that emits items that are the results of invoking the selector on items emitted by
   *         a `ConnectableObservable` that shares a single subscription to the source Observable,
   *         replaying all items that were emitted within the window defined by `time`
   */
  def replay[U >: T, R](selector: Observable[U] => Observable[R], time: Duration, scheduler: Scheduler): Observable[R] = {
    val thisJava = this.asJavaObservable.asInstanceOf[rx.Observable[U]]
    val fJava: Func1[rx.Observable[U], rx.Observable[R]] =
      (jo: rx.Observable[U]) => selector(toScalaObservable[U](jo)).asJavaObservable.asInstanceOf[rx.Observable[R]]
    toScalaObservable[R](thisJava.replay(fJava, time.length, time.unit, scheduler))
  }

  /**
   * Returns an Observable that emits items that are the results of invoking a specified selector on items
   * emitted by a `ConnectableObservable` that shares a single subscription to the source Observable.
   * <p>
   * <img width="640" src="https://raw.github.com/wiki/Netflix/RxJava/images/rx-operators/replay.fs.png">
   *
   * @param selector  a selector function, which can use the multicasted sequence as many times as needed, without
   *                  causing multiple subscriptions to the Observable
   * @param scheduler    the Scheduler where the replay is observed
   * @return an Observable that emits items that are the results of invoking the selector on items emitted by
   *         a `ConnectableObservable` that shares a single subscription to the source Observable,
   *         replaying all items
   */
  def replay[U >: T, R](selector: Observable[U] => Observable[R], scheduler: Scheduler): Observable[R] = {
    val thisJava = this.asJavaObservable.asInstanceOf[rx.Observable[U]]
    val fJava: Func1[rx.Observable[U], rx.Observable[R]] =
      (jo: rx.Observable[U]) => selector(toScalaObservable[U](jo)).asJavaObservable.asInstanceOf[rx.Observable[R]]
    toScalaObservable[R](thisJava.replay(fJava, scheduler))
  }

  /**
   * Returns a `ConnectableObservable` that shares a single subscription to the source Observable and
   * replays at most `bufferSize` items that were emitted during a specified time window.
   * <p>
   * <img width="640" src="https://raw.github.com/wiki/Netflix/RxJava/images/rx-operators/replay.nt.png">
   *
   * @param bufferSize the buffer size that limits the number of items that can be replayed
   * @param time the duration of the window in which the replayed items must have been emitted
   * @return a `ConnectableObservable` that shares a single subscription to the source Observable and
   *         replays at most `bufferSize` items that were emitted during the window defined by `time`
   */
  def replay(bufferSize: Int, time: Duration): ConnectableObservable[T] = {
    new ConnectableObservable[T](asJavaObservable.replay(bufferSize, time.length, time.unit))
  }

  /**
   * Returns a `ConnectableObservable` that shares a single subscription to the source Observable and
   * that replays a maximum of `bufferSize` items that are emitted within a specified time window.
   * <p>
   * <img width="640" src="https://raw.github.com/wiki/Netflix/RxJava/images/rx-operators/replay.nts.png">
   *
   * @param bufferSize the buffer size that limits the number of items that can be replayed
   * @param time the duration of the window in which the replayed items must have been emitted
   * @param scheduler the scheduler that is used as a time source for the window
   * @return a `ConnectableObservable` that shares a single subscription to the source Observable and
   *         replays at most `bufferSize` items that were emitted during the window defined by `time``
   * @throws IllegalArgumentException if `bufferSize` is less than zero
   */
  def replay(bufferSize: Int, time: Duration, scheduler: Scheduler): ConnectableObservable[T] = {
    new ConnectableObservable[T](asJavaObservable.replay(bufferSize, time.length, time.unit, scheduler))
  }

  /**
   * Returns an Observable that emits items that are the results of invoking a specified selector on items
   * emitted by a `ConnectableObservable` that shares a single subscription to the source Observable,
   * replaying all items that were emitted within a specified time window.
   * <p>
   * <img width="640" src="https://raw.github.com/wiki/Netflix/RxJava/images/rx-operators/replay.ft.png">
   *
   * @param selector  a selector function, which can use the multicasted sequence as many times as needed, without
   *                  causing multiple subscriptions to the Observable
   * @param time the duration of the window in which the replayed items must have been emitted
   * @return an Observable that emits items that are the results of invoking the selector on items emitted by
   *         a `ConnectableObservable` that shares a single subscription to the source Observable,
   *         replaying all items that were emitted within the window defined by `time``
   */
  def replay[U >: T, R](selector: Observable[U] => Observable[R], time: Duration): Observable[R] = {
    val thisJava = this.asJavaObservable.asInstanceOf[rx.Observable[U]]
    val fJava: Func1[rx.Observable[U], rx.Observable[R]] =
      (jo: rx.Observable[U]) => selector(toScalaObservable[U](jo)).asJavaObservable.asInstanceOf[rx.Observable[R]]
    toScalaObservable[R](thisJava.replay(fJava, time.length, time.unit))
  }

  /**
   * Returns a `ConnectableObservable` that shares a single subscription to the source Observable that
   * replays at most `bufferSize` items emitted by that Observable.
   * <p>
   * <img width="640" src="https://raw.github.com/wiki/Netflix/RxJava/images/rx-operators/replay.n.png">
   *
   * @param bufferSize the buffer size that limits the number of items that can be replayed
   * @return a `ConnectableObservable` that shares a single subscription to the source Observable and
   *         replays at most `bufferSize` items emitted by that Observable
   */
  def replay(bufferSize: Int): ConnectableObservable[T] = {
    new ConnectableObservable[T](asJavaObservable.replay(bufferSize))
  }

  /**
   * Returns a `ConnectableObservable` that shares a single subscription to the source Observable and
   * replays at most `bufferSize` items emitted by that Observable.
   * <p>
   * <img width="640" src="https://raw.github.com/wiki/Netflix/RxJava/images/rx-operators/replay.ns.png">
   *
   * @param bufferSize the buffer size that limits the number of items that can be replayed
   * @param scheduler the scheduler on which the Observers will observe the emitted items
   * @return a `ConnectableObservable` that shares a single subscription to the source Observable and
   *         replays at most `bufferSize` items that were emitted by the Observable
   */
  def replay(bufferSize: Int, scheduler: Scheduler): ConnectableObservable[T] = {
    new ConnectableObservable[T](asJavaObservable.replay(bufferSize, scheduler))
  }

  /**
   * Returns a `ConnectableObservable` that shares a single subscription to the source Observable and
   * replays all items emitted by that Observable within a specified time window.
   * <p>
   * <img width="640" src="https://raw.github.com/wiki/Netflix/RxJava/images/rx-operators/replay.t.png">
   *
   * @param time  the duration of the window in which the replayed items must have been emitted
   * @return a `ConnectableObservable` that shares a single subscription to the source Observable and
   *         replays the items that were emitted during the window defined by `time`
   */
  def replay(time: Duration): ConnectableObservable[T] = {
    new ConnectableObservable[T](asJavaObservable.replay(time.length, time.unit))
  }

  /**
   * Returns a `ConnectableObservable` that shares a single subscription to the source Observable and
   * replays all items emitted by that Observable within a specified time window.
   * <p>
   * <img width="640" src="https://raw.github.com/wiki/Netflix/RxJava/images/rx-operators/replay.ts.png">
   *
   * @param time the duration of the window in which the replayed items must have been emitted
   * @param scheduler the Scheduler that is the time source for the window
   * @return a `ConnectableObservable` that shares a single subscription to the source Observable and
   *         replays the items that were emitted during the window defined by `time`
   */
  def replay(time: Duration, scheduler: Scheduler): ConnectableObservable[T] = {
    new ConnectableObservable[T](asJavaObservable.replay(time.length, time.unit, scheduler))
  }

  /**
   * Returns a `ConnectableObservable` that shares a single subscription to the source Observable that
   * will replay all of its items and notifications to any future `Observer` on the given `Scheduler`.
   * <p>
   * <img width="640" src="https://raw.github.com/wiki/Netflix/RxJava/images/rx-operators/replay.s.png">
   *
   * @param scheduler the Scheduler on which the Observers will observe the emitted items
   * @return a `ConnectableObservable` that shares a single subscription to the source Observable that
   *         will replay all of its items and notifications to any future `bserver` on the given `Scheduler`
   */
  def replay(scheduler: Scheduler): ConnectableObservable[T] = {
    new ConnectableObservable[T](asJavaObservable.replay(scheduler))
  }

  /**
   * This method has similar behavior to [[rx.lang.scala.Observable.replay]] except that this auto-subscribes to
   * the source Observable rather than returning a start function and an Observable.
   *
   * <img width="640" src="https://github.com/Netflix/RxJava/wiki/images/rx-operators/cache.png">
   *
   * This is useful when you want an Observable to cache responses and you can't control the
   * subscribe/unsubscribe behavior of all the [[rx.lang.scala.Observer]]s.
   *
   * When you call `cache`, it does not yet subscribe to the
   * source Observable. This only happens when `subscribe` is called
   * the first time on the Observable returned by `cache()`.
   * 
   * Note: You sacrifice the ability to unsubscribe from the origin when you use the
   * `cache()` operator so be careful not to use this operator on Observables that
   * emit an infinite or very large number of items that will use up memory.
   *
   * @return an Observable that when first subscribed to, caches all of its notifications for
   *         the benefit of subsequent subscribers.
   */
  def cache: Observable[T] = {
    toScalaObservable[T](asJavaObservable.cache())
  }

  /**
   * Returns an Observable that emits a Boolean that indicates whether the source Observable emitted a
   * specified item.
   *
   * Note: this method uses `==` to compare elements. It's a bit different from RxJava which uses `Object.equals`.
   * <p>
   * <img width="640" src="https://raw.github.com/wiki/Netflix/RxJava/images/rx-operators/contains.png">
   *
   *@param elem the item to search for in the emissions from the source Observable
   * @return an Observable that emits `true` if the specified item is emitted by the source Observable,
   *         or `false` if the source Observable completes without emitting that item
   */
  def contains[U >: T](elem: U): Observable[Boolean] = {
    exists(_ == elem)
  }

  /**
   * Returns a a pair of a start function and an [[rx.lang.scala.Observable]], which waits until the start function is called before it begins emitting
   * items to those [[rx.lang.scala.Observer]]s that have subscribed to it.
   *
   * <img width="640" src="https://github.com/Netflix/RxJava/wiki/images/rx-operators/publishConnect.png">
   *
   * @return an [[rx.lang.scala.observables.ConnectableObservable]].
   */
  def publish: ConnectableObservable[T] = {
    new ConnectableObservable[T](asJavaObservable.publish())
  }


  /**
   * Returns an Observable that emits `initialValue` followed by the items emitted by a `ConnectableObservable` that shares a single subscription to the source Observable.
   * <p>
   * <img width="640" src="https://raw.github.com/wiki/Netflix/RxJava/images/rx-operators/publishConnect.i.png">
   *
   * @param initialValue the initial value to be emitted by the resulting Observable
   * @return a `ConnectableObservable` that shares a single subscription to the underlying Observable and starts with `initialValue`
   */
  def publish[U >: T](initialValue: U): ConnectableObservable[U] = {
    val thisJava = this.asJavaObservable.asInstanceOf[rx.Observable[U]]
    new ConnectableObservable[U](thisJava.publish(initialValue))
  }

  /**
   * Returns an Observable that emits the results of invoking a specified selector on items emitted by a `ConnectableObservable`
   * that shares a single subscription to the underlying sequence.
   * <p>
   * <img width="640" src="https://raw.github.com/wiki/Netflix/RxJava/images/rx-operators/publishConnect.f.png">
   *
   * @param selector a function that can use the multicasted source sequence as many times as needed, without
   *                 causing multiple subscriptions to the source sequence. Subscribers to the given source will
   *                 receive all notifications of the source from the time of the subscription forward.
   * @return an Observable that emits the results of invoking the selector on the items emitted by a `ConnectableObservable`
   *         that shares a single subscription to the underlying sequence
   */
  def publish[U >: T, R](selector: Observable[U] => Observable[R]): Observable[R] = {
    val thisJava = this.asJavaObservable.asInstanceOf[rx.Observable[U]]
    val fJava: Func1[rx.Observable[U], rx.Observable[R]] =
      (jo: rx.Observable[U]) => selector(toScalaObservable[U](jo)).asJavaObservable.asInstanceOf[rx.Observable[R]]
    toScalaObservable[R](thisJava.publish(fJava))
  }

  /**
   * Returns an Observable that emits `initialValue` followed by the results of invoking a specified
   * selector on items emitted by a `ConnectableObservable` that shares a single subscription to the
   * source Observable.
   * <p>
   * <img width="640" src="https://raw.github.com/wiki/Netflix/RxJava/images/rx-operators/publishConnect.if.png">
   *
   * @param selector a function that can use the multicasted source sequence as many times as needed, without
   *                 causing multiple subscriptions to the source Observable. Subscribers to the source will
   *                 receive all notifications of the source from the time of the subscription forward
   * @param initialValue  the initial value of the underlying `BehaviorSubject`
   * @return an Observable that emits `initialValue` followed by the results of invoking the selector
   *         on a `ConnectableObservable` that shares a single subscription to the underlying Observable
   */
  def publish[U >: T, R](selector: Observable[U] => Observable[R], initialValue: U): Observable[R] = {
    val thisJava = this.asJavaObservable.asInstanceOf[rx.Observable[U]]
    val fJava: Func1[rx.Observable[U], rx.Observable[R]] =
      (jo: rx.Observable[U]) => selector(toScalaObservable[U](jo)).asJavaObservable.asInstanceOf[rx.Observable[R]]
    toScalaObservable[R](thisJava.publish(fJava, initialValue))
  }

  // TODO add Scala-like aggregate function

  /**
   * Returns an Observable that applies a function of your choosing to the first item emitted by a
   * source Observable, then feeds the result of that function along with the second item emitted
   * by an Observable into the same function, and so on until all items have been emitted by the
   * source Observable, emitting the final result from the final call to your function as its sole
   * item.
   *
   * <img width="640" src="https://raw.github.com/wiki/Netflix/RxJava/images/rx-operators/reduceSeed.png">
   *
   * This technique, which is called "reduce" or "aggregate" here, is sometimes called "fold,"
   * "accumulate," "compress," or "inject" in other programming contexts. Groovy, for instance,
   * has an `inject` method that does a similar operation on lists.
   *
   * @param initialValue
   *            the initial (seed) accumulator value
   * @param accumulator
   *            an accumulator function to be invoked on each item emitted by the source
   *            Observable, the result of which will be used in the next accumulator call
   * @return an Observable that emits a single item that is the result of accumulating the output
   *         from the items emitted by the source Observable
   */
  def foldLeft[R](initialValue: R)(accumulator: (R, T) => R): Observable[R] = {
    toScalaObservable[R](asJavaObservable.reduce(initialValue, new Func2[R,T,R]{
      def call(t1: R, t2: T): R = accumulator(t1,t2)
    }))
  }

  /**
   * Returns an Observable that emits the results of sampling the items emitted by the source
   * Observable at a specified time interval.
   *
   * <img width="640" src="https://github.com/Netflix/RxJava/wiki/images/rx-operators/sample.png">
   *
   * @param duration the sampling rate
   * @return an Observable that emits the results of sampling the items emitted by the source
   *         Observable at the specified time interval
   */
  def sample(duration: Duration): Observable[T] = {
    toScalaObservable[T](asJavaObservable.sample(duration.length, duration.unit))
  }

  /**
   * Returns an Observable that emits the results of sampling the items emitted by the source
   * Observable at a specified time interval.
   *
   * <img width="640" src="https://github.com/Netflix/RxJava/wiki/images/rx-operators/sample.png">
   *
   * @param duration the sampling rate
   * @param scheduler
   *            the [[rx.lang.scala.Scheduler]] to use when sampling
   * @return an Observable that emits the results of sampling the items emitted by the source
   *         Observable at the specified time interval
   */
  def sample(duration: Duration, scheduler: Scheduler): Observable[T] = {
    toScalaObservable[T](asJavaObservable.sample(duration.length, duration.unit, scheduler))
  }

  /**
   * Return an Observable that emits the results of sampling the items emitted by the source Observable
   * whenever the specified sampler Observable emits an item or completes.
   *
   * <img width="640" src="https://raw.github.com/wiki/Netflix/RxJava/images/rx-operators/sample.o.png">
   *
   * @param sampler
   *            the Observable to use for sampling the source Observable
   * @return an Observable that emits the results of sampling the items emitted by this Observable whenever
   *         the sampler Observable emits an item or completes
   */
  def sample(sampler: Observable[Any]): Observable[T] = {
    toScalaObservable[T](asJavaObservable.sample(sampler))
  }

  /**
   * Returns an Observable that applies a function of your choosing to the first item emitted by a
   * source Observable, then feeds the result of that function along with the second item emitted
   * by an Observable into the same function, and so on until all items have been emitted by the
   * source Observable, emitting the result of each of these iterations.
   *
   * <img width="640" src="https://raw.github.com/wiki/Netflix/RxJava/images/rx-operators/scanSeed.png">
   *
   * This sort of function is sometimes called an accumulator.
   *
   * Note that when you pass a seed to `scan()` the resulting Observable will emit
   * that seed as its first emitted item.
   *
   * @param initialValue
   *            the initial (seed) accumulator value
   * @param accumulator
   *            an accumulator function to be invoked on each item emitted by the source
   *            Observable, whose result will be emitted to [[rx.lang.scala.Observer]]s via
   *            [[rx.lang.scala.Observer.onNext onNext]] and used in the next accumulator call.
   * @return an Observable that emits the results of each call to the accumulator function
   */
  def scan[R](initialValue: R)(accumulator: (R, T) => R): Observable[R] = {
    toScalaObservable[R](asJavaObservable.scan(initialValue, new Func2[R,T,R]{
      def call(t1: R, t2: T): R = accumulator(t1,t2)
    }))
  }

  /**
   * Returns an Observable that applies a function of your choosing to the
   * first item emitted by a source Observable, then feeds the result of that
   * function along with the second item emitted by an Observable into the
   * same function, and so on until all items have been emitted by the source
   * Observable, emitting the result of each of these iterations.
   * <p>
   * <img width="640" src="https://raw.github.com/wiki/Netflix/RxJava/images/rx-operators/scan.png">
   * <p>
   *
   * @param accumulator
   *            an accumulator function to be invoked on each item emitted by the source
   *            Observable, whose result will be emitted to [[rx.lang.scala.Observer]]s via
   *            [[rx.lang.scala.Observer.onNext onNext]] and used in the next accumulator call.
   * @return
   *         an Observable that emits the results of each call to the
   *         accumulator function
   */
  def scan[U >: T](accumulator: (U, U) => U): Observable[U] = {
    val func: Func2[_ >: U, _ >: U, _ <: U] = accumulator
    val func2 = func.asInstanceOf[Func2[T, T, T]]
    toScalaObservable[U](asJavaObservable.asInstanceOf[rx.Observable[T]].scan(func2))
  }

  /**
   * Returns an Observable that emits a Boolean that indicates whether all of the items emitted by
   * the source Observable satisfy a condition.
   *
   * <img width="640" src="https://github.com/Netflix/RxJava/wiki/images/rx-operators/all.png">
   *
   * @param predicate
   *            a function that evaluates an item and returns a Boolean
   * @return an Observable that emits `true` if all items emitted by the source
   *         Observable satisfy the predicate; otherwise, `false`
   */
  def forall(predicate: T => Boolean): Observable[Boolean] = {
    // type mismatch; found : rx.Observable[java.lang.Boolean] required: rx.Observable[_ <: scala.Boolean]
    // new Observable[Boolean](asJavaNotification.all(predicate))
    // it's more fun in Scala:
    this.map(predicate).foldLeft(true)(_ && _)
  }

  /**
   * Returns an Observable that skips the first `num` items emitted by the source
   * Observable and emits the remainder.
   *
   * <img width="640" src="https://raw.github.com/wiki/Netflix/RxJava/images/rx-operators/skip.png">
   *
   * @param n
   *            the number of items to skip
   * @return an Observable that is identical to the source Observable except that it does not
   *         emit the first `num` items that the source emits
   */
  def drop(n: Int): Observable[T] = {
    toScalaObservable[T](asJavaObservable.skip(n))
  }

  /**
   * Returns an Observable that drops values emitted by the source Observable before a specified time window
   * elapses.
   *
   * <img width="640" src="https://raw.github.com/wiki/Netflix/RxJava/images/rx-operators/skip.t.png">
   *
   * @param time the length of the time window to drop
   * @return an Observable that drops values emitted by the source Observable before the time window defined
   *         by `time` elapses and emits the remainder
   */
  def drop(time: Duration): Observable[T] = {
    toScalaObservable(asJavaObservable.skip(time.length, time.unit))
  }

  /**
   * Returns an Observable that drops values emitted by the source Observable before a specified time window
   * elapses.
   *
   * <img width="640" src="https://raw.github.com/wiki/Netflix/RxJava/images/rx-operators/skip.t.png">
   *
   * @param time the length of the time window to drop
   * @param scheduler the `Scheduler` on which the timed wait happens
   * @return an Observable that drops values emitted by the source Observable before the time window defined
   *         by `time` elapses and emits the remainder
   */
  def drop(time: Duration, scheduler: Scheduler): Observable[T] = {
    toScalaObservable(asJavaObservable.skip(time.length, time.unit, scheduler))
  }

  /**
   * Returns an Observable that bypasses all items from the source Observable as long as the specified
   * condition holds true. Emits all further source items as soon as the condition becomes false.
   *
   * <img width="640" src="https://raw.github.com/wiki/Netflix/RxJava/images/rx-operators/skipWhile.png">
   *
   * @param predicate
   *            A function to test each item emitted from the source Observable for a condition.
   * @return an Observable that emits all items from the source Observable as soon as the condition
   *         becomes false.
   */
  def dropWhile(predicate: T => Boolean): Observable[T] = {
    toScalaObservable(asJavaObservable.skipWhile(predicate))
  }

  /**
   * Returns an Observable that drops a specified number of items from the end of the sequence emitted by the
   * source Observable.
   * <p>
   * <img width="640" src="https://raw.github.com/wiki/Netflix/RxJava/images/rx-operators/skipLast.png">
   * <p>
   * This Observer accumulates a queue long enough to store the first `n` items. As more items are
   * received, items are taken from the front of the queue and emitted by the returned Observable. This causes
   * such items to be delayed.
   *
   * @param n number of items to drop from the end of the source sequence
   * @return an Observable that emits the items emitted by the source Observable except for the dropped ones
   *         at the end
   * @throws IndexOutOfBoundsException if `n` is less than zero
   */
  def dropRight(n: Int): Observable[T] = {
    toScalaObservable(asJavaObservable.skipLast(n))
  }

  /**
   * Returns an Observable that drops items emitted by the source Observable during a specified time window
   * before the source completes.
   * <p>
   * <img width="640" src="https://raw.github.com/wiki/Netflix/RxJava/images/rx-operators/skipLast.t.png">
   *
   * Note: this action will cache the latest items arriving in the specified time window.
   *
   * @param time the length of the time window
   * @return an Observable that drops those items emitted by the source Observable in a time window before the
   *         source completes defined by `time`
   */
  def dropRight(time: Duration): Observable[T] = {
    toScalaObservable(asJavaObservable.skipLast(time.length, time.unit))
  }

  /**
   * Returns an Observable that drops items emitted by the source Observable during a specified time window
   * (defined on a specified scheduler) before the source completes.
   * <p>
   * <img width="640" src="https://raw.github.com/wiki/Netflix/RxJava/images/rx-operators/skipLast.ts.png">
   *
   * Note: this action will cache the latest items arriving in the specified time window.
   *
   * @param time the length of the time window
   * @param scheduler the scheduler used as the time source
   * @return an Observable that drops those items emitted by the source Observable in a time window before the
   *         source completes defined by `time` and `scheduler`
   */
  def dropRight(time: Duration, scheduler: Scheduler): Observable[T] = {
    toScalaObservable(asJavaObservable.skipLast(time.length, time.unit, scheduler))
  }

  /**
   * Returns an Observable that skips items emitted by the source Observable until a second Observable emits an item.
   * <p>
   * <img width="640" src="https://raw.github.com/wiki/Netflix/RxJava/images/rx-operators/skipUntil.png">
   *
   * @param other the second Observable that has to emit an item before the source Observable's elements begin
   *              to be mirrored by the resulting Observable
   * @return an Observable that skips items from the source Observable until the second Observable emits an
   *         item, then emits the remaining items
   * @see <a href="https://github.com/Netflix/RxJava/wiki/Filtering-Observables#wiki-skipuntil">RxJava Wiki: skipUntil()</a>
   * @see <a href="http://msdn.microsoft.com/en-us/library/hh229358.aspx">MSDN: Observable.SkipUntil</a>
   */
  def dropUntil[E](other: Observable[E]): Observable[T] = {
    toScalaObservable[T](asJavaObservable.skipUntil(other))
  }

  /**
   * Returns an Observable that emits only the first `num` items emitted by the source
   * Observable.
   *
   * <img width="640" src="https://raw.github.com/wiki/Netflix/RxJava/images/rx-operators/take.png">
   *
   * This method returns an Observable that will invoke a subscribing [[rx.lang.scala.Observer]]'s 
   * [[rx.lang.scala.Observer.onNext onNext]] function a maximum of `num` times before invoking
   * [[rx.lang.scala.Observer.onCompleted onCompleted]].
   *
   * @param n
   *            the number of items to take
   * @return an Observable that emits only the first `num` items from the source
   *         Observable, or all of the items from the source Observable if that Observable emits
   *         fewer than `num` items
   */
  def take(n: Int): Observable[T] = {
    toScalaObservable[T](asJavaObservable.take(n))
  }

  /**
   * Returns an Observable that emits those items emitted by source Observable before a specified time runs out.
   * <p>
   * <img width="640" src="https://raw.github.com/wiki/Netflix/RxJava/images/rx-operators/take.t.png">
   *
   * @param time the length of the time window
   * @return an Observable that emits those items emitted by the source Observable before the time runs out
   */
  def take(time: Duration): Observable[T] = {
    toScalaObservable[T](asJavaObservable.take(time.length, time.unit))
  }

  /**
   * Returns an Observable that emits those items emitted by source Observable before a specified time (on
   * specified Scheduler) runs out
   * <p>
   * <img width="640" src="https://raw.github.com/wiki/Netflix/RxJava/images/rx-operators/take.ts.png">
   *
   * @param time the length of the time window
   * @param scheduler the Scheduler used for time source
   * @return an Observable that emits those items emitted by the source Observable before the time runs out,
   *         according to the specified Scheduler
   */
  def take(time: Duration, scheduler: Scheduler) {
    toScalaObservable[T](asJavaObservable.take(time.length, time.unit, scheduler.asJavaScheduler))
  }

  /**
   * Returns an Observable that emits items emitted by the source Observable so long as a
   * specified condition is true.
   *
   * <img width="640" src="https://github.com/Netflix/RxJava/wiki/images/rx-operators/takeWhile.png">
   *
   * @param predicate
   *            a function that evaluates an item emitted by the source Observable and returns a
   *            Boolean
   * @return an Observable that emits the items from the source Observable so long as each item
   *         satisfies the condition defined by `predicate`
   */
  def takeWhile(predicate: T => Boolean): Observable[T] = {
    toScalaObservable[T](asJavaObservable.takeWhile(predicate))
  }

  /**
   * Returns an Observable that emits only the last `count` items emitted by the source
   * Observable.
   *
   * <img width="640" src="https://github.com/Netflix/RxJava/wiki/images/rx-operators/last.png">
   *
   * @param count
   *            the number of items to emit from the end of the sequence emitted by the source
   *            Observable
   * @return an Observable that emits only the last `count` items emitted by the source
   *         Observable
   */
  def takeRight(count: Int): Observable[T] = {
    toScalaObservable[T](asJavaObservable.takeLast(count))
  }

  /**
   * Return an Observable that emits the items from the source Observable that were emitted in a specified
   * window of `time` before the Observable completed.
   * <p>
   * <img width="640" src="https://raw.github.com/wiki/Netflix/RxJava/images/rx-operators/takeLast.t.png">
   *
   * @param time the length of the time window
   * @return an Observable that emits the items from the source Observable that were emitted in the window of
   *         time before the Observable completed specified by `time`
   */
  def takeRight(time: Duration): Observable[T] = {
    toScalaObservable[T](asJavaObservable.takeLast(time.length, time.unit))
  }

  /**
   * Return an Observable that emits the items from the source Observable that were emitted in a specified
   * window of `time` before the Observable completed, where the timing information is provided by a specified
   * Scheduler.
   * <p>
   * <img width="640" src="https://raw.github.com/wiki/Netflix/RxJava/images/rx-operators/takeLast.ts.png">
   *
   * @param time the length of the time window
   * @param scheduler the Scheduler that provides the timestamps for the Observed items
   * @return an Observable that emits the items from the source Observable that were emitted in the window of
   *         time before the Observable completed specified by `time`, where the timing information is
   *         provided by `scheduler`
   */
  def takeRight(time: Duration, scheduler: Scheduler): Observable[T] = {
    toScalaObservable[T](asJavaObservable.takeLast(time.length, time.unit, scheduler.asJavaScheduler))
  }

  /**
   * Return an Observable that emits at most a specified number of items from the source Observable that were
   * emitted in a specified window of time before the Observable completed.
   * <p>
   * <img width="640" src="https://raw.github.com/wiki/Netflix/RxJava/images/rx-operators/takeLast.tn.png">
   *
   * @param count the maximum number of items to emit
   * @param time the length of the time window
   * @return an Observable that emits at most `count` items from the source Observable that were emitted
   *         in a specified window of time before the Observable completed
   * @throws IllegalArgumentException if `count` is less than zero
   */
  def takeRight(count: Int, time: Duration): Observable[T] = {
    toScalaObservable[T](asJavaObservable.takeLast(count, time.length, time.unit))
  }

  /**
   * Return an Observable that emits at most a specified number of items from the source Observable that were
   * emitted in a specified window of `time` before the Observable completed, where the timing information is
   * provided by a given Scheduler.
   * <p>
   * <img width="640" src="https://raw.github.com/wiki/Netflix/RxJava/images/rx-operators/takeLast.tns.png">
   *
   * @param count the maximum number of items to emit
   * @param time the length of the time window
   * @param scheduler the Scheduler that provides the timestamps for the observed items
   * @return an Observable that emits at most `count` items from the source Observable that were emitted
   *         in a specified window of time before the Observable completed, where the timing information is
   *         provided by the given `scheduler`
   * @throws IllegalArgumentException if `count` is less than zero
   */
  def takeRight(count: Int, time: Duration, scheduler: Scheduler): Observable[T] = {
    toScalaObservable[T](asJavaObservable.takeLast(count, time.length, time.unit, scheduler.asJavaScheduler))
  }

  /**
   * Returns an Observable that emits the items from the source Observable only until the
   * `other` Observable emits an item.
   *
   * <img width="640" src="https://github.com/Netflix/RxJava/wiki/images/rx-operators/takeUntil.png">
   *
   * @param that
   *            the Observable whose first emitted item will cause `takeUntil` to stop
   *            emitting items from the source Observable
   * @tparam E
   *            the type of items emitted by `other`
   * @return an Observable that emits the items of the source Observable until such time as
   *         `other` emits its first item
   */
  def takeUntil[E](that: Observable[E]): Observable[T] = {
    toScalaObservable[T](asJavaObservable.takeUntil(that.asJavaObservable))
  }

  /**
   * Returns an Observable that emits a single item, a list composed of all the items emitted by
   * the source Observable.
   *
   * <img width="640" src="https://raw.github.com/wiki/Netflix/RxJava/images/rx-operators/toList.png">
   *
   * Normally, an Observable that returns multiple items will do so by invoking its [[rx.lang.scala.Observer]]'s 
   * [[rx.lang.scala.Observer.onNext onNext]] method for each such item. You can change
   * this behavior, instructing the Observable to compose a list of all of these items and then to
   * invoke the Observer's `onNext` function once, passing it the entire list, by
   * calling the Observable's `toList` method prior to calling its `Observable.subscribe` method.
   *
   * Be careful not to use this operator on Observables that emit infinite or very large numbers
   * of items, as you do not have the option to unsubscribe.
   *
   * @return an Observable that emits a single item: a List containing all of the items emitted by
   *         the source Observable.
   */
  def toSeq: Observable[Seq[T]] = {
    Observable.jObsOfListToScObsOfSeq(asJavaObservable.toList)
      : Observable[Seq[T]] // SI-7818
  }

  /**
   * Groups the items emitted by this Observable according to a specified discriminator function.
   *
   * @param f
   *            a function that extracts the key from an item
   * @tparam K
   *            the type of keys returned by the discriminator function.
   * @return an Observable that emits `(key, observable)` pairs, where `observable`
   *         contains all items for which `f` returned `key`.
   */
  def groupBy[K](f: T => K): Observable[(K, Observable[T])] = {
    val o1 = asJavaObservable.groupBy[K](f) : rx.Observable[_ <: rx.observables.GroupedObservable[K, _ <: T]]
    val func = (o: rx.observables.GroupedObservable[K, _ <: T]) => (o.getKey, toScalaObservable[T](o))
    toScalaObservable[(K, Observable[T])](o1.map[(K, Observable[T])](func))
  }

  /**
   * Groups the items emitted by this Observable according to a specified discriminator function and terminates these groups
   * according to a function.
   *
   * @param f
   *            a function that extracts the key from an item
   * @param closings
   *            the function that accepts the key of a given group and an observable representing that group, and returns
   *            an observable that emits a single Closing when the group should be closed.
   * @tparam K 
   *            the type of the keys returned by the discriminator function.
   * @return an Observable that emits `(key, observable)` pairs, where `observable`
   *         contains all items for which `f` returned `key` before `closings` emits a value.
   */
  def groupByUntil[K](f: T => K, closings: (K, Observable[T])=>Observable[Any]): Observable[(K, Observable[T])] = {
    val fclosing: Func1[_ >: rx.observables.GroupedObservable[K, _ <: T], _ <: rx.Observable[_ <: Any]] =
      (jGrObs: rx.observables.GroupedObservable[K, _ <: T]) => closings(jGrObs.getKey, toScalaObservable[T](jGrObs)).asJavaObservable
    val o1 = asJavaObservable.groupByUntil[K, Any](f, fclosing) : rx.Observable[_ <: rx.observables.GroupedObservable[K, _ <: T]]
    val func = (o: rx.observables.GroupedObservable[K, _ <: T]) => (o.getKey, toScalaObservable[T](o))
    toScalaObservable[(K, Observable[T])](o1.map[(K, Observable[T])](func))
  }

  /**
   * Correlates the items emitted by two Observables based on overlapping durations.
   * <p>
   * <img width="640" src="https://raw.github.com/wiki/Netflix/RxJava/images/rx-operators/join_.png">
   *
   * @param other
   *          the second Observable to join items from
   * @param leftDurationSelector
   *          a function to select a duration for each item emitted by the source Observable,
   *          used to determine overlap
   * @param rightDurationSelector
   *         a function to select a duration for each item emitted by the inner Observable,
   *         used to determine overlap
   * @param resultSelector
   *         a function that computes an item to be emitted by the resulting Observable for any
   *         two overlapping items emitted by the two Observables
   * @return
   *         an Observable that emits items correlating to items emitted by the source Observables
   *         that have overlapping durations
   * @see <a href="https://github.com/Netflix/RxJava/wiki/Combining-Observables#join">RxJava Wiki: join()</a>
   * @see <a href="http://msdn.microsoft.com/en-us/library/hh229750.aspx">MSDN: Observable.Join</a>
   */
  def join[S, R] (
      other: Observable[S],
      leftDurationSelector:  T => Observable[Any],
      rightDurationSelector: S => Observable[Any],
      resultSelector: (T,S) => R
  ): Observable[R] = {

    val outer : rx.Observable[_ <: T] = this.asJavaObservable
    val inner : rx.Observable[_ <: S] = other.asJavaObservable
    val left:  Func1[_ >: T, _<: rx.Observable[_ <: Any]] =   (t: T) => leftDurationSelector(t).asJavaObservable
    val right: Func1[_ >: S, _<: rx.Observable[_ <: Any]] =  (s: S) => rightDurationSelector(s).asJavaObservable
    val f: Func2[_>: T, _ >: S, _ <: R] = resultSelector

    toScalaObservable[R](
      outer.asInstanceOf[rx.Observable[T]].join[S, Any, Any, R](
        inner.asInstanceOf[rx.Observable[S]],
        left. asInstanceOf[Func1[T, rx.Observable[Any]]],
        right.asInstanceOf[Func1[S, rx.Observable[Any]]],
        f.asInstanceOf[Func2[T,S,R]])
    )
  }

  /**
   * Given an Observable that emits Observables, creates a single Observable that
   * emits the items emitted by the most recently published of those Observables.
   *
   * <img width="640" src="https://github.com/Netflix/RxJava/wiki/images/rx-operators/switchDo.png">
   *
   * This operation is only available if `this` is of type `Observable[Observable[U]]` for some `U`,
   * otherwise you'll get a compilation error.
   *
   * @return an Observable that emits only the items emitted by the most recently published
   *         Observable
   *
   * @usecase def switch[U]: Observable[U]
   *   @inheritdoc
   */
  def switch[U](implicit evidence: Observable[T] <:< Observable[Observable[U]]): Observable[U] = {
    val o2: Observable[Observable[U]] = this
    val o3: Observable[rx.Observable[_ <: U]] = o2.map(_.asJavaObservable)
    val o4: rx.Observable[_ <: rx.Observable[_ <: U]] = o3.asJavaObservable
    val o5 = rx.Observable.switchOnNext[U](o4)
    toScalaObservable[U](o5)
  }
  // Naming: We follow C# (switch), not Java (switchOnNext), because Java just had to avoid clash with keyword

  /**
   * Flattens two Observables into one Observable, without any transformation.
   *
   * <img width="640" src="https://raw.github.com/wiki/Netflix/RxJava/images/rx-operators/merge.png">
   *
   * You can combine items emitted by two Observables so that they act like a single
   * Observable by using the `merge` method.
   *
   * @param that
   *            an Observable to be merged
   * @return an Observable that emits items from `this` and `that` until 
   *            `this` or `that` emits `onError` or `onComplete`.
   */
  def merge[U >: T](that: Observable[U]): Observable[U] = {
    val thisJava: rx.Observable[_ <: U] = this.asJavaObservable
    val thatJava: rx.Observable[_ <: U] = that.asJavaObservable
    toScalaObservable[U](rx.Observable.merge(thisJava, thatJava))
  }

  /**
   * This behaves like [[rx.lang.scala.Observable.merge]] except that if any of the merged Observables
   * notify of an error via [[rx.lang.scala.Observer.onError onError]], `mergeDelayError` will
   * refrain from propagating that error notification until all of the merged Observables have
   * finished emitting items.
   *
   * <img width="640" src="https://raw.github.com/wiki/Netflix/RxJava/images/rx-operators/mergeDelayError.png">
   *
   * Even if multiple merged Observables send `onError` notifications, `mergeDelayError` will only invoke the `onError` method of its
   * Observers once.
   *
   * This method allows an Observer to receive all successfully emitted items from all of the
   * source Observables without being interrupted by an error notification from one of them.
   *
   * @param that
   *            an Observable to be merged
   * @return an Observable that emits items that are the result of flattening the items emitted by
   *         `this` and `that`
   */
  def mergeDelayError[U >: T](that: Observable[U]): Observable[U] = {
    toScalaObservable[U](rx.Observable.mergeDelayError[U](this.asJavaObservable, that.asJavaObservable))
  }

  /**
   * Flattens the sequence of Observables emitted by `this` into one Observable, without any
   * transformation.
   *
   * <img width="640" src="https://raw.github.com/wiki/Netflix/RxJava/images/rx-operators/merge.png">
   *
   * You can combine the items emitted by multiple Observables so that they act like a single
   * Observable by using this method.
   *
   * This operation is only available if `this` is of type `Observable[Observable[U]]` for some `U`,
   * otherwise you'll get a compilation error.
   *
   * @return an Observable that emits items that are the result of flattening the items emitted
   *         by the Observables emitted by `this`
   *
   * @usecase def flatten[U]: Observable[U]
   *   @inheritdoc
   */
  def flatten[U](implicit evidence: Observable[T] <:< Observable[Observable[U]]): Observable[U] = {
    val o2: Observable[Observable[U]] = this
    val o3: Observable[rx.Observable[_ <: U]] = o2.map(_.asJavaObservable)
    val o4: rx.Observable[_ <: rx.Observable[_ <: U]] = o3.asJavaObservable
    val o5 = rx.Observable.merge[U](o4)
    toScalaObservable[U](o5)
  }

  /**
   * This behaves like `flatten` except that if any of the merged Observables
   * notify of an error via [[rx.lang.scala.Observer.onError onError]], this method will
   * refrain from propagating that error notification until all of the merged Observables have
   * finished emitting items.
   *
   * <img width="640" src="https://raw.github.com/wiki/Netflix/RxJava/images/rx-operators/mergeDelayError.png">
   *
   * Even if multiple merged Observables send `onError` notifications, this method will only invoke the `onError` method of its
   * Observers once.
   *
   * This method allows an Observer to receive all successfully emitted items from all of the
   * source Observables without being interrupted by an error notification from one of them.
   *
   * This operation is only available if `this` is of type `Observable[Observable[U]]` for some `U`,
   * otherwise you'll get a compilation error.
   *
   * @return an Observable that emits items that are the result of flattening the items emitted by
   *         the Observables emitted by the this Observable
   *
   * @usecase def flattenDelayError[U]: Observable[U]
   *   @inheritdoc
   */
  def flattenDelayError[U](implicit evidence: Observable[T] <:< Observable[Observable[U]]): Observable[U] = {
    val o2: Observable[Observable[U]] = this
    val o3: Observable[rx.Observable[_ <: U]] = o2.map(_.asJavaObservable)
    val o4: rx.Observable[_ <: rx.Observable[_ <: U]] = o3.asJavaObservable
    val o5 = rx.Observable.mergeDelayError[U](o4)
    toScalaObservable[U](o5)
  }

  /**
   * Combines two observables, emitting a pair of the latest values of each of
   * the source observables each time an event is received from one of the source observables, where the
   * aggregation is defined by the given function.
   *
   * @param that
   *            The second source observable.
   * @return An Observable that combines the source Observables
   */
  def combineLatest[U](that: Observable[U]): Observable[(T, U)] = {
    val f: Func2[_ >: T, _ >: U, _ <: (T, U)] = (t: T, u: U) => (t, u)
    toScalaObservable[(T, U)](rx.Observable.combineLatest[T, U, (T, U)](this.asJavaObservable, that.asJavaObservable, f))
  }


  /**
   * Combines two observables, emitting some type `R` specified in the function f,
   * each time an event is received from one of the source observables, where the aggregation
   * is defined by the given function.
   *
   *@param that
   *           The second source observable.
   *@param f
               The function that is used combine the emissions of the two observables.
   *@return An Observable that combines the source Observables according to the function f.
   */
  def combineLatest[U,R](that: Observable[U], f: (T, U) => R): Observable[R] = {
    toScalaObservable[R](rx.Observable.combineLatest[T, U, R](this.asJavaObservable, that.asJavaObservable, f))
  }

  /**
   * Debounces by dropping all values that are followed by newer values before the timeout value expires. The timer resets on each `onNext` call.
   *
   * NOTE: If events keep firing faster than the timeout then no data will be emitted.
   *
   * <img width="640" src="https://github.com/Netflix/RxJava/wiki/images/rx-operators/throttleWithTimeout.png">
   *
   * $debounceVsThrottle
   *
   * @param timeout
   *            The time each value has to be 'the most recent' of the [[rx.lang.scala.Observable]] to ensure that it's not dropped.
   *
   * @return An [[rx.lang.scala.Observable]] which filters out values which are too quickly followed up with newer values.
   * @see `Observable.debounce`
   */
  def throttleWithTimeout(timeout: Duration): Observable[T] = {
    toScalaObservable[T](asJavaObservable.throttleWithTimeout(timeout.length, timeout.unit))
  }

  /**
   * Debounces by dropping all values that are followed by newer values before the timeout value expires. The timer resets on each `onNext` call.
   *
   * NOTE: If events keep firing faster than the timeout then no data will be emitted.
   *
   * <img width="640" src="https://github.com/Netflix/RxJava/wiki/images/rx-operators/debounce.png">
   *
   * $debounceVsThrottle
   *
   * @param timeout
   *            The time each value has to be 'the most recent' of the [[rx.lang.scala.Observable]] to ensure that it's not dropped.
   *
   * @return An [[rx.lang.scala.Observable]] which filters out values which are too quickly followed up with newer values.
   * @see `Observable.throttleWithTimeout`
   */
  def debounce(timeout: Duration): Observable[T] = {
    toScalaObservable[T](asJavaObservable.debounce(timeout.length, timeout.unit))
  }

  /**
   * Debounces by dropping all values that are followed by newer values before the timeout value expires. The timer resets on each `onNext` call.
   *
   * NOTE: If events keep firing faster than the timeout then no data will be emitted.
   *
   * <img width="640" src="https://github.com/Netflix/RxJava/wiki/images/rx-operators/debounce.png">
   *
   * $debounceVsThrottle
   *
   * @param timeout
   *            The time each value has to be 'the most recent' of the [[rx.lang.scala.Observable]] to ensure that it's not dropped.
   * @param scheduler
   *            The [[rx.lang.scala.Scheduler]] to use internally to manage the timers which handle timeout for each event.
   * @return Observable which performs the throttle operation.
   * @see `Observable.throttleWithTimeout`
   */
  def debounce(timeout: Duration, scheduler: Scheduler): Observable[T] = {
    toScalaObservable[T](asJavaObservable.debounce(timeout.length, timeout.unit, scheduler))
  }

  /**
   * Debounces by dropping all values that are followed by newer values before the timeout value expires. The timer resets on each `onNext` call.
   *
   * NOTE: If events keep firing faster than the timeout then no data will be emitted.
   *
   * <img width="640" src="https://github.com/Netflix/RxJava/wiki/images/rx-operators/throttleWithTimeout.png">
   *
   * @param timeout
   *            The time each value has to be 'the most recent' of the [[rx.lang.scala.Observable]] to ensure that it's not dropped.
   * @param scheduler
   *            The [[rx.lang.scala.Scheduler]] to use internally to manage the timers which handle timeout for each event.
   * @return Observable which performs the throttle operation.
   * @see `Observable.debounce`
   */
  def throttleWithTimeout(timeout: Duration, scheduler: Scheduler): Observable[T] = {
    toScalaObservable[T](asJavaObservable.throttleWithTimeout(timeout.length, timeout.unit, scheduler))
  }

  /**
   * Throttles by skipping value until `skipDuration` passes and then emits the next received value.
   *
   * This differs from `Observable.throttleLast` in that this only tracks passage of time whereas `Observable.throttleLast` ticks at scheduled intervals.
   *
   * <img width="640" src="https://github.com/Netflix/RxJava/wiki/images/rx-operators/throttleFirst.png">
   *
   * @param skipDuration
   *            Time to wait before sending another value after emitting last value.
   * @param scheduler
   *            The [[rx.lang.scala.Scheduler]] to use internally to manage the timers which handle timeout for each event.
   * @return Observable which performs the throttle operation.
   */
  def throttleFirst(skipDuration: Duration, scheduler: Scheduler): Observable[T] = {
    toScalaObservable[T](asJavaObservable.throttleFirst(skipDuration.length, skipDuration.unit, scheduler))
  }

  /**
   * Throttles by skipping value until `skipDuration` passes and then emits the next received value.
   *
   * This differs from `Observable.throttleLast` in that this only tracks passage of time whereas `Observable.throttleLast` ticks at scheduled intervals.
   *
   * <img width="640" src="https://github.com/Netflix/RxJava/wiki/images/rx-operators/throttleFirst.png">
   *
   * @param skipDuration
   *            Time to wait before sending another value after emitting last value.
   * @return Observable which performs the throttle operation.
   */
  def throttleFirst(skipDuration: Duration): Observable[T] = {
    toScalaObservable[T](asJavaObservable.throttleFirst(skipDuration.length, skipDuration.unit))
  }

  /**
   * Throttles by returning the last value of each interval defined by 'intervalDuration'.
   *
   * This differs from `Observable.throttleFirst` in that this ticks along at a scheduled interval whereas `Observable.throttleFirst` does not tick, it just tracks passage of time.
   *
   * <img width="640" src="https://github.com/Netflix/RxJava/wiki/images/rx-operators/throttleLast.png">
   *
   * @param intervalDuration
   *            Duration of windows within with the last value will be chosen.
   * @return Observable which performs the throttle operation.
   */
  def throttleLast(intervalDuration: Duration): Observable[T] = {
    toScalaObservable[T](asJavaObservable.throttleLast(intervalDuration.length, intervalDuration.unit))
  }

  /**
   * Throttles by returning the last value of each interval defined by 'intervalDuration'.
   *
   * This differs from `Observable.throttleFirst` in that this ticks along at a scheduled interval whereas `Observable.throttleFirst` does not tick, it just tracks passage of time.
   *
   * <img width="640" src="https://github.com/Netflix/RxJava/wiki/images/rx-operators/throttleLast.png">
   *
   * @param intervalDuration
   *            Duration of windows within with the last value will be chosen.
   * @return Observable which performs the throttle operation.
   */
  def throttleLast(intervalDuration: Duration, scheduler: Scheduler): Observable[T] = {
    toScalaObservable[T](asJavaObservable.throttleLast(intervalDuration.length, intervalDuration.unit, scheduler))
  }

  /**
   * Applies a timeout policy for each item emitted by the Observable, using
   * the specified scheduler to run timeout timers. If the next item isn't
   * observed within the specified timeout duration starting from its
   * predecessor, observers are notified of a `TimeoutException`.
   * <p>
   * <img width="640" src="https://raw.github.com/wiki/Netflix/RxJava/images/rx-operators/timeout.1.png">
   *
   * @param timeout maximum duration between items before a timeout occurs
   * @return the source Observable modified to notify observers of a
   *         `TimeoutException` in case of a timeout
   */
  def timeout(timeout: Duration): Observable[T] = {
    toScalaObservable[T](asJavaObservable.timeout(timeout.length, timeout.unit))
  }

  /**
   * Applies a timeout policy for each item emitted by the Observable, using
   * the specified scheduler to run timeout timers. If the next item isn't
   * observed within the specified timeout duration starting from its
   * predecessor, a specified fallback Observable produces future items and
   * notifications from that point on.
   * <p>
   * <img width="640" src="https://raw.github.com/wiki/Netflix/RxJava/images/rx-operators/timeout.2.png">
   *
   * @param timeout maximum duration between items before a timeout occurs
   * @param other fallback Observable to use in case of a timeout
   * @return the source Observable modified to switch to the fallback
   *         Observable in case of a timeout
   */
  def timeout[U >: T](timeout: Duration, other: Observable[U]): Observable[U] = {
    val otherJava: rx.Observable[_ <: U] = other.asJavaObservable
    val thisJava = this.asJavaObservable.asInstanceOf[rx.Observable[U]]
    toScalaObservable[U](thisJava.timeout(timeout.length, timeout.unit, otherJava))
  }

  /**
   * Applies a timeout policy for each item emitted by the Observable, using
   * the specified scheduler to run timeout timers. If the next item isn't
   * observed within the specified timeout duration starting from its
   * predecessor, the observer is notified of a `TimeoutException`.
   * <p>
   * <img width="640" src="https://raw.github.com/wiki/Netflix/RxJava/images/rx-operators/timeout.1s.png">
   *
   * @param timeout maximum duration between items before a timeout occurs
   * @param scheduler Scheduler to run the timeout timers on
   * @return the source Observable modified to notify observers of a
   *         `TimeoutException` in case of a timeout
   */
  def timeout(timeout: Duration, scheduler: Scheduler): Observable[T] = {
    toScalaObservable[T](asJavaObservable.timeout(timeout.length, timeout.unit, scheduler.asJavaScheduler))
  }

  /**
   * Applies a timeout policy for each item emitted by the Observable, using
   * the specified scheduler to run timeout timers. If the next item isn't
   * observed within the specified timeout duration starting from its
   * predecessor, a specified fallback Observable sequence produces future
   * items and notifications from that point on.
   * <p>
   * <img width="640" src="https://raw.github.com/wiki/Netflix/RxJava/images/rx-operators/timeout.2s.png">
   *
   * @param timeout maximum duration between items before a timeout occurs
   * @param other Observable to use as the fallback in case of a timeout
   * @param scheduler Scheduler to run the timeout timers on
   * @return the source Observable modified so that it will switch to the
   *         fallback Observable in case of a timeout
   */
  def timeout[U >: T](timeout: Duration, other: Observable[U], scheduler: Scheduler): Observable[U] = {
    val otherJava: rx.Observable[_ <: U] = other.asJavaObservable
    val thisJava = this.asJavaObservable.asInstanceOf[rx.Observable[U]]
    toScalaObservable[U](thisJava.timeout(timeout.length, timeout.unit, otherJava, scheduler.asJavaScheduler))
  }

  /**
   * Returns an Observable that mirrors the source Observable, but emits a TimeoutException if an item emitted by
   * the source Observable doesn't arrive within a window of time after the emission of the
   * previous item, where that period of time is measured by an Observable that is a function
   * of the previous item.
   * <p>
   * <img width="640" src="https://raw.github.com/wiki/Netflix/RxJava/images/rx-operators/timeout3.png">
   * </p>
   * Note: The arrival of the first source item is never timed out.
   *
   * @param timeoutSelector
   *            a function that returns an observable for each item emitted by the source
   *            Observable and that determines the timeout window for the subsequent item
   * @return an Observable that mirrors the source Observable, but emits a TimeoutException if a item emitted by
   *         the source Observable takes longer to arrive than the time window defined by the
   *         selector for the previously emitted item
   */
  def timeout[V](timeoutSelector: T => Observable[V]): Observable[T] = {
    toScalaObservable[T](asJavaObservable.timeout({ t: T => timeoutSelector(t).asJavaObservable.asInstanceOf[rx.Observable[V]] }))
  }

  /**
   * Returns an Observable that mirrors the source Observable, but that switches to a fallback
   * Observable if an item emitted by the source Observable doesn't arrive within a window of time
   * after the emission of the previous item, where that period of time is measured by an
   * Observable that is a function of the previous item.
   * <p>
   * <img width="640" src="https://raw.github.com/wiki/Netflix/RxJava/images/rx-operators/timeout4.png">
   * </p>
   * Note: The arrival of the first source item is never timed out.
   * 
   * @param timeoutSelector
   *            a function that returns an observable for each item emitted by the source
   *            Observable and that determines the timeout window for the subsequent item
   * @param other
   *            the fallback Observable to switch to if the source Observable times out
   * @return an Observable that mirrors the source Observable, but switches to mirroring a
   *         fallback Observable if a item emitted by the source Observable takes longer to arrive
   *         than the time window defined by the selector for the previously emitted item
   */
  def timeout[V, O >: T](timeoutSelector: T => Observable[V], other: Observable[O]): Observable[O] = {
    val thisJava = this.asJavaObservable.asInstanceOf[rx.Observable[O]]
    toScalaObservable[O](thisJava.timeout(
      { t: O => timeoutSelector(t.asInstanceOf[T]).asJavaObservable.asInstanceOf[rx.Observable[V]] },
      other.asJavaObservable))
  }

  /**
   * Returns an Observable that mirrors the source Observable, but emits a TimeoutException
   * if either the first item emitted by the source Observable or any subsequent item
   * don't arrive within time windows defined by other Observables.
   * <p>
   * <img width="640" src="https://raw.github.com/wiki/Netflix/RxJava/images/rx-operators/timeout5.png">
   * </p>
   * @param firstTimeoutSelector
   *            a function that returns an Observable that determines the timeout window for the
   *            first source item
   * @param timeoutSelector
   *            a function that returns an Observable for each item emitted by the source
   *            Observable and that determines the timeout window in which the subsequent source
   *            item must arrive in order to continue the sequence
   * @return an Observable that mirrors the source Observable, but emits a TimeoutException if either the first item or any subsequent item doesn't
   *         arrive within the time windows specified by the timeout selectors
   */
  def timeout[U, V](firstTimeoutSelector: () => Observable[U], timeoutSelector: T => Observable[V]): Observable[T] = {
    toScalaObservable[T](asJavaObservable.timeout(
      { firstTimeoutSelector().asJavaObservable.asInstanceOf[rx.Observable[U]] },
      { t: T => timeoutSelector(t).asJavaObservable.asInstanceOf[rx.Observable[V]] }))
  }

  /**
   * Returns an Observable that mirrors the source Observable, but switches to a fallback
   * Observable if either the first item emitted by the source Observable or any subsequent item
   * don't arrive within time windows defined by other Observables.
   * <p>
   * <img width="640" src="https://raw.github.com/wiki/Netflix/RxJava/images/rx-operators/timeout6.png">
   * </p>
   * @param firstTimeoutSelector
   *            a function that returns an Observable which determines the timeout window for the
   *            first source item
   * @param timeoutSelector
   *            a function that returns an Observable for each item emitted by the source
   *            Observable and that determines the timeout window in which the subsequent source
   *            item must arrive in order to continue the sequence
   * @param other
   *            the fallback Observable to switch to if the source Observable times out
   * @return an Observable that mirrors the source Observable, but switches to the {@code other} Observable if either the first item emitted by the source Observable or any
   *         subsequent item don't arrive within time windows defined by the timeout selectors
   */
  def timeout[U, V, O >: T](firstTimeoutSelector: () => Observable[U], timeoutSelector: T => Observable[V], other: Observable[O]): Observable[O] = {
    val thisJava = this.asJavaObservable.asInstanceOf[rx.Observable[O]]
    toScalaObservable[O](thisJava.timeout(
      { firstTimeoutSelector().asJavaObservable.asInstanceOf[rx.Observable[U]] },
      { t: O => timeoutSelector(t.asInstanceOf[T]).asJavaObservable.asInstanceOf[rx.Observable[V]] },
      other.asJavaObservable))
  }

  /**
   * Returns an Observable that sums up the elements of this Observable.
   *
   * This operation is only available if the elements of this Observable are numbers, otherwise
   * you will get a compilation error.
   *
   * @return an Observable emitting the sum of all the elements of the source Observable
   *         as its single item.
   *
   * @usecase def sum: Observable[T]
   *   @inheritdoc
   */
  def sum[U >: T](implicit num: Numeric[U]): Observable[U] = {
    foldLeft(num.zero)(num.plus)
  }

  /**
   * Returns an Observable that multiplies up the elements of this Observable.
   *
   * This operation is only available if the elements of this Observable are numbers, otherwise
   * you will get a compilation error.
   *
   * @return an Observable emitting the product of all the elements of the source Observable
   *         as its single item.
   *
   * @usecase def product: Observable[T]
   *   @inheritdoc
   */
  def product[U >: T](implicit num: Numeric[U]): Observable[U] = {
    foldLeft(num.one)(num.times)
  }

  /**
   * Returns an Observable that emits only the very first item emitted by the source Observable, or
   * a default value if the source Observable is empty.
   *
   * <img width="640" src="https://raw.github.com/wiki/Netflix/RxJava/images/rx-operators/firstOrDefault.png">
   *
   * @param default
   *            The default value to emit if the source Observable doesn't emit anything.
   *            This is a by-name parameter, so it is only evaluated if the source Observable doesn't emit anything.
   * @return an Observable that emits only the very first item from the source, or a default value
   *         if the source Observable completes without emitting any item.
   */
  def firstOrElse[U >: T](default: => U): Observable[U] = {
    this.take(1).foldLeft[Option[U]](None)((v: Option[U], e: U) => Some(e)).map({
      case Some(element) => element
      case None => default
    })
  }

  /**
   * Returns an Observable that emits only the very first item emitted by the source Observable, or
   * a default value if the source Observable is empty.
   *
   * <img width="640" src="https://raw.github.com/wiki/Netflix/RxJava/images/rx-operators/firstOrDefault.png">
   *
   * @param default
   *            The default value to emit if the source Observable doesn't emit anything.
   *            This is a by-name parameter, so it is only evaluated if the source Observable doesn't emit anything.
   * @return an Observable that emits only the very first item from the source, or a default value
   *         if the source Observable completes without emitting any item.
   */
  def headOrElse[U >: T](default: => U): Observable[U] = firstOrElse(default)

  /**
   * Returns an Observable that emits only the very first item emitted by the source Observable, or raises an
   * `NoSuchElementException` if the source Observable is empty.
   * <p>
   * <img width="640" src="https://raw.github.com/wiki/Netflix/RxJava/images/rx-operators/first.png">
   * 
   * @return an Observable that emits only the very first item emitted by the source Observable, or raises an
   *         `NoSuchElementException` if the source Observable is empty
   * @see <a href="https://github.com/Netflix/RxJava/wiki/Filtering-Observables#wiki-first">RxJava Wiki: first()</a>
   * @see "MSDN: Observable.firstAsync()"
   */
  def first: Observable[T] = {
    toScalaObservable[T](asJavaObservable.first)
  }

  /**
   * Returns an Observable that emits only the very first item emitted by the source Observable, or raises an
   * `NoSuchElementException` if the source Observable is empty.
   * <p>
   * <img width="640" src="https://raw.github.com/wiki/Netflix/RxJava/images/rx-operators/first.png">
   * 
   * @return an Observable that emits only the very first item emitted by the source Observable, or raises an
   *         `NoSuchElementException` if the source Observable is empty
   * @see <a href="https://github.com/Netflix/RxJava/wiki/Filtering-Observables#wiki-first">RxJava Wiki: first()</a>
   * @see "MSDN: Observable.firstAsync()"
   * @see [[Observable.first]]
   */
  def head: Observable[T] = first

  /**
   * Returns an Observable that emits all items except the first one, or raises an `UnsupportedOperationException`
   * if the source Observable is empty.
   *
   * @return an Observable that emits all items except the first one, or raises an `UnsupportedOperationException`
   *         if the source Observable is empty.
   */
  def tail: Observable[T] = {
    lift {
      (subscriber: Subscriber[T]) => {
        var isFirst = true
        Subscriber[T](
          subscriber,
          (v: T) => if(isFirst) isFirst = false else subscriber.onNext(v),
          e => subscriber.onError(e),
          () => if(isFirst) subscriber.onError(new UnsupportedOperationException("tail of empty Observable")) else subscriber.onCompleted
        )
      }
    }
  }

  /**
   * Returns an Observable that emits the last item emitted by the source Observable or notifies observers of
   * an `NoSuchElementException` if the source Observable is empty.
   * 
   * <img width="640" src="https://raw.github.com/wiki/Netflix/RxJava/images/rx-operators/last.png">
   * 
   * @return an Observable that emits the last item from the source Observable or notifies observers of an
   *         error
   * @see <a href="https://github.com/Netflix/RxJava/wiki/Filtering-Observable-Operators#wiki-last">RxJava Wiki: last()</a>
   * @see "MSDN: Observable.lastAsync()"
   */
  def last: Observable[T] = {
    toScalaObservable[T](asJavaObservable.last)
  }

  /**
   * If the source Observable completes after emitting a single item, return an Observable that emits that
   * item. If the source Observable emits more than one item or no items, throw an `NoSuchElementException`.
   * 
   * <img width="640" src="https://raw.github.com/wiki/Netflix/RxJava/images/rx-operators/single.png">
   * 
   * @return an Observable that emits the single item emitted by the source Observable
   * @throws NoSuchElementException
   *             if the source emits more than one item or no items
   * @see <a href="https://github.com/Netflix/RxJava/wiki/Observable-Utility-Operators#wiki-single-and-singleordefault">RxJava Wiki: single()</a>
   * @see "MSDN: Observable.singleAsync()"
   */
  def single: Observable[T] = {
    toScalaObservable[T](asJavaObservable.single)
  }

  /**
   * Returns an Observable that forwards all sequentially distinct items emitted from the source Observable.
   *
   * <img width="640" src="https://raw.github.com/wiki/Netflix/RxJava/images/rx-operators/distinctUntilChanged.png">
   *
   * @return an Observable of sequentially distinct items
   */
  def distinctUntilChanged: Observable[T] = {
    toScalaObservable[T](asJavaObservable.distinctUntilChanged)
  }

  /**
   * Returns an Observable that forwards all items emitted from the source Observable that are sequentially
   * distinct according to a key selector function.
   *
   * <img width="640" src="https://raw.github.com/wiki/Netflix/RxJava/images/rx-operators/distinctUntilChanged.key.png">
   *
   * @param keySelector
   *            a function that projects an emitted item to a key value which is used for deciding whether an item is sequentially
   *            distinct from another one or not
   * @return an Observable of sequentially distinct items
   */
  def distinctUntilChanged[U](keySelector: T => U): Observable[T] = {
    toScalaObservable[T](asJavaObservable.distinctUntilChanged[U](keySelector))
  }

  /**
   * Returns an Observable that forwards all distinct items emitted from the source Observable.
   *
   * <img width="640" src="https://raw.github.com/wiki/Netflix/RxJava/images/rx-operators/distinct.png">
   *
   * @return an Observable of distinct items
   */
  def distinct: Observable[T] = {
    toScalaObservable[T](asJavaObservable.distinct())
  }

  /**
   * Returns an Observable that forwards all items emitted from the source Observable that are distinct according
   * to a key selector function.
   *
   * <img width="640" src="https://raw.github.com/wiki/Netflix/RxJava/images/rx-operators/distinct.key.png">
   *
   * @param keySelector
   *            a function that projects an emitted item to a key value which is used for deciding whether an item is
   *            distinct from another one or not
   * @return an Observable of distinct items
   */
  def distinct[U](keySelector: T => U): Observable[T] = {
    toScalaObservable[T](asJavaObservable.distinct[U](keySelector))
  }

  /**
   * Returns an Observable that counts the total number of elements in the source Observable.
   *
   * <img width="640" src="https://raw.github.com/wiki/Netflix/RxJava/images/rx-operators/count.png">
   *
   * @return an Observable emitting the number of counted elements of the source Observable
   *         as its single item.
   */
  def length: Observable[Int] = {
    toScalaObservable[Integer](asJavaObservable.count()).map(_.intValue())
  }

  /**
   * Returns an Observable that counts the total number of elements in the source Observable.
   *
   * <img width="640" src="https://raw.github.com/wiki/Netflix/RxJava/images/rx-operators/count.png">
   *
   * @return an Observable emitting the number of counted elements of the source Observable
   *         as its single item.
   */
  def size: Observable[Int] = length

  /**
   * Retry subscription to origin Observable upto given retry count.
   *
   * <img width="640" src="https://raw.github.com/wiki/Netflix/RxJava/images/rx-operators/retry.png">
   *
   * If [[rx.lang.scala.Observer.onError]] is invoked the source Observable will be re-subscribed to as many times as defined by retryCount.
   *
   * Any [[rx.lang.scala.Observer.onNext]] calls received on each attempt will be emitted and concatenated together.
   *
   * For example, if an Observable fails on first time but emits [1, 2] then succeeds the second time and
   * emits [1, 2, 3, 4, 5] then the complete output would be [1, 2, 1, 2, 3, 4, 5, onCompleted].
   *
   * @param retryCount
   *            Number of retry attempts before failing.
   * @return Observable with retry logic.
   */
  def retry(retryCount: Int): Observable[T] = {
    toScalaObservable[T](asJavaObservable.retry(retryCount))
  }

  /**
   * Retry subscription to origin Observable whenever onError is called (infinite retry count).
   *
   * <img width="640" src="https://raw.github.com/wiki/Netflix/RxJava/images/rx-operators/retry.png">
   *
   * If [[rx.lang.scala.Observer.onError]] is invoked the source Observable will be re-subscribed to.
   *
   * Any [[rx.lang.scala.Observer.onNext]] calls received on each attempt will be emitted and concatenated together.
   *
   * For example, if an Observable fails on first time but emits [1, 2] then succeeds the second time and
   * emits [1, 2, 3, 4, 5] then the complete output would be [1, 2, 1, 2, 3, 4, 5, onCompleted].
   * @return Observable with retry logic.
   */
  def retry: Observable[T] = {
    toScalaObservable[T](asJavaObservable.retry())
  }

  /**
   * Returns an Observable that repeats the sequence of items emitted by the source Observable indefinitely.
   * <p>
   * <img width="640" src="https://raw.github.com/wiki/Netflix/RxJava/images/rx-operators/repeat.o.png">
   *
   * @return an Observable that emits the items emitted by the source Observable repeatedly and in sequence
   * @see <a href="https://github.com/Netflix/RxJava/wiki/Creating-Observables#wiki-repeat">RxJava Wiki: repeat()</a>
   * @see <a href="http://msdn.microsoft.com/en-us/library/hh229428.aspx">MSDN: Observable.Repeat</a>
   */
  def repeat: Observable[T] = {
    toScalaObservable[T](asJavaObservable.repeat())
  }

  /**
   * Returns an Observable that repeats the sequence of items emitted by the source Observable indefinitely,
   * on a particular Scheduler.
   * <p>
   * <img width="640" src="https://raw.github.com/wiki/Netflix/RxJava/images/rx-operators/repeat.os.png">
   *
   * @param scheduler the Scheduler to emit the items on
   * @return an Observable that emits the items emitted by the source Observable repeatedly and in sequence
   * @see <a href="https://github.com/Netflix/RxJava/wiki/Creating-Observables#wiki-repeat">RxJava Wiki: repeat()</a>
   * @see <a href="http://msdn.microsoft.com/en-us/library/hh229428.aspx">MSDN: Observable.Repeat</a>
   */
  def repeat(scheduler: Scheduler): Observable[T] = {
    toScalaObservable[T](asJavaObservable.repeat(scheduler))
  }

  /**
   * Returns an Observable that repeats the sequence of items emitted by the source Observable at most `count` times.
   * <p>
   * <img width="640" src="https://raw.github.com/wiki/Netflix/RxJava/images/rx-operators/repeat.on.png">
   *
   * @param count the number of times the source Observable items are repeated,
   *              a count of 0 will yield an empty sequence
   * @return an Observable that repeats the sequence of items emitted by the source Observable at most `count` times
   * @throws IllegalArgumentException if `count` is less than zero
   * @see <a href="https://github.com/Netflix/RxJava/wiki/Creating-Observables#wiki-repeat">RxJava Wiki: repeat()</a>
   * @see <a href="http://msdn.microsoft.com/en-us/library/hh229428.aspx">MSDN: Observable.Repeat</a>
   */
  def repeat(count: Long): Observable[T] = {
    toScalaObservable[T](asJavaObservable.repeat(count))
  }

  /**
   * Returns an Observable that repeats the sequence of items emitted by the source Observable
   * at most `count` times, on a particular Scheduler.
   * <p>
   * <img width="640" src="https://raw.github.com/wiki/Netflix/RxJava/images/rx-operators/repeat.ons.png">
   *
   * @param count the number of times the source Observable items are repeated,
   *              a count of 0 will yield an empty sequence
   * @param scheduler the `Scheduler` to emit the items on
   * @return an Observable that repeats the sequence of items emitted by the source Observable at most `count` times
   *         on a particular Scheduler
   * @see <a href="https://github.com/Netflix/RxJava/wiki/Creating-Observables#wiki-repeat">RxJava Wiki: repeat()</a>
   * @see <a href="http://msdn.microsoft.com/en-us/library/hh229428.aspx">MSDN: Observable.Repeat</a>
   */
  def repeat(count: Long, scheduler: Scheduler): Observable[T] = {
    toScalaObservable[T](asJavaObservable.repeat(count, scheduler))
  }

  /**
   * Converts an Observable into a [[rx.lang.scala.observables.BlockingObservable]] (an Observable with blocking
   * operators).
   *
   * @see <a href="https://github.com/Netflix/RxJava/wiki/Blocking-Observable-Operators">Blocking Observable Operators</a>
   */
  def toBlockingObservable: BlockingObservable[T] = {
    new BlockingObservable[T](asJavaObservable.toBlockingObservable)
  }

  /**
   * Perform work in parallel by sharding an `Observable[T]` on a 
   * [[rx.lang.scala.schedulers.ComputationScheduler]] and return an `Observable[R]` with the output.
   *
   * @param f
   *            a function that applies Observable operators to `Observable[T]` in parallel and returns an `Observable[R]`
   * @return an Observable with the output of the function executed on a [[rx.lang.scala.Scheduler]]
   */
  def parallel[R](f: Observable[T] => Observable[R]): Observable[R] = {
    val fJava: Func1[rx.Observable[T], rx.Observable[R]] =
      (jo: rx.Observable[T]) => f(toScalaObservable[T](jo)).asJavaObservable.asInstanceOf[rx.Observable[R]]
    toScalaObservable(asJavaObservable.asInstanceOf[rx.Observable[T]].parallel[R](fJava))
  }

  /**
   * Perform work in parallel by sharding an `Observable[T]` on a [[rx.lang.scala.Scheduler]] and return an `Observable[R]` with the output.
   *
   * @param f
   *            a function that applies Observable operators to `Observable[T]` in parallel and returns an `Observable[R]`
   * @param scheduler
   *            a [[rx.lang.scala.Scheduler]] to perform the work on.
   * @return an Observable with the output of the function executed on a [[rx.lang.scala.Scheduler]]
   */
  def parallel[R](f: Observable[T] => Observable[R], scheduler: Scheduler): Observable[R] = {
    val fJava: Func1[rx.Observable[T], rx.Observable[R]] =
      (jo: rx.Observable[T]) => f(toScalaObservable[T](jo)).asJavaObservable.asInstanceOf[rx.Observable[R]]
    toScalaObservable(asJavaObservable.asInstanceOf[rx.Observable[T]].parallel[R](fJava, scheduler))
  }

  /** Tests whether a predicate holds for some of the elements of this `Observable`.
    *
    *  @param   p     the predicate used to test elements.
    *  @return        an Observable emitting one single Boolean, which is `true` if the given predicate `p`
    *                 holds for some of the elements of this Observable, and `false` otherwise.
    */
  def exists(p: T => Boolean): Observable[Boolean] = {
    toScalaObservable[java.lang.Boolean](asJavaObservable.exists(p)).map(_.booleanValue())
  }

  /** Tests whether this `Observable` emits no elements.
    *
    *  @return        an Observable emitting one single Boolean, which is `true` if this `Observable`
    *                 emits no elements, and `false` otherwise.
    */
  def isEmpty: Observable[Boolean] = {
    toScalaObservable[java.lang.Boolean](asJavaObservable.isEmpty()).map(_.booleanValue())
  }

  def withFilter(p: T => Boolean): WithFilter[T] = {
    new WithFilter[T](p, asJavaObservable)
  }

  /**
   * Returns an Observable that applies the given function to each item emitted by an
   * Observable.
   *
   * @param observer the observer
   *
   * @return an Observable with the side-effecting behavior applied.
   */
  def doOnEach(observer: Observer[T]): Observable[T] = {
    toScalaObservable[T](asJavaObservable.doOnEach(observer.asJavaObserver))
  }

  /**
   * Invokes an action when the source Observable calls <code>onNext</code>.
   *
   * @param onNext the action to invoke when the source Observable calls <code>onNext</code>
   * @return the source Observable with the side-effecting behavior applied
   */
  def doOnNext(onNext: T => Unit): Observable[T] = {
    toScalaObservable[T](asJavaObservable.doOnNext(onNext))
  }

  /**
   * Invokes an action if the source Observable calls `onError`.
   *
   * @param onError the action to invoke if the source Observable calls
   *                `onError`
   * @return the source Observable with the side-effecting behavior applied
   */
  def doOnError(onError: Throwable => Unit): Observable[T] = {
    toScalaObservable[T](asJavaObservable.doOnError(onError))
  }

  /**
   * Invokes an action when the source Observable calls `onCompleted`.
   *
   * @param onCompleted the action to invoke when the source Observable calls
   *                    `onCompleted`
   * @return the source Observable with the side-effecting behavior applied
   */
  def doOnCompleted(onCompleted: () => Unit): Observable[T] = {
    toScalaObservable[T](asJavaObservable.doOnCompleted(onCompleted))
  }

  /**
   * Returns an Observable that applies the given function to each item emitted by an
   * Observable.
   *
   * @param onNext this function will be called whenever the Observable emits an item
   *
   * @return an Observable with the side-effecting behavior applied.
   */
  def doOnEach(onNext: T => Unit): Observable[T] = {
    toScalaObservable[T](asJavaObservable.doOnNext(onNext))
  }

  /**
   * Returns an Observable that applies the given function to each item emitted by an
   * Observable.
   *
   * @param onNext this function will be called whenever the Observable emits an item
   * @param onError this function will be called if an error occurs
   *
   * @return an Observable with the side-effecting behavior applied.
   */
  def doOnEach(onNext: T => Unit, onError: Throwable => Unit): Observable[T] = {
    toScalaObservable[T](asJavaObservable.doOnEach(Observer(onNext, onError, ()=>{})))
  }

  /**
   * Returns an Observable that applies the given function to each item emitted by an
   * Observable.
   *
   * @param onNext this function will be called whenever the Observable emits an item
   * @param onError this function will be called if an error occurs
   * @param onCompleted the action to invoke when the source Observable calls
   *
   * @return an Observable with the side-effecting behavior applied.
   */
  def doOnEach(onNext: T => Unit, onError: Throwable => Unit, onCompleted: () => Unit): Observable[T] = {
    toScalaObservable[T](asJavaObservable.doOnEach(Observer(onNext, onError,onCompleted)))
  }

  /**
   * Modifies an Observable so that it invokes an action when it calls `onCompleted` or `onError`.
   * <p>
   * <img width="640" src="https://raw.github.com/wiki/Netflix/RxJava/images/rx-operators/doOnTerminate.png">
   * <p>
   * This differs from `finallyDo` in that this happens **before** `onCompleted/onError` are emitted.
   *
   * @param onTerminate the action to invoke when the source Observable calls `onCompleted` or `onError`
   * @return the source Observable with the side-effecting behavior applied
   * @see <a href="https://github.com/Netflix/RxJava/wiki/Observable-Utility-Operators#wiki-doonterminate">RxJava Wiki: doOnTerminate()</a>
   * @see <a href="http://msdn.microsoft.com/en-us/library/hh229804.aspx">MSDN: Observable.Do</a>
   */
  def doOnTerminate(onTerminate: () => Unit): Observable[T] = {
    toScalaObservable[T](asJavaObservable.doOnTerminate(onTerminate))
  }

  /**
   * Given two Observables, mirror the one that first emits an item.
   *
   * <img width="640" src="https://raw.github.com/wiki/Netflix/RxJava/images/rx-operators/amb.png">
   *
   * @param that
   *            an Observable competing to react first
   * @return an Observable that emits the same sequence of items as whichever of `this` or `that` first emitted an item.
   */
  def amb[U >: T](that: Observable[U]): Observable[U] = {
    val thisJava: rx.Observable[_ <: U] = this.asJavaObservable
    val thatJava: rx.Observable[_ <: U] = that.asJavaObservable
    toScalaObservable[U](rx.Observable.amb(thisJava, thatJava))
  }

  /**
   * Returns an Observable that emits the items emitted by the source Observable shifted forward in time by a
   * specified delay. Error notifications from the source Observable are not delayed.
   *
   * <img width="640" src="https://raw.github.com/wiki/Netflix/RxJava/images/rx-operators/delay.png">
   * 
   * @param delay the delay to shift the source by
   * @return the source Observable shifted in time by the specified delay
   */
  def delay(delay: Duration): Observable[T] = {
    toScalaObservable[T](asJavaObservable.delay(delay.length, delay.unit))
  }

  /**
   * Returns an Observable that emits the items emitted by the source Observable shifted forward in time by a
   * specified delay. Error notifications from the source Observable are not delayed.
   *
   * <img width="640" src="https://raw.github.com/wiki/Netflix/RxJava/images/rx-operators/delay.s.png">
   * 
   * @param delay the delay to shift the source by
   * @param scheduler the Scheduler to use for delaying
   * @return the source Observable shifted in time by the specified delay
   */
  def delay(delay: Duration, scheduler: Scheduler): Observable[T] = {
    toScalaObservable[T](asJavaObservable.delay(delay.length, delay.unit, scheduler))
  }

  /**
   * Returns an Observable that delays the emissions of the source Observable via another Observable on a
   * per-item basis.
   * <p>
   * <img width="640" src="https://raw.github.com/wiki/Netflix/RxJava/images/rx-operators/delay.o.png">
   * <p>
   * Note: the resulting Observable will immediately propagate any `onError` notification
   * from the source Observable.
   *
   * @param itemDelay a function that returns an Observable for each item emitted by the source Observable, which is
   *                  then used to delay the emission of that item by the resulting Observable until the Observable
   *                  returned from `itemDelay` emits an item
   * @return an Observable that delays the emissions of the source Observable via another Observable on a per-item basis
   */
  def delay(itemDelay: T => Observable[Any]): Observable[T] = {
    val itemDelayJava = new Func1[T, rx.Observable[Any]] {
      override def call(t: T): rx.Observable[Any] =
        itemDelay(t).asJavaObservable.asInstanceOf[rx.Observable[Any]]
    }
    toScalaObservable[T](asJavaObservable.delay[Any](itemDelayJava))
  }

  /**
   * Returns an Observable that delays the subscription to and emissions from the souce Observable via another
   * Observable on a per-item basis.
   * <p>
   * <img width="640" src="https://raw.github.com/wiki/Netflix/RxJava/images/rx-operators/delay.oo.png">
   * <p>
   * Note: the resulting Observable will immediately propagate any `onError` notification
   * from the source Observable.
   *
   * @param subscriptionDelay a function that returns an Observable that triggers the subscription to the source Observable
   *                          once it emits any item
   * @param itemDelay a function that returns an Observable for each item emitted by the source Observable, which is
   *                  then used to delay the emission of that item by the resulting Observable until the Observable
   *                  returned from `itemDelay` emits an item
   * @return an Observable that delays the subscription and emissions of the source Observable via another
   *         Observable on a per-item basis
   */
  def delay(subscriptionDelay: () => Observable[Any], itemDelay: T => Observable[Any]): Observable[T] = {
    val subscriptionDelayJava = new Func0[rx.Observable[Any]] {
      override def call(): rx.Observable[Any] =
        subscriptionDelay().asJavaObservable.asInstanceOf[rx.Observable[Any]]
    }
    val itemDelayJava = new Func1[T, rx.Observable[Any]] {
      override def call(t: T): rx.Observable[Any] =
        itemDelay(t).asJavaObservable.asInstanceOf[rx.Observable[Any]]
    }
    toScalaObservable[T](asJavaObservable.delay[Any, Any](subscriptionDelayJava, itemDelayJava))
  }

  /**
   * Return an Observable that delays the subscription to the source Observable by a given amount of time.
   *
   * <img width="640" src="https://raw.github.com/wiki/Netflix/RxJava/images/rx-operators/delaySubscription.png">
   * 
   * @param delay the time to delay the subscription
   * @return an Observable that delays the subscription to the source Observable by the given amount
   */
  def delaySubscription(delay: Duration): Observable[T] = {
    toScalaObservable[T](asJavaObservable.delaySubscription(delay.length, delay.unit))
  }

  /**
   * Return an Observable that delays the subscription to the source Observable by a given amount of time,
   * both waiting and subscribing on a given Scheduler.
   *
   * <img width="640" src="https://raw.github.com/wiki/Netflix/RxJava/images/rx-operators/delaySubscription.s.png">
   * 
   * @param delay the time to delay the subscription
   * @param scheduler the Scheduler on which the waiting and subscription will happen
   * @return an Observable that delays the subscription to the source Observable by a given
   *         amount, waiting and subscribing on the given Scheduler
   */
  def delaySubscription(delay: Duration, scheduler: Scheduler): Observable[T] = {
    toScalaObservable[T](asJavaObservable.delaySubscription(delay.length, delay.unit, scheduler))
  }

  /**
   * Returns an Observable that emits the single item at a specified index in a sequence of emissions from a
   * source Observbable.
   * 
   * <img width="640" src="https://raw.github.com/wiki/Netflix/RxJava/images/rx-operators/elementAt.png">
   * 
   * @param index
   *            the zero-based index of the item to retrieve
   * @return an Observable that emits a single item: the item at the specified position in the sequence of
   *         those emitted by the source Observable
   * @throws IndexOutOfBoundsException
   *             if index is greater than or equal to the number of items emitted by the source
   *             Observable, or index is less than 0
   */
  def elementAt(index: Int): Observable[T] = {
    toScalaObservable[T](asJavaObservable.elementAt(index))
  }

  /**
   * Returns an Observable that emits the item found at a specified index in a sequence of emissions from a
   * source Observable, or a default item if that index is out of range.
   * 
   * <img width="640" src="https://raw.github.com/wiki/Netflix/RxJava/images/rx-operators/elementAtOrDefault.png">
   * 
   * @param index
   *            the zero-based index of the item to retrieve
   * @param default
   *            the default item
   * @return an Observable that emits the item at the specified position in the sequence emitted by the source
   *         Observable, or the default item if that index is outside the bounds of the source sequence
   * @throws IndexOutOfBoundsException
   *             if {@code index} is less than 0
   */
  def elementAtOrDefault[U >: T](index: Int, default: U): Observable[U] = {
    val thisJava = asJavaObservable.asInstanceOf[rx.Observable[U]]
    toScalaObservable[U](thisJava.elementAtOrDefault(index, default))
  }

  /**
   * Return an Observable that emits a single Map containing all items emitted by the source Observable,
   * mapped by the keys returned by a specified {@code keySelector} function.
   * <p>
   * <img width="640" src="https://raw.github.com/wiki/Netflix/RxJava/images/rx-operators/toMap.png">
   * <p>
   * If more than one source item maps to the same key, the Map will contain the latest of those items.
   *
   * @param keySelector
     * the function that extracts the key from a source item to be used in the Map
   * @return an Observable that emits a single item: a Map containing the mapped items from the source
   *         Observable
   */
  def toMap[K] (keySelector: T => K): Observable[Map[K, T]]= {
    val thisJava = asJavaObservable.asInstanceOf[rx.Observable[T]]
    val o: rx.Observable[util.Map[K, T]] = thisJava.toMap[K](keySelector)
    toScalaObservable[util.Map[K,T]](o).map(m => m.toMap)
  }

  /**
   * Return an Observable that emits a single Map containing values corresponding to items emitted by the
   * source Observable, mapped by the keys returned by a specified {@code keySelector} function.
   * <p>
   * <img width="640" src="https://raw.github.com/wiki/Netflix/RxJava/images/rx-operators/toMap.png">
   * <p>
   * If more than one source item maps to the same key, the Map will contain a single entry that
   * corresponds to the latest of those items.
   *
   * @param keySelector
     *            the function that extracts the key from a source item to be used in the Map
   * @param valueSelector
     *            the function that extracts the value from a source item to be used in the Map
   * @return an Observable that emits a single item: a HashMap containing the mapped items from the source
   *         Observable
   */
  def toMap[K, V] (keySelector: T => K, valueSelector: T => V) : Observable[Map[K, V]] = {
    val thisJava = asJavaObservable.asInstanceOf[rx.Observable[T]]
    val o: rx.Observable[util.Map[K, V]] = thisJava.toMap[K, V](keySelector, valueSelector)
    toScalaObservable[util.Map[K, V]](o).map(m => m.toMap)
  }

  /**
   * Return an Observable that emits a single Map, returned by a specified {@code mapFactory} function, that
   * contains keys and values extracted from the items emitted by the source Observable.
   * <p>
   * <img width="640" src="https://raw.github.com/wiki/Netflix/RxJava/images/rx-operators/toMap.png">
   *
   * @param keySelector
     *            the function that extracts the key from a source item to be used in the Map
   * @param valueSelector
     *            the function that extracts the value from the source items to be used as value in the Map
   * @param mapFactory
     *            the function that returns a Map instance to be used
   * @return an Observable that emits a single item: a Map that contains the mapped items emitted by the
   *         source Observable
   */
  def toMap[K, V] (keySelector: T => K, valueSelector: T => V, mapFactory: () => Map[K, V]): Observable[Map[K, V]] = {
    val thisJava = asJavaObservable.asInstanceOf[rx.Observable[T]]
    val o: rx.Observable[util.Map[K, V]] = thisJava.toMap[K, V](keySelector, valueSelector)
    toScalaObservable[util.Map[K, V]](o).map(m => mapFactory() ++ m.toMap)
  }

  /**
   * Returns an Observable that emits a Boolean value that indicates whether `this` and `that` Observable sequences are the
   * same by comparing the items emitted by each Observable pairwise.
   * <p>
   * <img width="640" src="https://raw.github.com/wiki/Netflix/RxJava/images/rx-operators/sequenceEqual.png">
   *
   * Note: this method uses `==` to compare elements. It's a bit different from RxJava which uses `Object.equals`.
   *
   * @param that the Observable to compare
   * @return an Observable that emits a `Boolean` value that indicates whether the two sequences are the same
   */
  def sequenceEqual[U >: T](that: Observable[U]): Observable[Boolean] = {
    sequenceEqual(that, (_1: U, _2: U) => _1 == _2)
  }

  /**
   * Returns an Observable that emits a Boolean value that indicates whether `this` and `that` Observable sequences are the
   * same by comparing the items emitted by each Observable pairwise based on the results of a specified `equality` function.
   * <p>
   * <img width="640" src="https://raw.github.com/wiki/Netflix/RxJava/images/rx-operators/sequenceEqual.png">
   *
   * @param that the Observable to compare
   * @param equality a function used to compare items emitted by each Observable
   * @return an Observable that emits a `Boolean` value that indicates whether the two sequences are the same based on the `equality` function.
   */
  def sequenceEqual[U >: T](that: Observable[U], equality: (U, U) => Boolean): Observable[Boolean] = {
    val thisJava: rx.Observable[_ <: U] = this.asJavaObservable
    val thatJava: rx.Observable[_ <: U] = that.asJavaObservable
    val equalityJava: Func2[_ >: U, _ >: U, java.lang.Boolean] = equality
    toScalaObservable[java.lang.Boolean](rx.Observable.sequenceEqual[U](thisJava, thatJava, equalityJava)).map(_.booleanValue)
  }

  /**
   * Returns an Observable that emits records of the time interval between consecutive items emitted by the
   * source Obsegrvable.
   * <p>
   * <img width="640" src="https://raw.github.com/wiki/Netflix/RxJava/images/rx-operators/timeInterval.png">
   *
   * @return an Observable that emits time interval information items
   */
  def timeInterval: Observable[(Duration, T)] = {
    toScalaObservable(asJavaObservable.timeInterval())
      .map(inv => (Duration(inv.getIntervalInMilliseconds, MILLISECONDS), inv.getValue))
  }

  /**
   * Returns an Observable that emits records of the time interval between consecutive items emitted by the
   * source Observable, where this interval is computed on a specified Scheduler.
   * <p>
   * <img width="640" src="https://raw.github.com/wiki/Netflix/RxJava/images/rx-operators/timeInterval.s.png">
   *
   * @param scheduler the [[Scheduler]] used to compute time intervals
   * @return an Observable that emits time interval information items
   */
  def timeInterval(scheduler: Scheduler): Observable[(Duration, T)] = {
    toScalaObservable(asJavaObservable.timeInterval(scheduler.asJavaScheduler))
      .map(inv => (Duration(inv.getIntervalInMilliseconds, MILLISECONDS), inv.getValue))
  }

  /**
   * Lift a function to the current Observable and return a new Observable that when subscribed to will pass
   * the values of the current Observable through the function.
   * <p>
   * In other words, this allows chaining Observers together on an Observable for acting on the values within
   * the Observable.
   * {{{
   * observable.map(...).filter(...).take(5).lift(new ObserverA()).lift(new ObserverB(...)).subscribe()
   * }}}
   *
   * @param operator
   * @return an Observable that emits values that are the result of applying the bind function to the values
   *         of the current Observable
   */
  def lift[R](operator: Subscriber[R] => Subscriber[T]): Observable[R] = {
    toScalaObservable(asJavaObservable.lift(toJavaOperator[T, R](operator)))
  }
}

/**
 * Provides various ways to construct new Observables.
 */
object Observable {
  import scala.collection.JavaConverters._
  import scala.collection.immutable.Range
  import scala.concurrent.duration.Duration
  import scala.concurrent.{Future, ExecutionContext}
  import scala.util.{Success, Failure}
  import ImplicitFunctionConversions._
  import JavaConversions._
  import rx.lang.scala.subjects.AsyncSubject

  private[scala]
  def jObsOfListToScObsOfSeq[T](jObs: rx.Observable[_ <: java.util.List[T]]): Observable[Seq[T]] = {
    val oScala1: Observable[java.util.List[T]] = new Observable[java.util.List[T]]{ val asJavaObservable = jObs }
    oScala1.map((lJava: java.util.List[T]) => lJava.asScala)
  }

  private[scala]
  def jObsOfJObsToScObsOfScObs[T](jObs: rx.Observable[_ <: rx.Observable[_ <: T]]): Observable[Observable[T]] = {
    val oScala1: Observable[rx.Observable[_ <: T]] = new Observable[rx.Observable[_ <: T]]{ val asJavaObservable = jObs }
    oScala1.map((oJava: rx.Observable[_ <: T]) => oJava)
  }

  /**
   * Creates an Observable that will execute the given function when an [[rx.lang.scala.Observer]] subscribes to it.
   *
   * <img width="640" src="https://github.com/Netflix/RxJava/wiki/images/rx-operators/create.png">
   *
   * Write the function you pass to `create` so that it behaves as an Observable: It
   * should invoke the Observer's [[rx.lang.scala.Observer.onNext onNext]], [[rx.lang.scala.Observer.onError onError]], and [[rx.lang.scala.Observer.onCompleted onCompleted]] methods
   * appropriately.
   *
   * See <a href="http://go.microsoft.com/fwlink/?LinkID=205219">Rx Design Guidelines (PDF)</a>
   * for detailed information.
   *
   *
   * @tparam T
   *            the type of the items that this Observable emits.
   * @param func
   *            a function that accepts an `Observer[T]`, invokes its `onNext`, `onError`, and `onCompleted` methods
   *            as appropriate, and returns a [[rx.lang.scala.Subscription]] to allow the Observer to
   *            canceling the subscription.
   * @return
   *         an Observable that, when an [[rx.lang.scala.Observer]] subscribes to it, will execute the given function.
   */
  @deprecated("Use `apply[T](Subscriber[T] => Unit)` instead", "0.17.0")
  def create[T](func: Observer[T] => Subscription): Observable[T] = {
    toScalaObservable[T](rx.Observable.create(new OnSubscribeFunc[T] {
      def onSubscribe(t1: rx.Observer[_ >: T]): rx.Subscription = {
        func(Observer(t1))
      }
    }))
  }

  /*
  Note: It's dangerous to have two overloads where one takes an `Observer[T] => Subscription`
  function and the other takes a `Subscriber[T] => Unit` function, because expressions like
  `o => Subscription{}` have both of these types.
  So we call the old create method "create", and the new create method "apply".
  Try it out yourself here:
  def foo[T]: Unit = { 
    val fMeant: Observer[T] => Subscription = o => Subscription{}
    val fWrong: Subscriber[T] => Unit = o => Subscription{}
  }
  */

  /**
   * Returns an Observable that will execute the specified function when someone subscribes to it.
   *
   * <img width="640" src="https://raw.github.com/wiki/Netflix/RxJava/images/rx-operators/create.png">
   *
   * Write the function you pass so that it behaves as an Observable: It should invoke the
   * Subscriber's `onNext`, `onError`, and `onCompleted` methods appropriately.
   *
   * You can `add` custom [[Subscription]]s to [[Subscriber]]. These [[Subscription]]s will be called
   * <ul>
   *   <li>when someone calls `unsubscribe`.</li>
   *   <li>after `onCompleted` or `onError`.</li>
   * </ul>
   *
   * See <a href="http://go.microsoft.com/fwlink/?LinkID=205219">Rx Design Guidelines (PDF)</a> for detailed
   * information.
   *
   * See `<a href="https://github.com/Netflix/RxJava/blob/master/language-adaptors/rxjava-scala/src/examples/scala/rx/lang/scala/examples/RxScalaDemo.scala">RxScalaDemo</a>.createExampleGood`
   * and `<a href="https://github.com/Netflix/RxJava/blob/master/language-adaptors/rxjava-scala/src/examples/scala/rx/lang/scala/examples/RxScalaDemo.scala">RxScalaDemo</a>.createExampleGood2`.
   *
   * @param T
   *            the type of the items that this Observable emits
   * @param f
   *            a function that accepts a `Subscriber[T]`, and invokes its `onNext`,
   *            `onError`, and `onCompleted` methods as appropriate
   * @return an Observable that, when someone subscribes to it, will execute the specified
   *         function
   */
  def apply[T](f: Subscriber[T] => Unit): Observable[T] = {
    toScalaObservable(rx.Observable.create(f))
  }
  
  /**
   * Returns an Observable that invokes an [[rx.lang.scala.Observer]]'s [[rx.lang.scala.Observer.onError onError]]
   * method when the Observer subscribes to it.
   *
   * <img width="640" src="https://raw.github.com/wiki/Netflix/RxJava/images/rx-operators/error.png">
   *
   * @param exception
   *            the particular error to report
   * @tparam T
   *            the type of the items (ostensibly) emitted by the Observable
   * @return an Observable that invokes the [[rx.lang.scala.Observer]]'s [[rx.lang.scala.Observer.onError onError]]
   *         method when the Observer subscribes to it
   */
  def error[T](exception: Throwable): Observable[T] = {
    toScalaObservable[T](rx.Observable.error(exception))
  }

  /**
   * Returns an Observable that emits no data to the [[rx.lang.scala.Observer]] and
   * immediately invokes its [[rx.lang.scala.Observer#onCompleted onCompleted]] method
   * with the specified scheduler.
   * <p>
   * <img width="640" src="https://raw.github.com/wiki/Netflix/RxJava/images/rx-operators/empty.s.png">
   *
   * @return an Observable that returns no data to the [[rx.lang.scala.Observer]] and
   *         immediately invokes the [[rx.lang.scala.Observer]]r's
   *        [[rx.lang.scala.Observer#onCompleted onCompleted]] method with the
   *         specified scheduler
   * @see <a href="https://github.com/Netflix/RxJava/wiki/Creating-Observables#empty-error-and-never">RxJava Wiki: empty()</a>
   * @see <a href="http://msdn.microsoft.com/en-us/library/hh229066.aspx">MSDN: Observable.Empty Method (IScheduler)</a>
   */
  def empty: Observable[Nothing] = {
    toScalaObservable(rx.Observable.empty[Nothing]())
  }

  /**
   * Returns an Observable that emits no data to the [[rx.lang.scala.Observer]] and
   * immediately invokes its [[rx.lang.scala.Observer#onCompleted onCompleted]] method
   * with the specified scheduler.
   * <p>
   * <img width="640" src="https://raw.github.com/wiki/Netflix/RxJava/images/rx-operators/empty.s.png">
   *
   * @param scheduler the scheduler to call the
                        [[rx.lang.scala.Observer#onCompleted onCompleted]] method
   * @return an Observable that returns no data to the [[rx.lang.scala.Observer]] and
   *         immediately invokes the [[rx.lang.scala.Observer]]r's
   *        [[rx.lang.scala.Observer#onCompleted onCompleted]] method with the
   *         specified scheduler
   * @see <a href="https://github.com/Netflix/RxJava/wiki/Creating-Observables#empty-error-and-never">RxJava Wiki: empty()</a>
   * @see <a href="http://msdn.microsoft.com/en-us/library/hh229066.aspx">MSDN: Observable.Empty Method (IScheduler)</a>
   */
  def empty(scheduler: Scheduler): Observable[Nothing] = {
    toScalaObservable(rx.Observable.empty[Nothing](scalaSchedulerToJavaScheduler(scheduler)))
  }

  /**
   * Converts a sequence of values into an Observable.
   *
   * <img width="640" src="https://github.com/Netflix/RxJava/wiki/images/rx-operators/from.png">
   *
   * Implementation note: the entire array will be immediately emitted each time an [[rx.lang.scala.Observer]] subscribes.
   * Since this occurs before the [[rx.lang.scala.Subscription]] is returned,
   * it in not possible to unsubscribe from the sequence before it completes.
   *
   * @param items
   *            the source Array
   * @tparam T
   *            the type of items in the Array, and the type of items to be emitted by the
   *            resulting Observable
   * @return an Observable that emits each item in the source Array
   */
  def items[T](items: T*): Observable[T] = {
    toScalaObservable[T](rx.Observable.from(items.toIterable.asJava))
  }

 /** Returns an Observable emitting the value produced by the Future as its single item.
   * If the future fails, the Observable will fail as well.
   *
   * @param f Future whose value ends up in the resulting Observable
   * @return an Observable completed after producing the value of the future, or with an exception
   */
  def from[T](f: Future[T])(implicit execContext: ExecutionContext): Observable[T] = {
    val s = AsyncSubject[T]()
    f.onComplete {
      case Failure(e) =>
        s.onError(e)
      case Success(c) =>
        s.onNext(c)
        s.onCompleted()
    }
    s
  }

  /**
   * Converts an `Iterable` into an Observable.
   *
   * <img width="640" src="https://raw.github.com/wiki/Netflix/RxJava/images/rx-operators/from.png">
   *
   * Note: the entire iterable sequence is immediately emitted each time an
   * Observer subscribes. Since this occurs before the
   * `Subscription` is returned, it is not possible to unsubscribe from
   * the sequence before it completes.
   *
   * @param iterable the source `Iterable` sequence
   * @param T the type of items in the `Iterable` sequence and the
   *            type of items to be emitted by the resulting Observable
   * @return an Observable that emits each item in the source `Iterable`
   *         sequence
   */
  def from[T](iterable: Iterable[T]): Observable[T] = {
    toScalaObservable(rx.Observable.from(iterable.asJava))
  }

  /**
   *
   * @param iterable  the source `Iterable` sequence
   * @param scheduler the scheduler to use
   * @tparam T   the type of items in the `Iterable` sequence and the
   *            type of items to be emitted by the resulting Observable
   * @return   an Observable that emits each item in the source `Iterable`
   *         sequence
   */
  def from[T](iterable: Iterable[T], scheduler: Scheduler): Observable[T] = {
    toScalaObservable(rx.Observable.from(iterable.asJava, scheduler.asJavaScheduler))
  }


  /**
   * Returns an Observable that calls an Observable factory to create its Observable for each
   * new Observer that subscribes. That is, for each subscriber, the actual Observable is determined
   * by the factory function.
   *
   * <img width="640" src="https://github.com/Netflix/RxJava/wiki/images/rx-operators/defer.png">
   *
   * The defer operator allows you to defer or delay emitting items from an Observable until such
   * time as an Observer subscribes to the Observable. This allows an [[rx.lang.scala.Observer]] to easily
   * obtain updates or a refreshed version of the sequence.
   *
   * @param observable
   *            the Observable factory function to invoke for each [[rx.lang.scala.Observer]] that
   *            subscribes to the resulting Observable
   * @tparam T
   *            the type of the items emitted by the Observable
   * @return an Observable whose [[rx.lang.scala.Observer]]s trigger an invocation of the given Observable
   *         factory function
   */
  def defer[T](observable: => Observable[T]): Observable[T] = {
    toScalaObservable[T](rx.Observable.defer[T](() => observable.asJavaObservable))
  }

  /**
   * Returns an Observable that never sends any items or notifications to an [[rx.lang.scala.Observer]].
   *
   * <img width="640" src="https://github.com/Netflix/RxJava/wiki/images/rx-operators/never.png">
   *
   * This Observable is useful primarily for testing purposes.
   *
   * @return an Observable that never sends any items or notifications to an [[rx.lang.scala.Observer]]
   */
  def never: Observable[Nothing] = {
    toScalaObservable[Nothing](rx.Observable.never())
  }

  /**
   * Given 3 observables, returns an observable that emits Tuples of 3 elements each.
   * The first emitted Tuple will contain the first element of each source observable,
   * the second Tuple the second element of each source observable, and so on.
   *
   * @return an Observable that emits the zipped Observables
   */
  def zip[A, B, C](obA: Observable[A], obB: Observable[B], obC: Observable[C]): Observable[(A, B, C)] = {
    toScalaObservable[(A, B, C)](rx.Observable.zip[A, B, C, (A, B, C)](obA.asJavaObservable, obB.asJavaObservable, obC.asJavaObservable, (a: A, b: B, c: C) => (a, b, c)))
  }

  /**
   * Given 4 observables, returns an observable that emits Tuples of 4 elements each.
   * The first emitted Tuple will contain the first element of each source observable,
   * the second Tuple the second element of each source observable, and so on.
   *
   * @return an Observable that emits the zipped Observables
   */
  def zip[A, B, C, D](obA: Observable[A], obB: Observable[B], obC: Observable[C], obD: Observable[D]): Observable[(A, B, C, D)] = {
    toScalaObservable[(A, B, C, D)](rx.Observable.zip[A, B, C, D, (A, B, C, D)](obA.asJavaObservable, obB.asJavaObservable, obC.asJavaObservable, obD.asJavaObservable, (a: A, b: B, c: C, d: D) => (a, b, c, d)))
  }

  /**
   * Given an Observable emitting `N` source observables, returns an observable that
   * emits Seqs of `N` elements each.
   * The first emitted Seq will contain the first element of each source observable,
   * the second Seq the second element of each source observable, and so on.
   *
   * Note that the returned Observable will only start emitting items once the given
   * `Observable[Observable[T]]` has completed, because otherwise it cannot know `N`.
   *
   * @param observables
   *            An Observable emitting N source Observables
   * @return an Observable that emits the zipped Seqs
   */
  def zip[T](observables: Observable[Observable[T]]): Observable[Seq[T]] = {
    val f: FuncN[Seq[T]] = (args: Seq[java.lang.Object]) => {
      val asSeq: Seq[Object] = args.toSeq
      asSeq.asInstanceOf[Seq[T]]
    }
    val list = observables.map(_.asJavaObservable).asJavaObservable
    val o = rx.Observable.zip(list, f)
    toScalaObservable[Seq[T]](o)
  }

  /**
   * Emits `0`, `1`, `2`, `...` with a delay of `duration` between consecutive numbers.
   *
   * <img width="640" src="https://github.com/Netflix/RxJava/wiki/images/rx-operators/interval.png">
   *
   * @param duration
   *            duration between two consecutive numbers
   * @return An Observable that emits a number each time interval.
   */
  def interval(duration: Duration): Observable[Long] = {
    toScalaObservable[java.lang.Long](rx.Observable.interval(duration.length, duration.unit)).map(_.longValue())
    /*XXX*/
  }

  /**
   * Emits `0`, `1`, `2`, `...` with a delay of `duration` between consecutive numbers.
   *
   * <img width="640" src="https://github.com/Netflix/RxJava/wiki/images/rx-operators/interval.png">
   *
   * @param period
   *            duration between two consecutive numbers
   * @param scheduler
   *            the scheduler to use
   * @return An Observable that emits a number each time interval.
   */
  def interval(period: Duration, scheduler: Scheduler): Observable[Long] = {
    toScalaObservable[java.lang.Long](rx.Observable.interval(period.length, period.unit, scheduler)).map(_.longValue())
  }

  /**
   * Return an Observable that emits a 0L after the {@code initialDelay} and ever increasing
   * numbers after each {@code period} of time thereafter, on a specified Scheduler.
   * <p>
   * <img width="640" src="https://raw.github.com/wiki/Netflix/RxJava/images/rx-operators/timer.ps.png">
   *
   * @param initialDelay
   * the initial delay time to wait before emitting the first value of 0L
   * @param period
   * the period of time between emissions of the subsequent numbers
   * @return an Observable that emits a 0L after the { @code initialDelay} and ever increasing
   *                                                         numbers after each { @code period} of time thereafter, while running on the given { @code scheduler}
   */
  def timer(initialDelay: Duration, period: Duration): Observable[Long] = {
    toScalaObservable[java.lang.Long](rx.Observable.timer(initialDelay.toNanos, period.toNanos, duration.NANOSECONDS)).map(_.longValue())
    /*XXX*/
  }

  /**
   * Return an Observable that emits a 0L after the {@code initialDelay} and ever increasing
   * numbers after each {@code period} of time thereafter, on a specified Scheduler.
   * <p>
   * <img width="640" src="https://raw.github.com/wiki/Netflix/RxJava/images/rx-operators/timer.ps.png">
   *
   * @param initialDelay
   * the initial delay time to wait before emitting the first value of 0L
   * @param period
   * the period of time between emissions of the subsequent numbers
   * @param scheduler
     * the scheduler on which the waiting happens and items are emitted
   * @return an Observable that emits a 0L after the { @code initialDelay} and ever increasing
   *                                                         numbers after each { @code period} of time thereafter, while running on the given { @code scheduler}
   */
  def timer(initialDelay: Duration, period: Duration, scheduler: Scheduler): Observable[Long] = {
    toScalaObservable[java.lang.Long](rx.Observable.timer(initialDelay.toNanos, period.toNanos, duration.NANOSECONDS, scheduler)).map(_.longValue())
  }

  /**
   * Constructs an Observable that creates a dependent resource object.
   * <p>
   * <img width="640" src="https://raw.github.com/wiki/Netflix/RxJava/images/rx-operators/using.png">
   *
   * @param resourceFactory the factory function to create a resource object that depends on the Observable
   * @param observableFactory the factory function to obtain an Observable
   * @return the Observable whose lifetime controls the lifetime of the dependent resource object
   */
  def using[T, Resource <: Subscription](resourceFactory: () => Resource, observableFactory: Resource => Observable[T]): Observable[T] = {
    class ResourceSubscription(val resource: Resource) extends rx.Subscription {
      def unsubscribe = resource.unsubscribe

      def isUnsubscribed: Boolean = resource.isUnsubscribed
    }

    toScalaObservable(rx.Observable.using[T, ResourceSubscription](
      () => new ResourceSubscription(resourceFactory()),
      (s: ResourceSubscription) => observableFactory(s.resource).asJavaObservable
    ))
  }

}






