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

import java.util.Arrays;
import java.util.List;

import org.junit.Test;

import rx.CovarianceTest.HorrorMovie;
import rx.CovarianceTest.Media;
import rx.CovarianceTest.Movie;
import rx.Observable.OnSubscribe;

public class ConcatTests {

    @Test
    public void testConcatSimple() {
        Observable<String> o1 = Observable.from("one", "two");
        Observable<String> o2 = Observable.from("three", "four");

        List<String> values = Observable.concat(o1, o2).toList().toBlocking().single();

        assertEquals("one", values.get(0));
        assertEquals("two", values.get(1));
        assertEquals("three", values.get(2));
        assertEquals("four", values.get(3));
    }

    @Test
    public void testConcatWithObservableOfObservable() {
        Observable<String> o1 = Observable.from("one", "two");
        Observable<String> o2 = Observable.from("three", "four");
        Observable<String> o3 = Observable.from("five", "six");

        Observable<Observable<String>> os = Observable.from(o1, o2, o3);

        List<String> values = Observable.concat(os).toList().toBlocking().single();

        assertEquals("one", values.get(0));
        assertEquals("two", values.get(1));
        assertEquals("three", values.get(2));
        assertEquals("four", values.get(3));
        assertEquals("five", values.get(4));
        assertEquals("six", values.get(5));
    }

    @Test
    public void testConcatWithIterableOfObservable() {
        Observable<String> o1 = Observable.from("one", "two");
        Observable<String> o2 = Observable.from("three", "four");
        Observable<String> o3 = Observable.from("five", "six");

        @SuppressWarnings("unchecked")
        Iterable<Observable<String>> is = Arrays.asList(o1, o2, o3);

        List<String> values = Observable.concat(Observable.from(is)).toList().toBlocking().single();

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
    	
        Observable<Media> o1 = Observable.<Media> from(horrorMovie1, movie);
        Observable<Media> o2 = Observable.from(media, horrorMovie2);

        Observable<Observable<Media>> os = Observable.from(o1, o2);

        List<Media> values = Observable.concat(os).toList().toBlocking().single();
        
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
    	
        Observable<Media> o1 = Observable.from(horrorMovie1, movie, media1);
        Observable<Media> o2 = Observable.from(media2, horrorMovie2);

        Observable<Observable<Media>> os = Observable.from(o1, o2);

        List<Media> values = Observable.concat(os).toList().toBlocking().single();

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
    	
        Observable<Movie> o1 = Observable.from(horrorMovie1, movie);
        Observable<Media> o2 = Observable.from(media, horrorMovie2);

        List<Media> values = Observable.concat(o1, o2).toList().toBlocking().single();

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
    	
        Observable<Movie> o1 = Observable.create(new OnSubscribe<Movie>() {

            @Override
            public void call(Subscriber<? super Movie> o) {
                o.onNext(horrorMovie1);
                o.onNext(movie);
                //                o.onNext(new Media()); // correctly doesn't compile
                o.onCompleted();
            }
        });

        Observable<Media> o2 = Observable.from(media, horrorMovie2);

        List<Media> values = Observable.concat(o1, o2).toList().toBlocking().single();

        assertEquals(horrorMovie1, values.get(0));
        assertEquals(movie, values.get(1));
        assertEquals(media, values.get(2));
        assertEquals(horrorMovie2, values.get(3));
        assertEquals(4, values.size());
    }
}
