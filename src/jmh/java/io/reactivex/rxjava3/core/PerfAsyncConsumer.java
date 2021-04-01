/*
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

import java.util.concurrent.CountDownLatch;

import org.openjdk.jmh.infra.Blackhole;
import org.reactivestreams.Subscription;

import io.reactivex.rxjava3.disposables.Disposable;

/**
 * A multi-type asynchronous consumer.
 */
public final class PerfAsyncConsumer extends CountDownLatch implements FlowableSubscriber<Object>, Observer<Object>,
SingleObserver<Object>, CompletableObserver, MaybeObserver<Object> {

    final Blackhole bh;

    public PerfAsyncConsumer(Blackhole bh) {
        super(1);
        this.bh = bh;
    }

    @Override
    public void onSuccess(Object value) {
        bh.consume(value);
        countDown();
    }

    @Override
    public void onSubscribe(Disposable d) {
    }

    @Override
    public void onSubscribe(Subscription s) {
        s.request(Long.MAX_VALUE);
    }

    @Override
    public void onNext(Object t) {
        bh.consume(t);
    }

    @Override
    public void onError(Throwable t) {
        t.printStackTrace();
        countDown();
    }

    @Override
    public void onComplete() {
        bh.consume(true);
        countDown();
    }

    /**
     * Wait for the terminal signal.
     * @param count if less than 1001, a spin-wait is used
     * @return this
     */
    public PerfAsyncConsumer await(int count) {
        if (count <= 1000) {
            while (getCount() != 0) { }
        } else {
            try {
                await();
            } catch (InterruptedException ex) {
                throw new RuntimeException(ex);
            }
        }
        return this;
    }

}
