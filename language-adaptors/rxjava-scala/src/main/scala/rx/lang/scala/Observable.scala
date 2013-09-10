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
 * Users should import rx.lang.scala.All._ and nothing else 
 */
object All {
  // fix covariance/contravariance once and for all
  type Observer[-T] = rx.Observer[_ >: T]  
  type Notification[+T] = rx.Notification[_ <: T]
  type Timestamped[+T] = rx.util.Timestamped[_ <: T]
  
  type Subscription = rx.Subscription
  type Scheduler = rx.Scheduler

  object util {
    type Closing = rx.util.Closing
    type Opening = rx.util.Opening
  }
}

/**
 * The Observable interface that implements the Reactive Pattern.
 */
class Observable[+T](val asJava: rx.Observable[_ <: T])
  // Uncommenting this line combined with `new Observable(...)` instead of `new Observable[T](...)`
  // makes the compiler crash
  extends AnyVal 
{
  import All._
  import All.util._
  import rx.{Observable => JObservable}
  import ImplicitFunctionConversions._

  /**
   * An {@link Observer} must call an Observable's {@code subscribe} method in order to
   * receive items and notifications from the Observable.
   * 
   * <p>A typical implementation of {@code subscribe} does the following:
   * <p>
   * It stores a reference to the Observer in a collection object, such as a {@code List<T>} object.
   * <p>
   * It returns a reference to the {@link Subscription} interface. This enables Observers to
   * unsubscribe, that is, to stop receiving items and notifications before the Observable stops
   * sending them, which also invokes the Observer's {@link Observer#onCompleted onCompleted} method.
   * <p>
   * An <code>Observable&lt;T&gt;</code> instance is responsible for accepting all subscriptions
   * and notifying all Observers. Unless the documentation for a particular
   * <code>Observable&lt;T&gt;</code> implementation indicates otherwise, Observers should make no
   * assumptions about the order in which multiple Observers will receive their notifications.
   * <p>
   * 
   * @param observer
   *            the observer
   * @return a {@link Subscription} reference with which the {@link Observer} can stop receiving items
   *         before the Observable has finished sending them
   * @throws IllegalArgumentException
   *             if the {@link Observer} provided as the argument to {@code subscribe()} is {@code null}
   */
  def subscribe(observer: Observer[T]): Subscription = {
    asJava.subscribe(observer)
  }
  
  /**
   * An {@link Observer} must call an Observable's {@code subscribe} method in order to
   * receive items and notifications from the Observable.
   * 
   * <p>A typical implementation of {@code subscribe} does the following:
   * <p>
   * It stores a reference to the Observer in a collection object, such as a {@code List<T>} object.
   * <p>
   * It returns a reference to the {@link Subscription} interface. This enables Observers to
   * unsubscribe, that is, to stop receiving items and notifications before the Observable stops
   * sending them, which also invokes the Observer's {@link Observer#onCompleted onCompleted} method.
   * <p>
   * An {@code Observable<T>} instance is responsible for accepting all subscriptions
   * and notifying all Observers. Unless the documentation for a particular {@code Observable<T>} implementation indicates otherwise, Observers should make no
   * assumptions about the order in which multiple Observers will receive their notifications.
   * <p>
   * 
   * @param observer
   *            the observer
   * @param scheduler
   *            the {@link Scheduler} on which Observers subscribe to the Observable
   * @return a {@link Subscription} reference with which Observers can stop receiving items and
   *         notifications before the Observable has finished sending them
   * @throws IllegalArgumentException
   *             if an argument to {@code subscribe()} is {@code null}
   */
  def subscribe(observer: Observer[T], scheduler: Scheduler): Subscription = {
    asJava.subscribe(observer, scheduler)
  }
  
  def subscribe(onNext: T => Unit): Subscription = {
    asJava.subscribe(onNext)
  }
  
  def subscribe(onNext: T => Unit, onError: Throwable => Unit): Subscription = {
    asJava.subscribe(onNext, onError)
  }
  
  def subscribe(onNext: T => Unit, onError: Throwable => Unit, onComplete: () => Unit): Subscription = {
    asJava.subscribe(onNext, onError, onComplete)
  }
  
  def subscribe(onNext: T => Unit, onError: Throwable => Unit, onComplete: () => Unit, scheduler: Scheduler): Subscription = {
    asJava.subscribe(onNext, onError, onComplete, scheduler)
  }
  
  def subscribe(onNext: T => Unit, onError: Throwable => Unit, scheduler: Scheduler): Subscription = {
    asJava.subscribe(onNext, onError, scheduler)
  }
  
  def subscribe(onNext: T => Unit, scheduler: Scheduler): Subscription = {
    asJava.subscribe(onNext, scheduler)
  }

  /**
   * Returns a {@link ConnectableObservable} that upon connection causes the source Observable to
   * push results into the specified subject.
   * 
   * @param subject
   *            the {@link Subject} for the {@link ConnectableObservable} to push source items
   *            into
   * @param <R>
   *            result type
   * @return a {@link ConnectableObservable} that upon connection causes the source Observable to
   *         push results into the specified {@link Subject}
   */
  // public <R> ConnectableObservable<R> multicast(Subject<T, R> subject) TODO
  
  
  /**
   * Returns an Observable that first emits the items emitted by this, and then the items emitted
   * by that.
   * <p>
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
    new Observable[U](JObservable.concat(o1, o2))
  }

  
  /**
   * Wraps this Observable in another Observable that ensures that the resulting
   * Observable is chronologically well-behaved.
   * <p>
   * <img width="640" src="https://github.com/Netflix/RxJava/wiki/images/rx-operators/synchronize.png">
   * <p>
   * A well-behaved Observable does not interleave its invocations of the {@link Observer#onNext onNext}, {@link Observer#onCompleted onCompleted}, and {@link Observer#onError onError} methods of
   * its {@link Observer}s; it invokes {@code onCompleted} or {@code onError} only once; and it never invokes {@code onNext} after invoking either {@code onCompleted} or {@code onError}.
   * {@code synchronize} enforces this, and the Observable it returns invokes {@code onNext} and {@code onCompleted} or {@code onError} synchronously.
   * 
   * @param observable
   *            the source Observable
   * @param <T>
   *            the type of item emitted by the source Observable
   * @return an Observable that is a chronologically well-behaved version of the source
   *         Observable, and that synchronously notifies its {@link Observer}s
   */
  def synchronize: Observable[T] = {
    new Observable[T](JObservable.synchronize(asJava))
  }
  
  /**
   * Wraps each item emitted by a source Observable in a {@link Timestamped} object.
   * <p>
   * <img width="640" src="https://github.com/Netflix/RxJava/wiki/images/rx-operators/timestamp.png">
   * 
   * @return an Observable that emits timestamped items from the source Observable
   */
  // TODO this can only work if Timestamped is used as Timestamped<? extends T> in core
  def timestamp: Observable[Timestamped[T]] = {
    new Observable[Timestamped[T]](asJava.timestamp())
  }
  
  
  /**
   * TODO doc
   */
  def zip[U](that: Observable[U]): Observable[(T, U)] = {
    new Observable[(T, U)](JObservable.zip(this.asJava, that.asJava, (t: T, u: U) => (t, u)))
  }  
  
  // There is no method corresponding to
  // public static <T> Observable<Boolean> sequenceEqual(Observable<? extends T> first, Observable<? extends T> second)
  // because the Scala-idiomatic way of doing this is
  // (first zip second) map (p => p._1 == p._2)
  
  // There is no method corresponding to  
  // public static <T> Observable<Boolean> sequenceEqual(Observable<? extends T> first, Observable<? extends T> second, Func2<? super T, ? super T, Boolean> equality)
  // because the Scala-idiomatic way of doing this is
  // (first zip second) map (p => equality(p._1, p._2))
  
  /**
   * Creates an Observable which produces buffers of collected values.
   * 
   * <p>This Observable produces connected non-overlapping buffers. The current buffer is
   * emitted and replaced with a new buffer when the Observable produced by the specified {@link Func0} produces a {@link rx.util.Closing} object. The * {@link Func0} will then
   * be used to create a new Observable to listen for the end of the next buffer.
   * 
   * @param bufferClosingSelector
   *            The {@link Func0} which is used to produce an {@link Observable} for every buffer created.
   *            When this {@link Observable} produces a {@link rx.util.Closing} object, the associated buffer
   *            is emitted and replaced with a new one.
   * @return
   *         An {@link Observable} which produces connected non-overlapping buffers, which are emitted
   *         when the current {@link Observable} created with the {@link Func0} argument produces a {@link rx.util.Closing} object.
   */
  // TODO 1: buffer does not work because Java returns a mutable java.util.List[T] which is invariant in T
  // But in Scala, we would like to have an immutable List[T] which is covariant in T
  // TODO 2: But even if we try to get a java.util.List[T], there is a problem
  // -> core should respect covariance, but that's not the only problem
  def buffer(bufferClosingSelector: () => Observable[Closing]) : Observable[List[T]] = {
    val f: rx.util.functions.Func0[_ <: rx.Observable[_ <: Closing]] = bufferClosingSelector().asJava
    // type mismatch; found : rx.Observable[java.util.List[_$3]] required: rx.Observable[_ <: java.util.List[T]]
    // val o: rx.Observable[_ <: java.util.List[T]] = asJava.buffer(f)
    val o: rx.Observable[_ <: java.util.List[T]] = ???
    // type mismatch; found : rx.Observable[java.util.List[_$3]] required: rx.Observable[java.util.List[T]]
    // val o2: rx.Observable[java.util.List[T]] = asJava.buffer(f)
    val r = new Observable[java.util.List[T]](o)
    ???
  }
  
  // public Observable<List<T>> buffer(Func0<? extends Observable<? extends Closing>> bufferClosingSelector) 

  /**
   * Creates an Observable which produces buffers of collected values.
   * 
   * <p>This Observable produces buffers. Buffers are created when the specified "bufferOpenings"
   * Observable produces a {@link rx.util.Opening} object. Additionally the {@link Func0} argument
   * is used to create an Observable which produces {@link rx.util.Closing} objects. When this
   * Observable produces such an object, the associated buffer is emitted.
   * 
   * @param bufferOpenings
   *            The {@link Observable} which, when it produces a {@link rx.util.Opening} object, will cause
   *            another buffer to be created.
   * @param bufferClosingSelector
   *            The {@link Func0} which is used to produce an {@link Observable} for every buffer created.
   *            When this {@link Observable} produces a {@link rx.util.Closing} object, the associated buffer
   *            is emitted.
   * @return
   *         An {@link Observable} which produces buffers which are created and emitted when the specified {@link Observable}s publish certain objects.
   */
  // public Observable<List<T>> buffer(Observable<? extends Opening> bufferOpenings, Func1<Opening, ? extends Observable<? extends Closing>> bufferClosingSelector) 

  /**
   * Creates an Observable which produces buffers of collected values.
   * 
   * <p>This Observable produces connected non-overlapping buffers, each containing "count"
   * elements. When the source Observable completes or encounters an error, the current
   * buffer is emitted, and the event is propagated.
   * 
   * @param count
   *            The maximum size of each buffer before it should be emitted.
   * @return
   *         An {@link Observable} which produces connected non-overlapping buffers containing at most
   *         "count" produced values.
   */
  // public Observable<List<T>> buffer(int count) 

  /**
   * Creates an Observable which produces buffers of collected values.
   * 
   * <p>This Observable produces buffers every "skip" values, each containing "count"
   * elements. When the source Observable completes or encounters an error, the current
   * buffer is emitted, and the event is propagated.
   * 
   * @param count
   *            The maximum size of each buffer before it should be emitted.
   * @param skip
   *            How many produced values need to be skipped before starting a new buffer. Note that when "skip" and
   *            "count" are equals that this is the same operation as {@link Observable#buffer(int)}.
   * @return
   *         An {@link Observable} which produces buffers every "skipped" values containing at most
   *         "count" produced values.
   */
  // public Observable<List<T>> buffer(int count, int skip) 

  /**
   * Creates an Observable which produces buffers of collected values.
   * 
   * <p>This Observable produces connected non-overlapping buffers, each of a fixed duration
   * specified by the "timespan" argument. When the source Observable completes or encounters
   * an error, the current buffer is emitted and the event is propagated.
   * 
   * @param timespan
   *            The period of time each buffer is collecting values before it should be emitted, and
   *            replaced with a new buffer.
   * @param unit
   *            The unit of time which applies to the "timespan" argument.
   * @return
   *         An {@link Observable} which produces connected non-overlapping buffers with a fixed duration.
   */
  // public Observable<List<T>> buffer(long timespan, TimeUnit unit) 

  /**
   * Creates an Observable which produces buffers of collected values.
   * 
   * <p>This Observable produces connected non-overlapping buffers, each of a fixed duration
   * specified by the "timespan" argument. When the source Observable completes or encounters
   * an error, the current buffer is emitted and the event is propagated.
   * 
   * @param timespan
   *            The period of time each buffer is collecting values before it should be emitted, and
   *            replaced with a new buffer.
   * @param unit
   *            The unit of time which applies to the "timespan" argument.
   * @param scheduler
   *            The {@link Scheduler} to use when determining the end and start of a buffer.
   * @return
   *         An {@link Observable} which produces connected non-overlapping buffers with a fixed duration.
   */
  // public Observable<List<T>> buffer(long timespan, TimeUnit unit, Scheduler scheduler) 

  /**
   * Creates an Observable which produces buffers of collected values. This Observable produces connected
   * non-overlapping buffers, each of a fixed duration specified by the "timespan" argument or a maximum size
   * specified by the "count" argument (which ever is reached first). When the source Observable completes
   * or encounters an error, the current buffer is emitted and the event is propagated.
   * 
   * @param timespan
   *            The period of time each buffer is collecting values before it should be emitted, and
   *            replaced with a new buffer.
   * @param unit
   *            The unit of time which applies to the "timespan" argument.
   * @param count
   *            The maximum size of each buffer before it should be emitted.
   * @return
   *         An {@link Observable} which produces connected non-overlapping buffers which are emitted after
   *         a fixed duration or when the buffer has reached maximum capacity (which ever occurs first).
   */
  // public Observable<List<T>> buffer(long timespan, TimeUnit unit, int count) 

  /**
   * Creates an Observable which produces buffers of collected values. This Observable produces connected
   * non-overlapping buffers, each of a fixed duration specified by the "timespan" argument or a maximum size
   * specified by the "count" argument (which ever is reached first). When the source Observable completes
   * or encounters an error, the current buffer is emitted and the event is propagated.
   * 
   * @param timespan
   *            The period of time each buffer is collecting values before it should be emitted, and
   *            replaced with a new buffer.
   * @param unit
   *            The unit of time which applies to the "timespan" argument.
   * @param count
   *            The maximum size of each buffer before it should be emitted.
   * @param scheduler
   *            The {@link Scheduler} to use when determining the end and start of a buffer.
   * @return
   *         An {@link Observable} which produces connected non-overlapping buffers which are emitted after
   *         a fixed duration or when the buffer has reached maximum capacity (which ever occurs first).
   */
  // public Observable<List<T>> buffer(long timespan, TimeUnit unit, int count, Scheduler scheduler) 

  /**
   * Creates an Observable which produces buffers of collected values. This Observable starts a new buffer
   * periodically, which is determined by the "timeshift" argument. Each buffer is emitted after a fixed timespan
   * specified by the "timespan" argument. When the source Observable completes or encounters an error, the
   * current buffer is emitted and the event is propagated.
   * 
   * @param timespan
   *            The period of time each buffer is collecting values before it should be emitted.
   * @param timeshift
   *            The period of time after which a new buffer will be created.
   * @param unit
   *            The unit of time which applies to the "timespan" and "timeshift" argument.
   * @return
   *         An {@link Observable} which produces new buffers periodically, and these are emitted after
   *         a fixed timespan has elapsed.
   */
  // public Observable<List<T>> buffer(long timespan, long timeshift, TimeUnit unit) 

  /**
   * Creates an Observable which produces buffers of collected values. This Observable starts a new buffer
   * periodically, which is determined by the "timeshift" argument. Each buffer is emitted after a fixed timespan
   * specified by the "timespan" argument. When the source Observable completes or encounters an error, the
   * current buffer is emitted and the event is propagated.
   * 
   * @param timespan
   *            The period of time each buffer is collecting values before it should be emitted.
   * @param timeshift
   *            The period of time after which a new buffer will be created.
   * @param unit
   *            The unit of time which applies to the "timespan" and "timeshift" argument.
   * @param scheduler
   *            The {@link Scheduler} to use when determining the end and start of a buffer.
   * @return
   *         An {@link Observable} which produces new buffers periodically, and these are emitted after
   *         a fixed timespan has elapsed.
   */
  // public Observable<List<T>> buffer(long timespan, long timeshift, TimeUnit unit, Scheduler scheduler) 

  /**
   * Creates an Observable which produces windows of collected values. This Observable produces connected
   * non-overlapping windows. The current window is emitted and replaced with a new window when the
   * Observable produced by the specified {@link Func0} produces a {@link rx.util.Closing} object. The {@link Func0} will then be used to create a new Observable to listen for the end of the next
   * window.
   * 
   * @param closingSelector
   *            The {@link Func0} which is used to produce an {@link Observable} for every window created.
   *            When this {@link Observable} produces a {@link rx.util.Closing} object, the associated window
   *            is emitted and replaced with a new one.
   * @return
   *         An {@link Observable} which produces connected non-overlapping windows, which are emitted
   *         when the current {@link Observable} created with the {@link Func0} argument produces a {@link rx.util.Closing} object.
   */
  def window(closingSelector: () => Observable[Closing]): Observable[Observable[T]] = {
    val func : rx.util.functions.Func0[_ <: rx.Observable[_ <: Closing]] = closingSelector().asJava
    // type mismatch; found : rx.Observable[rx.Observable[_$4]] required: rx.Observable[rx.Observable[T]]
    // val o: rx.Observable[rx.Observable[T]] = asJava.window(func)
    val o = ??? // TODO
    new Observable[rx.Observable[T]](o).map((x: rx.Observable[T]) => new Observable[T](x))
  }
  // public Observable<Observable<T>> window(Func0<? extends Observable<? extends Closing>> closingSelector) 

  /**
   * Creates an Observable which produces windows of collected values. This Observable produces windows.
   * Chunks are created when the specified "windowOpenings" Observable produces a {@link rx.util.Opening} object.
   * Additionally the {@link Func0} argument is used to create an Observable which produces {@link rx.util.Closing} objects. When this Observable produces such an object, the associated window is
   * emitted.
   * 
   * @param windowOpenings
   *            The {@link Observable} which when it produces a {@link rx.util.Opening} object, will cause
   *            another window to be created.
   * @param closingSelector
   *            The {@link Func0} which is used to produce an {@link Observable} for every window created.
   *            When this {@link Observable} produces a {@link rx.util.Closing} object, the associated window
   *            is emitted.
   * @return
   *         An {@link Observable} which produces windows which are created and emitted when the specified {@link Observable}s publish certain objects.
   */
  // public Observable<Observable<T>> window(Observable<? extends Opening> windowOpenings, Func1<Opening, ? extends Observable<? extends Closing>> closingSelector) 

  /**
   * Creates an Observable which produces windows of collected values. This Observable produces connected
   * non-overlapping windows, each containing "count" elements. When the source Observable completes or
   * encounters an error, the current window is emitted, and the event is propagated.
   * 
   * @param count
   *            The maximum size of each window before it should be emitted.
   * @return
   *         An {@link Observable} which produces connected non-overlapping windows containing at most
   *         "count" produced values.
   */
  // public Observable<Observable<T>> window(int count) 

  /**
   * Creates an Observable which produces windows of collected values. This Observable produces windows every
   * "skip" values, each containing "count" elements. When the source Observable completes or encounters an error,
   * the current window is emitted and the event is propagated.
   * 
   * @param count
   *            The maximum size of each window before it should be emitted.
   * @param skip
   *            How many produced values need to be skipped before starting a new window. Note that when "skip" and
   *            "count" are equals that this is the same operation as {@link Observable#window(Observable, int)}.
   * @return
   *         An {@link Observable} which produces windows every "skipped" values containing at most
   *         "count" produced values.
   */
  // public Observable<Observable<T>> window(int count, int skip) 

  /**
   * Creates an Observable which produces windows of collected values. This Observable produces connected
   * non-overlapping windows, each of a fixed duration specified by the "timespan" argument. When the source
   * Observable completes or encounters an error, the current window is emitted and the event is propagated.
   * 
   * @param timespan
   *            The period of time each window is collecting values before it should be emitted, and
   *            replaced with a new window.
   * @param unit
   *            The unit of time which applies to the "timespan" argument.
   * @return
   *         An {@link Observable} which produces connected non-overlapping windows with a fixed duration.
   */
  // public Observable<Observable<T>> window(long timespan, TimeUnit unit) 

  /**
   * Creates an Observable which produces windows of collected values. This Observable produces connected
   * non-overlapping windows, each of a fixed duration specified by the "timespan" argument. When the source
   * Observable completes or encounters an error, the current window is emitted and the event is propagated.
   * 
   * @param timespan
   *            The period of time each window is collecting values before it should be emitted, and
   *            replaced with a new window.
   * @param unit
   *            The unit of time which applies to the "timespan" argument.
   * @param scheduler
   *            The {@link Scheduler} to use when determining the end and start of a window.
   * @return
   *         An {@link Observable} which produces connected non-overlapping windows with a fixed duration.
   */
  // public Observable<Observable<T>> window(long timespan, TimeUnit unit, Scheduler scheduler) 

  /**
   * Creates an Observable which produces windows of collected values. This Observable produces connected
   * non-overlapping windows, each of a fixed duration specified by the "timespan" argument or a maximum size
   * specified by the "count" argument (which ever is reached first). When the source Observable completes
   * or encounters an error, the current window is emitted and the event is propagated.
   * 
   * @param timespan
   *            The period of time each window is collecting values before it should be emitted, and
   *            replaced with a new window.
   * @param unit
   *            The unit of time which applies to the "timespan" argument.
   * @param count
   *            The maximum size of each window before it should be emitted.
   * @return
   *         An {@link Observable} which produces connected non-overlapping windows which are emitted after
   *         a fixed duration or when the window has reached maximum capacity (which ever occurs first).
   */
  // public Observable<Observable<T>> window(long timespan, TimeUnit unit, int count) 

  /**
   * Creates an Observable which produces windows of collected values. This Observable produces connected
   * non-overlapping windows, each of a fixed duration specified by the "timespan" argument or a maximum size
   * specified by the "count" argument (which ever is reached first). When the source Observable completes
   * or encounters an error, the current window is emitted and the event is propagated.
   * 
   * @param timespan
   *            The period of time each window is collecting values before it should be emitted, and
   *            replaced with a new window.
   * @param unit
   *            The unit of time which applies to the "timespan" argument.
   * @param count
   *            The maximum size of each window before it should be emitted.
   * @param scheduler
   *            The {@link Scheduler} to use when determining the end and start of a window.
   * @return
   *         An {@link Observable} which produces connected non-overlapping windows which are emitted after
   *         a fixed duration or when the window has reached maximum capacity (which ever occurs first).
   */
  // public Observable<Observable<T>> window(long timespan, TimeUnit unit, int count, Scheduler scheduler) 

  /**
   * Creates an Observable which produces windows of collected values. This Observable starts a new window
   * periodically, which is determined by the "timeshift" argument. Each window is emitted after a fixed timespan
   * specified by the "timespan" argument. When the source Observable completes or encounters an error, the
   * current window is emitted and the event is propagated.
   * 
   * @param timespan
   *            The period of time each window is collecting values before it should be emitted.
   * @param timeshift
   *            The period of time after which a new window will be created.
   * @param unit
   *            The unit of time which applies to the "timespan" and "timeshift" argument.
   * @return
   *         An {@link Observable} which produces new windows periodically, and these are emitted after
   *         a fixed timespan has elapsed.
   */
  // public Observable<Observable<T>> window(long timespan, long timeshift, TimeUnit unit) 

  /**
   * Creates an Observable which produces windows of collected values. This Observable starts a new window
   * periodically, which is determined by the "timeshift" argument. Each window is emitted after a fixed timespan
   * specified by the "timespan" argument. When the source Observable completes or encounters an error, the
   * current window is emitted and the event is propagated.
   * 
   * @param timespan
   *            The period of time each window is collecting values before it should be emitted.
   * @param timeshift
   *            The period of time after which a new window will be created.
   * @param unit
   *            The unit of time which applies to the "timespan" and "timeshift" argument.
   * @param scheduler
   *            The {@link Scheduler} to use when determining the end and start of a window.
   * @return
   *         An {@link Observable} which produces new windows periodically, and these are emitted after
   *         a fixed timespan has elapsed.
   */
  // public Observable<Observable<T>> window(long timespan, long timeshift, TimeUnit unit, Scheduler scheduler) 


  

  /**
   * <p>
   * <img width="640" src="https://raw.github.com/wiki/Netflix/RxJava/images/rx-operators/filter.png">
   * 
   * @param predicate
   *            a function that evaluates the items emitted by the source Observable, returning {@code true} if they pass the filter
   * @return an Observable that emits only those items in the original Observable that the filter
   *         evaluates as {@code true}
   */
  def filter(predicate: T => Boolean): Observable[T] = {
    new Observable[T](asJava.filter(predicate))
  }

  /**
   * Registers an {@link Action0} to be called when this Observable invokes {@link Observer#onCompleted onCompleted} or {@link Observer#onError onError}.
   * <p>
   * <img width="640" src="https://github.com/Netflix/RxJava/wiki/images/rx-operators/finallyDo.png">
   * 
   * @param action
   *            an {@link Action0} to be invoked when the source Observable finishes
   * @return an Observable that emits the same items as the source Observable, then invokes the {@link Action0}
   * @see <a href="http://msdn.microsoft.com/en-us/library/hh212133(v=vs.103).aspx">MSDN: Observable.Finally Method</a>
   */
  def finallyDo(action: () => Unit): Observable[T] = {
    new Observable[T](asJava.finallyDo(action))
  } 

  /**
   * Creates a new Observable by applying a function that you supply to each item emitted by
   * the source Observable, where that function returns an Observable, and then merging those
   * resulting Observables and emitting the results of this merger.
   * <p>
   * <img width="640" src="https://raw.github.com/wiki/Netflix/RxJava/images/rx-operators/flatMap.png">
   * <p>
   * Note: {@code mapMany} and {@code flatMap} are equivalent.
   * 
   * @param func
   *            a function that, when applied to an item emitted by the source Observable, returns
   *            an Observable
   * @return an Observable that emits the result of applying the transformation function to each
   *         item emitted by the source Observable and merging the results of the Observables
   *         obtained from this transformation.
   * @see #mapMany(Func1)
   */
  def flatMap[R](f: T => Observable[R]): Observable[R] = {
    new Observable[R](asJava.flatMap[R]((t: T) => f(t).asJava))
  }
  
  // There is no method like
  // public Observable<T> where(Func1<? super T, Boolean> predicate)
  // because that's called filter in Scala.

  /**
   * Returns an Observable that applies the given function to each item emitted by an
   * Observable and emits the result.
   * <p>
   * <img width="640" src="https://raw.github.com/wiki/Netflix/RxJava/images/rx-operators/map.png">
   * 
   * @param func
   *            a function to apply to each item emitted by the Observable
   * @return an Observable that emits the items from the source Observable, transformed by the
   *         given function
   */
  def map[R](func: T => R): Observable[R] = {
    new Observable[R](asJava.map(func))
  }
  
  // There's no method like
  // public <R> Observable<R> mapMany(Func1<? super T, ? extends Observable<? extends R>> func)
  // because that's called flatMap in Scala.
  
  /**
   * Turns all of the notifications from a source Observable into {@link Observer#onNext onNext} emissions, and marks them with their original notification types within {@link Notification} objects.
   * <p>
   * <img width="640" src="https://raw.github.com/wiki/Netflix/RxJava/images/rx-operators/materialize.png">
   * 
   * @return an Observable whose items are the result of materializing the items and
   *         notifications of the source Observable
   * @see <a href="http://msdn.microsoft.com/en-us/library/hh229453(v=VS.103).aspx">MSDN: Observable.materialize</a>
   */
  /*
  def materialize: Observable[Notification[T]] = {
    // new Observable[Notification[T]](asJava.materialize())
    ??? // TODO check with covariance in core
  } */

  /**
   * Asynchronously subscribes and unsubscribes Observers on the specified {@link Scheduler}.
   * <p>
   * <img width="640" src="https://github.com/Netflix/RxJava/wiki/images/rx-operators/subscribeOn.png">
   * 
   * @param scheduler
   *            the {@link Scheduler} to perform subscription and unsubscription actions on
   * @return the source Observable modified so that its subscriptions and unsubscriptions happen
   *         on the specified {@link Scheduler}
   */
  def subscribeOn(scheduler: Scheduler): Observable[T] = {
    new Observable[T](asJava.subscribeOn(scheduler))
  } 

  /**
   * Asynchronously notify {@link Observer}s on the specified {@link Scheduler}.
   * <p>
   * <img width="640" src="https://github.com/Netflix/RxJava/wiki/images/rx-operators/observeOn.png">
   * 
   * @param scheduler
   *            the {@link Scheduler} to notify {@link Observer}s on
   * @return the source Observable modified so that its {@link Observer}s are notified on the
   *         specified {@link Scheduler}
   */
  def observeOn(scheduler: Scheduler): Observable[T] = {
    new Observable[T](asJava.observeOn(scheduler))
  }
 
  /**
   * Returns an Observable that reverses the effect of {@link #materialize materialize} by
   * transforming the {@link Notification} objects emitted by the source Observable into the items
   * or notifications they represent.
   * <p>
   * <img width="640" src="https://github.com/Netflix/RxJava/wiki/images/rx-operators/dematerialize.png">
   * 
   * @return an Observable that emits the items and notifications embedded in the {@link Notification} objects emitted by the source Observable
   * @see <a href="http://msdn.microsoft.com/en-us/library/hh229047(v=vs.103).aspx">MSDN: Observable.dematerialize</a>
   * @throws Throwable
   *             if the source Observable is not of type {@code Observable<Notification<T>>}.
   */
  // @SuppressWarnings("unchecked")
  // public <T2> Observable<T2> dematerialize() 
  // TODO

  /**
   * Instruct an Observable to pass control to another Observable rather than invoking {@link Observer#onError onError} if it encounters an error.
   * <p>
   * <img width="640" src="https://raw.github.com/wiki/Netflix/RxJava/images/rx-operators/onErrorResumeNext.png">
   * <p>
   * By default, when an Observable encounters an error that prevents it from emitting the
   * expected item to its {@link Observer}, the Observable invokes its Observer's
   * <code>onError</code> method, and then quits without invoking any more of its Observer's
   * methods. The <code>onErrorResumeNext</code> method changes this behavior. If you pass a
   * function that returns an Observable (<code>resumeFunction</code>) to
   * <code>onErrorResumeNext</code>, if the original Observable encounters an error, instead of
   * invoking its Observer's <code>onError</code> method, it will instead relinquish control to
   * the Observable returned from <code>resumeFunction</code>, which will invoke the Observer's {@link Observer#onNext onNext} method if it is able to do so. In such a case, because no
   * Observable necessarily invokes <code>onError</code>, the Observer may never know that an
   * error happened.
   * <p>
   * You can use this to prevent errors from propagating or to supply fallback data should errors
   * be encountered.
   * 
   * @param resumeFunction
   *            a function that returns an Observable that will take over if the source Observable
   *            encounters an error
   * @return the original Observable, with appropriately modified behavior
   */
  // Since T is a covariant type parameter, someone might treat `this` as an Observable[Any].
  // Then they can call onErrorResumeNext(someFunc), with someFunc returning an Observable[Any].
  // That's why the method signature below is not allowed
  // TODO what to do about this?
  /*
  def onErrorResumeNext(resumeFunction: Throwable => Observable[T]): Observable[T] = {
    new Observable(asJava.onErrorResumeNext((t: Throwable) => resumeFunction(t).asJava))
  }
  */
  // public Observable<T> onErrorResumeNext(final Func1<Throwable, ? extends Observable<? extends T>> resumeFunction) 

  /**
   * Instruct an Observable to pass control to another Observable rather than invoking {@link Observer#onError onError} if it encounters an error.
   * <p>
   * <img width="640" src="https://raw.github.com/wiki/Netflix/RxJava/images/rx-operators/onErrorResumeNext.png">
   * <p>
   * By default, when an Observable encounters an error that prevents it from emitting the
   * expected item to its {@link Observer}, the Observable invokes its Observer's
   * <code>onError</code> method, and then quits without invoking any more of its Observer's
   * methods. The <code>onErrorResumeNext</code> method changes this behavior. If you pass
   * another Observable (<code>resumeSequence</code>) to an Observable's
   * <code>onErrorResumeNext</code> method, if the original Observable encounters an error,
   * instead of invoking its Observer's <code>onError</code> method, it will instead relinquish
   * control to <code>resumeSequence</code> which will invoke the Observer's {@link Observer#onNext onNext} method if it is able to do so. In such a case, because no
   * Observable necessarily invokes <code>onError</code>, the Observer may never know that an
   * error happened.
   * <p>
   * You can use this to prevent errors from propagating or to supply fallback data should errors
   * be encountered.
   * 
   * @param resumeSequence
   *            a function that returns an Observable that will take over if the source Observable
   *            encounters an error
   * @return the original Observable, with appropriately modified behavior
   */
  // TODO same as above
  // public Observable<T> onErrorResumeNext(final Observable<? extends T> resumeSequence) 

  /**
   * Instruct an Observable to pass control to another Observable rather than invoking {@link Observer#onError onError} if it encounters an error of type {@link java.lang.Exception}.
   * <p>
   * This differs from {@link #onErrorResumeNext} in that this one does not handle {@link java.lang.Throwable} or {@link java.lang.Error} but lets those continue through.
   * <p>
   * <img width="640" src="https://raw.github.com/wiki/Netflix/RxJava/images/rx-operators/onErrorResumeNext.png">
   * <p>
   * By default, when an Observable encounters an error that prevents it from emitting the
   * expected item to its {@link Observer}, the Observable invokes its Observer's
   * <code>onError</code> method, and then quits without invoking any more of its Observer's
   * methods. The <code>onErrorResumeNext</code> method changes this behavior. If you pass
   * another Observable (<code>resumeSequence</code>) to an Observable's
   * <code>onErrorResumeNext</code> method, if the original Observable encounters an error,
   * instead of invoking its Observer's <code>onError</code> method, it will instead relinquish
   * control to <code>resumeSequence</code> which will invoke the Observer's {@link Observer#onNext onNext} method if it is able to do so. In such a case, because no
   * Observable necessarily invokes <code>onError</code>, the Observer may never know that an
   * error happened.
   * <p>
   * You can use this to prevent errors from propagating or to supply fallback data should errors
   * be encountered.
   * 
   * @param resumeSequence
   *            a function that returns an Observable that will take over if the source Observable
   *            encounters an error
   * @return the original Observable, with appropriately modified behavior
   */
  // TODO same as above
  /*
  def onExceptionResumeNext(resumeSequence: Observable[T]) = {
    ???
  }
  */
  // public Observable<T> onExceptionResumeNext(final Observable<? extends T> resumeSequence) 

  /**
   * Instruct an Observable to emit an item (returned by a specified function) rather than
   * invoking {@link Observer#onError onError} if it encounters an error.
   * <p>
   * <img width="640" src="https://github.com/Netflix/RxJava/wiki/images/rx-operators/onErrorReturn.png">
   * <p>
   * By default, when an Observable encounters an error that prevents it from emitting the
   * expected item to its {@link Observer}, the Observable invokes its Observer's
   * <code>onError</code> method, and then quits without invoking any more of its Observer's
   * methods. The <code>onErrorReturn</code> method changes this behavior. If you pass a function
   * (<code>resumeFunction</code>) to an Observable's <code>onErrorReturn</code> method, if the
   * original Observable encounters an error, instead of invoking its Observer's
   * <code>onError</code> method, it will instead pass the return value of
   * <code>resumeFunction</code> to the Observer's {@link Observer#onNext onNext} method.
   * <p>
   * You can use this to prevent errors from propagating or to supply fallback data should errors
   * be encountered.
   * 
   * @param resumeFunction
   *            a function that returns an item that the new Observable will emit if the source
   *            Observable encounters an error
   * @return the original Observable with appropriately modified behavior
   */
  // TODO same as above
  /*
  def onErrorReturn(resumeFunction: Throwable => T): Observable[T] = {
    ???
  }
  */
  // public Observable<T> onErrorReturn(Func1<Throwable, ? extends T> resumeFunction) 

  /**
   * Returns an Observable that applies a function of your choosing to the first item emitted by a
   * source Observable, then feeds the result of that function along with the second item emitted
   * by the source Observable into the same function, and so on until all items have been emitted
   * by the source Observable, and emits the final result from the final call to your function as
   * its sole item.
   * <p>
   * <img width="640" src="https://raw.github.com/wiki/Netflix/RxJava/images/rx-operators/reduce.png">
   * <p>
   * This technique, which is called "reduce" or "aggregate" here, is sometimes called "fold,"
   * "accumulate," "compress," or "inject" in other programming contexts. Groovy, for instance,
   * has an <code>inject</code> method that does a similar operation on lists.
   * 
   * @param accumulator
   *            An accumulator function to be invoked on each item emitted by the source
   *            Observable, whose result will be used in the next accumulator call
   * @return an Observable that emits a single item that is the result of accumulating the
   *         output from the source Observable
   * @see <a href="http://msdn.microsoft.com/en-us/library/hh229154(v%3Dvs.103).aspx">MSDN: Observable.Aggregate</a>
   * @see <a href="http://en.wikipedia.org/wiki/Fold_(higher-order_function)">Wikipedia: Fold (higher-order function)</a>
   */
  def reduce[U >: T](f: (U, U) => U): Observable[U] = {
    val func: rx.util.functions.Func2[_ >: U, _ >: U, _ <: U] = f
    // new Observable(asJava.reduce(func))
    ??? // TODO once reduce has correct signature in core
  }
  // public Observable<T> reduce(Func2<? super T, ? super T, ? extends T> accumulator) 

  /**
   * Returns a {@link ConnectableObservable} that shares a single subscription to the underlying
   * Observable that will replay all of its items and notifications to any future {@link Observer}.
   * <p>
   * <img width="640" src="https://raw.github.com/wiki/Netflix/RxJava/images/rx-operators/replay.png">
   * 
   * @return a {@link ConnectableObservable} that upon connection causes the source Observable to
   *         emit items to its {@link Observer}s
   */
  def replay(): ConnectableObservable[T] = {
    new ConnectableObservable[T](asJava.replay())
  }

  /**
   * This method has similar behavior to {@link #replay} except that this auto-subscribes to
   * the source Observable rather than returning a {@link ConnectableObservable}.
   * <p>
   * <img width="640" src="https://github.com/Netflix/RxJava/wiki/images/rx-operators/cache.png">
   * <p>
   * This is useful when you want an Observable to cache responses and you can't control the
   * subscribe/unsubscribe behavior of all the {@link Observer}s.
   * <p>
   * NOTE: You sacrifice the ability to unsubscribe from the origin when you use the
   * <code>cache()</code> operator so be careful not to use this operator on Observables that
   * emit an infinite or very large number of items that will use up memory.
   * 
   * @return an Observable that when first subscribed to, caches all of its notifications for
   *         the benefit of subsequent subscribers.
   */
  def cache: Observable[T] = {
    new Observable[T](asJava.cache())
  }

  /**
   * Returns a {@link ConnectableObservable}, which waits until its {@link ConnectableObservable#connect connect} method is called before it begins emitting
   * items to those {@link Observer}s that have subscribed to it.
   * <p>
   * <img width="640" src="https://github.com/Netflix/RxJava/wiki/images/rx-operators/publishConnect.png">
   * 
   * @return a {@link ConnectableObservable} that upon connection causes the source Observable to
   *         emit items to its {@link Observer}s
   */
  def publish: ConnectableObservable[T] = {
    new ConnectableObservable[T](asJava.publish())
  }

  // There is no aggregate function with signature
  // public Observable<T> aggregate(Func2<? super T, ? super T, ? extends T> accumulator)
  // because that's called reduce in Scala.
  
  // TODO add Scala-like aggregate function
  
  /**
   * Returns an Observable that applies a function of your choosing to the first item emitted by a
   * source Observable, then feeds the result of that function along with the second item emitted
   * by an Observable into the same function, and so on until all items have been emitted by the
   * source Observable, emitting the final result from the final call to your function as its sole
   * item.
   * <p>
   * <img width="640" src="https://raw.github.com/wiki/Netflix/RxJava/images/rx-operators/reduceSeed.png">
   * <p>
   * This technique, which is called "reduce" or "aggregate" here, is sometimes called "fold,"
   * "accumulate," "compress," or "inject" in other programming contexts. Groovy, for instance,
   * has an <code>inject</code> method that does a similar operation on lists.
   * 
   * @param initialValue
   *            the initial (seed) accumulator value
   * @param accumulator
   *            an accumulator function to be invoked on each item emitted by the source
   *            Observable, the result of which will be used in the next accumulator call
   * @return an Observable that emits a single item that is the result of accumulating the output
   *         from the items emitted by the source Observable
   * @see <a href="http://msdn.microsoft.com/en-us/library/hh229154(v%3Dvs.103).aspx">MSDN: Observable.Aggregate</a>
   * @see <a href="http://en.wikipedia.org/wiki/Fold_(higher-order_function)">Wikipedia: Fold (higher-order function)</a>
   */
  def fold[R](initialValue: R)(accumulator: (R, T) => R): Observable[R] = {
    new Observable(asJava.reduce(initialValue, accumulator))
  }
  // corresponds to Java's
  // public <R> Observable<R> reduce(R initialValue, Func2<? super R, ? super T, ? extends R> accumulator) 
  // public <R> Observable<R> aggregate(R initialValue, Func2<? super R, ? super T, ? extends R> accumulator) 
  
  // There is no method like
  // public Observable<T> scan(Func2<? super T, ? super T, ? extends T> accumulator)
  // because scan has a seed in Scala
  
  /**
   * Returns an Observable that emits the results of sampling the items emitted by the source
   * Observable at a specified time interval.
   * <p>
   * <img width="640" src="https://github.com/Netflix/RxJava/wiki/images/rx-operators/sample.png">
   * 
   * @param period
   *            the sampling rate
   * @param unit
   *            the {@link TimeUnit} in which <code>period</code> is defined
   * @return an Observable that emits the results of sampling the items emitted by the source
   *         Observable at the specified time interval
   */
  // public Observable<T> sample(long period, TimeUnit unit)
  // TODO what TimeUnit?

  /**
   * Returns an Observable that emits the results of sampling the items emitted by the source
   * Observable at a specified time interval.
   * <p>
   * <img width="640" src="https://github.com/Netflix/RxJava/wiki/images/rx-operators/sample.png">
   * 
   * @param period
   *            the sampling rate
   * @param unit
   *            the {@link TimeUnit} in which <code>period</code> is defined
   * @param scheduler
   *            the {@link Scheduler} to use when sampling
   * @return an Observable that emits the results of sampling the items emitted by the source
   *         Observable at the specified time interval
   */
  // public Observable<T> sample(long period, TimeUnit unit, Scheduler scheduler) 
  // TODO what TimeUnit?
  
    /**
   * Returns an Observable that applies a function of your choosing to the first item emitted by a
   * source Observable, then feeds the result of that function along with the second item emitted
   * by an Observable into the same function, and so on until all items have been emitted by the
   * source Observable, emitting the result of each of these iterations.
   * <p>
   * <img width="640" src="https://raw.github.com/wiki/Netflix/RxJava/images/rx-operators/scanSeed.png">
   * <p>
   * This sort of function is sometimes called an accumulator.
   * <p>
   * Note that when you pass a seed to <code>scan()</code> the resulting Observable will emit
   * that seed as its first emitted item.
   * 
   * @param initialValue
   *            the initial (seed) accumulator value
   * @param accumulator
   *            an accumulator function to be invoked on each item emitted by the source
   *            Observable, whose result will be emitted to {@link Observer}s via {@link Observer#onNext onNext} and used in the next accumulator call.
   * @return an Observable that emits the results of each call to the accumulator function
   * @see <a href="http://msdn.microsoft.com/en-us/library/hh211665(v%3Dvs.103).aspx">MSDN: Observable.Scan</a>
   */
  def scan[R](initialValue: R)(accumulator: (R, T) => R): Observable[R] = {
    new Observable(asJava.scan(initialValue, accumulator))
  }
  // corresponds to Scala's
  // public <R> Observable<R> scan(R initialValue, Func2<? super R, ? super T, ? extends R> accumulator) 

  /**
   * Returns an Observable that emits a Boolean that indicates whether all of the items emitted by
   * the source Observable satisfy a condition.
   * <p>
   * <img width="640" src="https://github.com/Netflix/RxJava/wiki/images/rx-operators/all.png">
   * 
   * @param predicate
   *            a function that evaluates an item and returns a Boolean
   * @return an Observable that emits <code>true</code> if all items emitted by the source
   *         Observable satisfy the predicate; otherwise, <code>false</code>
   */
  def forall(predicate: T => Boolean): Observable[Boolean] = {
    // type mismatch; found : rx.Observable[java.lang.Boolean] required: rx.Observable[_ <: scala.Boolean]
    // new Observable[Boolean](asJava.all(predicate))
    // it's more fun in Scala:
    this.map(predicate).fold(true)(_ && _)
  }
  // corresponds to Java's
  // public Observable<Boolean> all(Func1<? super T, Boolean> predicate) 
    
  /**
   * Returns an Observable that skips the first <code>num</code> items emitted by the source
   * Observable and emits the remainder.
   * <p>
   * <img width="640" src="https://raw.github.com/wiki/Netflix/RxJava/images/rx-operators/skip.png">
   * <p>
   * You can ignore the first <code>num</code> items emitted by an Observable and attend only to
   * those items that come after, by modifying the Observable with the <code>skip</code> method.
   * 
   * @param num
   *            the number of items to skip
   * @return an Observable that is identical to the source Observable except that it does not
   *         emit the first <code>num</code> items that the source emits
   */
  def drop(n: Int): Observable[T] = {
    new Observable[T](asJava.skip(n))
  }
  // corresponds to Java's
  // public Observable<T> skip(int num)

  /**
   * Returns an Observable that emits only the first <code>num</code> items emitted by the source
   * Observable.
   * <p>
   * <img width="640" src="https://raw.github.com/wiki/Netflix/RxJava/images/rx-operators/take.png">
   * <p>
   * This method returns an Observable that will invoke a subscribing {@link Observer}'s {@link Observer#onNext onNext} function a maximum of <code>num</code> times before invoking
   * {@link Observer#onCompleted onCompleted}.
   * 
   * @param num
   *            the number of items to take
   * @return an Observable that emits only the first <code>num</code> items from the source
   *         Observable, or all of the items from the source Observable if that Observable emits
   *         fewer than <code>num</code> items
   */
  def take(n: Int): Observable[T] = {
    new Observable[T](asJava.take(n))
  }

  /**
   * Returns an Observable that emits items emitted by the source Observable so long as a
   * specified condition is true.
   * <p>
   * <img width="640" src="https://github.com/Netflix/RxJava/wiki/images/rx-operators/takeWhile.png">
   * 
   * @param predicate
   *            a function that evaluates an item emitted by the source Observable and returns a
   *            Boolean
   * @return an Observable that emits the items from the source Observable so long as each item
   *         satisfies the condition defined by <code>predicate</code>
   */
  def takeWhile(predicate: T => Boolean): Observable[T] = {
    new Observable[T](asJava.takeWhile(predicate))
  }
  
  /**
   * Returns an Observable that emits the items emitted by a source Observable so long as a given
   * predicate remains true, where the predicate can operate on both the item and its index
   * relative to the complete sequence.
   * <p>
   * <img width="640" src="https://github.com/Netflix/RxJava/wiki/images/rx-operators/takeWhileWithIndex.png">
   * 
   * @param predicate
   *            a function to test each item emitted by the source Observable for a condition;
   *            the second parameter of the function represents the index of the source item
   * @return an Observable that emits items from the source Observable so long as the predicate
   *         continues to return <code>true</code> for each item, then completes
   */
  // TODO: if we have zipWithIndex, takeWhileWithIndex is not needed any more
  def takeWhileWithIndex(predicate: (T, Integer) => Boolean): Observable[T] = {
    new Observable[T](asJava.takeWhileWithIndex(predicate))
  }

  /**
   * Returns an Observable that emits only the last <code>count</code> items emitted by the source
   * Observable.
   * <p>
   * <img width="640" src="https://github.com/Netflix/RxJava/wiki/images/rx-operators/last.png">
   * 
   * @param count
   *            the number of items to emit from the end of the sequence emitted by the source
   *            Observable
   * @return an Observable that emits only the last <code>count</code> items emitted by the source
   *         Observable
   */
  def takeRight(n: Int): Observable[T] = {
    new Observable[T](asJava.takeLast(n))
  }
  // corresponds to Java's
  // public Observable<T> takeLast(final int count) 
  
  /**
   * Returns an Observable that emits the items from the source Observable only until the
   * <code>other</code> Observable emits an item.
   * <p>
   * <img width="640" src="https://github.com/Netflix/RxJava/wiki/images/rx-operators/takeUntil.png">
   * 
   * @param that
   *            the Observable whose first emitted item will cause <code>takeUntil</code> to stop
   *            emitting items from the source Observable
   * @param <E>
   *            the type of items emitted by <code>other</code>
   * @return an Observable that emits the items of the source Observable until such time as
   *         <code>other</code> emits its first item
   */
  def takeUntil[E](that: Observable[E]): Observable[T] = {
    new Observable[T](asJava.takeUntil(that.asJava))
  } 
 
  /**
   * Returns an Observable that emits a single item, a list composed of all the items emitted by
   * the source Observable.
   * <p>
   * <img width="640" src="https://raw.github.com/wiki/Netflix/RxJava/images/rx-operators/toList.png">
   * <p>
   * Normally, an Observable that returns multiple items will do so by invoking its {@link Observer}'s {@link Observer#onNext onNext} method for each such item. You can change
   * this behavior, instructing the Observable to compose a list of all of these items and then to
   * invoke the Observer's <code>onNext</code> function once, passing it the entire list, by
   * calling the Observable's <code>toList</code> method prior to calling its {@link #subscribe} method.
   * <p>
   * Be careful not to use this operator on Observables that emit infinite or very large numbers
   * of items, as you do not have the option to unsubscribe.
   * 
   * @return an Observable that emits a single item: a List containing all of the items emitted by
   *         the source Observable.
   */
  /*
   * TODO variance/immutable?
  def toList: Observable[java.util.List[T]] = {
    // new Observable[java.util.List[T]](asJava.toList())
  }
  */

  // There are no toSortedList methods because Scala can sort itself
  // public Observable<List<T>> toSortedList() 
  // public Observable<List<T>> toSortedList(Func2<? super T, ? super T, Integer> sortFunction) 

  /**
   * Emit a specified set of items before beginning to emit items from the source Observable.
   * <p>
   * <img width="640" src="https://raw.github.com/wiki/Netflix/RxJava/images/rx-operators/startWith.png">
   * 
   * @param values
   *            the items you want the modified Observable to emit first
   * @return an Observable that exhibits the modified behavior
   */
  // TODO wait until core library does not need @SuppressWarnings("unchecked") anymore
  // public Observable<T> startWith(T... values) 
  
  /**
   * Groups the items emitted by an Observable according to a specified criterion, and emits these
   * grouped items as {@link GroupedObservable}s, one GroupedObservable per group.
   * <p>
   * <img width="640" src="https://github.com/Netflix/RxJava/wiki/images/rx-operators/groupBy.png">
   * 
   * @param keySelector
   *            a function that extracts the key from an item
   * @param elementSelector
   *            a function to map a source item to an item in a {@link GroupedObservable}
   * @param <K>
   *            the key type
   * @param <R>
   *            the type of items emitted by the resulting {@link GroupedObservable}s
   * @return an Observable that emits {@link GroupedObservable}s, each of which corresponds to a
   *         unique key value and emits items representing items from the source Observable that
   *         share that key value
   */
  /* TODO do we want to return Scala GroupedObservable or Java GroupedObservable ?
  def groupBy[K,R](keySelector: T => K, elementSelector: T => R ): Observable[GroupedObservable[K,R]] = {
    ???
  }
  */
  // public <K, R> Observable<GroupedObservable<K, R>> groupBy(final Func1<? super T, ? extends K> keySelector, final Func1<? super T, ? extends R> elementSelector) 

  /**
   * Groups the items emitted by an Observable according to a specified criterion, and emits these
   * grouped items as {@link GroupedObservable}s, one GroupedObservable per group.
   * <p>
   * <img width="640" src="https://github.com/Netflix/RxJava/wiki/images/rx-operators/groupBy.png">
   * 
   * @param keySelector
   *            a function that extracts the key for each item
   * @param <K>
   *            the key type
   * @return an Observable that emits {@link GroupedObservable}s, each of which corresponds to a
   *         unique key value and emits items representing items from the source Observable that
   *         share that key value
   */
  /* TODO do we want to return Scala GroupedObservable or Java GroupedObservable ?
  def groupBy[K](keySelector: T => K ): Observable[GroupedObservable[K,T]] = {
    ???
  }
  */
  // public <K> Observable<GroupedObservable<K, T>> groupBy(final Func1<? super T, ? extends K> keySelector) 

  /**
   * Converts an Observable into a {@link BlockingObservable} (an Observable with blocking
   * operators).
   * 
   * @see <a href="https://github.com/Netflix/RxJava/wiki/Blocking-Observable-Operators">Blocking Observable Operators</a>
   */
  // TODO make Scala BlockingObservable
  def toBlockingObservable: rx.observables.BlockingObservable[_ <: T] = {
    asJava.toBlockingObservable()
  }
  
}

