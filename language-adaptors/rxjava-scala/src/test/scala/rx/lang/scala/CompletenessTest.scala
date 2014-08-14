/**
 * Copyright 2013 Netflix, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package rx.lang.scala

import java.util.Calendar

import scala.collection.SortedMap
import scala.reflect.runtime.universe
import scala.reflect.runtime.universe.Symbol
import scala.reflect.runtime.universe.Type
import scala.reflect.runtime.universe.typeOf

import org.junit.Ignore
import org.junit.Test
import org.scalatest.junit.JUnitSuite

/**
 * These tests can be used to check if all methods of the Java Observable have a corresponding
 * method in the Scala Observable.
 *
 * These tests don't contain any assertions, so they will always succeed, but they print their
 * results to stdout.
 */
class CompletenessTest extends JUnitSuite {

  // some frequently used comments:
  val unnecessary = "[considered unnecessary in Scala land]"
  val deprecated = "[deprecated in RxJava]"
  val averageProblem = "[We can't have a general average method because Scala's `Numeric` does not have " +
     "scalar multiplication (we would need to calculate `(1.0/numberOfElements)*sum`). " +
     "You can use `fold` instead to accumulate `sum` and `numberOfElements` and divide at the end.]"
  val commentForFirstWithPredicate = "[use `.filter(condition).first`]"
  val fromFuture = "[TODO: Decide how Scala Futures should relate to Observables. Should there be a " +
     "common base interface for Future and Observable? And should Futures also have an unsubscribe method?]"
  val commentForTakeLastBuffer = "[use `takeRight(...).toSeq`]"

  /**
   * Maps each method from the Java Observable to its corresponding method in the Scala Observable
   */
  val correspondence = defaultMethodCorrespondence ++ correspondenceChanges // ++ overrides LHS with RHS

  /**
   * Creates default method correspondence mappings, assuming that Scala methods have the same
   * name and the same argument types as in Java
   */
  def defaultMethodCorrespondence: Map[String, String] = {
    val allMethods = getPublicInstanceAndCompanionMethods(typeOf[rx.Observable[_]])
    val tuples = for (javaM <- allMethods) yield (javaM, javaMethodSignatureToScala(javaM))
    tuples.toMap
  }

