package rx;

import static org.junit.Assert.*;

import java.util.ArrayList;
import java.util.List;

import org.junit.Test;

import rx.Observable.OnSubscribeFunc;
import rx.subscriptions.Subscriptions;
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
    public void testCovarianceOfFrom() {
        Observable.<Movie> from(new HorrorMovie());
        Observable.<Movie> from(new ArrayList<HorrorMovie>());
        // Observable.<HorrorMovie>from(new Movie()); // may not compile
    }

    /**
     * This won't compile if super/extends isn't done correctly on generics
     */
    @Test
    public void testCovarianceOfMerge() {
        Observable<HorrorMovie> horrors = Observable.from(new HorrorMovie());
        Observable<Observable<HorrorMovie>> metaHorrors = Observable.just(horrors);
        Observable.<Media> merge(metaHorrors);
    }

    /**
     * This won't compile if super/extends isn't done correctly on generics
     */
    @Test
    public void testCovarianceOfZip() {
        Observable<HorrorMovie> horrors = Observable.from(new HorrorMovie());
        Observable<CoolRating> ratings = Observable.from(new CoolRating());

        Observable.<Movie, CoolRating, Result> zip(horrors, ratings, combine).toBlockingObservable().forEach(action);
        Observable.<Movie, CoolRating, Result> zip(horrors, ratings, combine).toBlockingObservable().forEach(action);
        Observable.<Media, Rating, ExtendedResult> zip(horrors, ratings, combine).toBlockingObservable().forEach(extendedAction);
        Observable.<Media, Rating, Result> zip(horrors, ratings, combine).toBlockingObservable().forEach(action);
        Observable.<Media, Rating, ExtendedResult> zip(horrors, ratings, combine).toBlockingObservable().forEach(action);

        Observable.<Movie, CoolRating, Result> zip(horrors, ratings, combine);
    }

    /**
     * This won't compile if super/extends isn't done correctly on generics
     */
    @Test
    public void testCovarianceOfCombineLatest() {
        Observable<HorrorMovie> horrors = Observable.from(new HorrorMovie());
        Observable<CoolRating> ratings = Observable.from(new CoolRating());

        Observable.<Movie, CoolRating, Result> combineLatest(horrors, ratings, combine).toBlockingObservable().forEach(action);
        Observable.<Movie, CoolRating, Result> combineLatest(horrors, ratings, combine).toBlockingObservable().forEach(action);
        Observable.<Media, Rating, ExtendedResult> combineLatest(horrors, ratings, combine).toBlockingObservable().forEach(extendedAction);
        Observable.<Media, Rating, Result> combineLatest(horrors, ratings, combine).toBlockingObservable().forEach(action);
        Observable.<Media, Rating, ExtendedResult> combineLatest(horrors, ratings, combine).toBlockingObservable().forEach(action);

        Observable.<Movie, CoolRating, Result> combineLatest(horrors, ratings, combine);
    }

    @Test
    public void testConcatCovariance() {
        Observable<Media> o1 = Observable.<Media> from(new HorrorMovie(), new Movie());
        Observable<Media> o2 = Observable.from(new Media(), new HorrorMovie());

        Observable<Observable<Media>> os = Observable.from(o1, o2);

        List<Media> values = Observable.concat(os).toList().toBlockingObservable().single();
    }

    @Test
    public void testConcatCovariance2() {
        Observable<Media> o1 = Observable.from(new HorrorMovie(), new Movie(), new Media());
        Observable<Media> o2 = Observable.from(new Media(), new HorrorMovie());

        Observable<Observable<Media>> os = Observable.from(o1, o2);

        List<Media> values = Observable.concat(os).toList().toBlockingObservable().single();
    }
    
    @Test
    public void testConcatCovariance3() {
        Observable<Movie> o1 = Observable.from(new HorrorMovie(), new Movie());
        Observable<Media> o2 = Observable.from(new Media(), new HorrorMovie());

        List<Media> values = Observable.concat(o1, o2).toList().toBlockingObservable().single();
        
        assertTrue(values.get(0) instanceof HorrorMovie);
        assertTrue(values.get(1) instanceof Movie);
        assertTrue(values.get(2) instanceof Media);
        assertTrue(values.get(3) instanceof HorrorMovie);
    }

    @Test
    public void testConcatCovariance4() {

        Observable<Movie> o1 = Observable.create(new OnSubscribeFunc<Movie>() {

            @Override
            public Subscription onSubscribe(Observer<? super Movie> o) {
                o.onNext(new HorrorMovie());
                o.onNext(new Movie());
                //                o.onNext(new Media()); // correctly doesn't compile
                o.onCompleted();
                return Subscriptions.empty();
            }
        });

        Observable<Media> o2 = Observable.from(new Media(), new HorrorMovie());

        List<Media> values = Observable.concat(o1, o2).toList().toBlockingObservable().single();
        
        assertTrue(values.get(0) instanceof HorrorMovie);
        assertTrue(values.get(1) instanceof Movie);
        assertTrue(values.get(2) instanceof Media);
        assertTrue(values.get(3) instanceof HorrorMovie);
    }
    
    
    @Test
    public void testMergeCovariance() {
        Observable<Media> o1 = Observable.<Media> from(new HorrorMovie(), new Movie());
        Observable<Media> o2 = Observable.from(new Media(), new HorrorMovie());

        Observable<Observable<Media>> os = Observable.from(o1, o2);

        List<Media> values = Observable.merge(os).toList().toBlockingObservable().single();
    }

    @Test
    public void testMergeCovariance2() {
        Observable<Media> o1 = Observable.from(new HorrorMovie(), new Movie(), new Media());
        Observable<Media> o2 = Observable.from(new Media(), new HorrorMovie());

        Observable<Observable<Media>> os = Observable.from(o1, o2);

        List<Media> values = Observable.merge(os).toList().toBlockingObservable().single();
    }
    
    @Test
    public void testMergeCovariance3() {
        Observable<Movie> o1 = Observable.from(new HorrorMovie(), new Movie());
        Observable<Media> o2 = Observable.from(new Media(), new HorrorMovie());

        List<Media> values = Observable.merge(o1, o2).toList().toBlockingObservable().single();
        
        assertTrue(values.get(0) instanceof HorrorMovie);
        assertTrue(values.get(1) instanceof Movie);
        assertTrue(values.get(2) instanceof Media);
        assertTrue(values.get(3) instanceof HorrorMovie);
    }

    @Test
    public void testMergeCovariance4() {

        Observable<Movie> o1 = Observable.create(new OnSubscribeFunc<Movie>() {

            @Override
            public Subscription onSubscribe(Observer<? super Movie> o) {
                o.onNext(new HorrorMovie());
                o.onNext(new Movie());
                //                o.onNext(new Media()); // correctly doesn't compile
                o.onCompleted();
                return Subscriptions.empty();
            }
        });

        Observable<Media> o2 = Observable.from(new Media(), new HorrorMovie());

        List<Media> values = Observable.merge(o1, o2).toList().toBlockingObservable().single();
        
        assertTrue(values.get(0) instanceof HorrorMovie);
        assertTrue(values.get(1) instanceof Movie);
        assertTrue(values.get(2) instanceof Media);
        assertTrue(values.get(3) instanceof HorrorMovie);
    }

    Func2<Media, Rating, ExtendedResult> combine = new Func2<Media, Rating, ExtendedResult>() {
        @Override
        public ExtendedResult call(Media m, Rating r) {
            return new ExtendedResult();
        }
    };

    Action1<Result> action = new Action1<Result>() {
        @Override
        public void call(Result t1) {
            System.out.println("Result: " + t1);
        }
    };

    Action1<ExtendedResult> extendedAction = new Action1<ExtendedResult>() {
        @Override
        public void call(ExtendedResult t1) {
            System.out.println("Result: " + t1);
        }
    };

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
