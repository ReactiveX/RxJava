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

package io.reactivex.internal.operators.nbp;

import java.util.function.*;

import org.junit.Test;

import io.reactivex.NbpObservable;
import io.reactivex.subscribers.nbp.NbpTestSubscriber;

public class NbpOperatorMapNotificationTest {
    @Test
    public void testJust() {
        NbpTestSubscriber<Object> ts = new NbpTestSubscriber<>();
        NbpObservable.just(1)
        .flatMap(
                new Function<Integer, NbpObservable<Object>>() {
                    @Override
                    public NbpObservable<Object> apply(Integer item) {
                        return NbpObservable.just((Object)(item + 1));
                    }
                },
                new Function<Throwable, NbpObservable<Object>>() {
                    @Override
                    public NbpObservable<Object> apply(Throwable e) {
                        return NbpObservable.error(e);
                    }
                },
                new Supplier<NbpObservable<Object>>() {
                    @Override
                    public NbpObservable<Object> get() {
                        return NbpObservable.never();
                    }
                }
        ).subscribe(ts);
        
        ts.assertNoErrors();
        ts.assertNotComplete();
        ts.assertValue(2);
    }
}