  /**
   * Manually added mappings from Java Observable methods to Scala Observable methods
   */
  def correspondenceChanges = Map(
      // manually added entries for Java instance methods
      "all(Func1[_ >: T, Boolean])" -> "forall(T => Boolean)",
      "ambWith(Observable[_ <: T])" -> "amb(Observable[U])",
      "asObservable()" -> unnecessary,
      "buffer(Int)" -> "tumblingBuffer(Int)",
      "buffer(Int, Int)" -> "slidingBuffer(Int, Int)",
      "buffer(Long, TimeUnit)" -> "tumblingBuffer(Duration)",
      "buffer(Long, TimeUnit, Int)" -> "tumblingBuffer(Duration, Int)",
      "buffer(Long, TimeUnit, Int, Scheduler)" -> "tumblingBuffer(Duration, Int, Scheduler)",
      "buffer(Long, TimeUnit, Scheduler)" -> "tumblingBuffer(Duration, Scheduler)",
      "buffer(Long, Long, TimeUnit)" -> "slidingBuffer(Duration, Duration)",
      "buffer(Long, Long, TimeUnit, Scheduler)" -> "slidingBuffer(Duration, Duration, Scheduler)",
      "buffer(Func0[_ <: Observable[_ <: TClosing]])" -> "tumblingBuffer(=> Observable[Any])",
      "buffer(Observable[B])" -> "tumblingBuffer(=> Observable[Any])",
      "buffer(Observable[B], Int)" -> "tumblingBuffer(Observable[Any], Int)",
      "buffer(Observable[_ <: TOpening], Func1[_ >: TOpening, _ <: Observable[_ <: TClosing]])" -> "slidingBuffer(Observable[Opening])(Opening => Observable[Any])",
      "cast(Class[R])" -> "[RxJava needs this one because `rx.Observable` is invariant. `Observable` in RxScala is covariant and does not need this operator.]",
      "collect(R, Action2[R, _ >: T])" -> "foldLeft(R)((R, T) => R)",
      "concatWith(Observable[_ <: T])" -> "[use `o1 ++ o2`]",
      "contains(Any)" -> "contains(U)",
      "count()" -> "length",
      "debounce(Func1[_ >: T, _ <: Observable[U]])" -> "debounce(T => Observable[Any])",
      "defaultIfEmpty(T)" -> "orElse(=> U)",
      "delay(Func0[_ <: Observable[U]], Func1[_ >: T, _ <: Observable[V]])" -> "delay(() => Observable[Any], T => Observable[Any])",
      "delay(Func1[_ >: T, _ <: Observable[U]])" -> "delay(T => Observable[Any])",
      "dematerialize()" -> "dematerialize(<:<[Observable[T], Observable[Notification[U]]])",
      "doOnCompleted(Action0)" -> "doOnCompleted(=> Unit)",
      "doOnEach(Action1[Notification[_ >: T]])" -> "[use `doOnEach(T => Unit, Throwable => Unit, () => Unit)`]",
      "doOnTerminate(Action0)" -> "doOnTerminate(=> Unit)",
      "elementAtOrDefault(Int, T)" -> "elementAtOrDefault(Int, U)",
      "finallyDo(Action0)" -> "finallyDo(=> Unit)",
      "first(Func1[_ >: T, Boolean])" -> commentForFirstWithPredicate,
      "firstOrDefault(T)" -> "firstOrElse(=> U)",
      "firstOrDefault(T, Func1[_ >: T, Boolean])" -> "[use `.filter(condition).firstOrElse(default)`]",
      "forEach(Action1[_ >: T])" -> "foreach(T => Unit)",
      "forEach(Action1[_ >: T], Action1[Throwable])" -> "foreach(T => Unit, Throwable => Unit)",
      "forEach(Action1[_ >: T], Action1[Throwable], Action0)" -> "foreach(T => Unit, Throwable => Unit, () => Unit)",
      "groupByUntil(Func1[_ >: T, _ <: TKey], Func1[_ >: GroupedObservable[TKey, T], _ <: Observable[_ <: TDuration]])" -> "groupByUntil(T => K)((K, Observable[T]) => Observable[Any])",
      "groupByUntil(Func1[_ >: T, _ <: TKey], Func1[_ >: T, _ <: TValue], Func1[_ >: GroupedObservable[TKey, TValue], _ <: Observable[_ <: TDuration]])" -> "groupByUntil(T => K, T => V)((K, Observable[V]) => Observable[Any])",
      "groupJoin(Observable[T2], Func1[_ >: T, _ <: Observable[D1]], Func1[_ >: T2, _ <: Observable[D2]], Func2[_ >: T, _ >: Observable[T2], _ <: R])" -> "groupJoin(Observable[S])(T => Observable[Any], S => Observable[Any], (T, Observable[S]) => R)",
      "ignoreElements()" -> "[use `filter(_ => false)`]",
      "join(Observable[TRight], Func1[T, Observable[TLeftDuration]], Func1[TRight, Observable[TRightDuration]], Func2[T, TRight, R])" -> "join(Observable[S])(T => Observable[Any], S => Observable[Any], (T, S) => R)",
      "last(Func1[_ >: T, Boolean])" -> "[use `filter(predicate).last`]",
      "lastOrDefault(T)" -> "lastOrElse(=> U)",
      "lastOrDefault(T, Func1[_ >: T, Boolean])" -> "[use `filter(predicate).lastOrElse(default)`]",
      "lift(Operator[_ <: R, _ >: T])" -> "lift(Subscriber[R] => Subscriber[T])",
      "limit(Int)" -> "take(Int)",
      "flatMap(Func1[_ >: T, _ <: Observable[_ <: U]], Func2[_ >: T, _ >: U, _ <: R])" -> "flatMapWith(T => Observable[U])((T, U) => R)",
      "flatMapIterable(Func1[_ >: T, _ <: Iterable[_ <: U]], Func2[_ >: T, _ >: U, _ <: R])" -> "flatMapIterableWith(T => Iterable[U])((T, U) => R)",
      "mergeWith(Observable[_ <: T])" -> "merge(Observable[U])",
      "multicast(Subject[_ >: T, _ <: R])" -> "multicast(Subject[R])",
      "multicast(Func0[_ <: Subject[_ >: T, _ <: TIntermediate]], Func1[_ >: Observable[TIntermediate], _ <: Observable[TResult]])" -> "multicast(() => Subject[R])(Observable[R] => Observable[U])",
      "ofType(Class[R])" -> "[use `filter(_.isInstanceOf[Class])`]",
      "onErrorResumeNext(Func1[Throwable, _ <: Observable[_ <: T]])" -> "onErrorResumeNext(Throwable => Observable[U])",
      "onErrorResumeNext(Observable[_ <: T])" -> "onErrorResumeNext(Observable[U])",
      "onErrorReturn(Func1[Throwable, _ <: T])" -> "onErrorReturn(Throwable => U)",
      "onExceptionResumeNext(Observable[_ <: T])" -> "onExceptionResumeNext(Observable[U])",
      "parallel(Func1[Observable[T], Observable[R]])" -> "parallel(Observable[T] => Observable[R])",
      "parallel(Func1[Observable[T], Observable[R]], Scheduler)" -> "parallel(Observable[T] => Observable[R], Scheduler)",
      "publish(Func1[_ >: Observable[T], _ <: Observable[R]])" -> "publish(Observable[T] => Observable[R])",
      "publish(Func1[_ >: Observable[T], _ <: Observable[R]], T)" -> "publish(Observable[T] => Observable[R], T @uncheckedVariance)",
      "publishLast(Func1[_ >: Observable[T], _ <: Observable[R]])" -> "publishLast(Observable[T] => Observable[R])",
      "reduce(Func2[T, T, T])" -> "reduce((U, U) => U)",
      "reduce(R, Func2[R, _ >: T, R])" -> "foldLeft(R)((R, T) => R)",
      "repeatWhen(Func1[_ >: Observable[_ <: Notification[_]], _ <: Observable[_ <: Notification[_]]])" -> "repeatWhen(Observable[Notification[Any]] => Observable[Notification[Any]])",
      "repeatWhen(Func1[_ >: Observable[_ <: Notification[_]], _ <: Observable[_ <: Notification[_]]], Scheduler)" -> "repeatWhen(Observable[Notification[Any]] => Observable[Notification[Any]], Scheduler)",
      "replay(Func1[_ >: Observable[T], _ <: Observable[R]])" -> "replay(Observable[T] => Observable[R])",
      "replay(Func1[_ >: Observable[T], _ <: Observable[R]], Int)" -> "replay(Observable[T] => Observable[R], Int)",
      "replay(Func1[_ >: Observable[T], _ <: Observable[R]], Int, Long, TimeUnit)" -> "replay(Observable[T] => Observable[R], Int, Duration)",
      "replay(Func1[_ >: Observable[T], _ <: Observable[R]], Int, Long, TimeUnit, Scheduler)" -> "replay(Observable[T] => Observable[R], Int, Duration, Scheduler)",
      "replay(Func1[_ >: Observable[T], _ <: Observable[R]], Int, Scheduler)" -> "replay(Observable[T] => Observable[R], Int, Scheduler)",
      "replay(Func1[_ >: Observable[T], _ <: Observable[R]], Long, TimeUnit)" -> "replay(Observable[T] => Observable[R], Duration)",
      "replay(Func1[_ >: Observable[T], _ <: Observable[R]], Long, TimeUnit, Scheduler)" -> "replay(Observable[T] => Observable[R], Duration, Scheduler)",
      "replay(Func1[_ >: Observable[T], _ <: Observable[R]], Scheduler)" -> "replay(Observable[T] => Observable[R], Scheduler)",
      "retry(Func2[Integer, Throwable, Boolean])" -> "retry((Int, Throwable) => Boolean)",
      "retryWhen(Func1[_ >: Observable[_ <: Notification[_]], _ <: Observable[_ <: Notification[_]]], Scheduler)" -> "retryWhen(Observable[Notification[Any]] => Observable[Notification[Any]], Scheduler)",
      "retryWhen(Func1[_ >: Observable[_ <: Notification[_]], _ <: Observable[_]])" -> "retryWhen(Observable[Notification[Any]] => Observable[Any])",
      "sample(Observable[U])" -> "sample(Observable[Any])",
      "scan(Func2[T, T, T])" -> unnecessary,
      "scan(R, Func2[R, _ >: T, R])" -> "scan(R)((R, T) => R)",
      "single(Func1[_ >: T, Boolean])" -> "[use `filter(predicate).single`]",
      "singleOrDefault(T)" -> "singleOrElse(=> U)",
      "singleOrDefault(T, Func1[_ >: T, Boolean])" -> "[use `filter(predicate).singleOrElse(default)`]",
      "skip(Int)" -> "drop(Int)",
      "skip(Long, TimeUnit)" -> "drop(Duration)",
      "skip(Long, TimeUnit, Scheduler)" -> "drop(Duration, Scheduler)",
      "skipWhile(Func1[_ >: T, Boolean])" -> "dropWhile(T => Boolean)",
      "skipWhileWithIndex(Func2[_ >: T, Integer, Boolean])" -> unnecessary,
      "skipUntil(Observable[U])" -> "dropUntil(Observable[Any])",
      "startWith(T)" -> "[use `item +: o`]",
      "startWith(Array[T], Scheduler)" -> "[use `Observable.just(items).subscribeOn(scheduler) ++ o`]",
      "startWith(Iterable[T])" -> "[use `Observable.from(iterable) ++ o`]",
      "startWith(Iterable[T], Scheduler)" -> "[use `Observable.from(iterable).subscribeOn(scheduler) ++ o`]",
      "startWith(Observable[T])" -> "[use `++`]",
      "skipLast(Int)" -> "dropRight(Int)",
      "skipLast(Long, TimeUnit)" -> "dropRight(Duration)",
      "skipLast(Long, TimeUnit, Scheduler)" -> "dropRight(Duration, Scheduler)",
      "subscribe()" -> "subscribe()",
      "takeFirst(Func1[_ >: T, Boolean])" -> "[use `filter(condition).take(1)`]",
      "takeLast(Int)" -> "takeRight(Int)",
      "takeLast(Long, TimeUnit)" -> "takeRight(Duration)",
      "takeLast(Long, TimeUnit, Scheduler)" -> "takeRight(Duration, Scheduler)",
      "takeLast(Int, Long, TimeUnit)" -> "takeRight(Int, Duration)",
      "takeLast(Int, Long, TimeUnit, Scheduler)" -> "takeRight(Int, Duration, Scheduler)",
      "takeLastBuffer(Int)" -> commentForTakeLastBuffer,
      "takeLastBuffer(Int, Long, TimeUnit)" -> commentForTakeLastBuffer,
      "takeLastBuffer(Int, Long, TimeUnit, Scheduler)" -> commentForTakeLastBuffer,
      "takeLastBuffer(Long, TimeUnit)" -> commentForTakeLastBuffer,
      "takeLastBuffer(Long, TimeUnit, Scheduler)" -> commentForTakeLastBuffer,
      "takeUntil(Observable[_ <: E])" -> "takeUntil(Observable[Any])",
      "takeWhileWithIndex(Func2[_ >: T, _ >: Integer, Boolean])" -> "[use `.zipWithIndex.takeWhile{case (elem, index) => condition}.map(_._1)`]",
      "timeout(Func0[_ <: Observable[U]], Func1[_ >: T, _ <: Observable[V]], Observable[_ <: T])" -> "timeout(() => Observable[Any], T => Observable[Any], Observable[U])",
      "timeout(Func1[_ >: T, _ <: Observable[V]], Observable[_ <: T])" -> "timeout(T => Observable[Any], Observable[U])",
      "timeout(Func0[_ <: Observable[U]], Func1[_ >: T, _ <: Observable[V]])" -> "timeout(() => Observable[Any], T => Observable[Any])",
      "timeout(Func1[_ >: T, _ <: Observable[V]])" -> "timeout(T => Observable[Any])",
      "timeout(Long, TimeUnit, Observable[_ <: T])" -> "timeout(Duration, Observable[U])",
      "timeout(Long, TimeUnit, Observable[_ <: T], Scheduler)" -> "timeout(Duration, Observable[U], Scheduler)",
      "timer(Long, Long, TimeUnit)" -> "timer(Duration, Duration)",
      "timer(Long, Long, TimeUnit, Scheduler)" -> "timer(Duration, Duration, Scheduler)",
      "toList()" -> "toSeq",
      "toMultimap(Func1[_ >: T, _ <: K], Func1[_ >: T, _ <: V], Func0[_ <: Map[K, Collection[V]]])" -> "toMultimap(T => K, T => V, () => M)",
      "toMultimap(Func1[_ >: T, _ <: K], Func1[_ >: T, _ <: V], Func0[_ <: Map[K, Collection[V]]], Func1[_ >: K, _ <: Collection[V]])" -> "toMultimap(T => K, T => V, () => M, K => B)",
      "toSortedList()" -> "[Sorting is already done in Scala's collection library, use `.toSeq.map(_.sorted)`]",
      "toSortedList(Func2[_ >: T, _ >: T, Integer])" -> "[Sorting is already done in Scala's collection library, use `.toSeq.map(_.sortWith(f))`]",
      "window(Int)" -> "tumbling(Int)",
      "window(Int, Int)" -> "sliding(Int, Int)",
      "window(Long, TimeUnit)" -> "tumbling(Duration)",
      "window(Long, TimeUnit, Int)" -> "tumbling(Duration, Int)",
      "window(Long, TimeUnit, Int, Scheduler)" -> "tumbling(Duration, Int, Scheduler)",
      "window(Long, TimeUnit, Scheduler)" -> "tumbling(Duration, Scheduler)",
      "window(Observable[U])" -> "tumbling(=> Observable[Any])",
      "window(Func0[_ <: Observable[_ <: TClosing]])" -> "tumbling(=> Observable[Any])",
      "window(Observable[_ <: TOpening], Func1[_ >: TOpening, _ <: Observable[_ <: TClosing]])" -> "sliding(Observable[Opening])(Opening => Observable[Any])",
      "window(Long, Long, TimeUnit)" -> "sliding(Duration, Duration)",
      "window(Long, Long, TimeUnit, Scheduler)" -> "sliding(Duration, Duration, Scheduler)",

      // manually added entries for Java static methods
      "amb(Iterable[_ <: Observable[_ <: T]])" -> "amb(Observable[T]*)",
      "create(OnSubscribe[T])" -> "apply(Subscriber[T] => Unit)",
      "combineLatest(Observable[_ <: T1], Observable[_ <: T2], Func2[_ >: T1, _ >: T2, _ <: R])" -> "combineLatest(Observable[U])",
      "combineLatest(List[_ <: Observable[_ <: T]], FuncN[_ <: R])" -> "combineLatest(Seq[Observable[T]])(Seq[T] => R)",
      "concat(Observable[_ <: Observable[_ <: T]])" -> "concat(<:<[Observable[T], Observable[Observable[U]]])",
      "defer(Func0[Observable[T]])" -> "defer(=> Observable[T])",
      "from(<repeated...>[T])" -> "items(T*)",
      "from(Array[T], Scheduler)" -> "from(Iterable[T], Scheduler)",
      "from(Iterable[_ <: T])" -> "from(Iterable[T])",
      "from(Future[_ <: T])" -> fromFuture,
      "from(Future[_ <: T], Long, TimeUnit)" -> fromFuture,
      "from(Future[_ <: T], Scheduler)" -> fromFuture,
      "just(T)" -> "[use `items(T*)`]",
      "just(T, Scheduler)" -> "[use `items(T*).subscribeOn(scheduler)`]",
      "merge(Observable[_ <: T], Observable[_ <: T])" -> "merge(Observable[U])",
      "merge(Observable[_ <: Observable[_ <: T]])" -> "flatten(<:<[Observable[T], Observable[Observable[U]]])",
      "merge(Observable[_ <: Observable[_ <: T]], Int)" -> "flatten(Int)(<:<[Observable[T], Observable[Observable[U]]])",
      "merge(Array[Observable[_ <: T]])" -> "[use `Observable.from(array).flatten`]",
      "merge(Array[Observable[_ <: T]], Scheduler)" -> "[use `Observable.from(array, scheduler).flatten`]",
      "merge(Iterable[_ <: Observable[_ <: T]])" -> "[use `Observable.from(iter).flatten`]",
      "merge(Iterable[_ <: Observable[_ <: T]], Int)" -> "[use `Observable.from(iter).flatten(n)`]",
      "merge(Iterable[_ <: Observable[_ <: T]], Int, Scheduler)" -> "[use `Observable.from(iter, scheduler).flatten(n)`]",
      "merge(Iterable[_ <: Observable[_ <: T]], Scheduler)" -> "[use `Observable.from(iter, scheduler).flatten`]",
      "mergeDelayError(Observable[_ <: T], Observable[_ <: T])" -> "mergeDelayError(Observable[U])",
      "mergeDelayError(Observable[_ <: Observable[_ <: T]])" -> "flattenDelayError(<:<[Observable[T], Observable[Observable[U]]])",
      "parallelMerge(Observable[Observable[T]], Int)" -> "parallelMerge(Int)(<:<[Observable[T], Observable[Observable[U]]])",
      "parallelMerge(Observable[Observable[T]], Int, Scheduler)" -> "parallelMerge(Int, Scheduler)(<:<[Observable[T], Observable[Observable[U]]])",
      "sequenceEqual(Observable[_ <: T], Observable[_ <: T])" -> "sequenceEqual(Observable[U])",
      "sequenceEqual(Observable[_ <: T], Observable[_ <: T], Func2[_ >: T, _ >: T, Boolean])" -> "sequenceEqualWith(Observable[U])((U, U) => Boolean)",
      "range(Int, Int)" -> "[use `(start until (start + count)).toObservable` instead of `range(start, count)`]",
      "range(Int, Int, Scheduler)" -> "[use `(start until (start + count)).toObservable.subscribeOn(scheduler)` instead of `range(start, count, scheduler)`]",
      "switchOnNext(Observable[_ <: Observable[_ <: T]])" -> "switch(<:<[Observable[T], Observable[Observable[U]]])",
      "zip(Observable[_ <: T1], Observable[_ <: T2], Func2[_ >: T1, _ >: T2, _ <: R])" -> "[use instance method `zip` and `map`]",
      "zip(Observable[_ <: Observable[_]], FuncN[_ <: R])" -> "[use `zip` in companion object and `map`]",
      "zip(Iterable[_ <: Observable[_]], FuncN[_ <: R])" -> "[use `zip` in companion object and `map`]",
      "zipWith(Observable[_ <: T2], Func2[_ >: T, _ >: T2, _ <: R])" -> "zipWith(Observable[U])((T, U) => R)",
      "zip(Iterable[_ <: T2], Func2[_ >: T, _ >: T2, _ <: R])" -> "zipWith(Iterable[U])((T, U) => R)"
  ) ++ List.iterate("T, T", 8)(s => s + ", T").map(
      // all 9 overloads of startWith:
      "startWith(" + _ + ")" -> "[use `Observable.just(...) ++ o`]"
  ).toMap ++ List.iterate("Observable[_ <: T]", 9)(s => s + ", Observable[_ <: T]").map(
      // concat 2-9
      "concat(" + _ + ")" -> "[unnecessary because we can use `++` instead or `Observable(o1, o2, ...).concat`]"
  ).drop(1).toMap ++ List.iterate("T", 10)(s => s + ", T").map(
      // all 10 overloads of from:
      "from(" + _ + ")" -> "[use `items(T*)`]"
  ).toMap ++ (3 to 9).map(i => {
    // zip3-9:
    val obsArgs = (1 to i).map(j => s"Observable[_ <: T$j], ").mkString("")
    val funcParams = (1 to i).map(j => s"_ >: T$j, ").mkString("")
    ("zip(" + obsArgs + "Func" + i + "[" + funcParams + "_ <: R])", unnecessary)
  }).toMap ++ List.iterate("Observable[_ <: T]", 9)(s => s + ", Observable[_ <: T]").map(
      // merge 3-9:
      "merge(" + _ + ")" -> "[unnecessary because we can use `Observable(o1, o2, ...).flatten` instead]"
  ).drop(2).toMap ++ List.iterate("Observable[_ <: T]", 9)(s => s + ", Observable[_ <: T]").map(
      // mergeDelayError 3-9:
      "mergeDelayError(" + _ + ")" -> "[unnecessary because we can use `Observable(o1, o2, ...).flattenDelayError` instead]"
  ).drop(2).toMap ++ (3 to 9).map(i => {
    // combineLatest 3-9:
    val obsArgs = (1 to i).map(j => s"Observable[_ <: T$j], ").mkString("")
    val funcParams = (1 to i).map(j => s"_ >: T$j, ").mkString("")
    ("combineLatest(" + obsArgs + "Func" + i + "[" + funcParams + "_ <: R])", "[If C# doesn't need it, Scala doesn't need it either ;-)]")
  }).toMap ++ List.iterate("Observable[_ <: T]", 9)(s => s + ", Observable[_ <: T]").map(
    // amb 2-9
    "amb(" + _ + ")" -> "[unnecessary because we can use `o1 amb o2` instead or `amb(List(o1, o2, o3, ...)`]"
  ).drop(1).toMap

