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

import rx.util.functions.FunctionLanguageAdaptor
import org.junit.{Assert, Before, Test}
import rx.Observable
import org.scalatest.junit.JUnitSuite
import org.mockito.Mockito._
import org.mockito.{MockitoAnnotations, Mock}

import scala.collection.JavaConverters._
import collection.mutable.ArrayBuffer

class ScalaAdaptor extends FunctionLanguageAdaptor {

    val ON_NEXT = "onNext"
    val ON_ERROR = "onError"
    val ON_COMPLETED = "onCompleted"

    def getFunctionClass: Array[Class[_]] = {
        return Array(classOf[Map[String, _]], classOf[(AnyRef) => Object], classOf[(AnyRef, AnyRef) => Object],
            classOf[(AnyRef, AnyRef, AnyRef) => Object], classOf[(AnyRef, AnyRef, AnyRef, AnyRef) => Object],
            classOf[(AnyRef, AnyRef, AnyRef, AnyRef) => Object],
            classOf[(AnyRef, AnyRef, AnyRef, AnyRef, AnyRef) => Object],
            classOf[(AnyRef, AnyRef, AnyRef, AnyRef, AnyRef, AnyRef) => Object],
            classOf[(AnyRef, AnyRef, AnyRef, AnyRef, AnyRef, AnyRef, AnyRef) =>Object],
            classOf[(AnyRef, AnyRef, AnyRef, AnyRef, AnyRef, AnyRef, AnyRef, AnyRef) => Object],
            classOf[(AnyRef, AnyRef, AnyRef, AnyRef, AnyRef, AnyRef, AnyRef, AnyRef, AnyRef) => Object],
            classOf[(AnyRef, AnyRef, AnyRef, AnyRef, AnyRef, AnyRef, AnyRef, AnyRef, AnyRef, AnyRef) => Object],
            classOf[(AnyRef, AnyRef, AnyRef, AnyRef, AnyRef, AnyRef, AnyRef, AnyRef, AnyRef, AnyRef, AnyRef) => Object],
            classOf[(AnyRef, AnyRef, AnyRef, AnyRef, AnyRef, AnyRef, AnyRef, AnyRef, AnyRef, AnyRef, AnyRef, AnyRef) => Object],
            classOf[(AnyRef, AnyRef, AnyRef, AnyRef, AnyRef, AnyRef, AnyRef, AnyRef, AnyRef, AnyRef, AnyRef, AnyRef, AnyRef) => Object],
            classOf[(AnyRef, AnyRef, AnyRef, AnyRef, AnyRef, AnyRef, AnyRef, AnyRef, AnyRef, AnyRef, AnyRef, AnyRef, AnyRef, AnyRef) => Object],
            classOf[(AnyRef, AnyRef, AnyRef, AnyRef, AnyRef, AnyRef, AnyRef, AnyRef, AnyRef, AnyRef, AnyRef, AnyRef, AnyRef, AnyRef, AnyRef) => Object],
            classOf[(AnyRef, AnyRef, AnyRef, AnyRef, AnyRef, AnyRef, AnyRef, AnyRef, AnyRef, AnyRef, AnyRef, AnyRef, AnyRef, AnyRef, AnyRef, AnyRef) => Object],
            classOf[(AnyRef, AnyRef, AnyRef, AnyRef, AnyRef, AnyRef, AnyRef, AnyRef, AnyRef, AnyRef, AnyRef, AnyRef, AnyRef, AnyRef, AnyRef, AnyRef, AnyRef) => Object],
            classOf[(AnyRef, AnyRef, AnyRef, AnyRef, AnyRef, AnyRef, AnyRef, AnyRef, AnyRef, AnyRef, AnyRef, AnyRef, AnyRef, AnyRef, AnyRef, AnyRef, AnyRef, AnyRef) => Object],
            classOf[(AnyRef, AnyRef, AnyRef, AnyRef, AnyRef, AnyRef, AnyRef, AnyRef, AnyRef, AnyRef, AnyRef, AnyRef, AnyRef, AnyRef, AnyRef, AnyRef, AnyRef, AnyRef, AnyRef) => Object],
            classOf[(AnyRef, AnyRef, AnyRef, AnyRef, AnyRef, AnyRef, AnyRef, AnyRef, AnyRef, AnyRef, AnyRef, AnyRef, AnyRef, AnyRef, AnyRef, AnyRef, AnyRef, AnyRef, AnyRef, AnyRef) => Object])
    }

    def call(function: AnyRef, args: Array[AnyRef]) : Object = {
        function match {
            case (func: Map[String, _]) => return matchOption(func.get(ON_NEXT), args)
            case _ => return matchFunction(function, args)
        }
    }

    private def matchOption(funcOption: Option[_], args: Array[AnyRef]) : Object = {
        funcOption match {
            case Some(func: AnyRef) => return matchFunction(func, args)
            case _ => return None
        }
    }