object Observable {
  import scala.collection.JavaConverters._
  import scala.concurrent.Future
  import rx.lang.scala.All._
  import rx.{Observable => JObservable}
  import ImplicitFunctionConversions._

  /**
   * Creates an Observable that will execute the given function when an {@link Observer} subscribes to it.
   * <p>
   * <img width="640" src="https://github.com/Netflix/RxJava/wiki/images/rx-operators/create.png">
   * <p>
   * Write the function you pass to <code>create</code> so that it behaves as an Observable: It
   * should invoke the Observer's {@link Observer#onNext onNext}, {@link Observer#onError onError}, and {@link Observer#onCompleted onCompleted} methods
   * appropriately.
   * <p>
   * A well-formed Observable must invoke either the Observer's <code>onCompleted</code> method
   * exactly once or its <code>onError</code> method exactly once.
   * <p>
   * See <a href="http://go.microsoft.com/fwlink/?LinkID=205219">Rx Design Guidelines (PDF)</a>
   * for detailed information.
   * 
   * @param <T>
   *            the type of the items that this Observable emits
   * @param func
   *            a function that accepts an {@code Observer<T>}, invokes its {@code onNext}, {@code onError}, and {@code onCompleted} methods
   *            as appropriate, and returns a {@link Subscription} to allow the Observer to
   *            canceling the subscription
   * @return an Observable that, when an {@link Observer} subscribes to it, will execute the given
   *         function
   */
  def apply[T](func: Observer[T] => Subscription): Observable[T] = {
    //val f1: rx.Observer[T] => Subscription = (o: rx.Observer[T]) => func(new JavaObserverToScalaObserver(o))
    
    new Observable(JObservable.create(func))
  }
  // corresponds to Java's
  // public static <T> Observable<T> create(OnSubscribeFunc<T> func) 
  
