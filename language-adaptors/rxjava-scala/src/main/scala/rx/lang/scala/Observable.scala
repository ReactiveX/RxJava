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
  type Timestamped[T] = rx.util.Timestamped[_ <: T]
  type Observer[T] = rx.Observer[_ >: T]
  type Notification[T] = rx.Notification[_ <: T]
  // rx.Observable not here because we need its static methods, and users don't need it
  
  type Subscription = rx.Subscription
  type Scheduler = rx.Scheduler
}


/**
 * The Observable interface that implements the Reactive Pattern.
 */
class Observable[+T](val asJava: rx.Observable[_ <: T]) extends AnyVal {
  import All._
  import rx.{Observable => JObservable}
  import ImplicitFunctionConversions._
/*
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
  */
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
  /*
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
   * TODO doc
   */
  def ++[U >: T](that: Observable[U]): Observable[U] = {
    val o1: JObservable[_ <: U] = this.asJava
    val o2: JObservable[_ <: U] = that.asJava
    new Observable(JObservable.concat(o1, o2))
  }
  */

  
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
  // If 1st line commented, 2nd active: OK
  // If 1st line active, 2nd commented: compiler crashes
  //def synchronize: Observable[T] = {
  def synchronize: Any = {
    new Observable(JObservable.synchronize(asJava))
  }
  
  /*
  /**
   * Wraps each item emitted by a source Observable in a {@link Timestamped} object.
   * <p>
   * <img width="640" src="https://github.com/Netflix/RxJava/wiki/images/rx-operators/timestamp.png">
   * 
   * @return an Observable that emits timestamped items from the source Observable
   */
  def timestamp: Observable[Timestamped[T]] = {
    new Observable(asJava.timestamp())
  }
  
  */
  
}

object Observable {
  import scala.collection.JavaConverters._
  import rx.lang.scala.All._
  import rx.{Observable => JObservable}
  import ImplicitFunctionConversions._
  /*
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

*/  
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
  /*
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
  */

}
