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

package io.reactivex.rxjava3.flowable;

import static org.junit.Assert.*;

import java.util.List;

import org.junit.Test;
import org.reactivestreams.Publisher;

import io.reactivex.rxjava3.core.*;
import io.reactivex.rxjava3.flowable.FlowableCovarianceTest.*;
import io.reactivex.rxjava3.functions.Supplier;

public class FlowableMergeTests extends RxJavaTest {

    /**
     * This won't compile if super/extends isn't done correctly on generics.
     */
    @Test
    public void covarianceOfMerge() {
        Flowable<HorrorMovie> horrors = Flowable.just(new HorrorMovie());
        Flowable<Flowable<HorrorMovie>> metaHorrors = Flowable.just(horrors);
        Flowable.<Media> merge(metaHorrors);
    }

    @Test
    public void mergeCovariance() {
        Flowable<Media> f1 = Flowable.<Media> just(new HorrorMovie(), new Movie());
        Flowable<Media> f2 = Flowable.just(new Media(), new HorrorMovie());

        Flowable<Flowable<Media>> os = Flowable.just(f1, f2);

        List<Media> values = Flowable.merge(os).toList().blockingGet();

        assertEquals(4, values.size());
    }

    @Test
    public void mergeCovariance2() {
        Flowable<Media> f1 = Flowable.just(new HorrorMovie(), new Movie(), new Media());
        Flowable<Media> f2 = Flowable.just(new Media(), new HorrorMovie());

        Flowable<Flowable<Media>> os = Flowable.just(f1, f2);

        List<Media> values = Flowable.merge(os).toList().blockingGet();

        assertEquals(5, values.size());
    }

    @Test
    public void mergeCovariance3() {
        Flowable<Movie> f1 = Flowable.just(new HorrorMovie(), new Movie());
        Flowable<Media> f2 = Flowable.just(new Media(), new HorrorMovie());

        List<Media> values = Flowable.merge(f1, f2).toList().blockingGet();

        assertTrue(values.get(0) instanceof HorrorMovie);
        assertTrue(values.get(1) instanceof Movie);
        assertNotNull(values.get(2));
        assertTrue(values.get(3) instanceof HorrorMovie);
    }

    @Test
    public void mergeCovariance4() {

        Flowable<Movie> f1 = Flowable.defer(new Supplier<Publisher<Movie>>() {
            @Override
            public Publisher<Movie> get() {
                return Flowable.just(
                        new HorrorMovie(),
                        new Movie()
                );
            }
        });

        Flowable<Media> f2 = Flowable.just(new Media(), new HorrorMovie());

        List<Media> values = Flowable.merge(f1, f2).toList().blockingGet();

        assertTrue(values.get(0) instanceof HorrorMovie);
        assertTrue(values.get(1) instanceof Movie);
        assertNotNull(values.get(2));
        assertTrue(values.get(3) instanceof HorrorMovie);
    }

}