  // Java's
  // public static <T> Observable<T> empty()
  // is not needed in Scala because it's a special case of varargs apply
  
  /**
   * Returns an Observable that invokes an {@link Observer}'s {@link Observer#onError onError} method when the Observer subscribes to it
   * <p>
   * <img width="640" src="https://raw.github.com/wiki/Netflix/RxJava/images/rx-operators/error.png">
   * 
   * @param exception
   *            the particular error to report
   * @param <T>
   *            the type of the items (ostensibly) emitted by the Observable
   * @return an Observable that invokes the {@link Observer}'s {@link Observer#onError onError} method when the Observer subscribes to it
   */
  def apply(exception: Throwable): Observable[Nothing] = {
    new Observable(JObservable.error(exception))
  }
  // corresponds to Java's
  // public static <T> Observable<T> error(Throwable exception) 
  
  // There is no method corresponding to
  // public static <T> Observable<T> from(Iterable<? extends T> iterable) 
  // because Scala automatically uses the varargs apply for this

  /**
   * Converts a sequence of values into an Observable.
   * <p>
   * <img width="640" src="https://github.com/Netflix/RxJava/wiki/images/rx-operators/from.png">
   * 
   * <p>Implementation note: the entire array will be immediately emitted each time an {@link Observer} subscribes. Since this occurs before the {@link Subscription} is returned,
   * it in not possible to unsubscribe from the sequence before it completes.
   * 
   * @param items
   *            the source Array
   * @param <T>
   *            the type of items in the Array, and the type of items to be emitted by the
   *            resulting Observable
   * @return an Observable that emits each item in the source Array
   */
  def apply[T](args: T*): Observable[T] = {     
    new Observable(JObservable.from(args.toIterable.asJava))
  }
  // corresponds to Java's
  // public static <T> Observable<T> from(T... items) 
  
