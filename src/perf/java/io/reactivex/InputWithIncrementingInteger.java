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

package io.reactivex;

import java.util.Iterator;

import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.infra.Blackhole;
import org.reactivestreams.*;

import io.reactivex.internal.subscriptions.EmptySubscription;
import io.reactivex.subscribers.DefaultSubscriber;

/**
 * Exposes an Observable and Observer that increments n Integers and consumes them in a Blackhole.
 */
public abstract class InputWithIncrementingInteger {
    public Iterable<Integer> iterable;
    public Flowable<Integer> observable;
    public Flowable<Integer> firehose;
    public Blackhole bh;

    public abstract int getSize();

    @Setup
    public void setup(final Blackhole bh) {
        this.bh = bh;
        final int size = getSize();
        observable = Flowable.range(0, size);

        firehose = Flowable.unsafeCreate(new Publisher<Integer>() {

            @Override
            public void subscribe(Subscriber<? super Integer> s) {
                s.onSubscribe(EmptySubscription.INSTANCE);
                for (int i = 0; i < size; i++) {
                    s.onNext(i);
                }
                s.onComplete();
            }

        });
        iterable = new Iterable<Integer>() {
            @Override
            public Iterator<Integer> iterator() {
                return new Iterator<Integer>() {
                    int i;

                    @Override
                    public boolean hasNext() {
                        return i < size;
                    }

                    @Override
                    public Integer next() {
                        Blackhole.consumeCPU(10);
                        return i++;
                    }

                    @Override
                    public void remove() {

                    }
                };
            }
        };

    }

    public PerfSubscriber newLatchedObserver() {
        return new PerfSubscriber(bh);
    }

    public Subscriber<Integer> newSubscriber() {
        return new DefaultSubscriber<Integer>() {

            @Override
            public void onComplete() {

            }

            @Override
            public void onError(Throwable e) {

            }

            @Override
            public void onNext(Integer t) {
                bh.consume(t);
            }

        };
    }

}