  def removePackage(s: String) = s.replaceAll("(\\w+\\.)+(\\w+)", "$2")

  def methodMembersToMethodStrings(members: Iterable[Symbol]): Iterable[String] = {
    for (member <- members; alt <- member.asTerm.alternatives) yield {
      val m = alt.asMethod
      // multiple parameter lists in case of curried functions
      val paramListStrs = for (paramList <- m.paramss) yield {
        paramList.map(
            symb => removePackage(symb.typeSignature.toString.replaceAll(",(\\S)", ", $1"))
        ).mkString("(", ", ", ")")
      }
      val name = alt.asMethod.name.decoded
      name + paramListStrs.mkString("")
    }
  }

  def getPublicInstanceMethods(tp: Type): Iterable[String] = {
    // declarations: => only those declared in Observable
    // members => also those of superclasses
    methodMembersToMethodStrings(tp.declarations.filter {
      m =>
        m.isMethod && m.isPublic &&
          m.annotations.forall(_.toString != "java.lang.Deprecated") // don't check deprecated classes
    })
    // TODO how can we filter out instance methods which were put into companion because
    // of extends AnyVal in a way which does not depend on implementation-chosen name '$extension'?
    .filter(! _.contains("$extension"))
    // `access$000` is public. How to distinguish it from others without hard-code?
    .filter(! _.contains("access$000"))
  }

