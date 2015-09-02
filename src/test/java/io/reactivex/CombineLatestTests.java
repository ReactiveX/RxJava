/**
 * Copyright 2015 Netflix, Inc.
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
package io.reactivex;

import static io.reactivex.Observable.combineLatest;
import static org.junit.Assert.assertNull;

import java.util.function.*;

import org.junit.*;

import io.reactivex.CovarianceTest.*;
import io.reactivex.subjects.BehaviorSubject;

public class CombineLatestTests {
    /**
     * This won't compile if super/extends isn't done correctly on generics
     */
    @Test
    public void testCovarianceOfCombineLatest() {
        Observable<HorrorMovie> horrors = Observable.just(new HorrorMovie());
        Observable<CoolRating> ratings = Observable.just(new CoolRating());

        Observable.<Movie, CoolRating, Result> combineLatest(horrors, ratings, combine).toBlocking().forEach(action);
        Observable.<Movie, CoolRating, Result> combineLatest(horrors, ratings, combine).toBlocking().forEach(action);
        Observable.<Media, Rating, ExtendedResult> combineLatest(horrors, ratings, combine).toBlocking().forEach(extendedAction);
        Observable.<Media, Rating, Result> combineLatest(horrors, ratings, combine).toBlocking().forEach(action);
        Observable.<Media, Rating, ExtendedResult> combineLatest(horrors, ratings, combine).toBlocking().forEach(action);

        Observable.<Movie, CoolRating, Result> combineLatest(horrors, ratings, combine);
    }

    BiFunction<Media, Rating, ExtendedResult> combine = (m, r) -> new ExtendedResult();

    Consumer<Result> action = t1 -> System.out.println("Result: " + t1);

    Consumer<ExtendedResult> extendedAction = t1 -> System.out.println("Result: " + t1);

    @Ignore
    @Test
    public void testNullEmitting() throws Exception {
        // FIXME this is no longer allowed
        Observable<Boolean> nullObservable = BehaviorSubject.createDefault((Boolean) null);
        Observable<Boolean> nonNullObservable = BehaviorSubject.createDefault(true);
        Observable<Boolean> combined =
                combineLatest(nullObservable, nonNullObservable, new BiFunction<Boolean, Boolean, Boolean>() {
                    @Override
                    public Boolean apply(Boolean bool1, Boolean bool2) {
                        return bool1 == null ? null : bool2;
                    }
                });
        combined.subscribe(aBoolean -> assertNull(aBoolean));
    }
}