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

import static org.junit.Assert.*;

import java.util.List;

import org.junit.Test;

import io.reactivex.NbpObservable;
import io.reactivex.nbp.NbpCovarianceTest.*;

public class NbpMergeTests {

    /**
     * This won't compile if super/extends isn't done correctly on generics
     */
    @Test
    public void testCovarianceOfMerge() {
        NbpObservable<HorrorMovie> horrors = NbpObservable.just(new HorrorMovie());
        NbpObservable<NbpObservable<HorrorMovie>> metaHorrors = NbpObservable.just(horrors);
        NbpObservable.<Media> merge(metaHorrors);
    }

    @Test
    public void testMergeCovariance() {
        NbpObservable<Media> o1 = NbpObservable.<Media> just(new HorrorMovie(), new Movie());
        NbpObservable<Media> o2 = NbpObservable.just(new Media(), new HorrorMovie());

        NbpObservable<NbpObservable<Media>> os = NbpObservable.just(o1, o2);

        List<Media> values = NbpObservable.merge(os).toList().toBlocking().single();
        
        assertEquals(4, values.size());
    }

    @Test
    public void testMergeCovariance2() {
        NbpObservable<Media> o1 = NbpObservable.just(new HorrorMovie(), new Movie(), new Media());
        NbpObservable<Media> o2 = NbpObservable.just(new Media(), new HorrorMovie());

        NbpObservable<NbpObservable<Media>> os = NbpObservable.just(o1, o2);

        List<Media> values = NbpObservable.merge(os).toList().toBlocking().single();

        assertEquals(5, values.size());
    }

    @Test
    public void testMergeCovariance3() {
        NbpObservable<Movie> o1 = NbpObservable.just(new HorrorMovie(), new Movie());
        NbpObservable<Media> o2 = NbpObservable.just(new Media(), new HorrorMovie());

        List<Media> values = NbpObservable.merge(o1, o2).toList().toBlocking().single();

        assertTrue(values.get(0) instanceof HorrorMovie);
        assertTrue(values.get(1) instanceof Movie);
        assertTrue(values.get(2) != null);
        assertTrue(values.get(3) instanceof HorrorMovie);
    }

    @Test
    public void testMergeCovariance4() {

        NbpObservable<Movie> o1 = NbpObservable.defer(() -> NbpObservable.just(
                new HorrorMovie(),
                new Movie()
        ));
        
        NbpObservable<Media> o2 = NbpObservable.just(new Media(), new HorrorMovie());

        List<Media> values = NbpObservable.merge(o1, o2).toList().toBlocking().single();

        assertTrue(values.get(0) instanceof HorrorMovie);
        assertTrue(values.get(1) instanceof Movie);
        assertTrue(values.get(2) != null);
        assertTrue(values.get(3) instanceof HorrorMovie);
    }

}