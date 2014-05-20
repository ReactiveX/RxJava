/**
 * Copyright 2014 Netflix, Inc.
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
package rx;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.List;

import org.junit.Test;

import rx.CovarianceTest.HorrorMovie;
import rx.CovarianceTest.Media;
import rx.CovarianceTest.Movie;
import rx.Observable.OnSubscribe;

public class MergeTests {

    /**
     * This won't compile if super/extends isn't done correctly on generics
     */
    @Test
    public void testCovarianceOfMerge() {
        Observable<HorrorMovie> horrors = Observable.from(new HorrorMovie());
        Observable<Observable<HorrorMovie>> metaHorrors = Observable.just(horrors);
        Observable.<Media> merge(metaHorrors);
    }

    @Test
    public void testMergeCovariance() {
        Observable<Media> o1 = Observable.<Media> from(new HorrorMovie(), new Movie());
        Observable<Media> o2 = Observable.from(new Media(), new HorrorMovie());

        Observable<Observable<Media>> os = Observable.from(o1, o2);

        List<Media> values = Observable.merge(os).toList().toBlocking().single();
        
        assertEquals(4, values.size());
    }

    @Test
    public void testMergeCovariance2() {
        Observable<Media> o1 = Observable.from(new HorrorMovie(), new Movie(), new Media());
        Observable<Media> o2 = Observable.from(new Media(), new HorrorMovie());

        Observable<Observable<Media>> os = Observable.from(o1, o2);

        List<Media> values = Observable.merge(os).toList().toBlocking().single();

        assertEquals(5, values.size());
    }

    @Test
    public void testMergeCovariance3() {
        Observable<Movie> o1 = Observable.from(new HorrorMovie(), new Movie());
        Observable<Media> o2 = Observable.from(new Media(), new HorrorMovie());

        List<Media> values = Observable.merge(o1, o2).toList().toBlocking().single();

        assertTrue(values.get(0) instanceof HorrorMovie);
        assertTrue(values.get(1) instanceof Movie);
        assertTrue(values.get(2) instanceof Media);
        assertTrue(values.get(3) instanceof HorrorMovie);
    }

    @Test
    public void testMergeCovariance4() {

        Observable<Movie> o1 = Observable.create(new OnSubscribe<Movie>() {

            @Override
            public void call(Subscriber<? super Movie> o) {
                o.onNext(new HorrorMovie());
                o.onNext(new Movie());
                //                o.onNext(new Media()); // correctly doesn't compile
                o.onCompleted();
            }
        });

        Observable<Media> o2 = Observable.from(new Media(), new HorrorMovie());

        List<Media> values = Observable.merge(o1, o2).toList().toBlocking().single();

        assertTrue(values.get(0) instanceof HorrorMovie);
        assertTrue(values.get(1) instanceof Movie);
        assertTrue(values.get(2) instanceof Media);
        assertTrue(values.get(3) instanceof HorrorMovie);
    }

}
