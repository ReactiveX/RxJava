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

import java.util.concurrent.atomic.AtomicReference;

import rx.Observable;
import rx.Observable.Operator;
import rx.Scheduler;
import rx.Subscriber;
import rx.functions.Func1;
import rx.subjects.Subject;

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
        final UnicastPassThruSubject<T>[] subjects = new UnicastPassThruSubject[degreeOfParallelism];
        @SuppressWarnings("unchecked")
        final Observable<R>[] os = new Observable[degreeOfParallelism];
        for (int i = 0; i < subjects.length; i++) {
            subjects[i] = UnicastPassThruSubject.<T> create();
            os[i] = f.call(subjects[i].observeOn(scheduler));
        }

        // subscribe BEFORE receiving data so everything is hooked up
        Observable.merge(os).unsafeSubscribe(child);

        return new Subscriber<T>(child) {

            int index = 0; // trust that we receive data synchronously

            @Override
            public void onCompleted() {
                for (UnicastPassThruSubject<T> s : subjects) {
                    s.onCompleted();
                }
            }

            @Override
            public void onError(Throwable e) {
                // bypass the subjects and immediately terminate
                child.onError(e);
            }

            @Override
            public void onNext(T t) {
                // round-robin subjects
                subjects[index++].onNext(t);
                if (index >= degreeOfParallelism) {
                    index = 0;
                }
            }

        };

    }

    private static class UnicastPassThruSubject<T> extends Subject<T, T> {

        private static <T> UnicastPassThruSubject<T> create() {
            final AtomicReference<Subscriber<? super T>> subscriber = new AtomicReference<Subscriber<? super T>>();
            return new UnicastPassThruSubject<T>(subscriber, new OnSubscribe<T>() {

                @Override
                public void call(Subscriber<? super T> s) {
                    subscriber.set(s);
                }

            });

        }

        private final AtomicReference<Subscriber<? super T>> subscriber;

        protected UnicastPassThruSubject(AtomicReference<Subscriber<? super T>> subscriber, OnSubscribe<T> onSubscribe) {
            super(onSubscribe);
            this.subscriber = subscriber;
        }

        @Override
        public void onCompleted() {
            subscriber.get().onCompleted();
        }

        @Override
        public void onError(Throwable e) {
            subscriber.get().onError(e);
        }

        @Override
        public void onNext(T t) {
            subscriber.get().onNext(t);
        }

    }
}
