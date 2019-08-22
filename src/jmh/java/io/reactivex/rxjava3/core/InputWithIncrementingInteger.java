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

package io.reactivex.rxjava3.core;

import java.util.Iterator;

import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.infra.Blackhole;
import org.reactivestreams.*;

import io.reactivex.rxjava3.internal.subscriptions.EmptySubscription;
import io.reactivex.rxjava3.subscribers.DefaultSubscriber;

/**
 * Exposes an Observable and Observer that increments n Integers and consumes them in a Blackhole.
 */
public abstract class InputWithIncrementingInteger {
    final class DefaultSubscriberImpl extends DefaultSubscriber<Integer> {
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
    }

    final class IncrementingIterable implements Iterable<Integer> {

        final class IncrementingIterator implements Iterator<Integer> {
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
        }

        final int size;

        IncrementingIterable(int size) {
            this.size = size;
        }

        @Override
        public Iterator<Integer> iterator() {
            return new IncrementingIterator();
        }
    }

    final class IncrementingPublisher implements Publisher<Integer> {

        final int size;

        IncrementingPublisher(int size) {
            this.size = size;
        }

        @Override
        public void subscribe(Subscriber<? super Integer> s) {
            s.onSubscribe(EmptySubscription.INSTANCE);
            for (int i = 0; i < size; i++) {
                s.onNext(i);
            }
            s.onComplete();
        }
    }

    public Iterable<Integer> iterable;
    public Flowable<Integer> flowable;
    public Flowable<Integer> firehose;
    public Blackhole bh;

    public abstract int getSize();

    @Setup
    public void setup(final Blackhole bh) {
        this.bh = bh;
        final int size = getSize();
        flowable = Flowable.range(0, size);

        firehose = Flowable.unsafeCreate(new IncrementingPublisher(size));
        iterable = new IncrementingIterable(size);

    }

    public PerfSubscriber newLatchedObserver() {
        return new PerfSubscriber(bh);
    }

    public FlowableSubscriber<Integer> newSubscriber() {
        return new DefaultSubscriberImpl();
    }

}
