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
package rx.lang.scala.examples

import rx.lang.scala.Observable
import scala.concurrent.duration._

object Olympics {
  case class Medal(val year: Int, val games: String, val discipline: String, val medal: String, val athlete: String, val country: String)

  def mountainBikeMedals: Observable[Medal] = Observable.items(
    duration(100 millis), // a short delay because medals are only awarded some time after the Games began
    Observable.items(
      Medal(1996, "Atlanta 1996", "cross-country men", "Gold", "Bart BRENTJENS", "Netherlands"),
      Medal(1996, "Atlanta 1996", "cross-country women", "Gold", "Paola PEZZO", "Italy"),
      Medal(1996, "Atlanta 1996", "cross-country men", "Silver", "Thomas FRISCHKNECHT", "Switzerland"),
      Medal(1996, "Atlanta 1996", "cross-country women", "Silver", "Alison SYDOR", "Canada"),
      Medal(1996, "Atlanta 1996", "cross-country men", "Bronze", "Miguel MARTINEZ", "France"),
      Medal(1996, "Atlanta 1996", "cross-country women", "Bronze", "Susan DEMATTEI", "United States of America")
    ),
    fourYearsEmpty,
    Observable.items(
      Medal(2000, "Sydney 2000", "cross-country women", "Gold", "Paola PEZZO", "Italy"),
      Medal(2000, "Sydney 2000", "cross-country women", "Silver", "Barbara BLATTER", "Switzerland"),
      Medal(2000, "Sydney 2000", "cross-country women", "Bronze", "Marga FULLANA", "Spain"),
      Medal(2000, "Sydney 2000", "cross-country men", "Gold", "Miguel MARTINEZ", "France"),
      Medal(2000, "Sydney 2000", "cross-country men", "Silver", "Filip MEIRHAEGHE", "Belgium"),
      Medal(2000, "Sydney 2000", "cross-country men", "Bronze", "Christoph SAUSER", "Switzerland")
    ),
    fourYearsEmpty,
    Observable.items(
      Medal(2004, "Athens 2004", "cross-country men", "Gold", "Julien ABSALON", "France"),
      Medal(2004, "Athens 2004", "cross-country men", "Silver", "Jose Antonio HERMIDA RAMOS", "Spain"),
      Medal(2004, "Athens 2004", "cross-country men", "Bronze", "Bart BRENTJENS", "Netherlands"),
      Medal(2004, "Athens 2004", "cross-country women", "Gold", "Gunn-Rita DAHLE", "Norway"),
      Medal(2004, "Athens 2004", "cross-country women", "Silver", "Marie-Helene PREMONT", "Canada"),
      Medal(2004, "Athens 2004", "cross-country women", "Bronze", "Sabine SPITZ", "Germany")
    ),
    fourYearsEmpty,
    Observable.items(
      Medal(2008, "Beijing 2008", "cross-country women", "Gold", "Sabine SPITZ", "Germany"),
      Medal(2008, "Beijing 2008", "cross-country women", "Silver", "Maja WLOSZCZOWSKA", "Poland"),
      Medal(2008, "Beijing 2008", "cross-country women", "Bronze", "Irina KALENTYEVA", "Russian Federation"),
      Medal(2008, "Beijing 2008", "cross-country men", "Gold", "Julien ABSALON", "France"),
      Medal(2008, "Beijing 2008", "cross-country men", "Silver", "Jean-Christophe PERAUD", "France"),
      Medal(2008, "Beijing 2008", "cross-country men", "Bronze", "Nino SCHURTER", "Switzerland")
    ),
    fourYearsEmpty,
    Observable.items(
      Medal(2012, "London 2012", "cross-country men", "Gold", "Jaroslav KULHAVY", "Czech Republic"),
      Medal(2012, "London 2012", "cross-country men", "Silver", "Nino SCHURTER", "Switzerland"),
      Medal(2012, "London 2012", "cross-country men", "Bronze", "Marco Aurelio FONTANA", "Italy"),
      Medal(2012, "London 2012", "cross-country women", "Gold", "Julie BRESSET", "France"),
      Medal(2012, "London 2012", "cross-country women", "Silver", "Sabine SPITZ", "Germany"),
      Medal(2012, "London 2012", "cross-country women", "Bronze", "Georgia GOULD", "United States of America")
    )
  ).concat

  // speed it up :D
  val oneYear = 1000.millis

  //val neverUsedDummyMedal = Medal(3333, "?", "?", "?", "?", "?")

  /** runs an infinite loop, and returns Bottom type (Nothing) */
  def getNothing: Nothing = {
    println("You shouldn't have called this method ;-)")
    getNothing
  }
  
  /** returns an Observable which emits no elements and completes after a duration of d */
  def duration(d: Duration): Observable[Nothing] = Observable.interval(d).take(1).filter(_ => false).map(_ => getNothing)
  
  def fourYearsEmpty: Observable[Medal] = duration(4*oneYear)

  def yearTicks: Observable[Int] = 
    (Observable.from(1996 to 2014) zip (Observable.items(-1) ++ Observable.interval(oneYear))).map(_._1)
  
  /*
  def fourYearsEmptyOld: Observable[Medal] = {
    // TODO this should return an observable which emits nothing during fourYears and then completes
    // Because of https://github.com/Netflix/RxJava/issues/388, we get non-terminating tests
    // And this https://github.com/Netflix/RxJava/pull/289#issuecomment-24738668 also causes problems
    // So we don't use this:
    Observable.interval(fourYears).take(1).map(i => neverUsedDummyMedal).filter(m => false)
    // But we just return empty, which completes immediately
    // Observable.empty
  }*/

}