    private def matchFunction(function: AnyRef, args: Array[AnyRef]) : Object = function match {
        case (f: ((AnyRef) => Object)) => return f(args(0))
        case (f: ((AnyRef, AnyRef) => Object)) => return f(args(0), args(1))
        case (f: ((AnyRef, AnyRef, AnyRef) => Object)) => return f(args(0), args(1), args(2))
        case (f: ((AnyRef, AnyRef, AnyRef, AnyRef) => Object)) =>
            return f(args(0), args(1), args(2), args(3))
        case (f: ((AnyRef, AnyRef, AnyRef, AnyRef, AnyRef) => Object)) =>
            return f(args(0), args(1), args(2), args(3), args(4))
        case (f: ((AnyRef, AnyRef, AnyRef, AnyRef, AnyRef, AnyRef) => Object)) =>
            return f(args(0), args(1), args(2), args(3), args(4), args(5))
        case (f: ((AnyRef, AnyRef, AnyRef, AnyRef, AnyRef, AnyRef, AnyRef) => Object)) =>
            return f(args(0), args(1), args(2), args(3), args(4), args(5), args(6))
        case (f: ((AnyRef, AnyRef, AnyRef, AnyRef, AnyRef, AnyRef, AnyRef, AnyRef) => Object)) =>
            return f(args(0), args(1), args(2), args(3), args(4), args(5), args(6), args(7))
        case (f: ((AnyRef, AnyRef, AnyRef, AnyRef, AnyRef, AnyRef, AnyRef, AnyRef, AnyRef) => Object)) =>
            return f(args(0), args(1), args(2), args(3), args(4), args(5), args(6), args(7), args(8))
        case (f: ((AnyRef, AnyRef, AnyRef, AnyRef, AnyRef, AnyRef, AnyRef, AnyRef, AnyRef, AnyRef) => Object)) =>
            return f(args(0), args(1), args(2), args(3), args(4), args(5), args(6), args(7), args(8), args(9))
        case (f: ((AnyRef, AnyRef, AnyRef, AnyRef, AnyRef, AnyRef, AnyRef, AnyRef, AnyRef, AnyRef, AnyRef) => Object)) =>
            return f(args(0), args(1), args(2), args(3), args(4), args(5), args(6), args(7), args(8), args(9), args(10))
        case (f: ((AnyRef, AnyRef, AnyRef, AnyRef, AnyRef, AnyRef, AnyRef, AnyRef, AnyRef, AnyRef, AnyRef, AnyRef) => Object)) =>
            return f(args(0), args(1), args(2), args(3), args(4), args(5), args(6), args(7), args(8), args(9), args(10), args(11))
        case (f: ((AnyRef, AnyRef, AnyRef, AnyRef, AnyRef, AnyRef, AnyRef, AnyRef, AnyRef, AnyRef, AnyRef, AnyRef, AnyRef) => Object)) =>
            return f(args(0), args(1), args(2), args(3), args(4), args(5), args(6), args(7), args(8), args(9), args(10), args(11), args(12))
        case (f: ((AnyRef, AnyRef, AnyRef, AnyRef, AnyRef, AnyRef, AnyRef, AnyRef, AnyRef, AnyRef, AnyRef, AnyRef, AnyRef, AnyRef) => Object)) =>
            return f(args(0), args(1), args(2), args(3), args(4), args(5), args(6), args(7), args(8), args(9), args(10), args(11), args(12), args(13))
        case (f: ((AnyRef, AnyRef, AnyRef, AnyRef, AnyRef, AnyRef, AnyRef, AnyRef, AnyRef, AnyRef, AnyRef, AnyRef, AnyRef, AnyRef, AnyRef) => Object)) =>
            return f(args(0), args(1), args(2), args(3), args(4), args(5), args(6), args(7), args(8), args(9), args(10), args(11), args(12), args(13), args(14))
        case (f: ((AnyRef, AnyRef, AnyRef, AnyRef, AnyRef, AnyRef, AnyRef, AnyRef, AnyRef, AnyRef, AnyRef, AnyRef, AnyRef, AnyRef, AnyRef, AnyRef) => Object)) =>
            return f(args(0), args(1), args(2), args(3), args(4), args(5), args(6), args(7), args(8), args(9), args(10), args(11), args(12), args(13), args(14), args(15))
        case (f: ((AnyRef, AnyRef, AnyRef, AnyRef, AnyRef, AnyRef, AnyRef, AnyRef, AnyRef, AnyRef, AnyRef, AnyRef, AnyRef, AnyRef, AnyRef, AnyRef, AnyRef) => Object)) =>
            return f(args(0), args(1), args(2), args(3), args(4), args(5), args(6), args(7), args(8), args(9), args(10), args(11), args(12), args(13), args(14), args(15), args(16))
        case (f: ((AnyRef, AnyRef, AnyRef, AnyRef, AnyRef, AnyRef, AnyRef, AnyRef, AnyRef, AnyRef, AnyRef, AnyRef, AnyRef, AnyRef, AnyRef, AnyRef, AnyRef, AnyRef) => Object)) =>
            return f(args(0), args(1), args(2), args(3), args(4), args(5), args(6), args(7), args(8), args(9), args(10), args(11), args(12), args(13), args(14), args(15), args(16), args(17))
        case (f: ((AnyRef, AnyRef, AnyRef, AnyRef, AnyRef, AnyRef, AnyRef, AnyRef, AnyRef, AnyRef, AnyRef, AnyRef, AnyRef, AnyRef, AnyRef, AnyRef, AnyRef, AnyRef, AnyRef) => Object)) =>
            return f(args(0), args(1), args(2), args(3), args(4), args(5), args(6), args(7), args(8), args(9), args(10), args(11), args(12), args(13), args(14), args(15), args(16), args(17), args(18))
        case (f: ((AnyRef, AnyRef, AnyRef, AnyRef, AnyRef, AnyRef, AnyRef, AnyRef, AnyRef, AnyRef, AnyRef, AnyRef, AnyRef, AnyRef, AnyRef, AnyRef, AnyRef, AnyRef, AnyRef, AnyRef) => Object)) =>
            return f(args(0), args(1), args(2), args(3), args(4), args(5), args(6), args(7), args(8), args(9), args(10), args(11), args(12), args(13), args(14), args(15), args(16), args(17), args(18), args(19))
        case (f: ((AnyRef, AnyRef, AnyRef, AnyRef, AnyRef, AnyRef, AnyRef, AnyRef, AnyRef, AnyRef, AnyRef, AnyRef, AnyRef, AnyRef, AnyRef, AnyRef, AnyRef, AnyRef, AnyRef, AnyRef, AnyRef) => Object)) =>
            return f(args(0), args(1), args(2), args(3), args(4), args(5), args(6), args(7), args(8), args(9), args(10), args(11), args(12), args(13), args(14), args(15), args(16), args(17), args(18), args(19), args(20))
        case (f: ((AnyRef, AnyRef, AnyRef, AnyRef, AnyRef, AnyRef, AnyRef, AnyRef, AnyRef, AnyRef, AnyRef, AnyRef, AnyRef, AnyRef, AnyRef, AnyRef, AnyRef, AnyRef, AnyRef, AnyRef, AnyRef, AnyRef) => Object)) =>
            return f(args(0), args(1), args(2), args(3), args(4), args(5), args(6), args(7), args(8), args(9), args(10), args(11), args(12), args(13), args(14), args(15), args(16), args(17), args(18), args(19), args(20), args(21))

    }
}

