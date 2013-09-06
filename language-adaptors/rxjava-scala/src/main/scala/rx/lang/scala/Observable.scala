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
 */
class Observable[+T](val asJava: rx.Observable[_ <: T]) extends AnyVal {
  import rx.{Observer, Subscription, Scheduler}
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
  def subscribe(observer: Observer[_ >: T]): Subscription = {
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
  def subscribe(observer: Observer[_ >: T], scheduler: Scheduler): Subscription = {
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
  
  
  
}

object Observable {
  import scala.collection.JavaConverters._
  import rx.{Observable => JObservable, Observer, Subscription}
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
  def apply[T](func: Observer[_ >: T] => Subscription): Observable[T] = {
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
  import rx.{Notification, Observer, Subscription}
  import rx.observables.GroupedObservable
  
  @Test def testTest() = {
    val a: Observable[Int] = Observable()
    
    println("testTest()")
    assertEquals(4, Observable(1, 2, 3, 4).asJava.toBlockingObservable().last())
  }


}