  // There is no method corresponding to
  // public static Observable<Integer> range(int start, int count) 
  // because the Scala collection library provides enough methods to create Iterables.
  // Examples: Observable(1 to 5), Observable(1 until 10)
  
  
  /**
   * Returns an Observable that calls an Observable factory to create its Observable for each
   * new Observer that subscribes. That is, for each subscriber, the actuall Observable is determined
   * by the factory function.
   * 
   * <p>
   * <img width="640" src="https://github.com/Netflix/RxJava/wiki/images/rx-operators/defer.png">
   * <p>
   * The defer operator allows you to defer or delay emitting items from an Observable until such
   * time as an Observer subscribes to the Observable. This allows an {@link Observer} to easily
   * obtain updates or a refreshed version of the sequence.
   * 
   * @param observableFactory
   *            the Observable factory function to invoke for each {@link Observer} that
   *            subscribes to the resulting Observable
   * @param <T>
   *            the type of the items emitted by the Observable
   * @return an Observable whose {@link Observer}s trigger an invocation of the given Observable
   *         factory function
   */
  def defer[T](observable: => Observable[T]): Observable[T] = {
    new Observable(JObservable.defer(observable.asJava))
  }
  // corresponds to Java's
  // public static <T> Observable<T> defer(Func0<? extends Observable<? extends T>> observableFactory) 