  // also applicable for Java types
  def getPublicInstanceAndCompanionMethods(tp: Type): Iterable[String] =
    getPublicInstanceMethods(tp) ++
      getPublicInstanceMethods(tp.typeSymbol.companionSymbol.typeSignature)

  def printMethodSet(title: String, tp: Type) {
    println("\n" + title)
    println(title.map(_ => '-') + "\n")
    getPublicInstanceMethods(tp).toList.sorted.foreach(println(_))
  }

  @Ignore // because spams output
  @Test def printJavaInstanceMethods(): Unit = {
    printMethodSet("Instance methods of rx.Observable",
                   typeOf[rx.Observable[_]])
  }

  @Ignore // because spams output
  @Test def printScalaInstanceMethods(): Unit = {
    printMethodSet("Instance methods of rx.lang.scala.Observable",
                   typeOf[rx.lang.scala.Observable[_]])
  }

  @Ignore // because spams output
  @Test def printJavaStaticMethods(): Unit = {
    printMethodSet("Static methods of rx.Observable",
                   typeOf[rx.Observable[_]].typeSymbol.companionSymbol.typeSignature)
  }

  @Ignore // because spams output
  @Test def printScalaCompanionMethods(): Unit = {
    printMethodSet("Companion methods of rx.lang.scala.Observable",
                   typeOf[rx.lang.scala.Observable.type])
  }

