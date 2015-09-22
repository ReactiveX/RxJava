/**
 * Copyright 2015 Netflix, Inc.
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
package io.reactivex.nbp;

import static org.junit.Assert.assertEquals;

import java.util.*;

import org.junit.Test;

import io.reactivex.NbpObservable;
import io.reactivex.nbp.NbpCovarianceTest.*;

public class NbpConcatTests {

    @Test
    public void testConcatSimple() {
        NbpObservable<String> o1 = NbpObservable.just("one", "two");
        NbpObservable<String> o2 = NbpObservable.just("three", "four");

        List<String> values = NbpObservable.concat(o1, o2).toList().toBlocking().single();

        assertEquals("one", values.get(0));
        assertEquals("two", values.get(1));
        assertEquals("three", values.get(2));
        assertEquals("four", values.get(3));
    }

    @Test
    public void testConcatWithNbpObservableOfNbpObservable() {
        NbpObservable<String> o1 = NbpObservable.just("one", "two");
        NbpObservable<String> o2 = NbpObservable.just("three", "four");
        NbpObservable<String> o3 = NbpObservable.just("five", "six");

        NbpObservable<NbpObservable<String>> os = NbpObservable.just(o1, o2, o3);

        List<String> values = NbpObservable.concat(os).toList().toBlocking().single();

        assertEquals("one", values.get(0));
        assertEquals("two", values.get(1));
        assertEquals("three", values.get(2));
        assertEquals("four", values.get(3));
        assertEquals("five", values.get(4));
        assertEquals("six", values.get(5));
    }

    @Test
    public void testConcatWithIterableOfNbpObservable() {
        NbpObservable<String> o1 = NbpObservable.just("one", "two");
        NbpObservable<String> o2 = NbpObservable.just("three", "four");
        NbpObservable<String> o3 = NbpObservable.just("five", "six");

        Iterable<NbpObservable<String>> is = Arrays.asList(o1, o2, o3);

        List<String> values = NbpObservable.concat(NbpObservable.fromIterable(is)).toList().toBlocking().single();

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
        
        NbpObservable<Media> o1 = NbpObservable.<Media> just(horrorMovie1, movie);
        NbpObservable<Media> o2 = NbpObservable.just(media, horrorMovie2);

        NbpObservable<NbpObservable<Media>> os = NbpObservable.just(o1, o2);

        List<Media> values = NbpObservable.concat(os).toList().toBlocking().single();
        
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
        
        NbpObservable<Media> o1 = NbpObservable.just(horrorMovie1, movie, media1);
        NbpObservable<Media> o2 = NbpObservable.just(media2, horrorMovie2);

        NbpObservable<NbpObservable<Media>> os = NbpObservable.just(o1, o2);

        List<Media> values = NbpObservable.concat(os).toList().toBlocking().single();

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
        
        NbpObservable<Movie> o1 = NbpObservable.just(horrorMovie1, movie);
        NbpObservable<Media> o2 = NbpObservable.just(media, horrorMovie2);

        List<Media> values = NbpObservable.concat(o1, o2).toList().toBlocking().single();

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
        
        NbpObservable<Movie> o1 = NbpObservable.create(o -> {
                o.onNext(horrorMovie1);
                o.onNext(movie);
                //                o.onNext(new Media()); // correctly doesn't compile
                o.onComplete();
        });

        NbpObservable<Media> o2 = NbpObservable.just(media, horrorMovie2);

        List<Media> values = NbpObservable.concat(o1, o2).toList().toBlocking().single();

        assertEquals(horrorMovie1, values.get(0));
        assertEquals(movie, values.get(1));
        assertEquals(media, values.get(2));
        assertEquals(horrorMovie2, values.get(3));
        assertEquals(4, values.size());
    }
}