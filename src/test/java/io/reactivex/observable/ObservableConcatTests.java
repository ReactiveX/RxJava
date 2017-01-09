/**
 * Copyright (c) 2016-present, RxJava Contributors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in
 * compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is
 * distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See
 * the License for the specific language governing permissions and limitations under the License.
 */
package io.reactivex.observable;

import static org.junit.Assert.assertEquals;

import java.util.*;

import org.junit.Test;

import io.reactivex.Observable;
import io.reactivex.ObservableSource;
import io.reactivex.Observer;
import io.reactivex.observable.ObservableCovarianceTest.*;

public class ObservableConcatTests {

    @Test
    public void testConcatSimple() {
        Observable<String> o1 = Observable.just("one", "two");
        Observable<String> o2 = Observable.just("three", "four");

        List<String> values = Observable.concat(o1, o2).toList().blockingGet();

        assertEquals("one", values.get(0));
        assertEquals("two", values.get(1));
        assertEquals("three", values.get(2));
        assertEquals("four", values.get(3));
    }

    @Test
    public void testConcatWithObservableOfObservable() {
        Observable<String> o1 = Observable.just("one", "two");
        Observable<String> o2 = Observable.just("three", "four");
        Observable<String> o3 = Observable.just("five", "six");

        Observable<Observable<String>> os = Observable.just(o1, o2, o3);

        List<String> values = Observable.concat(os).toList().blockingGet();

        assertEquals("one", values.get(0));
        assertEquals("two", values.get(1));
        assertEquals("three", values.get(2));
        assertEquals("four", values.get(3));
        assertEquals("five", values.get(4));
        assertEquals("six", values.get(5));
    }

    @Test
    public void testConcatWithIterableOfObservable() {
        Observable<String> o1 = Observable.just("one", "two");
        Observable<String> o2 = Observable.just("three", "four");
        Observable<String> o3 = Observable.just("five", "six");

        @SuppressWarnings("unchecked")
        Iterable<Observable<String>> is = Arrays.asList(o1, o2, o3);

        List<String> values = Observable.concat(Observable.fromIterable(is)).toList().blockingGet();

        assertEquals("one", values.get(0));
        assertEquals("two", values.get(1));
        assertEquals("three", values.get(2));
        assertEquals("four", values.get(3));
        assertEquals("five", values.get(4));
        assertEquals("six", values.get(5));
    }

    @Test
    public void testConcatCovariance() {
        HorrorMovie horrorMovie1 = new HorrorMovie();
        Movie movie = new Movie();
        Media media = new Media();
        HorrorMovie horrorMovie2 = new HorrorMovie();

        Observable<Media> o1 = Observable.<Media> just(horrorMovie1, movie);
        Observable<Media> o2 = Observable.just(media, horrorMovie2);

        Observable<Observable<Media>> os = Observable.just(o1, o2);

        List<Media> values = Observable.concat(os).toList().blockingGet();

        assertEquals(horrorMovie1, values.get(0));
        assertEquals(movie, values.get(1));
        assertEquals(media, values.get(2));
        assertEquals(horrorMovie2, values.get(3));
        assertEquals(4, values.size());
    }

    @Test
    public void testConcatCovariance2() {
        HorrorMovie horrorMovie1 = new HorrorMovie();
        Movie movie = new Movie();
        Media media1 = new Media();
        Media media2 = new Media();
        HorrorMovie horrorMovie2 = new HorrorMovie();

        Observable<Media> o1 = Observable.just(horrorMovie1, movie, media1);
        Observable<Media> o2 = Observable.just(media2, horrorMovie2);

        Observable<Observable<Media>> os = Observable.just(o1, o2);

        List<Media> values = Observable.concat(os).toList().blockingGet();

        assertEquals(horrorMovie1, values.get(0));
        assertEquals(movie, values.get(1));
        assertEquals(media1, values.get(2));
        assertEquals(media2, values.get(3));
        assertEquals(horrorMovie2, values.get(4));
        assertEquals(5, values.size());
    }

    @Test
    public void testConcatCovariance3() {
        HorrorMovie horrorMovie1 = new HorrorMovie();
        Movie movie = new Movie();
        Media media = new Media();
        HorrorMovie horrorMovie2 = new HorrorMovie();

        Observable<Movie> o1 = Observable.just(horrorMovie1, movie);
        Observable<Media> o2 = Observable.just(media, horrorMovie2);

        List<Media> values = Observable.concat(o1, o2).toList().blockingGet();

        assertEquals(horrorMovie1, values.get(0));
        assertEquals(movie, values.get(1));
        assertEquals(media, values.get(2));
        assertEquals(horrorMovie2, values.get(3));
        assertEquals(4, values.size());
    }

    @Test
    public void testConcatCovariance4() {
        final HorrorMovie horrorMovie1 = new HorrorMovie();
        final Movie movie = new Movie();
        Media media = new Media();
        HorrorMovie horrorMovie2 = new HorrorMovie();

        Observable<Movie> o1 = Observable.unsafeCreate(new ObservableSource<Movie>() {
            @Override
            public void subscribe(Observer<? super Movie> o) {
                    o.onNext(horrorMovie1);
                    o.onNext(movie);
                    //                o.onNext(new Media()); // correctly doesn't compile
                    o.onComplete();
            }
        });

        Observable<Media> o2 = Observable.just(media, horrorMovie2);

        List<Media> values = Observable.concat(o1, o2).toList().blockingGet();

        assertEquals(horrorMovie1, values.get(0));
        assertEquals(movie, values.get(1));
        assertEquals(media, values.get(2));
        assertEquals(horrorMovie2, values.get(3));
        assertEquals(4, values.size());
    }
}
