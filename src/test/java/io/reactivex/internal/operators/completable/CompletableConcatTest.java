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

package io.reactivex.internal.operators.completable;

import static org.junit.Assert.*;
import org.junit.Test;
import org.reactivestreams.*;

import io.reactivex.*;
import io.reactivex.exceptions.MissingBackpressureException;
import io.reactivex.internal.subscriptions.BooleanSubscription;

public class CompletableConcatTest {

    @Test
    public void overflowReported() {
        Completable.concat(
            Flowable.fromPublisher(new Publisher<Completable>() {
                @Override
                public void subscribe(Subscriber<? super Completable> s) {
                    s.onSubscribe(new BooleanSubscription());
                    s.onNext(Completable.never());
                    s.onNext(Completable.never());
                    s.onNext(Completable.never());
                    s.onNext(Completable.never());
                    s.onComplete();
                }
            }), 1
        )
        .test()
        .assertFailure(MissingBackpressureException.class);
    }
    
    @Test
    public void invalidPrefetch() {
        try {
            Completable.concat(Flowable.just(Completable.complete()), -99);
            fail("Should have thrown IllegalArgumentExceptio");
        } catch (IllegalArgumentException ex) {
            assertEquals("prefetch > 0 required but it was -99", ex.getMessage());
        }
    }
}
