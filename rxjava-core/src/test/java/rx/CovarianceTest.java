package rx;

import org.junit.Test;

import rx.util.functions.Action1;
import rx.util.functions.Func2;

/**
 * Test super/extends of generics.
 * 
 * See https://github.com/Netflix/RxJava/pull/331
 */
public class CovarianceTest {

    /**
     * This won't compile if super/extends isn't done correctly on generics
     */
    @Test
    public void testCovarianceOfZip() {
        Observable<HorrorMovie> horrors = Observable.from(new HorrorMovie());
        Observable<CoolRating> ratings = Observable.from(new CoolRating());

        Func2<Media, Rating, ExtendedResult> combine = new Func2<Media, Rating, ExtendedResult>() {
            @Override
            public ExtendedResult call(Media m, Rating r) {
                return new ExtendedResult();
            }
        };

        Observable.<Result, Movie, CoolRating> zip(horrors, ratings, combine).toBlockingObservable().forEach(new Action1<Result>() {

            @Override
            public void call(Result t1) {
                System.out.println("Result: " + t1);
            }

        });

        Observable.<Result, Movie, CoolRating> zip(horrors, ratings, combine).toBlockingObservable().forEach(new Action1<Result>() {

            @Override
            public void call(Result t1) {
                System.out.println("Result: " + t1);
            }

        });

        Observable.<ExtendedResult, Media, Rating> zip(horrors, ratings, combine).toBlockingObservable().forEach(new Action1<ExtendedResult>() {

            @Override
            public void call(ExtendedResult t1) {
                System.out.println("Result: " + t1);
            }

        });

        Observable.<Result, Media, Rating> zip(horrors, ratings, combine).toBlockingObservable().forEach(new Action1<Result>() {

            @Override
            public void call(Result t1) {
                System.out.println("Result: " + t1);
            }

        });

        Observable.<ExtendedResult, Media, Rating> zip(horrors, ratings, combine).toBlockingObservable().forEach(new Action1<Result>() {

            @Override
            public void call(Result t1) {
                System.out.println("Result: " + t1);
            }

        });

        Observable.<Result, Movie, CoolRating> zip(horrors, ratings, combine);

    }

    static class Media {
    }

    static class Movie extends Media {
    }

    static class HorrorMovie extends Movie {
    }

    static class Rating {
    }

    static class CoolRating extends Rating {
    }

    static class Result {
    }

    static class ExtendedResult extends Result {
    }
}
