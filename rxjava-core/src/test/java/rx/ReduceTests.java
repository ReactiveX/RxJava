package rx;

import static org.junit.Assert.*;

import org.junit.Test;

import rx.CovarianceTest.HorrorMovie;
import rx.CovarianceTest.Movie;
import rx.operators.OperationScan;
import rx.util.functions.Func2;

public class ReduceTests {

    @Test
    public void reduceInts() {
        Observable<Integer> o = Observable.from(1, 2, 3);
        int value = o.reduce(new Func2<Integer, Integer, Integer>() {

            @Override
            public Integer call(Integer t1, Integer t2) {
                return t1 + t2;
            }
        }).toBlockingObservable().single();

        assertEquals(6, value);
    }

    @Test
    public void reduceWithObjects() {
        Observable<Movie> horrorMovies = Observable.<Movie> from(new HorrorMovie());

        Func2<Movie, Movie, Movie> chooseSecondMovie =
                new Func2<Movie, Movie, Movie>() {
                    public Movie call(Movie t1, Movie t2) {
                        return t2;
                    }
                };

        Observable<Movie> reduceResult = Observable.create(OperationScan.scan(horrorMovies, chooseSecondMovie)).takeLast(1);

        Observable<Movie> reduceResult2 = horrorMovies.reduce(chooseSecondMovie);
    }

    @Test
    public void reduceWithCovariantObjects() {
        Observable<HorrorMovie> horrorMovies = Observable.from(new HorrorMovie());

        Func2<Movie, Movie, Movie> chooseSecondMovie =
                new Func2<Movie, Movie, Movie>() {
                    public Movie call(Movie t1, Movie t2) {
                        return t2;
                    }
                };

        Observable<Movie> reduceResult = Observable.create(OperationScan.scan(horrorMovies, chooseSecondMovie)).takeLast(1);

        //TODO this isn't compiling
        //        Observable<Movie> reduceResult2 = horrorMovies.reduce(chooseSecondMovie);
    }

    @Test
    public void reduceCovariance() {
        Observable<HorrorMovie> horrorMovies = Observable.from(new HorrorMovie());

        // do something with horrorMovies, relying on the fact that all are HorrorMovies
        // and not just any Movies...

        // pass it to library (works because it takes Observable<? extends Movie>)
        libraryFunctionActingOnMovieObservables(horrorMovies);
    }

    public void libraryFunctionActingOnMovieObservables(Observable<? extends Movie> obs) {
        Func2<Movie, Movie, Movie> chooseSecondMovie =
                new Func2<Movie, Movie, Movie>() {
                    public Movie call(Movie t1, Movie t2) {
                        return t2;
                    }
                };

        Observable<Movie> reduceResult = Observable.create(OperationScan.scan(obs, chooseSecondMovie)).takeLast(1);

        //TODO this isn't compiling
        //        Observable<Movie> reduceResult2 = obs.reduce(chooseSecondMovie);
        // do something with reduceResult...
    }

}
