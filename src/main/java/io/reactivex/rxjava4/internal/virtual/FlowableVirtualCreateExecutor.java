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

package io.reactivex.rxjava4.internal.virtual;

import java.util.Objects;
import java.util.concurrent.*;
import java.util.concurrent.Flow.*;
import java.util.concurrent.atomic.AtomicLong;

import io.reactivex.rxjava4.core.*;
import io.reactivex.rxjava4.exceptions.Exceptions;
import io.reactivex.rxjava4.internal.util.BackpressureHelper;

/**
 * Runs a generator callback on a virtual thread backed by a Worker of the given scheduler
 * and signals events emitted by the generator considering any downstream backpressure.
 *
 * @param <T> the element type of the flow
 * @since 4.0.0
 */
public final class FlowableVirtualCreateExecutor<T> extends Flowable<T> {

    final VirtualGenerator<T> generator;

    final ExecutorService executor;

    public FlowableVirtualCreateExecutor(VirtualGenerator<T> generator, ExecutorService executor) {
        this.generator = generator;
        this.executor = executor;
    }

    @Override
    protected void subscribeActual(Subscriber<? super T> s) {
        var parent = new ExecutorVirtualCreateSubscription<>(s, generator);
        s.onSubscribe(parent);

        executor.submit(parent);
    }

    static final class ExecutorVirtualCreateSubscription<T> extends AtomicLong 
    implements Subscription, Callable<Void>, VirtualEmitter<T> {

        private static final long serialVersionUID = -6959205135542203083L;

        Subscriber<? super T> downstream;

        final VirtualGenerator<T> generator;

        final VirtualResumable consumerReady;

        volatile boolean cancelled;

        static final Throwable STOP = new Throwable("Downstream cancelled");

        long produced;

        ExecutorVirtualCreateSubscription(Subscriber<? super T> downstream, VirtualGenerator<T> generator) {
            this.downstream = downstream;
            this.generator = generator;
            this.consumerReady = new VirtualResumable();
        }

        @Override
        public Void call() {
            try {
                try {
                    generator.generate(this);
                } catch (Throwable ex) {
                    Exceptions.throwIfFatal(ex);
                    if (ex != STOP && !cancelled) {
                        downstream.onError(ex);
                    }
                    return null;
                }
                if (!cancelled) {
                    downstream.onComplete();
                }
            } finally {
                downstream = null;
            }
            return null;
        }

        @Override
        public void request(long n) {
            BackpressureHelper.add(this, n);
            consumerReady.resume();
        }

        @Override
        public void cancel() {
            cancelled = true;
            request(1);
        }

        @Override
        public void emit(T item) throws Throwable {
            Objects.requireNonNull(item, "item is null");
            var p = produced;
            while (get() == p && !cancelled) {
                consumerReady.await();
            }

            if (cancelled) {
                throw STOP;
            }

            downstream.onNext(item);
            produced = p + 1;
        }
    }
}