  /**
   * Returns an Observable that emits a single item and then completes.
   * <p>
   * <img width="640" src="https://raw.github.com/wiki/Netflix/RxJava/images/rx-operators/just.png">
   * <p>
   * To convert any object into an Observable that emits that object, pass that object into the
   * <code>just</code> method.
   * <p>
   * This is similar to the {@link #apply(Iterable[T])} method, except that
   * {@link #apply(Iterable[T])} will convert an {@link Iterable} object into an Observable that emits
   * each of the items in the Iterable, one at a time, while the <code>just()</code> method
   * converts an Iterable into an Observable that emits the entire Iterable as a single item.
   * 
   * @param value
   *            the item to pass to the {@link Observer}'s {@link Observer#onNext onNext} method
   * @param <T>
   *            the type of that item
   * @return an Observable that emits a single item and then completes
   */
  def just[T](value: T): Observable[T] = {
    new Observable(JObservable.just(value))
  }
  // corresponds to Java's
  // public static <T> Observable<T> just(T value) 
  
  /**
   * Flattens a list of Observables into one Observable, without any transformation.
   * <p>
   * <img width="640" src="https://raw.github.com/wiki/Netflix/RxJava/images/rx-operators/merge.png">
   * <p>
   * You can combine the items emitted by multiple Observables so that they act like a single
   * Observable, by using the <code>merge</code> method.
   * 
   * @param source
   *            a list of Observables
   * @return an Observable that emits items that are the result of flattening the {@code source} list of Observables
   * @see <a href="http://msdn.microsoft.com/en-us/library/hh229099(v=vs.103).aspx">MSDN: Observable.Merge</a>
   */
  // public static <T> Observable<T> merge(List<? extends Observable<? extends T>> source) 
  // TODO decide if instance method mergeWith (?)
  