  def javaMethodSignatureToScala(s: String): String = {
    s.replaceAllLiterally("Long, TimeUnit", "Duration")
     .replaceAll("Action0", "() => Unit")
     // nested [] can't be parsed with regex, so these will have to be added manually
     .replaceAll("Action1\\[([^]]*)\\]", "$1 => Unit")
     .replaceAll("Action2\\[([^]]*), ([^]]*)\\]", "($1, $2) => Unit")
     .replaceAll("Func0\\[([^]]*)\\]", "() => $1")
     .replaceAll("Func1\\[([^]]*), ([^]]*)\\]", "$1 => $2")
     .replaceAll("Func2\\[([^]]*), ([^]]*), ([^]]*)\\]", "($1, $2) => $3")
     .replaceAllLiterally("_ <: ", "")
     .replaceAllLiterally("_ >: ", "")
     .replaceAll("(\\w+)\\(\\)", "$1")
  }

  @Ignore // because spams output
  @Test def printDefaultMethodCorrespondence(): Unit = {
    println("\nDefault Method Correspondence")
    println(  "-----------------------------\n")
    val c = SortedMap(defaultMethodCorrespondence.toSeq : _*)
    val len = c.keys.map(_.length).max + 2
    for ((javaM, scalaM) <- c) {
      println(s"""      %-${len}s -> %s,""".format("\"" + javaM + "\"", "\"" + scalaM + "\""))
    }
  }

