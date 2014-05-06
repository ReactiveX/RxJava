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
import rx.exceptions.OnErrorThrowable;
import rx.functions.Func2;
import rx.functions.Func3;
import rx.functions.Func4;
import rx.functions.Func5;
import rx.functions.Func6;
import rx.functions.Func7;
import rx.functions.Func8;
import rx.functions.Func9;
import rx.functions.FuncN;
import rx.functions.Functions;
import rx.subscriptions.CompositeSubscription;

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

    public OperatorZip(FuncN<? extends R> f) {
        this.zipFunction = f;
    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
    public OperatorZip(Func2 f) {
        this.zipFunction = Functions.fromFunc(f);
    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
    public OperatorZip(Func3 f) {
        this.zipFunction = Functions.fromFunc(f);
    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
    public OperatorZip(Func4 f) {
        this.zipFunction = Functions.fromFunc(f);
    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
    public OperatorZip(Func5 f) {
        this.zipFunction = Functions.fromFunc(f);
    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
    public OperatorZip(Func6 f) {
        this.zipFunction = Functions.fromFunc(f);
    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
    public OperatorZip(Func7 f) {
        this.zipFunction = Functions.fromFunc(f);
    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
    public OperatorZip(Func8 f) {
        this.zipFunction = Functions.fromFunc(f);
    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
    public OperatorZip(Func9 f) {
        this.zipFunction = Functions.fromFunc(f);
    }

    @SuppressWarnings("rawtypes")
    @Override
    public Subscriber<? super Observable[]> call(final Subscriber<? super R> observer) {
        return new Subscriber<Observable[]>(observer) {

            boolean started = false;

            @Override
            public void onCompleted() {
                if (!started) {
                    // this means we have not received a valid onNext before termination so we emit the onCompleted
                    observer.onCompleted();
                }
            }

            @Override
            public void onError(Throwable e) {
                observer.onError(e);
            }

            @Override
            public void onNext(Observable[] observables) {
                if (observables == null || observables.length == 0) {
                    observer.onCompleted();
                } else {
                    started = true;
                    new Zip<R>(observables, observer, zipFunction).zip();
                }
            }

        };
    }

    static final NotificationLite<Object> on = NotificationLite.instance();
    private static class Zip<R> {
        @SuppressWarnings("rawtypes")
        final Observable[] os;
        final Object[] observers;
        final Observer<? super R> observer;
        final FuncN<? extends R> zipFunction;
        final CompositeSubscription childSubscription = new CompositeSubscription();
        

        @SuppressWarnings("rawtypes")
        public Zip(Observable[] os, final Subscriber<? super R> observer, FuncN<? extends R> zipFunction) {
            this.os = os;
            this.observer = observer;
            this.zipFunction = zipFunction;
            observers = new Object[os.length];
            for (int i = 0; i < os.length; i++) {
                InnerObserver io = new InnerObserver();
                observers[i] = io;
                childSubscription.add(io);
            }

            observer.add(childSubscription);
        }

        @SuppressWarnings("unchecked")
        public void zip() {
            for (int i = 0; i < os.length; i++) {
                os[i].unsafeSubscribe((InnerObserver) observers[i]);
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
                    final Object[] vs = new Object[observers.length];
                    boolean allHaveValues = true;
                    for (int i = 0; i < observers.length; i++) {
                        Object n = ((InnerObserver) observers[i]).items.peek();

                        if (n == null) {
                            allHaveValues = false;
                            continue;
                        }

                        switch (on.kind(n)) {
                        case OnNext:
                            vs[i] = on.getValue(n);
                            break;
                        case OnCompleted:
                            observer.onCompleted();
                            // we need to unsubscribe from all children since children are
                            // independently subscribed
                            childSubscription.unsubscribe();
                            return;
                        default:
                            // shouldn't get here
                        }
                    }
                    if (allHaveValues) {
                        try {
                            // all have something so emit
                            observer.onNext(zipFunction.call(vs));
                        } catch (Throwable e) {
                            observer.onError(OnErrorThrowable.addValueAsLastCause(e, vs));
                            return;
                        }
                        // now remove them
                        for (int i = 0; i < observers.length; i++) {
                            ((InnerObserver) observers[i]).items.poll();
                            // eagerly check if the next item on this queue is an onComplete
                            if (on.isCompleted(((InnerObserver) observers[i]).items.peek())) {
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

        // used to observe each Observable we are zipping together
        // it collects all items in an internal queue
        @SuppressWarnings("rawtypes")
        final class InnerObserver extends Subscriber {
            // Concurrent* since we need to read it from across threads
            final ConcurrentLinkedQueue items = new ConcurrentLinkedQueue();

            @SuppressWarnings("unchecked")
            @Override
            public void onCompleted() {
                items.add(on.completed());
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
                items.add(on.next(t));
                tick();
            }
        };
    }

}
