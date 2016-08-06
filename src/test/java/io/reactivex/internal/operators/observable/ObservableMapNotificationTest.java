/**
 * Copyright 2016 Netflix, Inc.
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

package io.reactivex.internal.operators.observable;

import java.util.concurrent.Callable;

import org.junit.Test;

import io.reactivex.Observable;
import io.reactivex.functions.Function;
import io.reactivex.observers.TestObserver;

public class ObservableMapNotificationTest {
    @Test
    public void testJust() {
        TestObserver<Object> ts = new TestObserver<Object>();
        Observable.just(1)
        .flatMap(
                new Function<Integer, Observable<Object>>() {
                    @Override
                    public Observable<Object> apply(Integer item) {
                        return Observable.just((Object)(item + 1));
                    }
                },
                new Function<Throwable, Observable<Object>>() {
                    @Override
                    public Observable<Object> apply(Throwable e) {
                        return Observable.error(e);
                    }
                },
                new Callable<Observable<Object>>() {
                    @Override
                    public Observable<Object> call() {
                        return Observable.never();
                    }
                }
        ).subscribe(ts);
        
        ts.assertNoErrors();
        ts.assertNotComplete();
        ts.assertValue(2);
    }
}