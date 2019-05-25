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
package io.reactivex.observable;

import org.junit.*;

import io.reactivex.Observable;
import io.reactivex.ObservableEmitter;
import io.reactivex.ObservableOnSubscribe;
import io.reactivex.functions.*;
import io.reactivex.observable.ObservableCovarianceTest.*;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;

public class ObservableCombineLatestTests {
    /**
     * This won't compile if super/extends isn't done correctly on generics.
     */
    @Test
    public void testCovarianceOfCombineLatest() {
        Observable<HorrorMovie> horrors = Observable.just(new HorrorMovie());
        Observable<CoolRating> ratings = Observable.just(new CoolRating());

        Observable.<Movie, CoolRating, Result>combineLatest(horrors, ratings, combine).blockingForEach(action);
        Observable.<Movie, CoolRating, Result>combineLatest(horrors, ratings, combine).blockingForEach(action);
        Observable.<Media, Rating, ExtendedResult>combineLatest(horrors, ratings, combine).blockingForEach(extendedAction);
        Observable.<Media, Rating, Result>combineLatest(horrors, ratings, combine).blockingForEach(action);
        Observable.<Media, Rating, ExtendedResult>combineLatest(horrors, ratings, combine).blockingForEach(action);

        Observable.<Movie, CoolRating, Result>combineLatest(horrors, ratings, combine);
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

    @Test
    public void testNullEmitting() throws Exception {
        final Consumer<Boolean> onNext = spy(new Consumer<Boolean>() {
            @Override public void accept(final Boolean aBoolean) {
            }
        });

        final Consumer<Throwable> onError = spy(new Consumer<Throwable>() {
            @Override public void accept(final Throwable throwable) throws Exception {
            }
        });

        Observable<Boolean> nullObservable = Observable.create(new ObservableOnSubscribe<Boolean>() {
            @Override public void subscribe(final ObservableEmitter<Boolean> emitter) {
                emitter.onNext(null);
            }
        });

        Observable<Boolean> nonNullObservable = Observable.create(new ObservableOnSubscribe<Boolean>() {
            @Override public void subscribe(final ObservableEmitter<Boolean> emitter) {
                emitter.onNext(true);
            }
        });

        Observable<Boolean> combined =
                Observable.combineLatest(nullObservable, nonNullObservable, new BiFunction<Boolean, Boolean, Boolean>() {
                    @Override
                    public Boolean apply(Boolean bool1, Boolean bool2) {
                        return false;
                    }
                });

        combined.subscribe(onNext, onError);

        verify(onNext, never()).accept(any(Boolean.class));
        verify(onError).accept(any(NullPointerException.class));
    }
}