  @Ignore // because spams output
  @Test def printCorrectedMethodCorrespondence(): Unit = {
    println("\nCorrected Method Correspondence")
    println(  "-------------------------------\n")
    val c = SortedMap(correspondence.toSeq : _*)
    for ((javaM, scalaM) <- c) {
      println("%s -> %s,".format("\"" + javaM + "\"", "\"" + scalaM + "\""))
    }
  }

  def checkMethodPresence(expectedMethods: Iterable[String], tp: Type): Unit = {
    val actualMethods = getPublicInstanceAndCompanionMethods(tp).toSet
    val expMethodsSorted = expectedMethods.toList.sorted
    var good = 0
    var bad = 0
    for (m <- expMethodsSorted) if (actualMethods.contains(m) || m.charAt(0) == '[') {
      good += 1
    } else {
      bad += 1
      println(s"Warning: $m is NOT present in $tp")
    }
    val status = if (bad == 0) "SUCCESS" else "BAD"
    println(s"$status: $bad out of ${bad+good} methods were not found in $tp")
  }

  @Test def checkScalaMethodPresenceVerbose(): Unit = {
    println("\nTesting that all mentioned Scala methods exist")
    println(  "----------------------------------------------\n")

    val actualMethods = getPublicInstanceAndCompanionMethods(typeOf[rx.lang.scala.Observable[_]]).toSet
    var good = 0
    var bad = 0
    for ((javaM, scalaM) <- SortedMap(correspondence.toSeq :_*)) {
      if (actualMethods.contains(scalaM) || scalaM.charAt(0) == '[') {
        good += 1
      } else {
        bad += 1
        println(s"Warning:")
        println(s"$scalaM is NOT present in Scala Observable")
        println(s"$javaM is the method in Java Observable generating this warning")
      }
    }
    val status = if (bad == 0) "SUCCESS" else "BAD"
    println(s"\n$status: $bad out of ${bad+good} methods were not found in Scala Observable")
  }

