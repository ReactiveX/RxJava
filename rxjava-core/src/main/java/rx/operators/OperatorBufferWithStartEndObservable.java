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
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import rx.Observable;
import rx.Observable.Operator;
import rx.Observer;
import rx.Subscriber;
import rx.functions.Func1;
import rx.observers.SerializedSubscriber;
import rx.subscriptions.CompositeSubscription;

/**
 * This operation takes
 * values from the specified {@link Observable} source and stores them in the currently active chunks.
 * Initially there are no chunks active.
 * <p>
 * Chunks can be created by pushing a {@code TOpening} value to the "bufferOpenings"
 * {@link Observable}. This creates a new buffer which will then start recording values which are produced
 * by the "source" {@link Observable}. Additionally the "bufferClosingSelector" will be used to construct an
 * {@link Observable} which can produce values. When it does so it will close this (and only this) newly
 * created buffer. When the source {@link Observable} completes or produces an error, all chunks are
 * emitted, and the event is propagated to all subscribed {@link Observer}s.
 * </p><p>
 * Note that when using this operation <strong>multiple overlapping chunks</strong> could be active at any
 * one point.
 * </p>
 * 
 * @param <T> the buffered value type
 */

public final class OperatorBufferWithStartEndObservable<T, TOpening, TClosing> implements Operator<List<T>, T> {
    final Observable<? extends TOpening> bufferOpening;
    final Func1<? super TOpening, ? extends Observable<? extends TClosing>> bufferClosing;

    /**
     * @param bufferOpenings
     *            an {@link Observable} which when it produces a {@code TOpening} value will create a
     *            new buffer which instantly starts recording the "source" {@link Observable}
     * @param bufferClosingSelector
     *            a {@link Func1} object which produces {@link Observable}s. These {@link Observable}s determine
     *            when a buffer is emitted and replaced by simply producing an object.
     */
    public OperatorBufferWithStartEndObservable(Observable<? extends TOpening> bufferOpenings, Func1<? super TOpening, ? extends Observable<? extends TClosing>> bufferClosingSelector) {
        this.bufferOpening = bufferOpenings;
        this.bufferClosing = bufferClosingSelector;
    }

    @Override
    public Subscriber<? super T> call(final Subscriber<? super List<T>> child) {
        
        final BufferingSubscriber bsub = new BufferingSubscriber(new SerializedSubscriber<List<T>>(child));
        
        Subscriber<TOpening> openSubscriber = new Subscriber<TOpening>() {

            @Override
            public void onNext(TOpening t) {
                bsub.startBuffer(t);
            }

            @Override
            public void onError(Throwable e) {
                bsub.onError(e);
            }

            @Override
            public void onCompleted() {
                bsub.onCompleted();
            }
            
        };
        child.add(openSubscriber);
        child.add(bsub);
        
        bufferOpening.unsafeSubscribe(openSubscriber);
        
        return bsub;
    }
    final class BufferingSubscriber extends Subscriber<T> {
        final Subscriber<? super List<T>> child;
        /** Guarded by this. */
        final List<List<T>> chunks;
        /** Guarded by this. */
        boolean done;
        final CompositeSubscription closingSubscriptions;
        public BufferingSubscriber(Subscriber<? super List<T>> child) {
            this.child = child;
            this.chunks = new LinkedList<List<T>>();
            this.closingSubscriptions = new CompositeSubscription();
            add(this.closingSubscriptions);
        }

        @Override
        public void onNext(T t) {
            synchronized (this) {
                for (List<T> chunk : chunks) {
                    chunk.add(t);
                }
            }
        }

        @Override
        public void onError(Throwable e) {
            synchronized (this) {
                if (done) {
                    return;
                }
                done = true;
                chunks.clear();
            }
            child.onError(e);
            unsubscribe();
        }

        @Override
        public void onCompleted() {
            try {
                List<List<T>> toEmit;
                synchronized (this) {
                    if (done) {
                        return;
                    }
                    done = true;
                    toEmit = new LinkedList<List<T>>(chunks);
                    chunks.clear();
                }
                for (List<T> chunk : toEmit) {
                    child.onNext(chunk);
                }
            } catch (Throwable t) {
                child.onError(t);
                return;
            }
            child.onCompleted();
            unsubscribe();
        }
        void startBuffer(TOpening v) {
            final List<T> chunk = new ArrayList<T>();
            synchronized (this) {
                if (done) {
                    return;
                }
                chunks.add(chunk);
            }
            Observable<? extends TClosing> cobs;
            try {
                cobs = bufferClosing.call(v);
            } catch (Throwable t) {
                onError(t);
                return;
            }
            Subscriber<TClosing> closeSubscriber = new Subscriber<TClosing>() {

                @Override
                public void onNext(TClosing t) {
                    closingSubscriptions.remove(this);
                    endBuffer(chunk);
                }

                @Override
                public void onError(Throwable e) {
                    BufferingSubscriber.this.onError(e);
                }

                @Override
                public void onCompleted() {
                    closingSubscriptions.remove(this);
                    endBuffer(chunk);
                }
                
            };
            closingSubscriptions.add(closeSubscriber);
            
            cobs.unsafeSubscribe(closeSubscriber);
        }
        void endBuffer(List<T> toEnd) {
            boolean canEnd = false;
            synchronized (this) {
                if (done) {
                    return;
                }
                Iterator<List<T>> it = chunks.iterator();
                while (it.hasNext()) {
                    List<T> chunk = it.next();
                    if (chunk == toEnd) {
                        canEnd = true;
                        it.remove();
                        break;
                    }
                }
            }
            if (canEnd) {
                child.onNext(toEnd);
            }
        }
        
    }
}
