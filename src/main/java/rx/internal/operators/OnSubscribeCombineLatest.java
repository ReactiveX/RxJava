/**
 * Copyright 2014 Netflix, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package rx.internal.operators;

import java.util.BitSet;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicLongFieldUpdater;

import rx.Observable;
import rx.Observable.OnSubscribe;
import rx.Producer;
import rx.Subscriber;
import rx.exceptions.MissingBackpressureException;
import rx.functions.FuncN;
import rx.internal.util.RxRingBuffer;

/**
 * Returns an Observable that combines the emissions of multiple source observables. Once each
 * source Observable has emitted at least one item, combineLatest emits an item whenever any of
 * the source Observables emits an item, by combining the latest emissions from each source
 * Observable with a specified function.
 * <p>
 * <img width="640" src="https://github.com/ReactiveX/RxJava/wiki/images/rx-operators/combineLatest.png" alt="">
 * 
 * @param <T>
 *            the common basetype of the source values
 * @param <R>
 *            the result type of the combinator function
 */
public final class OnSubscribeCombineLatest<T, R> implements OnSubscribe<R> {
    final List<? extends Observable<? extends T>> sources;
    final FuncN<? extends R> combinator;

    public OnSubscribeCombineLatest(List<? extends Observable<? extends T>> sources, FuncN<? extends R> combinator) {
        this.sources = sources;
        this.combinator = combinator;
        if (sources.size() > 128) {
            // For design simplicity this is limited to 128. If more are really needed we'll need to adjust 
            // the design of how RxRingBuffer is used in the implementation below.
            throw new IllegalArgumentException("More than 128 sources to combineLatest is not supported.");
        }
    }

    @Override
    public void call(final Subscriber<? super R> child) {
        if (sources.isEmpty()) {
            child.onCompleted();
            return;
        }
        if (sources.size() == 1) {
            child.setProducer(new SingleSourceProducer<T, R>(child, sources.get(0), combinator));
        } else {
            child.setProducer(new MultiSourceProducer<T, R>(child, sources, combinator));
        }

    }

    /*
     * benjchristensen => This implementation uses a buffer enqueue/drain pattern. It could be optimized to have a fast-path to
     * skip the buffer and emit directly when no conflict, but that is quite complicated and I don't have the time to attempt it right now.
     */
    final static class MultiSourceProducer<T, R> implements Producer {
        private final AtomicBoolean started = new AtomicBoolean();
        private final AtomicLong requested = new AtomicLong();
        private final List<? extends Observable<? extends T>> sources;
        private final Subscriber<? super R> child;
        private final FuncN<? extends R> combinator;
        private final MultiSourceRequestableSubscriber<T, R>[] subscribers;

        /* following are guarded by WIP */
        private final RxRingBuffer buffer = RxRingBuffer.getSpmcInstance();
        private final Object[] collectedValues;
        private final BitSet haveValues;
        private volatile int haveValuesCount; // does this need to be volatile or is WIP sufficient?
        private final BitSet completion;
        private volatile int completionCount; // does this need to be volatile or is WIP sufficient?

        @SuppressWarnings("unused")
        private volatile long counter;
        @SuppressWarnings("rawtypes")
        private static final AtomicLongFieldUpdater<MultiSourceProducer> WIP = AtomicLongFieldUpdater.newUpdater(MultiSourceProducer.class, "counter");

        @SuppressWarnings("unchecked")
        public MultiSourceProducer(final Subscriber<? super R> child, final List<? extends Observable<? extends T>> sources, FuncN<? extends R> combinator) {
            this.sources = sources;
            this.child = child;
            this.combinator = combinator;

            int n = sources.size();
            this.subscribers = new MultiSourceRequestableSubscriber[n];
            this.collectedValues = new Object[n];
            this.haveValues = new BitSet(n);
            this.completion = new BitSet(n);
        }

        @Override
        public void request(long n) {
            requested.getAndAdd(n);
            if (!started.get() && started.compareAndSet(false, true)) {
                /*
                 * NOTE: this logic will ONLY work if we don't have more sources than the size of the buffer.
                 * 
                 * We would likely need to make an RxRingBuffer that can be sized to [numSources * n] instead
                 * of the current global default size it has.
                 */
                int sizePerSubscriber = RxRingBuffer.SIZE / sources.size();
                int leftOver = RxRingBuffer.SIZE % sources.size();
                for (int i = 0; i < sources.size(); i++) {
                    Observable<? extends T> o = sources.get(i);
                    int toRequest = sizePerSubscriber;
                    if (i == sources.size() - 1) {
                        toRequest += leftOver;
                    }
                    MultiSourceRequestableSubscriber<T, R> s = new MultiSourceRequestableSubscriber<T, R>(i, toRequest, child, this);
                    subscribers[i] = s;
                    o.unsafeSubscribe(s);
                }
            }
            tick();
        }