  def setTodoForMissingMethods(corresp: Map[String, String]): Map[String, String] = {
    val actualMethods = getPublicInstanceAndCompanionMethods(typeOf[rx.lang.scala.Observable[_]]).toSet
    for ((javaM, scalaM) <- corresp) yield
      (javaM, if (actualMethods.contains(scalaM) || scalaM.charAt(0) == '[') scalaM else "[**TODO: missing**]")
  }

  @Test def checkJavaMethodPresence(): Unit = {
    println("\nTesting that all mentioned Java methods exist")
    println(  "---------------------------------------------\n")
    checkMethodPresence(correspondence.keys, typeOf[rx.Observable[_]])
  }

  @Ignore // because we prefer the verbose version
  @Test def checkScalaMethodPresence(): Unit = {
    checkMethodPresence(correspondence.values, typeOf[rx.lang.scala.Observable[_]])
  }

  def scalaToJavaSignature(s: String) =
    s.replaceAllLiterally("_ <:", "? extends")
     .replaceAllLiterally("_ >:", "? super")
     .replaceAllLiterally("[", "<")
     .replaceAllLiterally("]", ">")
     .replaceAllLiterally("Array<T>", "T[]")

  def escapeJava(s: String) =
    s.replaceAllLiterally("<", "&lt;")
     .replaceAllLiterally(">", "&gt;")

