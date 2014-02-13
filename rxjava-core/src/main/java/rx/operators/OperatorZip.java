/**
 * Copyright 2014 Netflix, Inc.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package rx.operators;

import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicLong;

import rx.Observable;
import rx.Observable.Operator;
import rx.Observer;
import rx.Subscriber;
import rx.operators.OperatorObserveOn.InterruptibleBlockingQueue;
import rx.subscriptions.CompositeSubscription;
import rx.util.functions.Func2;
import rx.util.functions.Func3;
import rx.util.functions.Func4;
import rx.util.functions.Func5;
import rx.util.functions.Func6;
import rx.util.functions.Func7;
import rx.util.functions.Func8;
import rx.util.functions.Func9;
import rx.util.functions.FuncN;
import rx.util.functions.Functions;

/**
 * Returns an Observable that emits the results of a function applied to sets of items emitted, in
 * sequence, by two or more other Observables.
 * <p>
 * <img width="640" src="https://github.com/Netflix/RxJava/wiki/images/rx-operators/zip.png">
 * <p>
 * The zip operation applies this function in strict sequence, so the first item emitted by the new
 * Observable will be the result of the function applied to the first item emitted by each zipped
 * Observable; the second item emitted by the new Observable will be the result of the function
 * applied to the second item emitted by each zipped Observable; and so forth.
 * <p>
 * The resulting Observable returned from zip will invoke <code>onNext</code> as many times as the
 * number of <code>onNext</code> invocations of the source Observable that emits the fewest items.
 */
public final class OperatorZip<R> implements Operator<R, Observable<?>[]> {
    /*
     * Raw types are used so we can use a single implementation for all arities such as zip(t1, t2) and zip(t1, t2, t3) etc.
     * The types will be cast on the edges so usage will be the type-safe but the internals are not.
     */

    final FuncN<? extends R> zipFunction;
    /** The buffer size, nonpositive value indicates an unbounded buffer for each source. */
    final int bufferSize;

