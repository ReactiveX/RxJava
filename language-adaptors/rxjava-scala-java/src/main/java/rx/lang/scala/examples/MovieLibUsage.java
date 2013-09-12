package rx.lang.scala.examples;

import org.junit.Test;

import rx.Observable;
import rx.util.functions.Action1;


public class MovieLibUsage {
	
	Action1<Movie> moviePrinter = new Action1<Movie>() {
		public void call(Movie m) {
			System.out.println("A movie of length " + m.lengthInSeconds() + "s");
		}
	};
	
	@Test
	public void test() {
		MovieLib lib = new MovieLib(Observable.from(new Movie(3000), new Movie(1000), new Movie(2000)));
		
		lib.longMovies().subscribe(moviePrinter);		
	}

}
