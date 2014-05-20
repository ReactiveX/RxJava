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

import org.junit.Test;

import rx.CovarianceTest.CoolRating;
import rx.CovarianceTest.ExtendedResult;
import rx.CovarianceTest.HorrorMovie;
import rx.CovarianceTest.Media;
import rx.CovarianceTest.Movie;
import rx.CovarianceTest.Rating;
import rx.CovarianceTest.Result;
import rx.functions.Action1;
import rx.functions.Func2;

public class CombineLatestTests {
    /**
     * This won't compile if super/extends isn't done correctly on generics
     */
    @Test
    public void testCovarianceOfCombineLatest() {
        Observable<HorrorMovie> horrors = Observable.from(new HorrorMovie());
        Observable<CoolRating> ratings = Observable.from(new CoolRating());

        Observable.<Movie, CoolRating, Result> combineLatest(horrors, ratings, combine).toBlocking().forEach(action);
        Observable.<Movie, CoolRating, Result> combineLatest(horrors, ratings, combine).toBlocking().forEach(action);
        Observable.<Media, Rating, ExtendedResult> combineLatest(horrors, ratings, combine).toBlocking().forEach(extendedAction);
        Observable.<Media, Rating, Result> combineLatest(horrors, ratings, combine).toBlocking().forEach(action);
        Observable.<Media, Rating, ExtendedResult> combineLatest(horrors, ratings, combine).toBlocking().forEach(action);

        Observable.<Movie, CoolRating, Result> combineLatest(horrors, ratings, combine);
    }

    Func2<Media, Rating, ExtendedResult> combine = new Func2<Media, Rating, ExtendedResult>() {
        @Override
        public ExtendedResult call(Media m, Rating r) {
            return new ExtendedResult();
        }
    };

    Action1<Result> action = new Action1<Result>() {
        @Override
        public void call(Result t1) {
            System.out.println("Result: " + t1);
        }
    };

    Action1<ExtendedResult> extendedAction = new Action1<ExtendedResult>() {
        @Override
        public void call(ExtendedResult t1) {
            System.out.println("Result: " + t1);
        }
    };
}