    public OperatorZip(FuncN<? extends R> f, int bufferSize) {
        this.zipFunction = f;
        this.bufferSize = bufferSize;
    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
    public OperatorZip(Func2 f, int bufferSize) {
        this.zipFunction = Functions.fromFunc(f);
        this.bufferSize = bufferSize;
    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
    public OperatorZip(Func3 f, int bufferSize) {
        this.zipFunction = Functions.fromFunc(f);
        this.bufferSize = bufferSize;
    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
    public OperatorZip(Func4 f, int bufferSize) {
        this.zipFunction = Functions.fromFunc(f);
        this.bufferSize = bufferSize;
    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
    public OperatorZip(Func5 f, int bufferSize) {
        this.zipFunction = Functions.fromFunc(f);
        this.bufferSize = bufferSize;
    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
    public OperatorZip(Func6 f, int bufferSize) {
        this.zipFunction = Functions.fromFunc(f);
        this.bufferSize = bufferSize;
    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
    public OperatorZip(Func7 f, int bufferSize) {
        this.zipFunction = Functions.fromFunc(f);
        this.bufferSize = bufferSize;
    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
    public OperatorZip(Func8 f, int bufferSize) {
        this.zipFunction = Functions.fromFunc(f);
        this.bufferSize = bufferSize;
    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
    public OperatorZip(Func9 f, int bufferSize) {
        this.zipFunction = Functions.fromFunc(f);
        this.bufferSize = bufferSize;
    }

    @SuppressWarnings("rawtypes")
    @Override
    public Subscriber<? super Observable[]> call(final Subscriber<? super R> observer) {
        return new Subscriber<Observable[]>(observer) {

            @Override
            public void onCompleted() {
                // we only complete once a child Observable completes or errors
            }

            @Override
            public void onError(Throwable e) {
                observer.onError(e);
            }

            @Override
            public void onNext(Observable[] observables) {
                new Zip<R>(observables, observer, zipFunction, bufferSize).zip();
            }

        };
    }

    private static class Zip<R> {
        @SuppressWarnings("rawtypes")
        final Observable[] os;
        final InnerInteraction[] observers;
        final Subscriber<? super R> observer;
        final FuncN<? extends R> zipFunction;
        final CompositeSubscription childSubscription = new CompositeSubscription();

        static Object NULL_SENTINEL = new Object();
        static Object COMPLETE_SENTINEL = new Object();
        /** The buffer size, nonpositive value indicates an unbounded buffer for each source. */
        final int bufferSize;

        @SuppressWarnings("rawtypes")
        public Zip(Observable[] os, final Subscriber<? super R> observer, FuncN<? extends R> zipFunction, int bufferSize) {
            this.os = os;
            this.observer = observer;
            this.zipFunction = zipFunction;
            this.bufferSize = bufferSize > 0 ? OperatorObserveOn.roundToNextPowerOfTwoIfNecessary(bufferSize) : 0;
            this.observers = new InnerInteraction[os.length];
            
            for (int i = 0; i < os.length; i++) {
                if (bufferSize == 0) {
                    InnerObserver io = new InnerObserver();
                    observers[i] = io;
                    childSubscription.add(io);
                } else {
                    InnerBlockingObserver io = new InnerBlockingObserver();
                    observers[i] = io;
                    childSubscription.add(io);
                }
            }

            observer.add(childSubscription);
        }

        @SuppressWarnings("unchecked")
        public void zip() {
            for (int i = 0; i < os.length; i++) {
                if (bufferSize == 0) {
                    os[i].subscribe((InnerObserver) observers[i]);
                } else {
                    os[i].subscribe((InnerBlockingObserver) observers[i]);
                }
            }
        }

        final AtomicLong counter = new AtomicLong(0);

        /**
         * check if we have values for each and emit if we do
         * 
         * This will only allow one thread at a time to do the work, but ensures via `counter` increment/decrement
         * that there is always once who acts on each `tick`. Same concept as used in OperationObserveOn.
         * 
         */
        @SuppressWarnings("unchecked")
        void tick() {
            if (counter.getAndIncrement() == 0) {
                do {
                    Object[] vs = new Object[observers.length];
                    boolean allHaveValues = true;
                    for (int i = 0; i < observers.length; i++) {
                        InnerInteraction io = observers[i];
                        Object v = io.peek();
                        if (v == NULL_SENTINEL) {
                            // special handling for null
                            v = null;
                        } else if (v == COMPLETE_SENTINEL) {
                            // special handling for onComplete
                            observer.onCompleted();
                            // we need to unsubscribe from all children since children are independently subscribed
                            childSubscription.unsubscribe();
                            return;
                        } else if (v == null) {
                            allHaveValues = false;
                            // we continue as there may be an onCompleted on one of the others
                            continue;
                        }
                        vs[i] = v;
                    }
                    if (allHaveValues) {
                        // all have something so emit
                        observer.onNext(zipFunction.call(vs));
                        // now remove them
                        for (InnerInteraction io : observers) {
                            io.poll();
                            // eagerly check if the next item on this queue is an onComplete
                            if (io.peek() == COMPLETE_SENTINEL) {
                                // it is an onComplete so shut down
                                observer.onCompleted();
                                // we need to unsubscribe from all children since children are independently subscribed
                                childSubscription.unsubscribe();
                                return;
                            }
                        }
                    }
                } while (counter.decrementAndGet() > 0);
            }

        }
        /** Exposes the peek and poll queue calls. */
        interface InnerInteraction {
            Object peek();
            Object poll();
        }
        // used to observe each Observable we are zipping together
        // it collects all items in an internal queue
        @SuppressWarnings("rawtypes")
        final class InnerObserver extends Subscriber implements InnerInteraction {
            // Concurrent* since we need to read it from across threads
            final ConcurrentLinkedQueue items = new ConcurrentLinkedQueue();

            @SuppressWarnings("unchecked")
            @Override
            public void onCompleted() {
                items.add(COMPLETE_SENTINEL);
                tick();
            }

            @Override
            public void onError(Throwable e) {
                // emit error and shut down
                observer.onError(e);
            }

            @SuppressWarnings("unchecked")
            @Override
            public void onNext(Object t) {
                if (t == null) {
                    items.add(NULL_SENTINEL);
                } else {
                    items.add(t);
                }
                tick();
            }

            @Override
            public Object peek() {
                return items.peek();
            }

            @Override
            public Object poll() {
                return items.poll();
            }
            
        }
        @SuppressWarnings({ "rawtypes", "unchecked" })
        final class InnerBlockingObserver extends Subscriber implements InnerInteraction {
            final InterruptibleBlockingQueue items = new InterruptibleBlockingQueue(bufferSize);

            @Override
            public void onNext(Object t) {
                try {
                    items.addBlocking(t != null ? t : NULL_SENTINEL);
                    tick();
                } catch (InterruptedException ex) {
                    if (!observer.isUnsubscribed()) {
                        observer.onError(ex);
                    }
                }                
            }

            @Override
            public void onError(Throwable e) {
                observer.onError(e);
            }

            @Override
            public void onCompleted() {
                try {
                    items.addBlocking(COMPLETE_SENTINEL);
                    tick();
                } catch (InterruptedException ex) {
                    if (!observer.isUnsubscribed()) {
                        observer.onError(ex);
                    }
                }
            }

            @Override
            public Object peek() {
                return items.peek();
            }

            @Override
            public Object poll() {
                return items.poll();
            }
            
        }
    }

}
