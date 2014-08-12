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
package rx.internal.operators;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

import rx.Observable;
import rx.Observable.OnSubscribe;
import rx.Observable.Operator;
import rx.Producer;
import rx.Scheduler;
import rx.Subscriber;
import rx.functions.Func1;

/**
 * Identifies unit of work that can be executed in parallel on a given Scheduler.
 */
public final class OperatorParallel<T, R> implements Operator<R, T> {

    private final Scheduler scheduler;
    private final Func1<Observable<T>, Observable<R>> f;
    private final int degreeOfParallelism;

    public OperatorParallel(Func1<Observable<T>, Observable<R>> f, Scheduler scheduler) {
        this.scheduler = scheduler;
        this.f = f;
        this.degreeOfParallelism = scheduler.parallelism();
    }

    @Override
    public Subscriber<? super T> call(final Subscriber<? super R> child) {
        @SuppressWarnings("unchecked")
        final Observable<R>[] os = new Observable[degreeOfParallelism];
        @SuppressWarnings("unchecked")
        final Subscriber<? super T>[] ss = new Subscriber[degreeOfParallelism];
        final ParentSubscriber subscriber = new ParentSubscriber(child, ss);
        for (int i = 0; i < os.length; i++) {
            final int index = i;
            Observable<T> o = Observable.create(new OnSubscribe<T>() {

                @Override
                public void call(Subscriber<? super T> inner) {
                    ss[index] = inner;
                    child.add(inner); // unsubscribe chain
                    inner.setProducer(new Producer() {

                        @Override
                        public void request(long n) {
                            // as we receive requests from observeOn propagate upstream to the parent Subscriber
                            subscriber.requestMore(n);
                        }

                    });
                }

            });
            os[i] = f.call(o.observeOn(scheduler));
        }

        // subscribe BEFORE receiving data so everything is hooked up
        Observable.merge(os).unsafeSubscribe(child);
        return subscriber;
    }

    private class ParentSubscriber extends Subscriber<T> {

        final Subscriber<? super R> child;
        final Subscriber<? super T>[] ss;
        int index = 0;
        final AtomicLong initialRequest = new AtomicLong();
        final AtomicBoolean started = new AtomicBoolean();

        private ParentSubscriber(Subscriber<? super R> child, Subscriber<? super T>[] ss) {
            super(child);
            this.child = child;
            this.ss = ss;
        }

        public void requestMore(long n) {
            if (started.get()) {
                request(n);
            } else {
                initialRequest.addAndGet(n);
            }
        }

        @Override
        public void onStart() {
            if (started.compareAndSet(false, true)) {
                // if no request via requestMore has been sent yet, we start with 0 (rather than default Long.MAX_VALUE).
                request(initialRequest.get());
            }
        }

        @Override
        public void onCompleted() {
            for (Subscriber<? super T> s : ss) {
                s.onCompleted();
            }
        }

        @Override
        public void onError(Throwable e) {
            child.onError(e);
        }

        @Override
        public void onNext(T t) {
            /*
             * There is a possible bug here ... we could get a MissingBackpressureException
             * if the processing on each of the threads is unbalanced. In other words, if 1 of the
             * observeOn queues has space, but another is full, this could try emitting to one that
             * is full and get a MissingBackpressureException.
             * 
             * To solve that we'd need to check the outstanding request per Subscriber, which will
             * need a more complicated mechanism to expose a type that has both the requested + the
             * Subscriber to emit to.
             */
            ss[index++].onNext(t);
            if (index >= degreeOfParallelism) {
                index = 0;
            }
        }

    };

}
