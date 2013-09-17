package rx.lang.scala.examples

import rx.lang.scala.Observable
import scala.concurrent.duration._

object Olympics {
  case class Medal(val year: Int, val games: String, val discipline: String, val medal: String, val athlete: String, val country: String)
  
  def mountainBikeMedals: Observable[Medal] = Observable(
    Medal(1996, "Atlanta 1996", "cross-country men", "Gold", "Bart BRENTJENS", "Netherlands"),
    Medal(1996, "Atlanta 1996", "cross-country women", "Gold", "Paola PEZZO", "Italy"),
    Medal(1996, "Atlanta 1996", "cross-country men", "Silver", "Thomas FRISCHKNECHT", "Switzerland"),
    Medal(1996, "Atlanta 1996", "cross-country women", "Silver", "Alison SYDOR", "Canada"),
    Medal(1996, "Atlanta 1996", "cross-country men", "Bronze", "Miguel MARTINEZ", "France"),
    Medal(1996, "Atlanta 1996", "cross-country women", "Bronze", "Susan DEMATTEI", "United States of America")
  ) ++ fourYearsEmpty ++ Observable( 
    Medal(2000, "Sydney 2000", "cross-country women", "Gold", "Paola PEZZO", "Italy"),
    Medal(2000, "Sydney 2000", "cross-country women", "Silver", "Barbara BLATTER", "Switzerland"),
    Medal(2000, "Sydney 2000", "cross-country women", "Bronze", "Marga FULLANA", "Spain"),
    Medal(2000, "Sydney 2000", "cross-country men", "Gold", "Miguel MARTINEZ", "France"),
    Medal(2000, "Sydney 2000", "cross-country men", "Silver", "Filip MEIRHAEGHE", "Belgium"),
    Medal(2000, "Sydney 2000", "cross-country men", "Bronze", "Christoph SAUSER", "Switzerland")
  ) ++ fourYearsEmpty ++ Observable( 
    Medal(2004, "Athens 2004", "cross-country men", "Gold", "Julien ABSALON", "France"),
    Medal(2004, "Athens 2004", "cross-country men", "Silver", "Jose Antonio HERMIDA RAMOS", "Spain"),
    Medal(2004, "Athens 2004", "cross-country men", "Bronze", "Bart BRENTJENS", "Netherlands"),
    Medal(2004, "Athens 2004", "cross-country women", "Gold", "Gunn-Rita DAHLE", "Norway"),
    Medal(2004, "Athens 2004", "cross-country women", "Silver", "Marie-Helene PREMONT", "Canada"),
    Medal(2004, "Athens 2004", "cross-country women", "Bronze", "Sabine SPITZ", "Germany")
  ) ++ fourYearsEmpty ++ Observable( 
    Medal(2008, "Beijing 2008", "cross-country women", "Gold", "Sabine SPITZ", "Germany"),
    Medal(2008, "Beijing 2008", "cross-country women", "Silver", "Maja WLOSZCZOWSKA", "Poland"),
    Medal(2008, "Beijing 2008", "cross-country women", "Bronze", "Irina KALENTYEVA", "Russian Federation"),
    Medal(2008, "Beijing 2008", "cross-country men", "Gold", "Julien ABSALON", "France"),
    Medal(2008, "Beijing 2008", "cross-country men", "Silver", "Jean-Christophe PERAUD", "France"),
    Medal(2008, "Beijing 2008", "cross-country men", "Bronze", "Nino SCHURTER", "Switzerland")
  ) ++ fourYearsEmpty ++ Observable(
    Medal(2012, "London 2012", "cross-country men", "Gold", "Jaroslav KULHAVY", "Czech Republic"),
    Medal(2012, "London 2012", "cross-country men", "Silver", "Nino SCHURTER", "Switzerland"),
    Medal(2012, "London 2012", "cross-country men", "Bronze", "Marco Aurelio FONTANA", "Italy"),
    Medal(2012, "London 2012", "cross-country women", "Gold", "Julie BRESSET", "France"),
    Medal(2012, "London 2012", "cross-country women", "Silver", "Sabine SPITZ", "Germany"),
    Medal(2012, "London 2012", "cross-country women", "Bronze", "Georgia GOULD", "United States of America")
  )
    
  // speed it up :D
  val fourYears = 4000.millis
  
  val neverUsedDummyMedal = Medal(3333, "?", "?", "?", "?", "?")
  
  def fourYearsEmpty: Observable[Medal] = {
    Observable.interval(fourYears).take(1).map(i => neverUsedDummyMedal).filter(m => false)
  }
  
}