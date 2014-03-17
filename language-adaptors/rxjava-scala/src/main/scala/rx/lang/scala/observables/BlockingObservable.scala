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
import rx.lang.scala.ImplicitFunctionConversions._


/**
 * An Observable that provides blocking operators.
 * 
 * You can obtain a BlockingObservable from an Observable using [[rx.lang.scala.Observable.toBlockingObservable]]
 */
// constructor is private because users should use Observable.toBlockingObservable
class BlockingObservable[+T] private[scala] (val asJava: rx.observables.BlockingObservable[_ <: T]) 
  extends AnyVal 
{

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

  // last                 -> use toIterable.last
  // lastOrDefault        -> use toIterable.lastOption
  // first                -> use toIterable.head
  // firstOrDefault       -> use toIterable.headOption
  // single(predicate)    -> use filter and single
  // singleOrDefault      -> use singleOption

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
   * If this {@link Observable} completes after emitting a single item, return that item,
   * otherwise throw an exception.
   * <p>
   * <img width="640" src="https://github.com/Netflix/RxJava/wiki/images/rx-operators/B.single.png">
   *
   * @return the single item emitted by the {@link Observable}
   */
  def single: T = {
    asJava.single(): T // useless ascription because of compiler bug
  }

  /**
   * If this {@link Observable} completes after emitting a single item, return an Option containing
   * this item, otherwise return {@code None}.
   */
  def singleOption: Option[T] = {
    var size: Int = 0
    var last: Option[T] = None
    for (t <- toIterable) {
      size += 1
      last = Some(t)
    }
    if (size == 1) last else None
  }

  // TODO toFuture()

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


