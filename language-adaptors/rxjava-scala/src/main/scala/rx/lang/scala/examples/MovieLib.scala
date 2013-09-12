package rx.lang.scala.examples

import rx.lang.scala.Observable

class Movie(val lengthInSeconds: Int) { }

class MovieLib(val moviesStream: Observable[Movie]) {
  
  val threshold = 1200
  
  def shortMovies: Observable[Movie] = moviesStream.filter(_.lengthInSeconds <= threshold)
  
  def longMovies: Observable[Movie] = moviesStream.filter(_.lengthInSeconds > threshold)

}
