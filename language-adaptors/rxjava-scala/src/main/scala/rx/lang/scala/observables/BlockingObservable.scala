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
package rx.lang.scala.observables

import scala.collection.JavaConverters._
import scala.concurrent.{Future, Promise}
import rx.lang.scala.ImplicitFunctionConversions._
import rx.lang.scala.Observable
import rx.observables.{BlockingObservable => JBlockingObservable}


/**
 * An Observable that provides blocking operators.
 * 
 * You can obtain a BlockingObservable from an Observable using [[rx.lang.scala.Observable.toBlocking]]
 */
// constructor is private because users should use Observable.toBlocking
class BlockingObservable[+T] private[scala] (val o: Observable[T])
  extends AnyVal
{
  // This is def because "field definition is not allowed in value class"
  private def asJava: JBlockingObservable[_ <: T] = o.asJavaObservable.toBlocking
  /**
   * Invoke a method on each item emitted by the {@link Observable}; block until the Observable
   * completes.
   * 
   * NOTE: This will block even if the Observable is asynchronous.
   * 
   * This is similar to {@link Observable#subscribe(Observer)}, but it blocks. Because it blocks it does
   * not need the {@link Observer#onCompleted()} or {@link Observer#onError(Throwable)} methods.
   * 
   * <img width="640" src="https://github.com/Netflix/RxJava/wiki/images/rx-operators/B.forEach.png">
   *
   * @param f
   *            the {@link Action1} to invoke for every item emitted by the {@link Observable}
   * @throws RuntimeException
   *             if an error occurs
   */
  def foreach(f: T => Unit): Unit = {
    asJava.forEach(f)
  }
  
  def withFilter(p: T => Boolean): WithFilter[T] = {
    new WithFilter[T](p, asJava)
  }

  /**
   * Returns the last item emitted by a specified [[Observable]], or
   * throws `NoSuchElementException` if it emits no items.
   * 
   * <img width="640" src="https://github.com/Netflix/RxJava/wiki/images/rx-operators/B.last.png">
   * 
   * @return the last item emitted by the source [[Observable]]
   * @throws NoSuchElementException
   *             if source contains no elements
   * @see <a href="https://github.com/Netflix/RxJava/wiki/Blocking-Observable-Operators#last-and-lastordefault">RxJava Wiki: last()</a>
   * @see <a href="http://msdn.microsoft.com/en-us/library/system.reactive.linq.observable.last.aspx">MSDN: Observable.Last</a>
   */
  def last : T = {
    asJava.last : T
  }

  /**
   * Returns an `Option` with the last item emitted by the source Observable,
   * or `None` if the source Observable completes without emitting any items.
   *
   * @return an `Option` with the last item emitted by the source Observable,
   *         or `None` if the source Observable is empty
   */
  def lastOption: Option[T] = {
    o.lastOption.toBlocking.single
  }

  /**
   * Returns the last item emitted by the source Observable, or a default item
   * if the source Observable completes without emitting any items.
   *
   * <img width="640" src="https://raw.github.com/wiki/Netflix/RxJava/images/rx-operators/lastOrDefault.png">
   *
   * @param default the default item to emit if the source Observable is empty.
   *                This is a by-name parameter, so it is only evaluated if the source Observable doesn't emit anything.
   * @return the last item emitted by the source Observable, or a default item if the source Observable is empty
   */
  def lastOrElse[U >: T](default: => U): U = {
    lastOption getOrElse default
  }

  /**
   * Returns the first item emitted by a specified [[Observable]], or
   * `NoSuchElementException` if source contains no elements.
   * 
   * @return the first item emitted by the source [[Observable]]
   * @throws NoSuchElementException
   *             if source contains no elements
   * @see <a href="https://github.com/Netflix/RxJava/wiki/Blocking-Observable-Operators#first-and-firstordefault">RxJava Wiki: first()</a>
   * @see <a href="http://msdn.microsoft.com/en-us/library/hh229177.aspx">MSDN: Observable.First</a>
   */
  def first : T = {
    asJava.first : T
  }

  /**
   * Returns the first item emitted by a specified [[Observable]], or
   * `NoSuchElementException` if source contains no elements.
   * 
   * @return the first item emitted by the source [[Observable]]
   * @throws NoSuchElementException
   *             if source contains no elements
   * @see <a href="https://github.com/Netflix/RxJava/wiki/Blocking-Observable-Operators#first-and-firstordefault">RxJava Wiki: first()</a>
   * @see <a href="http://msdn.microsoft.com/en-us/library/hh229177.aspx">MSDN: Observable.First</a>
   * @see [[BlockingObservable.first]]
   */
  def head : T = first

  /**
   * Returns an `Option` with the very first item emitted by the source Observable,
   * or `None` if the source Observable is empty.
   *
   * @return an `Option` with the very first item from the source,
   *         or `None` if the source Observable completes without emitting any item.
   */
  def headOption: Option[T] = {
    o.headOption.toBlocking.single
  }

  /**
   * Returns the very first item emitted by the source Observable, or a default value if the source Observable is empty.
   *
   * <img width="640" src="https://raw.github.com/wiki/Netflix/RxJava/images/rx-operators/firstOrDefault.png">
   *
   * @param default The default value to emit if the source Observable doesn't emit anything.
   *                This is a by-name parameter, so it is only evaluated if the source Observable doesn't emit anything.
   * @return the very first item from the source, or a default value if the source Observable completes without emitting any item.
   */
  def headOrElse[U >: T](default: => U): U = {
    headOption getOrElse default
  }

  /**
   * Returns an {@link Iterable} that always returns the item most recently emitted by an {@link Observable}.
   * <p>
   * <img width="640" src="https://github.com/Netflix/RxJava/wiki/images/rx-operators/B.mostRecent.png">
   *
   * @param initialValue
   *            the initial value that will be yielded by the {@link Iterable} sequence if the {@link Observable} has not yet emitted an item
   * @return an {@link Iterable} that on each iteration returns the item that the {@link Observable} has most recently emitted
   */
  def mostRecent[U >: T](initialValue: U): Iterable[U] = {
    val asJavaU = asJava.asInstanceOf[rx.observables.BlockingObservable[U]]
    asJavaU.mostRecent(initialValue).asScala: Iterable[U] // useless ascription because of compiler bug
  }

  /**
   * Returns an {@link Iterable} that blocks until the {@link Observable} emits another item,
   * then returns that item.
   * <p>
   * <img width="640" src="https://github.com/Netflix/RxJava/wiki/images/rx-operators/B.next.png">
   *
   * @return an {@link Iterable} that blocks upon each iteration until the {@link Observable} emits a new item, whereupon the Iterable returns that item
   */
  def next: Iterable[T] = {
    asJava.next().asScala: Iterable[T] // useless ascription because of compiler bug
  }

  /**
   * If the source Observable completes after emitting a single item, return that item. If the source Observable
   * emits more than one item or no items, notify of an `IllegalArgumentException` or `NoSuchElementException` respectively.
   *
   * <img width="640" src="https://raw.github.com/wiki/Netflix/RxJava/images/rx-operators/single.png">
   *
   * @return an Observable that emits the single item emitted by the source Observable
   * @throws IllegalArgumentException if the source emits more than one item
   * @throws NoSuchElementException if the source emits no items
   */
  def single: T = {
    asJava.single(): T // useless ascription because of compiler bug
  }

  /**
   * If the source Observable completes after emitting a single item, return an `Option` with that item;
   * if the source Observable is empty, return `None`. If the source Observable emits more than one item,
   * throw an `IllegalArgumentException`.
   *
   * @return an `Option` with the single item emitted by the source Observable, or
   *         `None` if the source Observable is empty
   * @throws IllegalArgumentException if the source Observable emits more than one item
   */
  def singleOption: Option[T] = {
    o.singleOption.toBlocking.single
  }

  /**
   * If the source Observable completes after emitting a single item, return that item;
   * if the source Observable is empty, return a default item. If the source Observable
   * emits more than one item, throw an `IllegalArgumentException`.
   *
   * <img width="640" src="https://raw.github.com/wiki/Netflix/RxJava/images/rx-operators/singleOrDefault.png">
   *
   * @param default a default value to emit if the source Observable emits no item.
   *                This is a by-name parameter, so it is only evaluated if the source Observable doesn't emit anything.
   * @return the single item emitted by the source Observable, or a default item if
   *         the source Observable is empty
   * @throws IllegalArgumentException if the source Observable emits more than one item
   */
  def singleOrElse[U >: T](default: => U): U = {
    singleOption getOrElse default
  }

  /**
   * Returns an {@link Iterator} that iterates over all items emitted by this {@link Observable}.
   */
  def toIterable: Iterable[T] = {
    asJava.toIterable.asScala: Iterable[T] // useless ascription because of compiler bug
  }

  /**
   * Returns a {@link List} that contains all items emitted by this {@link Observable}.
   */
  def toList: List[T] = {
    asJava.toIterable.asScala.toList: List[T] // useless ascription because of compiler bug
  }

  /**
   * Returns an `Iterable` that returns the latest item emitted by this `BlockingObservable`,
   * waiting if necessary for one to become available.
   *
   * If this `BlockingObservable` produces items faster than `Iterator.next` takes them,
   * `onNext` events might be skipped, but `onError` or `onCompleted` events are not.
   *
   * Note also that an `onNext` directly followed by `onCompleted` might hide the `onNext` event.
   *
   * @return an `Iterable` that always returns the latest item emitted by this `BlockingObservable`
   */
  def latest: Iterable[T] = {
    asJava.latest.asScala: Iterable[T] // useless ascription because of compiler bug
  }

  /**
   * Returns a `Future` representing the single value emitted by this `BlockingObservable`.
   *
   * `toFuture` throws an `IllegalArgumentException` if the `BlockingObservable` emits more than one item. If the
   * `BlockingObservable` may emit more than item, use `BlockingObservable.toList.toFuture`.
   *
   * `toFuture` throws an `NoSuchElementException` if the `BlockingObservable` is empty.
   *
   * <img width="640" height="395" src="https://github.com/Netflix/RxJava/wiki/images/rx-operators/B.toFuture.png">
   *
   * @return a `Future` that expects a single item to be emitted by this `BlockingObservable`.
   */
  def toFuture: Future[T] = {
    val p = Promise[T]()
    o.single.subscribe(t => p.success(t), e => p.failure(e))
    p.future
  }
}

// Cannot yet have inner class because of this error message: 
// "implementation restriction: nested class is not allowed in value class.
// This restriction is planned to be removed in subsequent releases."  
private[observables] class WithFilter[+T] (p: T => Boolean, asJava: rx.observables.BlockingObservable[_ <: T]) {
  import rx.lang.scala.ImplicitFunctionConversions._
  
  // there's no map and flatMap here, they're only available on Observable
  
  def withFilter(q: T => Boolean) = new WithFilter[T]((x: T) => p(x) && q(x), asJava)
  
  def foreach(f: T => Unit): Unit = {
    asJava.forEach((e: T) => {
      if (p(e)) f(e)
    })
  }
  
}


