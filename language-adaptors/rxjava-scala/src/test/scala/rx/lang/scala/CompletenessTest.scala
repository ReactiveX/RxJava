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
      "aggregate(Func2[T, T, T])" -> "reduce((U, U) => U)",
      "aggregate(R, Func2[R, _ >: T, R])" -> "foldLeft(R)((R, T) => R)",
      "all(Func1[_ >: T, Boolean])" -> "forall(T => Boolean)",
      "buffer(Long, Long, TimeUnit)" -> "buffer(Duration, Duration)",
      "buffer(Long, Long, TimeUnit, Scheduler)" -> "buffer(Duration, Duration, Scheduler)",
      "contains(T)" -> "contains(Any)",
      "count()" -> "length",
      "dematerialize()" -> "dematerialize(<:<[Observable[T], Observable[Notification[U]]])",
      "elementAt(Int)" -> "[use `.drop(index).first`]",
      "elementAtOrDefault(Int, T)" -> "[use `.drop(index).firstOrElse(default)`]",
      "first(Func1[_ >: T, Boolean])" -> commentForFirstWithPredicate,
      "firstOrDefault(T)" -> "firstOrElse(=> U)",
      "firstOrDefault(Func1[_ >: T, Boolean], T)" -> "[use `.filter(condition).firstOrElse(default)`]",
      "groupBy(Func1[_ >: T, _ <: K], Func1[_ >: T, _ <: R])" -> "[use `groupBy` and `map`]",
      "lift(Operator[_ <: R, _ >: T])" -> "lift(Subscriber[R] => Subscriber[T])",
      "mapMany(Func1[_ >: T, _ <: Observable[_ <: R]])" -> "flatMap(T => Observable[R])",
      "mapWithIndex(Func2[_ >: T, Integer, _ <: R])" -> "[combine `zipWithIndex` with `map` or with a for comprehension]",
      "onErrorResumeNext(Func1[Throwable, _ <: Observable[_ <: T]])" -> "onErrorResumeNext(Throwable => Observable[U])",
      "onErrorResumeNext(Observable[_ <: T])" -> "onErrorResumeNext(Observable[U])",
      "onErrorReturn(Func1[Throwable, _ <: T])" -> "onErrorReturn(Throwable => U)",
      "onExceptionResumeNext(Observable[_ <: T])" -> "onExceptionResumeNext(Observable[U])",
      "parallel(Func1[Observable[T], Observable[R]])" -> "parallel(Observable[T] => Observable[R])",
      "parallel(Func1[Observable[T], Observable[R]], Scheduler)" -> "parallel(Observable[T] => Observable[R], Scheduler)",
      "reduce(Func2[T, T, T])" -> "reduce((U, U) => U)",
      "reduce(R, Func2[R, _ >: T, R])" -> "foldLeft(R)((R, T) => R)",
      "retry()" -> "retry()",
      "scan(Func2[T, T, T])" -> unnecessary,
      "scan(R, Func2[R, _ >: T, R])" -> "scan(R)((R, T) => R)",
      "skip(Int)" -> "drop(Int)",
      "skip(Long, TimeUnit)" -> "drop(Duration)",
      "skip(Long, TimeUnit, Scheduler)" -> "drop(Duration, Scheduler)",
      "skipWhile(Func1[_ >: T, Boolean])" -> "dropWhile(T => Boolean)",
      "skipWhileWithIndex(Func2[_ >: T, Integer, Boolean])" -> unnecessary,
      "skipUntil(Observable[U])" -> "dropUntil(Observable[E])",
      "startWith(Array[T])" -> "startWith(Iterable[U])",
      "startWith(Array[T], Scheduler)" -> "startWith(Iterable[U], Scheduler)",
      "startWith(Iterable[T])" -> "startWith(Iterable[U])",
      "startWith(Iterable[T], Scheduler)" -> "startWith(Iterable[U], Scheduler)",
      "startWith(Observable[T])" -> "startWith(Observable[U])",
      "skipLast(Int)" -> "dropRight(Int)",
      "skipLast(Long, TimeUnit)" -> "dropRight(Duration)",
      "skipLast(Long, TimeUnit, Scheduler)" -> "dropRight(Duration, Scheduler)",
      "takeFirst()" -> "first",
      "takeFirst(Func1[_ >: T, Boolean])" -> commentForFirstWithPredicate,
      "takeLast(Int)" -> "takeRight(Int)",
      "takeWhileWithIndex(Func2[_ >: T, _ >: Integer, Boolean])" -> "[use `.zipWithIndex.takeWhile{case (elem, index) => condition}.map(_._1)`]",
      "timeout(Func0[_ <: Observable[U]], Func1[_ >: T, _ <: Observable[V]], Observable[_ <: T])" -> "timeout(() => Observable[U], T => Observable[V], Observable[O])",
      "timeout(Func1[_ >: T, _ <: Observable[V]], Observable[_ <: T])" -> "timeout(() => Observable[U], T => Observable[V])",
      "timeout(Long, TimeUnit, Observable[_ <: T])" -> "timeout(Duration, Observable[U])",
      "timeout(Long, TimeUnit, Observable[_ <: T], Scheduler)" -> "timeout(Duration, Observable[U], Scheduler)",
      "timer(Long, Long, TimeUnit)" -> "timer(Duration, Duration)",
      "timer(Long, Long, TimeUnit, Scheduler)" -> "timer(Duration, Duration, Scheduler)",
      "toList()" -> "toSeq",
      "toSortedList()" -> "[Sorting is already done in Scala's collection library, use `.toSeq.map(_.sorted)`]",
      "toSortedList(Func2[_ >: T, _ >: T, Integer])" -> "[Sorting is already done in Scala's collection library, use `.toSeq.map(_.sortWith(f))`]",
      "where(Func1[_ >: T, Boolean])" -> "filter(T => Boolean)",
      "window(Long, Long, TimeUnit)" -> "window(Duration, Duration)",
      "window(Long, Long, TimeUnit, Scheduler)" -> "window(Duration, Duration, Scheduler)",

      // manually added entries for Java static methods
      "average(Observable[Integer])" -> averageProblem,
      "averageDoubles(Observable[Double])" -> averageProblem,
      "averageFloats(Observable[Float])" -> averageProblem,
      "averageLongs(Observable[Long])" -> averageProblem,
      "create(OnSubscribeFunc[T])" -> "apply(Observer[T] => Subscription)",
      "combineLatest(Observable[_ <: T1], Observable[_ <: T2], Func2[_ >: T1, _ >: T2, _ <: R])" -> "combineLatest(Observable[U])",
      "concat(Observable[_ <: Observable[_ <: T]])" -> "concat(<:<[Observable[T], Observable[Observable[U]]])",
      "defer(Func0[_ <: Observable[_ <: T]])" -> "defer(=> Observable[T])",
      "empty()" -> "apply(T*)",
      "error(Throwable)" -> "apply(Throwable)",
      "from(Array[T])" -> "apply(T*)",
      "from(Iterable[_ <: T])" -> "apply(T*)",
      "from(Future[_ <: T])" -> fromFuture,
      "from(Future[_ <: T], Long, TimeUnit)" -> fromFuture,
      "from(Future[_ <: T], Scheduler)" -> fromFuture,
      "just(T)" -> "apply(T*)",
      "merge(Observable[_ <: T], Observable[_ <: T])" -> "merge(Observable[U])",
      "merge(Observable[_ <: Observable[_ <: T]])" -> "flatten(<:<[Observable[T], Observable[Observable[U]]])",
      "mergeDelayError(Observable[_ <: T], Observable[_ <: T])" -> "mergeDelayError(Observable[U])",
      "mergeDelayError(Observable[_ <: Observable[_ <: T]])" -> "flattenDelayError(<:<[Observable[T], Observable[Observable[U]]])",
      "range(Int, Int)" -> "apply(Range)",
      "repeat()" -> "repeat()",
      "retry()" -> "retry()",
      "sequenceEqual(Observable[_ <: T], Observable[_ <: T])" -> "[use `(first zip second) map (p => p._1 == p._2)`]",
      "sequenceEqual(Observable[_ <: T], Observable[_ <: T], Func2[_ >: T, _ >: T, Boolean])" -> "[use `(first zip second) map (p => equality(p._1, p._2))`]",
      "sum(Observable[Integer])" -> "sum(Numeric[U])",
      "sumDoubles(Observable[Double])" -> "sum(Numeric[U])",
      "sumFloats(Observable[Float])" -> "sum(Numeric[U])",
      "sumLongs(Observable[Long])" -> "sum(Numeric[U])",
      "synchronize(Observable[T])" -> "synchronize",
      "switchDo(Observable[_ <: Observable[_ <: T]])" -> deprecated,
      "switchOnNext(Observable[_ <: Observable[_ <: T]])" -> "switch(<:<[Observable[T], Observable[Observable[U]]])",
      "zip(Observable[_ <: T1], Observable[_ <: T2], Func2[_ >: T1, _ >: T2, _ <: R])" -> "[use instance method `zip` and `map`]",
      "zip(Observable[_ <: Observable[_]], FuncN[_ <: R])" -> "[use `zip` in companion object and `map`]",
      "zip(Iterable[_ <: Observable[_]], FuncN[_ <: R])" -> "[use `zip` in companion object and `map`]"
  ) ++ List.iterate("T", 9)(s => s + ", T").map(
      // all 9 overloads of startWith:
      "startWith(" + _ + ")" -> "[unnecessary because we can just use `+` instead]"
  ).toMap ++ List.iterate("Observable[_ <: T]", 9)(s => s + ", Observable[_ <: T]").map(
      // concat 2-9
      "concat(" + _ + ")" -> "[unnecessary because we can use `++` instead or `Observable(o1, o2, ...).concat`]"
  ).drop(1).toMap ++ List.iterate("T", 10)(s => s + ", T").map(
      // all 10 overloads of from:
      "from(" + _ + ")" -> "apply(T*)"
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
  }).toMap

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
    methodMembersToMethodStrings(tp.declarations.filter(m => m.isMethod && m.isPublic))
    // TODO how can we filter out instance methods which were put into companion because
    // of extends AnyVal in a way which does not depend on implementation-chosen name '$extension'?
    .filter(! _.contains("$extension"))
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
## Comparison of Scala Observable and Java Observable

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