  @Ignore // because spams output
  @Test def printMarkdownCorrespondenceTable() {
    def isInteresting(p: (String, String)): Boolean =
      p._1.replaceAllLiterally("()", "") != p._2
    def groupingKey(p: (String, String)): (String, String) =
      (if (p._1.startsWith("average")) "average" else p._1.takeWhile(_ != '('), p._2)
    def formatJavaCol(name: String, alternatives: Iterable[String]): String = {
      alternatives.toList.sorted.map(scalaToJavaSignature(_)).map(s => {
        if (s.length > 64) {
          val toolTip = escapeJava(s)
          "<span title=\"" + toolTip + "\"><code>" + name + "(...)</code></span>"
        } else {
          "`" + s + "`"
        }
      }).mkString("<br/>")
    }
    def formatScalaCol(s: String): String =
      if (s.startsWith("[") && s.endsWith("]")) s.drop(1).dropRight(1) else "`" + s + "`"
    def escape(s: String) = s.replaceAllLiterally("[", "&lt;").replaceAllLiterally("]", "&gt;")

    println("""
---
layout: comparison
title: Comparison of Scala Observable and Java Observable
---

Note:

*    This table contains both static methods and instance methods.
*    If a signature is too long, move your mouse over it to get the full signature.


| Java Method | Scala Method |
|-------------|--------------|""")

    val ps = setTodoForMissingMethods(correspondence)

    (for (((javaName, scalaCol), pairs) <- ps.groupBy(groupingKey(_)).toList.sortBy(_._1._1)) yield {
      "| " + formatJavaCol(javaName, pairs.map(_._1)) + " | " + formatScalaCol(scalaCol) + " |"
    }).foreach(println(_))
    println(s"\nThis table was generated on ${Calendar.getInstance().getTime}.")
    println(s"**Do not edit**. Instead, edit `${getClass.getCanonicalName}`.")
  }

}
