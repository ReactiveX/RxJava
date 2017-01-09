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
package io.reactivex.flowable;

import static org.junit.Assert.assertEquals;

import java.util.*;

import org.junit.Test;
import org.reactivestreams.*;

import io.reactivex.Flowable;
import io.reactivex.flowable.FlowableCovarianceTest.*;

public class FlowableConcatTests {

    @Test
    public void testConcatSimple() {
        Flowable<String> o1 = Flowable.just("one", "two");
        Flowable<String> o2 = Flowable.just("three", "four");

        List<String> values = Flowable.concat(o1, o2).toList().blockingGet();

        assertEquals("one", values.get(0));
        assertEquals("two", values.get(1));
        assertEquals("three", values.get(2));
        assertEquals("four", values.get(3));
    }

    @Test
    public void testConcatWithFlowableOfFlowable() {
        Flowable<String> o1 = Flowable.just("one", "two");
        Flowable<String> o2 = Flowable.just("three", "four");
        Flowable<String> o3 = Flowable.just("five", "six");

        Flowable<Flowable<String>> os = Flowable.just(o1, o2, o3);

        List<String> values = Flowable.concat(os).toList().blockingGet();

        assertEquals("one", values.get(0));
        assertEquals("two", values.get(1));
        assertEquals("three", values.get(2));
        assertEquals("four", values.get(3));
        assertEquals("five", values.get(4));
        assertEquals("six", values.get(5));
    }

    @Test
    public void testConcatWithIterableOfFlowable() {
        Flowable<String> o1 = Flowable.just("one", "two");
        Flowable<String> o2 = Flowable.just("three", "four");
        Flowable<String> o3 = Flowable.just("five", "six");

        @SuppressWarnings("unchecked")
        Iterable<Flowable<String>> is = Arrays.asList(o1, o2, o3);

        List<String> values = Flowable.concat(Flowable.fromIterable(is)).toList().blockingGet();

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

        Flowable<Media> o1 = Flowable.<Media> just(horrorMovie1, movie);
        Flowable<Media> o2 = Flowable.just(media, horrorMovie2);

        Flowable<Flowable<Media>> os = Flowable.just(o1, o2);

        List<Media> values = Flowable.concat(os).toList().blockingGet();

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

        Flowable<Media> o1 = Flowable.just(horrorMovie1, movie, media1);
        Flowable<Media> o2 = Flowable.just(media2, horrorMovie2);

        Flowable<Flowable<Media>> os = Flowable.just(o1, o2);

        List<Media> values = Flowable.concat(os).toList().blockingGet();

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

        Flowable<Movie> o1 = Flowable.just(horrorMovie1, movie);
        Flowable<Media> o2 = Flowable.just(media, horrorMovie2);

        List<Media> values = Flowable.concat(o1, o2).toList().blockingGet();

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

        Flowable<Movie> o1 = Flowable.unsafeCreate(new Publisher<Movie>() {
            @Override
            public void subscribe(Subscriber<? super Movie> o) {
                    o.onNext(horrorMovie1);
                    o.onNext(movie);
                    //                o.onNext(new Media()); // correctly doesn't compile
                    o.onComplete();
            }
        });

        Flowable<Media> o2 = Flowable.just(media, horrorMovie2);

        List<Media> values = Flowable.concat(o1, o2).toList().blockingGet();

        assertEquals(horrorMovie1, values.get(0));
        assertEquals(movie, values.get(1));
        assertEquals(media, values.get(2));
        assertEquals(horrorMovie2, values.get(3));
        assertEquals(4, values.size());
    }
}