    /**
   * Flattens a sequence of Observables emitted by an Observable into one Observable, without any
   * transformation.
   * <p>
   * <img width="640" src="https://raw.github.com/wiki/Netflix/RxJava/images/rx-operators/merge.png">
   * <p>
   * You can combine the items emitted by multiple Observables so that they act like a single
   * Observable, by using the {@code merge} method.
   * 
   * @param source
   *            an Observable that emits Observables
   * @return an Observable that emits items that are the result of flattening the items emitted
   *         by the Observables emitted by the {@code source} Observable
   * @see <a href="http://msdn.microsoft.com/en-us/library/hh229099(v=vs.103).aspx">MSDN: Observable.Merge Method</a>
   */
  // public static <T> Observable<T> merge(Observable<? extends Observable<? extends T>> source)
  // TODO decide if instance method mergeWith (?)

  /**
   * Flattens a series of Observables into one Observable, without any transformation.
   * <p>
   * <img width="640" src="https://raw.github.com/wiki/Netflix/RxJava/images/rx-operators/merge.png">
   * <p>
   * You can combine items emitted by multiple Observables so that they act like a single
   * Observable, by using the {@code merge} method.
   * 
   * @param source
   *            a series of Observables
   * @return an Observable that emits items that are the result of flattening the items emitted
   *         by the {@code source} Observables
   * @see <a href="http://msdn.microsoft.com/en-us/library/hh229099(v=vs.103).aspx">MSDN: Observable.Merge Method</a>
   */
  // public static <T> Observable<T> merge(Observable<? extends T>... source) 
  // TODO decide if instance method mergeWith (?)

