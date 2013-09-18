package rx.lang.scala

import scala.reflect.runtime.universe._
import org.scalatest.junit.JUnitSuite
import org.junit.Test
import rx.util.functions._
import scala.collection.SortedSet
import scala.collection.SortedMap
import org.junit.Ignore
import java.lang.reflect.Modifier

class CompletenessTest extends JUnitSuite {
  
  val unnecessary = "[considered unnecessary in Scala land]"
  
  val correspondence = defaultMethodCorrespondence ++ Map(
      // manually added entries for Java instance methods
      "aggregate(Func2[T, T, T])" -> "reduce((U, U) => U)",
      "aggregate(R, Func2[R, _ >: T, R])" -> "fold(R)((R, T) => R)",
      "all(Func1[_ >: T, Boolean])" -> "forall(T => Boolean)",
      "buffer(Long, Long, TimeUnit)" -> "buffer(Duration, Duration)",
      "buffer(Long, Long, TimeUnit, Scheduler)" -> "buffer(Duration, Duration, Scheduler)",
      "dematerialize()" -> "dematerialize(<:<[T, Notification[U]])",
      "groupBy(Func1[_ >: T, _ <: K], Func1[_ >: T, _ <: R])" -> "groupBy(T => K)",
      "mapMany(Func1[_ >: T, _ <: Observable[_ <: R]])" -> "flatMap(T => Observable[R])",
      "onErrorResumeNext(Func1[Throwable, _ <: Observable[_ <: T]])" -> "onErrorResumeNext(Throwable => Observable[U])",
      "onErrorResumeNext(Observable[_ <: T])" -> "onErrorResumeNext(Observable[U])",
      "onErrorReturn(Func1[Throwable, _ <: T])" -> "onErrorReturn(Throwable => U)",
      "onExceptionResumeNext(Observable[_ <: T])" -> "onExceptionResumeNext(Observable[U])",
      "reduce(Func2[T, T, T])" -> "reduce((U, U) => U)",
      "reduce(R, Func2[R, _ >: T, R])" -> "fold(R)((R, T) => R)",
      "scan(Func2[T, T, T])" -> unnecessary,
      "scan(R, Func2[R, _ >: T, R])" -> "scan(R)((R, T) => R)",
      "skip(Int)" -> "drop(Int)",
      "skipWhile(Func1[_ >: T, Boolean])" -> "dropWhile(T => Boolean)",
      "skipWhileWithIndex(Func2[_ >: T, Integer, Boolean])" -> unnecessary,
      "startWith(Iterable[T])" -> "[unnecessary because we can just use ++ instead]",
      "toList()" -> "toSeq",
      "toSortedList()" -> unnecessary,
      "toSortedList(Func2[_ >: T, _ >: T, Integer])" -> unnecessary,
      "where(Func1[_ >: T, Boolean])" -> "filter(T => Boolean)",
      "window(Long, Long, TimeUnit)" -> "window(Duration, Duration)",
      "window(Long, Long, TimeUnit, Scheduler)" -> "window(Duration, Duration, Scheduler)",
      
      // manually added entries for Java static methods
      "create(OnSubscribeFunc[T])" -> "apply(Observer[T] => Subscription)",
      "defer(Func0[_ <: Observable[_ <: T]])" -> "defer(=> Observable[T])",
      "empty()" -> "apply(T*)",
      "error(Throwable)" -> "apply(Throwable)",
      "from(Array[T])" -> "apply(T*)",
      "from(Iterable[_ <: T])" -> "apply(T*)", 
      "merge(Observable[_ <: T], Observable[_ <: T])" -> "merge(Observable[T])",
      "mergeDelayError(Observable[_ <: T], Observable[_ <: T])" -> "mergeDelayError(Observable[T])",
      "range(Int, Int)" -> "apply(Range)",
      "sequenceEqual(Observable[_ <: T], Observable[_ <: T])" -> "[use (first zip second) map (p => p._1 == p._2)]",
      "sequenceEqual(Observable[_ <: T], Observable[_ <: T], Func2[_ >: T, _ >: T, Boolean])" -> "[use (first zip second) map (p => equality(p._1, p._2))]",
      "switchDo(Observable[_ <: Observable[_ <: T]])" -> "switch",
      "synchronize(Observable[_ <: T])" -> "synchronize",
      "zip(Observable[_ <: T1], Observable[_ <: T2], Func2[_ >: T1, _ >: T2, _ <: R])" -> "[use instance method zip and map]"
  ) ++ List.iterate("T", 9)(s => s + ", T").map(
      // all 9 overloads of startWith:
      "startWith(" + _ + ")" -> "[unnecessary because we can just use ++ instead]"
  ).toMap ++ List.iterate("Observable[_ <: T]", 9)(s => s + ", Observable[_ <: T]").map(
      // concat 2-9
      "concat(" + _ + ")" -> "[unnecessary because we can use ++ instead]"
  ).drop(1).toMap ++ List.iterate("T", 10)(s => s + ", T").map(
      // all 10 overloads of from:
      "from(" + _ + ")" -> "apply(T*)"
  ).toMap ++ (3 to 9).map(i => {
    // zip3-9:
    val obsArgs = (1 to i).map(j => s"Observable[_ <: T$j], ").mkString("")
    val funcParams = (1 to i).map(j => s"_ >: T$j, ").mkString("")
    ("zip(" + obsArgs + "Func" + i + "[" + funcParams + "_ <: R])", unnecessary)
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
  @Test def printJavaInstanceMethods: Unit = {
    printMethodSet("Instance methods of rx.Observable", 
                   typeOf[rx.Observable[_]])
  }
  
  @Ignore // because spams output
  @Test def printScalaInstanceMethods: Unit = {
    printMethodSet("Instance methods of rx.lang.scala.Observable", 
                   typeOf[rx.lang.scala.Observable[_]])
  }
  
  @Ignore // because spams output
  @Test def printJavaStaticMethods: Unit = {
    printMethodSet("Static methods of rx.Observable", 
                   typeOf[rx.Observable[_]].typeSymbol.companionSymbol.typeSignature)
  }
  
  @Ignore // because spams output
  @Test def printScalaCompanionMethods: Unit = {
    printMethodSet("Companion methods of rx.lang.scala.Observable",
                   typeOf[rx.lang.scala.Observable.type])
  }
  
  def javaMethodSignatureToScala(s: String): String = {
    s.replaceAllLiterally("Long, TimeUnit", "Duration")
     .replaceAll("Action0", "() => Unit")
     // nested [] can't be parsed with regex, but we still do it, please forgive us ;-)
     .replaceAll("Action1\\[([^]]*)\\]", "$1 => Unit")
     .replaceAll("Action2\\[([^]]*), ([^]]*)\\]", "($1, $2) => Unit")
     .replaceAll("Func0\\[([^]]*)\\]", "() => $1")
     .replaceAll("Func1\\[([^]]*), ([^]]*)\\]", "$1 => $2")
     .replaceAll("Func2\\[([^]]*), ([^]]*), ([^]]*)\\]", "($1, $2) => $3")
     .replaceAllLiterally("_ <: ", "")
     .replaceAllLiterally("_ >: ", "")
     .replaceAll("(\\w+)\\(\\)", "$1")
  }

  def defaultMethodCorrespondence: Map[String, String] = {
    val allMethods = getPublicInstanceAndCompanionMethods(typeOf[rx.Observable[_]])      
    val tuples = for (javaM <- allMethods) yield (javaM, javaMethodSignatureToScala(javaM))
    tuples.toMap
  }
  
  @Ignore // because spams output
  @Test def printDefaultMethodCorrespondence: Unit = {
    println("\nDefault Method Correspondence")
    println(  "-----------------------------\n")
    val c = SortedMap(defaultMethodCorrespondence.toSeq : _*)
    val len = c.keys.map(_.length).max + 2
    for ((javaM, scalaM) <- c) {
      println(s"""      %-${len}s -> %s,""".format("\"" + javaM + "\"", "\"" + scalaM + "\"")) 
    }
  }
  
  @Ignore // because spams output
  @Test def printCorrectedMethodCorrespondence: Unit = {
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
  
  @Test def checkScalaMethodPresenceVerbose: Unit = {
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
   
  @Test def checkJavaMethodPresence: Unit = {
    println("\nTesting that all mentioned Java methods exist")
    println(  "---------------------------------------------\n")
    checkMethodPresence(correspondence.keys, typeOf[rx.Observable[_]])
  }
  
  @Ignore // because we prefer the verbose version
  @Test def checkScalaMethodPresence: Unit = {
    checkMethodPresence(correspondence.values, typeOf[rx.lang.scala.Observable[_]])
  }
  
}
