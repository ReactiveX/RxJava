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

package io.reactivex.rxjava3.internal.operators.flowable;

import static org.junit.Assert.*;

import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;

import org.junit.Test;

import io.reactivex.rxjava3.core.*;
import io.reactivex.rxjava3.functions.*;
import io.reactivex.rxjava3.subscribers.DefaultSubscriber;

public class FlowableDoOnRequestTest extends RxJavaTest {

    @Test
    public void unsubscribeHappensAgainstParent() {
        final AtomicBoolean unsubscribed = new AtomicBoolean(false);
        Flowable.just(1).concatWith(Flowable.<Integer>never())
        //
                .doOnCancel(new Action() {
                    @Override
                    public void run() {
                        unsubscribed.set(true);
                    }
                })
                //
                .doOnRequest(new LongConsumer() {
                    @Override
                    public void accept(long n) {
                        // do nothing
                    }
                })
                //
                .subscribe().dispose();
        assertTrue(unsubscribed.get());
    }

    @Test
    public void doRequest() {
        final List<Long> requests = new ArrayList<>();
        Flowable.range(1, 5)
        //
                .doOnRequest(new LongConsumer() {
                    @Override
                    public void accept(long n) {
                        requests.add(n);
                    }
                })
                //
                .subscribe(new DefaultSubscriber<Integer>() {

                    @Override
                    public void onStart() {
                        request(3);
                    }

                    @Override
                    public void onComplete() {

                    }

                    @Override
                    public void onError(Throwable e) {

                    }

                    @Override
                    public void onNext(Integer t) {
                        request(t);
                    }
                });
        assertEquals(Arrays.asList(3L, 1L, 2L, 3L, 4L, 5L), requests);
    }
}
