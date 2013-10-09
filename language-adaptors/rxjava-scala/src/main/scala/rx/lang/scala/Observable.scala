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

/**
 * The Observable interface that implements the Reactive Pattern.
 * 
 * @param asJava the underlying Java observable
 * 
 * @define subscribeObserverMain
 * Call this method to subscribe an [[Observer]] for receiving 
 * items and notifications from the Observable.
 * 
 * A typical implementation of `subscribe` does the following:
 *
 * It stores a reference to the Observer in a collection object, such as a `List[T]` object.
 *
 * It returns a reference to the [[Subscription]] interface. This enables Observers to
 * unsubscribe, that is, to stop receiving items and notifications before the Observable stops
 * sending them, which also invokes the Observer's [[Observer.onCompleted onCompleted]] method.
 *
 * An `Observable[T]` instance is responsible for accepting all subscriptions
 * and notifying all Observers. Unless the documentation for a particular
 * `Observable[T]` implementation indicates otherwise, Observers should make no
 * assumptions about the order in which multiple Observers will receive their notifications.
 *
 * @define subscribeObserverParamObserver 
 *         the observer
 * @define subscribeObserverParamScheduler 
 *         the [[Scheduler]] on which Observers subscribe to the Observable
 * @define subscribeAllReturn 
 *         a [[Subscription]] reference whose `unsubscribe` method can be called to  stop receiving items
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
// constructor is private because users should use apply in companion
class Observable[+T] private[scala] (val asJava: rx.Observable[_ <: T])
  // Uncommenting this line combined with `new Observable(...)` instead of `new Observable[T](...)`
  // makes the compiler crash
  extends AnyVal 
{
  import scala.collection.JavaConverters._
  import scala.collection.Seq
  import scala.concurrent.duration.{Duration, TimeUnit}
  import rx.{Observable => JObservable}
  import rx.util.functions._
  import rx.lang.scala.util._
  import rx.lang.scala.subjects.Subject
  import rx.lang.scala.observables.BlockingObservable
  import rx.lang.scala.ImplicitFunctionConversions._
  
  /**
   * $subscribeObserverMain
   * 
   * @param observer $subscribeObserverParamObserver
   * @param scheduler $subscribeObserverParamScheduler
   * @return $subscribeAllReturn
   */
  def subscribe(observer: Observer[T], scheduler: Scheduler): Subscription = {
    asJava.subscribe(observer, scheduler)
  }

  /**
   * $subscribeObserverMain
   * 
   * @param observer $subscribeObserverParamObserver
   * @return $subscribeAllReturn
   */
  def subscribe(observer: Observer[T]): Subscription = {
    asJava.subscribe(observer)
  }

  /**
   * $subscribeCallbacksMainNoNotifications
   *
   * @param onNext $subscribeCallbacksParamOnNext
   * @return $subscribeAllReturn
   */
  def subscribe(onNext: T => Unit): Subscription = {
    asJava.subscribe(onNext)
  }
  
  /**
   * $subscribeCallbacksMainWithNotifications
   *
   * @param onNext $subscribeCallbacksParamOnNext
   * @param onError $subscribeCallbacksParamOnError
   * @return $subscribeAllReturn
   */  
  def subscribe(onNext: T => Unit, onError: Throwable => Unit): Subscription = {
    asJava.subscribe(onNext, onError)
  }

  /**
   * $subscribeCallbacksMainWithNotifications
   *
   * @param onNext $subscribeCallbacksParamOnNext
   * @param onError $subscribeCallbacksParamOnError
   * @param onComplete $subscribeCallbacksParamOnComplete
   * @return $subscribeAllReturn
   */
  def subscribe(onNext: T => Unit, onError: Throwable => Unit, onComplete: () => Unit): Subscription = {
    asJava.subscribe(onNext, onError, onComplete)
  }

  /**
   * $subscribeCallbacksMainWithNotifications
   *
   * @param onNext $subscribeCallbacksParamOnNext
   * @param onError $subscribeCallbacksParamOnError
   * @param onComplete $subscribeCallbacksParamOnComplete
   * @param scheduler $subscribeCallbacksParamScheduler
   * @return $subscribeAllReturn
   */
  def subscribe(onNext: T => Unit, onError: Throwable => Unit, onComplete: () => Unit, scheduler: Scheduler): Subscription = {
    asJava.subscribe(onNext, onError, onComplete, scheduler)
  }
  
  /**
   * $subscribeCallbacksMainWithNotifications
   *
   * @param onNext $subscribeCallbacksParamOnNext
   * @param onError $subscribeCallbacksParamOnError
   * @param scheduler $subscribeCallbacksParamScheduler
   * @return $subscribeAllReturn
   */
  def subscribe(onNext: T => Unit, onError: Throwable => Unit, scheduler: Scheduler): Subscription = {
    asJava.subscribe(onNext, onError, scheduler)
  }

  /**
   * $subscribeCallbacksMainNoNotifications
   *
   * @param onNext $subscribeCallbacksParamOnNext
   * @param scheduler $subscribeCallbacksParamScheduler
   * @return $subscribeAllReturn
   */
  def subscribe(onNext: T => Unit, scheduler: Scheduler): Subscription = {
    asJava.subscribe(onNext, scheduler)
  }

  /**
   * Returns a pair of a start function and an [[Observable]] that upon calling the start function causes the source Observable to
   * push results into the specified subject.
   * 
   * @param subject
   *            the `rx.lang.scala.subjects.Subject` to push source items into
   * @tparam R
   *            result type
   * @return a pair of a start function and an [[Observable]] such that when the start function
   *         is called, the Observable starts to push results into the specified Subject
   */
  def multicast[R](subject: Subject[T, R]): (() => Subscription, Observable[R]) = {
    val javaCO = asJava.multicast[R](subject)
    (() => javaCO.connect(), Observable[R](javaCO))
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
    val o1: JObservable[_ <: U] = this.asJava
    val o2: JObservable[_ <: U] = that.asJava
    Observable(JObservable.concat(o1, o2))
  }

  /**
   * Returns an Observable that emits the items emitted by several Observables, one after the
   * other.
   * 
   * This operation is only available if `this` is of type `Observable[Observable[U]]` for some `U`,
   * otherwise you'll get a compilation error.
   * 
   * @usecase def concat[U]: Observable[U]
   *    @inheritdoc
   */
  def concat[U](implicit evidence: Observable[T] <:< Observable[Observable[U]]): Observable[U] = {
    val o2: Observable[Observable[U]] = this
    val o3: Observable[rx.Observable[_ <: U]] = o2.map(_.asJava)
    val o4: rx.Observable[_ <: rx.Observable[_ <: U]] = o3.asJava
    val o5 = rx.Observable.concat[U](o4)
    Observable[U](o5)
  }

  /**
   * Wraps this Observable in another Observable that ensures that the resulting
   * Observable is chronologically well-behaved.
   *
   * <img width="640" src="https://github.com/Netflix/RxJava/wiki/images/rx-operators/synchronize.png">
   *
   * A well-behaved Observable does not interleave its invocations of the [[Observer.onNext onNext]], [[Observer.onCompleted onCompleted]], and [[Observer.onError onError]] methods of
   * its [[Observer]]s; it invokes `onCompleted` or `onError` only once; and it never invokes `onNext` after invoking either `onCompleted` or `onError`.
   * `synchronize` enforces this, and the Observable it returns invokes `onNext` and `onCompleted` or `onError` synchronously.
   * 
   * @param observable
   *            the source Observable
   * @return an Observable that is a chronologically well-behaved version of the source
   *         Observable, and that synchronously notifies its [[Observer]]s
   */
  def synchronize: Observable[T] = {
    Observable[T](asJava.synchronize)
  }
  
  /**
   * Wraps each item emitted by a source Observable in a timestamped tuple.
   *
   * <img width="640" src="https://github.com/Netflix/RxJava/wiki/images/rx-operators/timestamp.png">
   * 
   * @return an Observable that emits timestamped items from the source Observable
   */
  def timestamp: Observable[(Long, T)] = {
    Observable[rx.util.Timestamped[_ <: T]](asJava.timestamp())
      .map((t: rx.util.Timestamped[_ <: T]) => (t.getTimestampMillis, t.getValue()))
  }
  
  /**
   * Returns an Observable formed from this Observable and another Observable by combining 
   * corresponding elements in pairs. 
   * The number of `onNext` invocations of the resulting `Observable[(T, U)]`
   * is the minumum of the number of `onNext` invocations of `this` and `that`. 
   */
  def zip[U](that: Observable[U]): Observable[(T, U)] = {
    Observable[(T, U)](JObservable.zip[T, U, (T, U)](this.asJava, that.asJava, (t: T, u: U) => (t, u)))
  }

  /**
   * Zips this Observable with its indices.
   * 
   * @return An Observable emitting pairs consisting of all elements of this Observable paired with 
   *         their index. Indices start at 0.
   */
  def zipWithIndex: Observable[(T, Int)] = {
    val fScala: (T, Integer) => (T, Int) = (elem: T, index: Integer) => (elem, index)
    val fJava : Func2[_ >: T, Integer, _ <: (T, Int)] = fScala
    Observable[(T, Int)](asJava.mapWithIndex[(T, Int)](fJava))
  }
    
  /**
   * Creates an Observable which produces buffers of collected values.
   * 
   * This Observable produces connected non-overlapping buffers. The current buffer is
   * emitted and replaced with a new buffer when the Observable produced by the specified function produces a [[rx.lang.scala.util.Closing]] object. The function will then
   * be used to create a new Observable to listen for the end of the next buffer.
   * 
   * @param closings
   *            The function which is used to produce an [[Observable]] for every buffer created.
   *            When this [[Observable]] produces a [[rx.lang.scala.util.Closing]] object, the associated buffer
   *            is emitted and replaced with a new one.
   * @return
   *         An [[Observable]] which produces connected non-overlapping buffers, which are emitted
   *         when the current [[Observable]] created with the function argument produces a [[rx.lang.scala.util.Closing]] object.
   */
  def buffer(closings: () => Observable[Closing]) : Observable[Seq[T]] = {
    val f: Func0[_ <: rx.Observable[_ <: Closing]] = closings().asJava
    val jObs: rx.Observable[_ <: java.util.List[_]] = asJava.buffer(f)
    Observable.jObsOfListToScObsOfSeq(jObs.asInstanceOf[rx.Observable[_ <: java.util.List[T]]])
  }

  /**
   * Creates an Observable which produces buffers of collected values.
   * 
   * This Observable produces buffers. Buffers are created when the specified `openings`
   * Observable produces a [[rx.lang.scala.util.Opening]] object. Additionally the function argument
   * is used to create an Observable which produces [[rx.lang.scala.util.Closing]] objects. When this
   * Observable produces such an object, the associated buffer is emitted.
   * 
   * @param openings
   *            The [[Observable]] which, when it produces a [[rx.lang.scala.util.Opening]] object, will cause
   *            another buffer to be created.
   * @param closings
   *            The function which is used to produce an [[Observable]] for every buffer created.
   *            When this [[Observable]] produces a [[rx.lang.scala.util.Closing]] object, the associated buffer
   *            is emitted.
   * @return
   *         An [[Observable]] which produces buffers which are created and emitted when the specified [[Observable]]s publish certain objects.
   */
  def buffer(openings: Observable[Opening], closings: Opening => Observable[Closing]): Observable[Seq[T]] = {
    val opening: rx.Observable[_ <: Opening] = openings.asJava 
    val closing: Func1[Opening, _ <: rx.Observable[_ <: Closing]] = (o: Opening) => closings(o).asJava
    val jObs: rx.Observable[_ <: java.util.List[_]] = asJava.buffer(opening, closing)
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
   *         An [[Observable]] which produces connected non-overlapping buffers containing at most
   *         `count` produced values.
   */
  def buffer(count: Int): Observable[Seq[T]] = {
    val oJava: rx.Observable[_ <: java.util.List[_]] = asJava.buffer(count)
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
   *         An [[Observable]] which produces buffers every `skip` values containing at most
   *         `count` produced values.
   */
  def buffer(count: Int, skip: Int): Observable[Seq[T]] = {
    val oJava: rx.Observable[_ <: java.util.List[_]] = asJava.buffer(count, skip)
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
   *         An [[Observable]] which produces connected non-overlapping buffers with a fixed duration.
   */
  def buffer(timespan: Duration): Observable[Seq[T]] = {
    val oJava: rx.Observable[_ <: java.util.List[_]] = asJava.buffer(timespan.length, timespan.unit)
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
   *            The [[Scheduler]] to use when determining the end and start of a buffer.
   * @return
   *         An [[Observable]] which produces connected non-overlapping buffers with a fixed duration.
   */
  def buffer(timespan: Duration, scheduler: Scheduler): Observable[Seq[T]] = {
    val oJava: rx.Observable[_ <: java.util.List[_]] = asJava.buffer(timespan.length, timespan.unit, scheduler)
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
   *         An [[Observable]] which produces connected non-overlapping buffers which are emitted after
   *         a fixed duration or when the buffer has reached maximum capacity (which ever occurs first).
   */
  def buffer(timespan: Duration, count: Int): Observable[Seq[T]] = {
    val oJava: rx.Observable[_ <: java.util.List[_]] = asJava.buffer(timespan.length, timespan.unit, count)
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
   *            The [[Scheduler]] to use when determining the end and start of a buffer.
   * @return
   *         An [[Observable]] which produces connected non-overlapping buffers which are emitted after
   *         a fixed duration or when the buffer has reached maximum capacity (which ever occurs first).
   */
  def buffer(timespan: Duration, count: Int, scheduler: Scheduler): Observable[Seq[T]] = {
    val oJava: rx.Observable[_ <: java.util.List[_]] = asJava.buffer(timespan.length, timespan.unit, count, scheduler)
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
   *         An [[Observable]] which produces new buffers periodically, and these are emitted after
   *         a fixed timespan has elapsed.
   */
  def buffer(timespan: Duration, timeshift: Duration): Observable[Seq[T]] = {
    val span: Long = timespan.length
    val shift: Long = timespan.unit.convert(timeshift.length, timeshift.unit)
    val unit: TimeUnit = timespan.unit
    val oJava: rx.Observable[_ <: java.util.List[_]] = asJava.buffer(span, shift, unit)
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
   *            The [[Scheduler]] to use when determining the end and start of a buffer.
   * @return
   *         An [[Observable]] which produces new buffers periodically, and these are emitted after
   *         a fixed timespan has elapsed.
   */
  def buffer(timespan: Duration, timeshift: Duration, scheduler: Scheduler): Observable[Seq[T]] = {
    val span: Long = timespan.length
    val shift: Long = timespan.unit.convert(timeshift.length, timeshift.unit)
    val unit: TimeUnit = timespan.unit
    val oJava: rx.Observable[_ <: java.util.List[_]] = asJava.buffer(span, shift, unit, scheduler)
    Observable.jObsOfListToScObsOfSeq(oJava.asInstanceOf[rx.Observable[_ <: java.util.List[T]]])
  } 

  /**
   * Creates an Observable which produces windows of collected values. This Observable produces connected
   * non-overlapping windows. The current window is emitted and replaced with a new window when the
   * Observable produced by the specified function produces a [[rx.lang.scala.util.Closing]] object. 
   * The function will then be used to create a new Observable to listen for the end of the next
   * window.
   * 
   * @param closings
   *            The function which is used to produce an [[Observable]] for every window created.
   *            When this [[Observable]] produces a [[rx.lang.scala.util.Closing]] object, the associated window
   *            is emitted and replaced with a new one.
   * @return
   *         An [[Observable]] which produces connected non-overlapping windows, which are emitted
   *         when the current [[Observable]] created with the function argument produces a [[rx.lang.scala.util.Closing]] object.
   */
  def window(closings: () => Observable[Closing]): Observable[Observable[T]] = {
    val func : Func0[_ <: rx.Observable[_ <: Closing]] = closings().asJava
    val o1: rx.Observable[_ <: rx.Observable[_]] = asJava.window(func)
    val o2 = new Observable[rx.Observable[_]](o1).map((x: rx.Observable[_]) => {
      val x2 = x.asInstanceOf[rx.Observable[_ <: T]]
      Observable[T](x2)
    })
    o2
  }

  /**
   * Creates an Observable which produces windows of collected values. This Observable produces windows.
   * Chunks are created when the specified `openings` Observable produces a [[rx.lang.scala.util.Opening]] object.
   * Additionally the `closings` argument is used to create an Observable which produces [[rx.lang.scala.util.Closing]] objects. 
   * When this Observable produces such an object, the associated window is emitted.
   * 
   * @param openings
   *            The [[Observable]] which when it produces a [[rx.lang.scala.util.Opening]] object, will cause
   *            another window to be created.
   * @param closings
   *            The function which is used to produce an [[Observable]] for every window created.
   *            When this [[Observable]] produces a [[rx.lang.scala.util.Closing]] object, the associated window
   *            is emitted.
   * @return
   *         An [[Observable]] which produces windows which are created and emitted when the specified [[Observable]]s publish certain objects.
   */
  def window(openings: Observable[Opening], closings: Opening => Observable[Closing]) = {
    Observable.jObsOfJObsToScObsOfScObs(
        asJava.window(openings.asJava, (op: Opening) => closings(op).asJava))
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
   *         An [[Observable]] which produces connected non-overlapping windows containing at most
   *         `count` produced values.
   */
  def window(count: Int): Observable[Observable[T]] = {
    // this unnecessary ascription is needed because of this bug (without, compiler crashes):
    // https://issues.scala-lang.org/browse/SI-7818
    Observable.jObsOfJObsToScObsOfScObs(asJava.window(count)) : Observable[Observable[T]]
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
   *         An [[Observable]] which produces windows every `skip` values containing at most
   *         `count` produced values.
   */
  def window(count: Int, skip: Int): Observable[Observable[T]] = {
    Observable.jObsOfJObsToScObsOfScObs(asJava.window(count, skip))
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
   *         An [[Observable]] which produces connected non-overlapping windows with a fixed duration.
   */
  def window(timespan: Duration): Observable[Observable[T]] = {
    Observable.jObsOfJObsToScObsOfScObs(asJava.window(timespan.length, timespan.unit))
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
   *            The [[Scheduler]] to use when determining the end and start of a window.
   * @return
   *         An [[Observable]] which produces connected non-overlapping windows with a fixed duration.
   */
  def window(timespan: Duration, scheduler: Scheduler): Observable[Observable[T]] = {
    Observable.jObsOfJObsToScObsOfScObs(asJava.window(timespan.length, timespan.unit, scheduler))
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
   *         An [[Observable]] which produces connected non-overlapping windows which are emitted after
   *         a fixed duration or when the window has reached maximum capacity (which ever occurs first).
   */
  def window(timespan: Duration, count: Int): Observable[Observable[T]] = {
    Observable.jObsOfJObsToScObsOfScObs(asJava.window(timespan.length, timespan.unit, count))
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
   *            The [[Scheduler]] to use when determining the end and start of a window.
   * @return
   *         An [[Observable]] which produces connected non-overlapping windows which are emitted after
   *         a fixed duration or when the window has reached maximum capacity (which ever occurs first).
   */
  def window(timespan: Duration, count: Int, scheduler: Scheduler): Observable[Observable[T]] = {
    Observable.jObsOfJObsToScObsOfScObs(asJava.window(timespan.length, timespan.unit, count, scheduler))
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
   *         An [[Observable]] which produces new windows periodically, and these are emitted after
   *         a fixed timespan has elapsed.
   */
  def window(timespan: Duration, timeshift: Duration): Observable[Observable[T]] = {
    val span: Long = timespan.length
    val shift: Long = timespan.unit.convert(timeshift.length, timeshift.unit)
    val unit: TimeUnit = timespan.unit
    Observable.jObsOfJObsToScObsOfScObs(asJava.window(span, shift, unit))
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
   *            The [[Scheduler]] to use when determining the end and start of a window.
   * @return
   *         An [[Observable]] which produces new windows periodically, and these are emitted after
   *         a fixed timespan has elapsed.
   */
  def window(timespan: Duration, timeshift: Duration, scheduler: Scheduler): Observable[Observable[T]] = {
    val span: Long = timespan.length
    val shift: Long = timespan.unit.convert(timeshift.length, timeshift.unit)
    val unit: TimeUnit = timespan.unit
    Observable.jObsOfJObsToScObsOfScObs(asJava.window(span, shift, unit, scheduler))
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
    Observable[T](asJava.filter(predicate))
  }

  /**
   * Registers an function to be called when this Observable invokes [[Observer.onCompleted onCompleted]] or [[Observer.onError onError]].
   *
   * <img width="640" src="https://github.com/Netflix/RxJava/wiki/images/rx-operators/finallyDo.png">
   * 
   * @param action
   *            an function to be invoked when the source Observable finishes
   * @return an Observable that emits the same items as the source Observable, then invokes the function
   */
  def finallyDo(action: () => Unit): Observable[T] = {
    Observable[T](asJava.finallyDo(action))
  } 

  /**
   * Creates a new Observable by applying a function that you supply to each item emitted by
   * the source Observable, where that function returns an Observable, and then merging those
   * resulting Observables and emitting the results of this merger.
   *
   * <img width="640" src="https://raw.github.com/wiki/Netflix/RxJava/images/rx-operators/flatMap.png">
   * 
   * @param func
   *            a function that, when applied to an item emitted by the source Observable, returns
   *            an Observable
   * @return an Observable that emits the result of applying the transformation function to each
   *         item emitted by the source Observable and merging the results of the Observables
   *         obtained from this transformation.
   */
  def flatMap[R](f: T => Observable[R]): Observable[R] = {
    Observable[R](asJava.flatMap[R]((t: T) => f(t).asJava))
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
    Observable[R](asJava.map[R](func))
  }
  
  /**
   * Turns all of the notifications from a source Observable into [[Observer.onNext onNext]] emissions, and marks them with their original notification types within [[Notification]] objects.
   *
   * <img width="640" src="https://raw.github.com/wiki/Netflix/RxJava/images/rx-operators/materialize.png">
   * 
   * @return an Observable whose items are the result of materializing the items and
   *         notifications of the source Observable
   */
  def materialize: Observable[Notification[T]] = {
    Observable[rx.Notification[_ <: T]](asJava.materialize()).map(Notification(_))
  }

  /**
   * Asynchronously subscribes and unsubscribes Observers on the specified [[Scheduler]].
   *
   * <img width="640" src="https://github.com/Netflix/RxJava/wiki/images/rx-operators/subscribeOn.png">
   * 
   * @param scheduler
   *            the [[Scheduler]] to perform subscription and unsubscription actions on
   * @return the source Observable modified so that its subscriptions and unsubscriptions happen
   *         on the specified [[Scheduler]]
   */
  def subscribeOn(scheduler: Scheduler): Observable[T] = {
    Observable[T](asJava.subscribeOn(scheduler))
  } 

  /**
   * Asynchronously notify [[Observer]]s on the specified [[Scheduler]].
   *
   * <img width="640" src="https://github.com/Netflix/RxJava/wiki/images/rx-operators/observeOn.png">
   * 
   * @param scheduler
   *            the [[Scheduler]] to notify [[Observer]]s on
   * @return the source Observable modified so that its [[Observer]]s are notified on the
   *         specified [[Scheduler]]
   */
  def observeOn(scheduler: Scheduler): Observable[T] = {
    Observable[T](asJava.observeOn(scheduler))
  }
 
  /**
   * Returns an Observable that reverses the effect of [[Observable.materialize]] by
   * transforming the [[Notification]] objects emitted by the source Observable into the items
   * or notifications they represent.
   * 
   * This operation is only available if `this` is of type `Observable[Notification[U]]` for some `U`, 
   * otherwise you will get a compilation error.
   *
   * <img width="640" src="https://github.com/Netflix/RxJava/wiki/images/rx-operators/dematerialize.png">
   * 
   * @return an Observable that emits the items and notifications embedded in the [[Notification]] objects emitted by the source Observable
   * 
   * @usecase def dematerialize[U]: Observable[U]
   *   @inheritdoc
   *   
   */
  // with =:= it does not work, why?
  def dematerialize[U](implicit evidence: Observable[T] <:< Observable[Notification[U]]): Observable[U] = {
    val o1: Observable[Notification[U]] = this
    val o2: Observable[rx.Notification[_ <: U]] = o1.map(_.asJava)
    val o3 = o2.asJava.dematerialize[U]()
    Observable[U](o3)
  }
  
  /**
   * Instruct an Observable to pass control to another Observable rather than invoking [[Observer.onError onError]] if it encounters an error.
   *
   * <img width="640" src="https://raw.github.com/wiki/Netflix/RxJava/images/rx-operators/onErrorResumeNext.png">
   *
   * By default, when an Observable encounters an error that prevents it from emitting the
   * expected item to its [[Observer]], the Observable invokes its Observer's
   * `onError` method, and then quits without invoking any more of its Observer's
   * methods. The `onErrorResumeNext` method changes this behavior. If you pass a
   * function that returns an Observable (`resumeFunction`) to
   * `onErrorResumeNext`, if the original Observable encounters an error, instead of
   * invoking its Observer's `onError` method, it will instead relinquish control to
   * the Observable returned from `resumeFunction`, which will invoke the Observer's 
   * [[Observer.onNext onNext]] method if it is able to do so. In such a case, because no
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
    val f: Func1[Throwable, rx.Observable[_ <: U]] = (t: Throwable) => resumeFunction(t).asJava
    val f2 = f.asInstanceOf[Func1[Throwable, rx.Observable[Nothing]]]
    Observable[U](asJava.onErrorResumeNext(f2))
  }
  
  /**
   * Instruct an Observable to pass control to another Observable rather than invoking [[Observer.onError onError]] if it encounters an error.
   *
   * <img width="640" src="https://raw.github.com/wiki/Netflix/RxJava/images/rx-operators/onErrorResumeNext.png">
   *
   * By default, when an Observable encounters an error that prevents it from emitting the
   * expected item to its [[Observer]], the Observable invokes its Observer's
   * `onError` method, and then quits without invoking any more of its Observer's
   * methods. The `onErrorResumeNext` method changes this behavior. If you pass
   * another Observable (`resumeSequence`) to an Observable's
   * `onErrorResumeNext` method, if the original Observable encounters an error,
   * instead of invoking its Observer's `onError` method, it will instead relinquish
   * control to `resumeSequence` which will invoke the Observer's [[Observer.onNext onNext]] 
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
    val rSeq1: rx.Observable[_ <: U] = resumeSequence.asJava
    val rSeq2: rx.Observable[Nothing] = rSeq1.asInstanceOf[rx.Observable[Nothing]]
    Observable[U](asJava.onErrorResumeNext(rSeq2))
  }

  /**
   * Instruct an Observable to pass control to another Observable rather than invoking [[Observer.onError onError]] if it encounters an error of type `java.lang.Exception`.
   *
   * This differs from `Observable.onErrorResumeNext` in that this one does not handle `java.lang.Throwable` or `java.lang.Error` but lets those continue through.
   *
   * <img width="640" src="https://raw.github.com/wiki/Netflix/RxJava/images/rx-operators/onErrorResumeNext.png">
   *
   * By default, when an Observable encounters an error that prevents it from emitting the
   * expected item to its [[Observer]], the Observable invokes its Observer's
   * `onError` method, and then quits without invoking any more of its Observer's
   * methods. The `onErrorResumeNext` method changes this behavior. If you pass
   * another Observable (`resumeSequence`) to an Observable's
   * `onErrorResumeNext` method, if the original Observable encounters an error,
   * instead of invoking its Observer's `onError` method, it will instead relinquish
   * control to `resumeSequence` which will invoke the Observer's [[Observer.onNext onNext]] 
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
    val rSeq1: rx.Observable[_ <: U] = resumeSequence.asJava
    val rSeq2: rx.Observable[Nothing] = rSeq1.asInstanceOf[rx.Observable[Nothing]]
    Observable[U](asJava.onExceptionResumeNext(rSeq2))
  }

  /**
   * Instruct an Observable to emit an item (returned by a specified function) rather than
   * invoking [[Observer.onError onError]] if it encounters an error.
   *
   * <img width="640" src="https://github.com/Netflix/RxJava/wiki/images/rx-operators/onErrorReturn.png">
   *
   * By default, when an Observable encounters an error that prevents it from emitting the
   * expected item to its [[Observer]], the Observable invokes its Observer's
   * `onError` method, and then quits without invoking any more of its Observer's
   * methods. The `onErrorReturn` method changes this behavior. If you pass a function
   * (`resumeFunction`) to an Observable's `onErrorReturn` method, if the
   * original Observable encounters an error, instead of invoking its Observer's
   * `onError` method, it will instead pass the return value of
   * `resumeFunction` to the Observer's [[Observer.onNext onNext]] method.
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
    Observable[U](asJava.onErrorReturn(f2))
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
  def reduce[U >: T](f: (U, U) => U): Observable[U] = {
    val func: Func2[_ >: U, _ >: U, _ <: U] = f
    val func2 = func.asInstanceOf[Func2[T, T, T]]
    Observable[U](asJava.asInstanceOf[rx.Observable[T]].reduce(func2))
  } 

  /**
   * Returns a pair of a start function and an [[Observable]] that shares a single subscription to the underlying
   * Observable that will replay all of its items and notifications to any future [[Observer]].
   *
   * <img width="640" src="https://raw.github.com/wiki/Netflix/RxJava/images/rx-operators/replay.png">
   * 
   * @return a pair of a start function and an [[Observable]] such that when the start function
   *         is called, the Observable starts to emit items to its [[Observer]]s
   */
  def replay: (() => Subscription, Observable[T]) = {
    val javaCO = asJava.replay()
    (() => javaCO.connect(), Observable[T](javaCO))
  }

  /**
   * This method has similar behavior to [[Observable.replay]] except that this auto-subscribes to
   * the source Observable rather than returning a start function and an Observable.
   *
   * <img width="640" src="https://github.com/Netflix/RxJava/wiki/images/rx-operators/cache.png">
   *
   * This is useful when you want an Observable to cache responses and you can't control the
   * subscribe/unsubscribe behavior of all the [[Observer]]s.
   *
   * NOTE: You sacrifice the ability to unsubscribe from the origin when you use the
   * `cache()` operator so be careful not to use this operator on Observables that
   * emit an infinite or very large number of items that will use up memory.
   * 
   * @return an Observable that when first subscribed to, caches all of its notifications for
   *         the benefit of subsequent subscribers.
   */
  def cache: Observable[T] = {
    Observable[T](asJava.cache())
  }

  /**
   * Returns a a pair of a start function and an [[Observable]], which waits until the start function is called before it begins emitting
   * items to those [[Observer]]s that have subscribed to it.
   *
   * <img width="640" src="https://github.com/Netflix/RxJava/wiki/images/rx-operators/publishConnect.png">
   * 
   * @return a pair of a start function and an [[Observable]] such that when the start function
   *         is called, the Observable starts to emit items to its [[Observer]]s
   */
  def publish: (() => Subscription, Observable[T]) = {
    val javaCO = asJava.publish()
    (() => javaCO.connect(), Observable[T](javaCO))
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
    Observable[R](asJava.reduce(initialValue, accumulator))
  }
  
  /**
   * Returns an Observable that emits the results of sampling the items emitted by the source
   * Observable at a specified time interval.
   *
   * <img width="640" src="https://github.com/Netflix/RxJava/wiki/images/rx-operators/sample.png">
   * 
   * @param period
   *            the sampling rate
   * @param unit
   *            the [[TimeUnit]] in which `period` is defined
   * @return an Observable that emits the results of sampling the items emitted by the source
   *         Observable at the specified time interval
   */
  def sample(duration: Duration): Observable[T] = {
    Observable[T](asJava.sample(duration.length, duration.unit))
  }

  /**
   * Returns an Observable that emits the results of sampling the items emitted by the source
   * Observable at a specified time interval.
   *
   * <img width="640" src="https://github.com/Netflix/RxJava/wiki/images/rx-operators/sample.png">
   * 
   * @param period
   *            the sampling rate
   * @param unit
   *            the [[TimeUnit]] in which `period` is defined
   * @param scheduler
   *            the [[Scheduler]] to use when sampling
   * @return an Observable that emits the results of sampling the items emitted by the source
   *         Observable at the specified time interval
   */
  def sample(duration: Duration, scheduler: Scheduler): Observable[T] = {
    Observable[T](asJava.sample(duration.length, duration.unit, scheduler))
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
   *            Observable, whose result will be emitted to [[Observer]]s via [[Observer.onNext onNext]] and used in the next accumulator call.
   * @return an Observable that emits the results of each call to the accumulator function
   */
  def scan[R](initialValue: R)(accumulator: (R, T) => R): Observable[R] = {
    Observable[R](asJava.scan(initialValue, accumulator))
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
    // new Observable[Boolean](asJava.all(predicate))
    // it's more fun in Scala:
    this.map(predicate).foldLeft(true)(_ && _)
  }
    
  /**
   * Returns an Observable that skips the first `num` items emitted by the source
   * Observable and emits the remainder.
   *
   * <img width="640" src="https://raw.github.com/wiki/Netflix/RxJava/images/rx-operators/skip.png">
   *
   * @param num
   *            the number of items to skip
   * @return an Observable that is identical to the source Observable except that it does not
   *         emit the first `num` items that the source emits
   */
  def drop(n: Int): Observable[T] = {
    Observable[T](asJava.skip(n))
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
    Observable[T](asJava.skipWhile(predicate))
  }

  /**
   * Returns an Observable that emits only the first `num` items emitted by the source
   * Observable.
   *
   * <img width="640" src="https://raw.github.com/wiki/Netflix/RxJava/images/rx-operators/take.png">
   *
   * This method returns an Observable that will invoke a subscribing [[Observer]]'s 
   * [[Observer.onNext onNext]] function a maximum of `num` times before invoking
   * [[Observer.onCompleted onCompleted]].
   * 
   * @param num
   *            the number of items to take
   * @return an Observable that emits only the first `num` items from the source
   *         Observable, or all of the items from the source Observable if that Observable emits
   *         fewer than `num` items
   */
  def take(n: Int): Observable[T] = {
    Observable[T](asJava.take(n))
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
    Observable[T](asJava.takeWhile(predicate))
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
  def takeRight(n: Int): Observable[T] = {
    Observable[T](asJava.takeLast(n))
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
    Observable[T](asJava.takeUntil(that.asJava))
  } 
 
  /**
   * Returns an Observable that emits a single item, a list composed of all the items emitted by
   * the source Observable.
   *
   * <img width="640" src="https://raw.github.com/wiki/Netflix/RxJava/images/rx-operators/toList.png">
   *
   * Normally, an Observable that returns multiple items will do so by invoking its [[Observer]]'s 
   * [[Observer.onNext onNext]] method for each such item. You can change
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
    Observable.jObsOfListToScObsOfSeq(asJava.toList())
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
    val o1 = asJava.groupBy[K](f) : rx.Observable[_ <: rx.observables.GroupedObservable[K, _ <: T]] 
    val func = (o: rx.observables.GroupedObservable[K, _ <: T]) => (o.getKey(), Observable[T](o))
    Observable[(K, Observable[T])](o1.map[(K, Observable[T])](func))
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
   * @param sequenceOfSequences
   *            the source Observable that emits Observables
   * @return an Observable that emits only the items emitted by the most recently published
   *         Observable
   * 
   * @usecase def switch[U]: Observable[U]
   *   @inheritdoc 
   */
  def switch[U](implicit evidence: Observable[T] <:< Observable[Observable[U]]): Observable[U] = {
    val o2: Observable[Observable[U]] = this
    val o3: Observable[rx.Observable[_ <: U]] = o2.map(_.asJava)
    val o4: rx.Observable[_ <: rx.Observable[_ <: U]] = o3.asJava
    val o5 = rx.Observable.switchOnNext[U](o4)
    Observable[U](o5)
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
    val thisJava: rx.Observable[_ <: U] = this.asJava
    val thatJava: rx.Observable[_ <: U] = that.asJava
    Observable[U](rx.Observable.merge(thisJava, thatJava))
  }

  /**
   * This behaves like [[Observable.merge]] except that if any of the merged Observables
   * notify of an error via [[Observer.onError onError]], `mergeDelayError` will
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
    Observable[U](rx.Observable.mergeDelayError[U](this.asJava, that.asJava))
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
    val o3: Observable[rx.Observable[_ <: U]] = o2.map(_.asJava)
    val o4: rx.Observable[_ <: rx.Observable[_ <: U]] = o3.asJava
    val o5 = rx.Observable.merge[U](o4)
    Observable[U](o5)
  }

  /**
   * This behaves like `flatten` except that if any of the merged Observables
   * notify of an error via [[Observer.onError onError]], this method will
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
    val o3: Observable[rx.Observable[_ <: U]] = o2.map(_.asJava)
    val o4: rx.Observable[_ <: rx.Observable[_ <: U]] = o3.asJava
    val o5 = rx.Observable.mergeDelayError[U](o4)
    Observable[U](o5)
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
    Observable[(T, U)](rx.Observable.combineLatest[T, U, (T, U)](this.asJava, that.asJava, f))
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
   *            The time each value has to be 'the most recent' of the [[Observable]] to ensure that it's not dropped.
   *
   * @return An [[Observable]] which filters out values which are too quickly followed up with newer values.
   * @see `Observable.debounce`
   */
  def throttleWithTimeout(timeout: Duration): Observable[T] = {
    Observable[T](asJava.throttleWithTimeout(timeout.length, timeout.unit))
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
   *            The time each value has to be 'the most recent' of the [[Observable]] to ensure that it's not dropped.
   *
   * @return An [[Observable]] which filters out values which are too quickly followed up with newer values.
   * @see `Observable.throttleWithTimeout`
   */
  def debounce(timeout: Duration): Observable[T] = {
    Observable[T](asJava.debounce(timeout.length, timeout.unit))
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
   *            The time each value has to be 'the most recent' of the [[Observable]] to ensure that it's not dropped.
   * @param scheduler
   *            The [[Scheduler]] to use internally to manage the timers which handle timeout for each event.
   * @return Observable which performs the throttle operation.
   * @see `Observable.throttleWithTimeout`
   */
  def debounce(timeout: Duration, scheduler: Scheduler): Observable[T] = {
    Observable[T](asJava.debounce(timeout.length, timeout.unit, scheduler))
  }

  /**
   * Debounces by dropping all values that are followed by newer values before the timeout value expires. The timer resets on each `onNext` call.
   *
   * NOTE: If events keep firing faster than the timeout then no data will be emitted.
   *
   * <img width="640" src="https://github.com/Netflix/RxJava/wiki/images/rx-operators/throttleWithTimeout.png">
   *
   * @param timeout
   *            The time each value has to be 'the most recent' of the [[Observable]] to ensure that it's not dropped.
   * @param scheduler
   *            The [[Scheduler]] to use internally to manage the timers which handle timeout for each event.
   * @return Observable which performs the throttle operation.
   * @see `Observable.debounce`
   */
  def throttleWithTimeout(timeout: Duration, scheduler: Scheduler): Observable[T] = {
    Observable[T](asJava.throttleWithTimeout(timeout.length, timeout.unit, scheduler))
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
   *            The [[Scheduler]] to use internally to manage the timers which handle timeout for each event.
   * @return Observable which performs the throttle operation.
   */
  def throttleFirst(skipDuration: Duration, scheduler: Scheduler): Observable[T] = {
    Observable[T](asJava.throttleFirst(skipDuration.length, skipDuration.unit, scheduler))
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
    Observable[T](asJava.throttleFirst(skipDuration.length, skipDuration.unit))
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
    Observable[T](asJava.throttleLast(intervalDuration.length, intervalDuration.unit))
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
    Observable[T](asJava.throttleLast(intervalDuration.length, intervalDuration.unit, scheduler))
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
   * Returns an Observable that emits only the very first item emitted by the source Observable.
   * This is just a shorthand for `take(1)`.
   *
   * <img width="640" src="https://raw.github.com/wiki/Netflix/RxJava/images/rx-operators/first.png">
   *
   * @return an Observable that emits only the very first item from the source, or none if the
   *         source Observable completes without emitting a single item.
   */
  def first: Observable[T] = take(1)
  
  /*
  
  TODO once https://github.com/Netflix/RxJava/issues/417 is fixed, we can add head and tail methods
  
  /**
   * emits NoSuchElementException("head of empty Observable") if empty
   */
  def head: Observable[T] = {
    this.take(1).fold[Option[T]](None)((v: Option[T], e: T) => Some(e)).map({
      case Some(element) => element
      case None => throw new NoSuchElementException("head of empty Observable")
    })
  }
  
  /**
   * emits an UnsupportedOperationException("tail of empty list") if empty
   */
  def tail: Observable[T] = ???
  
  */
  
  /**
   * Returns an Observable that forwards all sequentially distinct items emitted from the source Observable.
   *
   * <img width="640" src="https://raw.github.com/wiki/Netflix/RxJava/images/rx-operators/distinctUntilChanged.png">
   *
   * @return an Observable of sequentially distinct items
   */
  def distinctUntilChanged: Observable[T] = {
    Observable[T](asJava.distinctUntilChanged)
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
    Observable[T](asJava.distinctUntilChanged[U](keySelector))
  }

  /**
   * Returns an Observable that forwards all distinct items emitted from the source Observable.
   *
   * <img width="640" src="https://raw.github.com/wiki/Netflix/RxJava/images/rx-operators/distinct.png">
   *
   * @return an Observable of distinct items
   */
  def distinct: Observable[T] = {
    Observable[T](asJava.distinct())
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
    Observable[T](asJava.distinct[U](keySelector))
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
    Observable[Integer](asJava.count()).map(_.intValue())
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
   * If [[Observer.onError]] is invoked the source Observable will be re-subscribed to as many times as defined by retryCount.
   *
   * Any [[Observer.onNext]] calls received on each attempt will be emitted and concatenated together.
   *
   * For example, if an Observable fails on first time but emits [1, 2] then succeeds the second time and
   * emits [1, 2, 3, 4, 5] then the complete output would be [1, 2, 1, 2, 3, 4, 5, onCompleted].
   *
   * @param retryCount
   *            Number of retry attempts before failing.
   * @return Observable with retry logic.
   */
  def retry(retryCount: Int): Observable[T] = {
    Observable[T](asJava.retry(retryCount))
  }

  /**
   * Retry subscription to origin Observable whenever onError is called (infinite retry count).
   *
   * <img width="640" src="https://raw.github.com/wiki/Netflix/RxJava/images/rx-operators/retry.png">
   *
   * If [[Observer.onError]] is invoked the source Observable will be re-subscribed to.
   *
   * Any [[Observer.onNext]] calls received on each attempt will be emitted and concatenated together.
   *
   * For example, if an Observable fails on first time but emits [1, 2] then succeeds the second time and
   * emits [1, 2, 3, 4, 5] then the complete output would be [1, 2, 1, 2, 3, 4, 5, onCompleted].
   * @return Observable with retry logic.
   */
  def retry: Observable[T] = {
    Observable[T](asJava.retry())
  }
  
  /**
   * Converts an Observable into a [[rx.lang.scala.observables.BlockingObservable]] (an Observable with blocking
   * operators).
   *
   * @see <a href="https://github.com/Netflix/RxJava/wiki/Blocking-Observable-Operators">Blocking Observable Operators</a>
   */
  def toBlockingObservable: BlockingObservable[T] = {
    new BlockingObservable[T](asJava.toBlockingObservable())
  }

  /**
   * Perform work in parallel by sharding an `Observable[T]` on a 
   * [[rx.lang.scala.concurrency.Schedulers.threadPoolForComputation computation]] 
   * [[Scheduler]] and return an `Observable[R]` with the output.
   *
   * @param f
   *            a function that applies Observable operators to `Observable[T]` in parallel and returns an `Observable[R]`
   * @return an Observable with the output of the function executed on a [[Scheduler]]
   */
  def parallel[R](f: Observable[T] => Observable[R]): Observable[R] = {
    val fJava: Func1[rx.Observable[T], rx.Observable[R]] =
      (jo: rx.Observable[T]) => f(Observable[T](jo)).asJava.asInstanceOf[rx.Observable[R]]
    Observable[R](asJava.asInstanceOf[rx.Observable[T]].parallel[R](fJava))
  }

  /**
   * Perform work in parallel by sharding an `Observable[T]` on a [[Scheduler]] and return an `Observable[R]` with the output.
   *
   * @param f
   *            a function that applies Observable operators to `Observable[T]` in parallel and returns an `Observable[R]`
   * @param s
   *            a [[Scheduler]] to perform the work on.
   * @return an Observable with the output of the function executed on a [[Scheduler]]
   */
  def parallel[R](f: Observable[T] => Observable[R], scheduler: Scheduler): Observable[R] = {
    val fJava: Func1[rx.Observable[T], rx.Observable[R]] =
      (jo: rx.Observable[T]) => f(Observable[T](jo)).asJava.asInstanceOf[rx.Observable[R]]
    Observable[R](asJava.asInstanceOf[rx.Observable[T]].parallel[R](fJava, scheduler))
  }

  /** Tests whether a predicate holds for some of the elements of this `Observable`.
   *
   *  @param   p     the predicate used to test elements.
   *  @return        an Observable emitting one single Boolean, which is `true` if the given predicate `p` 
   *                 holds for some of the elements of this Observable, and `false` otherwise.
   */
  def exists(p: T => Boolean): Observable[Boolean] = {
    Observable[java.lang.Boolean](asJava.exists(p)).map(_.booleanValue())
  }

  /** Tests whether this `Observable` emits no elements.
   *
   *  @return        an Observable emitting one single Boolean, which is `true` if this `Observable` 
   *                 emits no elements, and `false` otherwise.
   */
  def isEmpty: Observable[Boolean] = {
    Observable[java.lang.Boolean](asJava.isEmpty).map(_.booleanValue())
  }
  
  def withFilter(p: T => Boolean): WithFilter[T] = {
    new WithFilter[T](p, asJava)
  }
  
}

/**
 * Provides various ways to construct new Observables.
 */
object Observable {
  import scala.collection.JavaConverters._
  import scala.collection.immutable.Range
  import scala.concurrent.duration.Duration
  import rx.{Observable => JObservable}
  import rx.lang.scala.util._
  import rx.util.functions._
  import rx.lang.scala.ImplicitFunctionConversions._
 
  private[scala] 
  def jObsOfListToScObsOfSeq[T](jObs: rx.Observable[_ <: java.util.List[T]]): Observable[Seq[T]] = {
    val oScala1: Observable[java.util.List[T]] = new Observable[java.util.List[T]](jObs)
    oScala1.map((lJava: java.util.List[T]) => lJava.asScala)
  }
  
  private[scala] 
  def jObsOfJObsToScObsOfScObs[T](jObs: rx.Observable[_ <: rx.Observable[_ <: T]]): Observable[Observable[T]] = {
    val oScala1: Observable[rx.Observable[_ <: T]] = new Observable[rx.Observable[_ <: T]](jObs)
    oScala1.map((oJava: rx.Observable[_ <: T]) => new Observable[T](oJava))
  }
  
  /**
   * Creates a new Scala Observable from a given Java Observable.
   */
  def apply[T](asJava: rx.Observable[_ <: T]): Observable[T] = {
    new Observable[T](asJava)
  }
  
  /**
   * Creates an Observable that will execute the given function when an [[Observer]] subscribes to it.
   *
   * <img width="640" src="https://github.com/Netflix/RxJava/wiki/images/rx-operators/create.png">
   *
   * Write the function you pass to `create` so that it behaves as an Observable: It
   * should invoke the Observer's [[Observer.onNext onNext]], [[Observer.onError onError]], and [[Observer.onCompleted onCompleted]] methods
   * appropriately.
   *
   * A well-formed Observable must invoke either the Observer's `onCompleted` method
   * exactly once or its `onError` method exactly once.
   *
   * See <a href="http://go.microsoft.com/fwlink/?LinkID=205219">Rx Design Guidelines (PDF)</a>
   * for detailed information.
   * 
   * 
   * @tparam T
   *            the type of the items that this Observable emits
   * @param func
   *            a function that accepts an `Observer[T]`, invokes its `onNext`, `onError`, and `onCompleted` methods
   *            as appropriate, and returns a [[Subscription]] to allow the Observer to
   *            canceling the subscription
   * @return an Observable that, when an [[Observer]] subscribes to it, will execute the given
   *         function
   */
  def apply[T](func: Observer[T] => Subscription): Observable[T] = {
    Observable[T](JObservable.create(func))
  }
  
  /**
   * Returns an Observable that invokes an [[Observer]]'s [[Observer.onError onError]] method when the Observer subscribes to it
   *
   * <img width="640" src="https://raw.github.com/wiki/Netflix/RxJava/images/rx-operators/error.png">
   * 
   * @param exception
   *            the particular error to report
   * @tparam T
   *            the type of the items (ostensibly) emitted by the Observable
   * @return an Observable that invokes the [[Observer]]'s [[Observer.onError onError]] method when the Observer subscribes to it
   */
  def apply[T](exception: Throwable): Observable[T] = {
    Observable[T](JObservable.error(exception))
  }

  /**
   * Converts a sequence of values into an Observable.
   *
   * <img width="640" src="https://github.com/Netflix/RxJava/wiki/images/rx-operators/from.png">
   * 
   * Implementation note: the entire array will be immediately emitted each time an [[Observer]] subscribes. 
   * Since this occurs before the [[Subscription]] is returned,
   * it in not possible to unsubscribe from the sequence before it completes.
   * 
   * @param items
   *            the source Array
   * @tparam T
   *            the type of items in the Array, and the type of items to be emitted by the
   *            resulting Observable
   * @return an Observable that emits each item in the source Array
   */
  def apply[T](args: T*): Observable[T] = {     
    Observable[T](JObservable.from(args.toIterable.asJava))
  }

  /**
   * Generates an Observable that emits a sequence of integers within a specified range.
   * 
   * <img width="640" src="https://github.com/Netflix/RxJava/wiki/images/rx-operators/range.png">
   *
   * Implementation note: the entire range will be immediately emitted each time an [[Observer]] subscribes. 
   * Since this occurs before the [[Subscription]] is returned,
   * it in not possible to unsubscribe from the sequence before it completes.
   *
   * @param range the range
   * @return an Observable that emits a range of sequential integers
   */
  def apply(range: Range): Observable[Int] = {
    Observable[Int](JObservable.from(range.toIterable.asJava))
  }
  
  /**
   * Returns an Observable that calls an Observable factory to create its Observable for each
   * new Observer that subscribes. That is, for each subscriber, the actual Observable is determined
   * by the factory function.
   * 
   * <img width="640" src="https://github.com/Netflix/RxJava/wiki/images/rx-operators/defer.png">
   *
   * The defer operator allows you to defer or delay emitting items from an Observable until such
   * time as an Observer subscribes to the Observable. This allows an [[Observer]] to easily
   * obtain updates or a refreshed version of the sequence.
   * 
   * @param observableFactory
   *            the Observable factory function to invoke for each [[Observer]] that
   *            subscribes to the resulting Observable
   * @tparam T
   *            the type of the items emitted by the Observable
   * @return an Observable whose [[Observer]]s trigger an invocation of the given Observable
   *         factory function
   */
  def defer[T](observable: => Observable[T]): Observable[T] = {
    Observable[T](JObservable.defer[T](() => observable.asJava))
  }
  
  /**
   * Returns an Observable that never sends any items or notifications to an [[Observer]].
   *
   * <img width="640" src="https://github.com/Netflix/RxJava/wiki/images/rx-operators/never.png">
   *
   * This Observable is useful primarily for testing purposes.
   * 
   * @return an Observable that never sends any items or notifications to an [[Observer]]
   */
  def never: Observable[Nothing] = {
    Observable[Nothing](JObservable.never())
  }

  /**
   * Given 3 observables, returns an observable that emits Tuples of 3 elements each.
   * The first emitted Tuple will contain the first element of each source observable,
   * the second Tuple the second element of each source observable, and so on.
   * 
   * @return an Observable that emits the zipped Observables
   */
  def zip[A, B, C](obA: Observable[A], obB: Observable[B], obC: Observable[C]): Observable[(A, B, C)] = {
    Observable[(A, B, C)](rx.Observable.zip[A, B, C, (A, B, C)](obA.asJava, obB.asJava, obC.asJava, (a: A, b: B, c: C) => (a, b, c)))
  }
  
  /**
   * Given 4 observables, returns an observable that emits Tuples of 4 elements each.
   * The first emitted Tuple will contain the first element of each source observable,
   * the second Tuple the second element of each source observable, and so on.
   * 
   * @return an Observable that emits the zipped Observables
   */
  def zip[A, B, C, D](obA: Observable[A], obB: Observable[B], obC: Observable[C], obD: Observable[D]): Observable[(A, B, C, D)] = {
    Observable[(A, B, C, D)](rx.Observable.zip[A, B, C, D, (A, B, C, D)](obA.asJava, obB.asJava, obC.asJava, obD.asJava, (a: A, b: B, c: C, d: D) => (a, b, c, d)))
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
    val list = observables.map(_.asJava).asJava
    val o = rx.Observable.zip(list, f)
    Observable[Seq[T]](o)
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
    (new Observable[java.lang.Long](JObservable.interval(duration.length, duration.unit))).map(_.longValue())
  }

  /**
   * Emits `0`, `1`, `2`, `...` with a delay of `duration` between consecutive numbers.
   * 
   * <img width="640" src="https://github.com/Netflix/RxJava/wiki/images/rx-operators/interval.png">
   *
   * @param duration
   *            duration between two consecutive numbers
   * @param scheduler
   *            the scheduler to use
   * @return An Observable that emits a number each time interval.
   */
  def interval(duration: Duration, scheduler: Scheduler): Observable[Long] = {
    (new Observable[java.lang.Long](JObservable.interval(duration.length, duration.unit, scheduler))).map(_.longValue())
  }
  
}

// Cannot yet have inner class because of this error message: 
// "implementation restriction: nested class is not allowed in value class.
// This restriction is planned to be removed in subsequent releases."  
private[scala] class WithFilter[+T] (p: T => Boolean, asJava: rx.Observable[_ <: T]) {
  import rx.lang.scala.ImplicitFunctionConversions._
  
  def map[B](f: T => B): Observable[B] = {
    Observable[B](asJava.filter(p).map[B](f))
  }
  
  def flatMap[B](f: T => Observable[B]): Observable[B] = {
    Observable[B](asJava.filter(p).flatMap[B]((x: T) => f(x).asJava))
  }
  
  def withFilter(q: T => Boolean): Observable[T] = {
    Observable[T](asJava.filter((x: T) => p(x) && q(x)))
  }
  
  // there is no foreach here, that's only available on BlockingObservable
}

private[scala] class UnitTestSuite extends org.scalatest.junit.JUnitSuite {
  import scala.concurrent.duration._
  import org.junit.{Before, Test, Ignore}
  import org.junit.Assert._
  import org.mockito.Matchers._
  import org.mockito.Mockito._
  import org.mockito.{ MockitoAnnotations, Mock }
  
  // Tests which needn't be run:
  
  @Ignore def testCovariance = {
    println("hey, you shouldn't run this test")
    
    val o1: Observable[Nothing] = Observable()
    val o2: Observable[Int] = o1
    val o3: Observable[App] = o1
    val o4: Observable[Any] = o2
    val o5: Observable[Any] = o3
  }
  
  // Tests which have to be run:
  
  @Test def testDematerialize() {
    val o = Observable(1, 2, 3)
    val mat = o.materialize
    val demat = mat.dematerialize
    
    // correctly rejected:
    // val wrongDemat = Observable("hello").dematerialize
    
    assertEquals(demat.toBlockingObservable.toIterable.toList, List(1, 2, 3))
  }
    
  // Test that Java's firstOrDefault propagates errors.
  // If this changes (i.e. it suppresses errors and returns default) then Scala's firstOrElse
  // should be changed accordingly.
  @Test def testJavaFirstOrDefault() {
    assertEquals(1, rx.Observable.from(1, 2).firstOrDefault(10).toBlockingObservable().single)
    assertEquals(10, rx.Observable.empty().firstOrDefault(10).toBlockingObservable().single)
    val msg = "msg6251"
    var receivedMsg = "none"
    try {
      rx.Observable.error(new Exception(msg)).firstOrDefault(10).toBlockingObservable().single
    } catch {
      case e: Exception => receivedMsg = e.getCause().getMessage()
    }
    assertEquals(receivedMsg, msg)
  }
  
  @Test def testFirstOrElse() {
    def mustNotBeCalled: String = sys.error("this method should not be called")
    def mustBeCalled: String = "this is the default value"
    assertEquals("hello", Observable("hello").firstOrElse(mustNotBeCalled).toBlockingObservable.single)
    assertEquals("this is the default value", Observable().firstOrElse(mustBeCalled).toBlockingObservable.single)
  }
  
  @Test def testFirstOrElseWithError() {
    val msg = "msg6251"
    var receivedMsg = "none"
    try {
      Observable[Int](new Exception(msg)).firstOrElse(10).toBlockingObservable.single
    } catch {
      case e: Exception => receivedMsg = e.getCause().getMessage()
    }
    assertEquals(receivedMsg, msg)
  }
  
  /*
  @Test def testHead() {
    val observer = mock(classOf[Observer[Int]])
    val o = Observable().head
    val sub = o.subscribe(observer)

    verify(observer, never).onNext(any(classOf[Int]))
    verify(observer, never).onCompleted()
    verify(observer, times(1)).onError(any(classOf[NoSuchElementException]))
  }
  */
  
  @Test def testTest() = {
    val a: Observable[Int] = Observable()
    assertEquals(4, Observable(1, 2, 3, 4).toBlockingObservable.toIterable.last)
    println("This UnitTestSuite.testTest() for rx.lang.scala.Observable")
  }
  
}