class UnitTestSuite extends JUnitSuite {
    @Mock private[this]
    val assertion: ScriptAssertion = null

    @Before def before {
        MockitoAnnotations.initMocks(this)
    }

    @Test def testTake() {
        Observable.toObservable("1", "2", "3").take(1).subscribe(Map(
            "onNext" -> ((callback: String) => {
                print("testTake: callback = " + callback)
                assertion.received(callback)
            })
        ))
        verify(assertion, times(1)).received("1")
    }

    @Test def testFilterWithToList() {
        val numbers = Observable.toObservable[Int](1, 2, 3, 4, 5, 6, 7, 8, 9)
        numbers.filter((x: Int) => 0 == (x % 2)).toList().subscribe(
            (callback: java.util.List[Int]) => {
                val lst = callback.asScala.toList
                println("filter onNext -> got " + lst)
                assertion.received(lst)
            }
        )
        verify(assertion, times(1)).received(List(2,4,6,8))
    }

    @Test def testLast() {
        val numbers = Observable.toObservable[Int](1, 2, 3, 4, 5, 6, 7, 8, 9)
        numbers.last().subscribe((callback: Int) => {
            println("testLast: onNext -> got " + callback)
            assertion.received(callback)
        })
        verify(assertion, times(1)).received(9)
    }

    @Test def testMap() {
        val numbers = Observable.toObservable(1, 2, 3, 4, 5, 6, 7, 8, 9)
        val mappedNumbers = new ArrayBuffer[Int]()
        numbers.map(((x: Int)=> { x * x })).subscribe(((squareVal: Int) => {
            println("square is " + squareVal )
            mappedNumbers += squareVal
        }))
        Assert.assertEquals(List(1,4,9,16,25,36,49,64,81), mappedNumbers.toList)

    }

    @Test def testZip() {
        val numbers = Observable.toObservable(1, 2, 3)
        val colors = Observable.toObservable("red", "green", "blue")
        val characters = Observable.toObservable("lion-o", "cheetara", "panthro")

        Observable.zip(numbers.toList, colors.toList, characters.toList, ((n: java.util.List[Int], c: java.util.List[String], t: java.util.List[String]) => { Map(
            "numbers" -> n,
            "colors" -> c,
            "thundercats" -> t
        )})).subscribe((m: Map[String, _]) => {
            println("zipped map is " + m.toString())
        })


    }

    trait ScriptAssertion {
        def error(ex: Exception)

        def received(obj: Any)
    }
}
