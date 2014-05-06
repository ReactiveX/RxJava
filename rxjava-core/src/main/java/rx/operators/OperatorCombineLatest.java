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
package rx.operators;

import java.util.ArrayList;
import java.util.BitSet;
import java.util.LinkedList;
import java.util.List;

import rx.Observable;
import rx.Observable.OnSubscribe;
import rx.Subscriber;
import rx.functions.FuncN;
import rx.observers.SerializedSubscriber;

/**
 * Returns an Observable that combines the emissions of multiple source observables. Once each
 * source Observable has emitted at least one item, combineLatest emits an item whenever any of
 * the source Observables emits an item, by combining the latest emissions from each source
 * Observable with a specified function.
 * <p>
 * <img width="640" src="https://github.com/Netflix/RxJava/wiki/images/rx-operators/combineLatest.png">
 * 
 * @param <T> the common basetype of the source values
 * @param <R> the result type of the combinator function
 */
public final class OperatorCombineLatest<T, R> implements OnSubscribe<R> {
    final List<? extends Observable<? extends T>> sources;
    final FuncN<? extends R> combinator;

    public OperatorCombineLatest(List<? extends Observable<? extends T>> sources, FuncN<? extends R> combinator) {
        this.sources = sources;
        this.combinator = combinator;
    }

    @Override
    public void call(final Subscriber<? super R> child) {
        if (sources.isEmpty()) {
            child.onCompleted();
            return;
        } else
        if (sources.size() == 1) {
            sources.get(0).unsafeSubscribe(new Subscriber<T>(child) {
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
            });
            return;
        }
        
        SerializedSubscriber<R> s = new SerializedSubscriber<R>(child);
        List<SourceSubscriber> sourceSubscribers = new ArrayList<SourceSubscriber>(sources.size());
        Collector collector = new Collector(s, sources.size());
        
        for (int i = 0; i < sources.size(); i++) {
            SourceSubscriber sourceSub = new SourceSubscriber(i, collector);
            child.add(sourceSub);
            sourceSubscribers.add(sourceSub);
        }
        
        for (int i = 0; i < sources.size(); i++) {
            if (!child.isUnsubscribed()) {
                sources.get(i).unsafeSubscribe(sourceSubscribers.get(i));
            }
        }        
    }
    /** Combines values from each source subscriber. */
    final class Collector {
        final Subscriber<R> s;
        /** Guarded by this. */
        Object[] collectedValues;
        /** Guarded by this. */
        final BitSet haveValues;
        /** Guarded by this. */
        int haveValuesCount;
        /** Guarded by this. */
        final BitSet completion;
        /** Guarded by this. */
        int completionCount;
        /** Guarded by this. */
        boolean emitting;
        /** Guarded by this. */
        List<Object[]> queue;
        public Collector(Subscriber<R> s, int size) {
            this.s = s;
            int n = size;
            this.collectedValues = new Object[n];
            this.haveValues = new BitSet(n);
            this.completion = new BitSet(n);
        }
        void next(int index, T value) {
            Object[] localValues;
            List<Object[]> localQueue;
            
            synchronized (this) {
                if (!haveValues.get(index)) {
                    haveValues.set(index);
                    haveValuesCount++;
                }
                collectedValues[index] = value;
                if (haveValuesCount != collectedValues.length) {
                    return;
                }
                localValues = collectedValues.clone();
                if (emitting) {
                    if (queue == null) {
                        queue = new LinkedList<Object[]>();
                    }
                    queue.add(localValues);
                    return;
                }
                localQueue = queue;
                queue = null;
                emitting = true;
            }
            boolean once = true;
            boolean skipFinal = false;
            /** 
             * This logic, similar to SerializedSubscriber, ensures that the
             * emission order is consistent with the order the synchronized above
             * was won.
             */
            try {
                do {
                    try {
                        if (localQueue != null) {
                            for (Object[] o : localQueue) {
                                s.onNext(combinator.call(o));
                            }
                        }
                        if (once) {
                            once = false;
                            s.onNext(combinator.call(localValues));
                        }
                    } catch (Throwable e) {
                        error(e);
                        return;
                    }

                    synchronized (this) {
                        localQueue = queue;
                        queue = null;
                        if (localQueue == null) {
                            skipFinal = true;
                            emitting = false;
                            return;
                        }
                    }
                } while (!s.isUnsubscribed());
            } finally {
                if (!skipFinal) {
                    synchronized (this) {
                        emitting = false;
                    }
                }
            }
            
        }
        void error(Throwable e) {
            s.onError(e);
            s.unsubscribe();
        }
        void complete(int index, boolean hadValue) {
            if (!hadValue) {
                s.onCompleted();
                s.unsubscribe();
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
                s.onCompleted();
            }
        }
    }
    /** Subscibed to a source. */
    final class SourceSubscriber extends Subscriber<T> {
        final int index;
        final Collector collector;
        private boolean hasValue;
        public SourceSubscriber(int index, Collector collector) {
            this.index = index;
            this.collector = collector;
        }
        @Override
        public void onNext(T t) {
            hasValue = true;
            collector.next(index, t);
        }

        @Override
        public void onError(Throwable e) {
            collector.error(e);
        }

        @Override
        public void onCompleted() {
            collector.complete(index, hasValue);
        }
        
        
    }
}