  // There is no method corresponding to
  // public static <T> Observable<T> concat(Observable<? extends T>... source) 
  // because we have the instance method ++ instead
  
  /**
   * This behaves like {@link #merge(java.util.List)} except that if any of the merged Observables
   * notify of an error via {@link Observer#onError onError}, {@code mergeDelayError} will
   * refrain from propagating that error notification until all of the merged Observables have
   * finished emitting items.
   * <p>
   * <img width="640" src="https://raw.github.com/wiki/Netflix/RxJava/images/rx-operators/mergeDelayError.png">
   * <p>
   * Even if multiple merged Observables send {@code onError} notifications, {@code mergeDelayError} will only invoke the {@code onError} method of its
   * Observers once.
   * <p>
   * This method allows an Observer to receive all successfully emitted items from all of the
   * source Observables without being interrupted by an error notification from one of them.
   * 
   * @param source
   *            a list of Observables
   * @return an Observable that emits items that are the result of flattening the items emitted by
   *         the {@code source} list of Observables
   * @see <a href="http://msdn.microsoft.com/en-us/library/hh229099(v=vs.103).aspx">MSDN: Observable.Merge Method</a>
   */
  // public static <T> Observable<T> mergeDelayError(List<? extends Observable<? extends T>> source) 
  // TODO decide if instance method mergeWithDelayError (?)

