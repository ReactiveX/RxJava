/**
 * Copyright (c) 2016-present, RxJava Contributors.
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
package io.reactivex.flowable;

import io.reactivex.RxJavaTest;
import org.junit.*;

import io.reactivex.Flowable;
import io.reactivex.flowable.FlowableCovarianceTest.*;
import io.reactivex.functions.*;

public class FlowableCombineLatestTests extends RxJavaTest {
    /**
     * This won't compile if super/extends isn't done correctly on generics.
     */
    @Test
    public void covarianceOfCombineLatest() {
        Flowable<HorrorMovie> horrors = Flowable.just(new HorrorMovie());
        Flowable<CoolRating> ratings = Flowable.just(new CoolRating());

        Flowable.<Movie, CoolRating, Result> combineLatest(horrors, ratings, combine).blockingForEach(action);
        Flowable.<Movie, CoolRating, Result> combineLatest(horrors, ratings, combine).blockingForEach(action);
        Flowable.<Media, Rating, ExtendedResult> combineLatest(horrors, ratings, combine).blockingForEach(extendedAction);
        Flowable.<Media, Rating, Result> combineLatest(horrors, ratings, combine).blockingForEach(action);
        Flowable.<Media, Rating, ExtendedResult> combineLatest(horrors, ratings, combine).blockingForEach(action);

        Flowable.<Movie, CoolRating, Result> combineLatest(horrors, ratings, combine);
    }

    BiFunction<Media, Rating, ExtendedResult> combine = new BiFunction<Media, Rating, ExtendedResult>() {
        @Override
        public ExtendedResult apply(Media m, Rating r) {
            return new ExtendedResult();
        }
    };

    Consumer<Result> action = new Consumer<Result>() {
        @Override
        public void accept(Result t1) {
            System.out.println("Result: " + t1);
        }
    };

    Consumer<ExtendedResult> extendedAction = new Consumer<ExtendedResult>() {
        @Override
        public void accept(ExtendedResult t1) {
            System.out.println("Result: " + t1);
        }
    };
}
