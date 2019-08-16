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

package io.reactivex.rxjava3.observable;

import static org.junit.Assert.*;

import java.util.List;

import org.junit.Test;

import io.reactivex.rxjava3.core.*;
import io.reactivex.rxjava3.functions.Supplier;
import io.reactivex.rxjava3.observable.ObservableCovarianceTest.*;

public class ObservableMergeTests extends RxJavaTest {

    /**
     * This won't compile if super/extends isn't done correctly on generics.
     */
    @Test
    public void covarianceOfMerge() {
        Observable<HorrorMovie> horrors = Observable.just(new HorrorMovie());
        Observable<Observable<HorrorMovie>> metaHorrors = Observable.just(horrors);
        Observable.<Media> merge(metaHorrors);
    }

    @Test
    public void mergeCovariance() {
        Observable<Media> o1 = Observable.<Media> just(new HorrorMovie(), new Movie());
        Observable<Media> o2 = Observable.just(new Media(), new HorrorMovie());

        Observable<Observable<Media>> os = Observable.just(o1, o2);

        List<Media> values = Observable.merge(os).toList().blockingGet();

        assertEquals(4, values.size());
    }

    @Test
    public void mergeCovariance2() {
        Observable<Media> o1 = Observable.just(new HorrorMovie(), new Movie(), new Media());
        Observable<Media> o2 = Observable.just(new Media(), new HorrorMovie());

        Observable<Observable<Media>> os = Observable.just(o1, o2);

        List<Media> values = Observable.merge(os).toList().blockingGet();

        assertEquals(5, values.size());
    }

    @Test
    public void mergeCovariance3() {
        Observable<Movie> o1 = Observable.just(new HorrorMovie(), new Movie());
        Observable<Media> o2 = Observable.just(new Media(), new HorrorMovie());

        List<Media> values = Observable.merge(o1, o2).toList().blockingGet();

        assertTrue(values.get(0) instanceof HorrorMovie);
        assertTrue(values.get(1) instanceof Movie);
        assertNotNull(values.get(2));
        assertTrue(values.get(3) instanceof HorrorMovie);
    }

    @Test
    public void mergeCovariance4() {

        Observable<Movie> o1 = Observable.defer(new Supplier<Observable<Movie>>() {
            @Override
            public Observable<Movie> get() {
                return Observable.just(
                        new HorrorMovie(),
                        new Movie()
                );
            }
        });

        Observable<Media> o2 = Observable.just(new Media(), new HorrorMovie());

        List<Media> values = Observable.merge(o1, o2).toList().blockingGet();

        assertTrue(values.get(0) instanceof HorrorMovie);
        assertTrue(values.get(1) instanceof Movie);
        assertNotNull(values.get(2));
        assertTrue(values.get(3) instanceof HorrorMovie);
    }

}
