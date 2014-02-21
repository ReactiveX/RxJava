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

import rx.Observable;
import rx.Observable.OnSubscribeFunc;
import rx.Observer;
import rx.Subscription;
import rx.functions.Func1;
import rx.subscriptions.CompositeSubscription;
import rx.subscriptions.SerialSubscription;

/**
 * Transforms an Observable that emits Observables into a single Observable that
 * emits the items emitted by the most recently published of those Observables.
 * <p>
 * <img width="640" src=
 * "https://github.com/Netflix/RxJava/wiki/images/rx-operators/switchDo.png">
 */
public final class OperationSwitch {

    /**
     * This function transforms an {@link Observable} sequence of {@link Observable} sequences into a single {@link Observable} sequence
     * which produces values from the most recently published {@link Observable} .
     * 
     * @param sequences
     *            The {@link Observable} sequence consisting of {@link Observable} sequences.
     * @return A {@link Func1} which does this transformation.
     */
    public static <T> OnSubscribeFunc<T> switchDo(final Observable<? extends Observable<? extends T>> sequences) {
        return new OnSubscribeFunc<T>() {
            @Override
            public Subscription onSubscribe(Observer<? super T> observer) {
                return new Switch<T>(sequences).onSubscribe(observer);
            }
        };
    }

    private static class Switch<T> implements OnSubscribeFunc<T> {

        private final Observable<? extends Observable<? extends T>> sequences;

        public Switch(Observable<? extends Observable<? extends T>> sequences) {
            this.sequences = sequences;
        }

        @Override
        public Subscription onSubscribe(Observer<? super T> observer) {
            SafeObservableSubscription parent;
            parent = new SafeObservableSubscription();

            SerialSubscription child = new SerialSubscription();

            parent.wrap(sequences.subscribe(new SwitchObserver<T>(observer, parent, child)));

            return new CompositeSubscription(parent, child);
        }
    }

    private static class SwitchObserver<T> implements Observer<Observable<? extends T>> {

        private final Object gate;
        private final Observer<? super T> observer;
        private final SafeObservableSubscription parent;
        private final SerialSubscription child;
        private long latest;
        private boolean stopped;
        private boolean hasLatest;

        public SwitchObserver(Observer<? super T> observer, SafeObservableSubscription parent,
                SerialSubscription child) {
            this.observer = observer;
            this.parent = parent;
            this.child = child;
            this.gate = new Object();
        }

        @Override
        public void onNext(Observable<? extends T> args) {
            final long id;
            synchronized (gate) {
                id = ++latest;
                this.hasLatest = true;
            }

            final SafeObservableSubscription sub = new SafeObservableSubscription();
            sub.wrap(args.subscribe(new Observer<T>() {
                @Override
                public void onNext(T args) {
                    synchronized (gate) {
                        if (latest == id) {
                            SwitchObserver.this.observer.onNext(args);
                        }
                    }
                }

                @Override
                public void onError(Throwable e) {
                    sub.unsubscribe();
                    SafeObservableSubscription s = null;
                    synchronized (gate) {
                        if (latest == id) {
                            SwitchObserver.this.observer.onError(e);
                            s = SwitchObserver.this.parent;
                        }
                    }
                    if (s != null) {
                        s.unsubscribe();
                    }
                }

                @Override
                public void onCompleted() {
                    sub.unsubscribe();
                    SafeObservableSubscription s = null;
                    synchronized (gate) {
                        if (latest == id) {
                            SwitchObserver.this.hasLatest = false;

                            if (stopped) {
                                SwitchObserver.this.observer.onCompleted();
                                s = SwitchObserver.this.parent;
                            }
                        }
                    }
                    if (s != null) {
                        s.unsubscribe();
                    }
                }

            }));

            this.child.setSubscription(sub);
        }

        @Override
        public void onError(Throwable e) {
            synchronized (gate) {
                this.observer.onError(e);
            }

            this.parent.unsubscribe();
        }

        @Override
        public void onCompleted() {
            SafeObservableSubscription s = null;
            synchronized (gate) {
                this.stopped = true;
                if (!this.hasLatest) {
                    this.observer.onCompleted();
                    s = this.parent;
                }
            }
            if (s != null) {
                s.unsubscribe();
            }
        }

    }
}