        /**
         * This will only allow one thread at a time to do the work, but ensures via `counter` increment/decrement
         * that there is always once who acts on each `tick`. Same concept as used in OperationObserveOn.
         */
        void tick() {
            if (WIP.getAndIncrement(this) == 0) {
                int emitted = 0;
                do {
                    // we only emit if requested > 0
                    if (requested.get() > 0) {
                        Object o = buffer.poll();
                        if (o != null) {
                            if (buffer.isCompleted(o)) {
                                child.onCompleted();
                            } else {
                                buffer.accept(o, child);
                                emitted++;
                                requested.decrementAndGet();
                            }
                        }
                    }
                } while (WIP.decrementAndGet(this) > 0);
                if (emitted > 0) {
                    for (MultiSourceRequestableSubscriber<T, R> s : subscribers) {
                        s.requestUpTo(emitted);
                    }
                }
            }
        }

        public void onCompleted(int index, boolean hadValue) {
            if (!hadValue) {
                child.onCompleted();
                return;
            }
            boolean done = false;
            synchronized (this) {
                if (!completion.get(index)) {
                    completion.set(index);
                    completionCount++;
                    done = completionCount == collectedValues.length;
                }
            }
            if (done) {
                buffer.onCompleted();
                tick();
            }
        }

        /**
         * @return boolean true if propagated value
         */
        public boolean onNext(int index, T t) {
            synchronized (this) {
                if (!haveValues.get(index)) {
                    haveValues.set(index);
                    haveValuesCount++;
                }
                collectedValues[index] = t;
                if (haveValuesCount != collectedValues.length) {
                    // haven't received value from each source yet so won't emit
                    return false;
                } else {
                    try {
                        buffer.onNext(combinator.call(collectedValues));
                    } catch (MissingBackpressureException e) {
                        onError(e);
                    } catch (Throwable e) {
                        onError(e);
                    }
                }
            }
            tick();
            return true;
        }

        public void onError(Throwable e) {
            child.onError(e);
        }
    }

    final static class MultiSourceRequestableSubscriber<T, R> extends Subscriber<T> {

        final MultiSourceProducer<T, R> producer;
        final int index;
        final AtomicLong emitted = new AtomicLong();
        boolean hasValue = false;

        public MultiSourceRequestableSubscriber(int index, int initial, Subscriber<? super R> child, MultiSourceProducer<T, R> producer) {
            super(child);
            this.index = index;
            this.producer = producer;
            request(initial);
        }

        public void requestUpTo(long n) {
            long r = Math.min(emitted.get(), n);
            request(r);
            emitted.addAndGet(-r);
        }

        @Override
        public void onCompleted() {
            producer.onCompleted(index, hasValue);
        }

        @Override
        public void onError(Throwable e) {
            producer.onError(e);
        }

        @Override
        public void onNext(T t) {
            hasValue = true;
            emitted.incrementAndGet();
            boolean emitted = producer.onNext(index, t);
            if (!emitted) {
                request(1);
            }
        }

    }

    final static class SingleSourceProducer<T, R> implements Producer {
        final AtomicBoolean started = new AtomicBoolean();
        final Observable<? extends T> source;
        final Subscriber<? super R> child;
        final FuncN<? extends R> combinator;
        final SingleSourceRequestableSubscriber<T, R> subscriber;

        public SingleSourceProducer(final Subscriber<? super R> child, Observable<? extends T> source, FuncN<? extends R> combinator) {
            this.source = source;
            this.child = child;
            this.combinator = combinator;
            this.subscriber = new SingleSourceRequestableSubscriber<T, R>(child, combinator);
        }

        @Override
        public void request(final long n) {
            subscriber.requestMore(n);
            if (started.compareAndSet(false, true)) {
                source.unsafeSubscribe(subscriber);
            }

        }

    }

    final static class SingleSourceRequestableSubscriber<T, R> extends Subscriber<T> {

        private final Subscriber<? super R> child;
        private final FuncN<? extends R> combinator;

        SingleSourceRequestableSubscriber(Subscriber<? super R> child, FuncN<? extends R> combinator) {
            super(child);
            this.child = child;
            this.combinator = combinator;
        }

        public void requestMore(long n) {
            request(n);
        }

        @Override
        public void onNext(T t) {
            child.onNext(combinator.call(t));
        }

        @Override
        public void onError(Throwable e) {
            child.onError(e);
        }

        @Override
        public void onCompleted() {
            child.onCompleted();
        }
    }
}