    /**
   * This behaves like {@link #merge(Observable)} except that if any of the merged Observables
   * notify of an error via {@link Observer#onError onError}, {@code mergeDelayError} will
   * refrain from propagating that error notification until all of the merged Observables have
   * finished emitting items.
   * <p>
   * <img width="640" src="https://raw.github.com/wiki/Netflix/RxJava/images/rx-operators/mergeDelayError.png">
   * <p>
   * Even if multiple merged Observables send {@code onError} notifications, {@code mergeDelayError} will only invoke the {@code onError} method of its
   * Observers once.
   * <p>
   * This method allows an Observer to receive all successfully emitted items from all of the
   * source Observables without being interrupted by an error notification from one of them.
   * 
   * @param source
   *            an Observable that emits Observables
   * @return an Observable that emits items that are the result of flattening the items emitted by
   *         the Observables emitted by the {@code source} Observable
   * @see <a href="http://msdn.microsoft.com/en-us/library/hh229099(v=vs.103).aspx">MSDN: Observable.Merge Method</a>
   */
  // public static <T> Observable<T> mergeDelayError(Observable<? extends Observable<? extends T>> source) 
  // TODO decide if instance method mergeWithDelayError (?)
  
  /**
   * This behaves like {@link #merge(Observable...)} except that if any of the merged Observables
   * notify of an error via {@link Observer#onError onError}, {@code mergeDelayError} will
   * refrain from propagating that error notification until all of the merged Observables have
   * finished emitting items.
   * <p>
   * <img width="640" src="https://raw.github.com/wiki/Netflix/RxJava/images/rx-operators/mergeDelayError.png">
   * <p>
   * Even if multiple merged Observables send {@code onError} notifications, {@code mergeDelayError} will only invoke the {@code onError} method of its
   * Observers once.
   * <p>
   * This method allows an Observer to receive all successfully emitted items from all of the
   * source Observables without being interrupted by an error notification from one of them.
   * 
   * @param source
   *            a series of Observables
   * @return an Observable that emits items that are the result of flattening the items emitted by
   *         the {@code source} Observables
   * @see <a href="http://msdn.microsoft.com/en-us/library/hh229099(v=vs.103).aspx">MSDN: Observable.Merge Method</a>
   */
  // public static <T> Observable<T> mergeDelayError(Observable<? extends T>... source) 
  // TODO decide if instance method mergeWithDelayError (?)
  
  /**
   * Returns an Observable that never sends any items or notifications to an {@link Observer}.
   * <p>
   * <img width="640" src="https://github.com/Netflix/RxJava/wiki/images/rx-operators/never.png">
   * <p>
   * This Observable is useful primarily for testing purposes.
   * 
   * @return an Observable that never sends any items or notifications to an {@link Observer}
   */
  def never: Observable[Nothing] = {
    new Observable(JObservable.never())
  }
  
  // There is no method corresponding to
  // public static <T> Observable<T> switchDo(Observable<? extends Observable<? extends T>> sequenceOfSequences) 
  // because it's deprecated.
  
  /**
   * Given an Observable that emits Observables, creates a single Observable that
   * emits the items emitted by the most recently published of those Observables.
   * <p>
   * <img width="640" src="https://github.com/Netflix/RxJava/wiki/images/rx-operators/switchDo.png">
   * 
   * @param sequenceOfSequences
   *            the source Observable that emits Observables
   * @return an Observable that emits only the items emitted by the most recently published
   *         Observable
   */
  // public static <T> Observable<T> switchOnNext(Observable<? extends Observable<? extends T>> sequenceOfSequences) 
  // TODO this has to be an instance method which is only available on Observable whose type
  // parameter also is an Observable.
  
  // There is no method here corresponding to
  // public static <T> Observable<T> synchronize(Observable<? extends T> observable) 
  // because that's an instance method.

  def apply[T](f: Future[T]): Observable[T] = {
    ??? // TODO convert Scala Future to Java Future
  } 
  // corresponds to
  // public static <T> Observable<T> from(Future<? extends T> future)
  
  def apply[T](f: Future[T], scheduler: Scheduler): Observable[T] = {
    ??? // TODO convert Scala Future to Java Future
  }
  // public static <T> Observable<T> from(Future<? extends T> future, Scheduler scheduler)
  
  // TODO which TimeUnit should we use? scala.actors.threadpool or java.util.concurrent?
  def apply[T](f: Future[T], timeout: Long, unit: Nothing): Observable[T] = {
    ??? // TODO convert Scala Future to Java Future
  }
  // corresponds to
  // public static <T> Observable<T> from(Future<? extends T> future, long timeout, TimeUnit unit)
  
  // There is no method here corresponding to
  // public static <T1, T2, R> Observable<R> zip(Observable<? extends T1> o1, Observable<? extends T2> o2, Func2<? super T1, ? super T2, ? extends R> zipFunction)
  // because it's an instance method
  
  // There is no method corresponding to
  // public static <T1, T2, T3, R> Observable<R> zip(Observable<? extends T1> o1, Observable<? extends T2> o2, Observable<? extends T3> o3, Func3<? super T1, ? super T2, ? super T3, ? extends R> zipFunction)
  // because zip3 is not known in the Scala world
  // Also applies to all zipN with N > 3 ;-)

  /**
   * Combines the given observables, emitting an event containing an aggregation of the latest values of each of the source observables
   * each time an event is received from one of the source observables, where the aggregation is defined by the given function.
   * <p>
   * <img width="640" src="https://github.com/Netflix/RxJava/wiki/images/rx-operators/combineLatest.png">
   * 
   * @param o1
   *            The first source observable.
   * @param o2
   *            The second source observable.
   * @param combineFunction
   *            The aggregation function used to combine the source observable values.
   * @return An Observable that combines the source Observables with the given combine function
   */
  // public static <T1, T2, R> Observable<R> combineLatest(Observable<? extends T1> o1, Observable<? extends T2> o2, Func2<? super T1, ? super T2, ? extends R> combineFunction) 
  // TODO do we want this as an instance method?
  // TODO then decide about combineLatest with > 2 Observables
  
  // TODO what about these two?
  // public static <R> Observable<R> zip(Observable<? extends Observable<?>> ws, final FuncN<? extends R> zipFunction)
  // // public static <R> Observable<R> zip(Collection<? extends Observable<?>> ws, FuncN<? extends R> zipFunction)
  
  
}

// Cannot yet have inner class because of this error message:
// implementation restriction: nested class is not allowed in value class.
// This restriction is planned to be removed in subsequent releases.  
class WithFilter[+T] private[scala] (p: T => Boolean, wrapped: rx.Observable[_ <: T]) {
  import ImplicitFunctionConversions._
  
  def map[B](f: T => B): Observable[B] = new Observable(wrapped.filter(p).map(f))
  def flatMap[B](f: T => Observable[B]): Observable[B] = {
    ??? // TODO
  }
  def foreach(f: T => Unit): Unit = wrapped.filter(p).toBlockingObservable.forEach(f)
  def withFilter(p: T => Boolean): Observable[T] = new Observable(wrapped.filter(p))
}

/*
 * To remove all method bodies from Observable.java and to comment out the signatures,
 * use this regex (works within Eclipse except for 1 long complex method)
 * find: `^    public(.*)\{\s*?\n(.|\n)*?^    }`
 * replace with: `    // public$1`
 */

import org.scalatest.junit.JUnitSuite

class UnitTestSuite extends JUnitSuite {
  import org.junit.{Before, Test}
  import org.junit.Assert._
  import org.mockito.Matchers.any
  import org.mockito.Mockito._
  import org.mockito.{ MockitoAnnotations, Mock }
  import rx.lang.scala.All._
  
  // Tests which needn't be run:
  
  def testCovariance = {
    val o1: Observable[Nothing] = Observable()
    val o2: Observable[Int] = o1
    val o3: Observable[App] = o1
    val o4: Observable[Any] = o2
    val o5: Observable[Any] = o3
  }
  
  // Tests which have to be run:
  
  @Test def testTest() = {
    val a: Observable[Int] = Observable()
    
    println("testTest")
    assertEquals(4, Observable(1, 2, 3, 4).asJava.toBlockingObservable().last())
  }
  
  @Test def testSequenceEqualUnnecessary() {
    // the sequenceEqual is unnecessary
    val first = Observable(10, 11, 12)
    val second = Observable(10, 11, 12)
    
    val b1 = (first zip second) map (p => p._1 == p._2)
    
    val equality = (a: Any, b: Any) => a == b
    
    val b2 = (first zip second) map (p => equality(p._1, p._2))
    
    // TODO assertions
  }
  

}
