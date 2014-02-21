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
import rx.Subscriber;
import rx.Subscription;
import rx.functions.Action0;
import rx.functions.Func0;
import rx.functions.Func1;
import rx.observables.ConnectableObservable;
import rx.subjects.Subject;
import rx.subscriptions.CompositeSubscription;
import rx.subscriptions.Subscriptions;

public class OperationMulticast {
    public static <T, R> ConnectableObservable<R> multicast(Observable<? extends T> source, final Subject<? super T, ? extends R> subject) {
        return new MulticastConnectableObservable<T, R>(source, subject);
    }

    private static class MulticastConnectableObservable<T, R> extends ConnectableObservable<R> {
        private final Object lock = new Object();

        private final Observable<? extends T> source;
        private final Subject<? super T, ? extends R> subject;

        private Subscription subscription;

        public MulticastConnectableObservable(Observable<? extends T> source, final Subject<? super T, ? extends R> subject) {
            super(new OnSubscribe<R>() {
                @Override
                public void call(Subscriber<? super R> observer) {
                    subject.subscribe(observer);
                }
            });
            this.source = source;
            this.subject = subject;
        }

        public Subscription connect() {
            synchronized (lock) {
                if (subscription == null) {
                    subscription = source.subscribe(new Observer<T>() {
                        @Override
                        public void onCompleted() {
                            subject.onCompleted();
                        }

                        @Override
                        public void onError(Throwable e) {
                            subject.onError(e);
                        }

                        @Override
                        public void onNext(T args) {
                            subject.onNext(args);
                        }
                    });
                }
            }

            return Subscriptions.create(new Action0() {
                @Override
                public void call() {
                    synchronized (lock) {
                        if (subscription != null) {
                            subscription.unsubscribe();
                            subscription = null;
                        }
                    }
                }
            });
        }

    }

    /**
     * Returns an observable sequence that contains the elements of a sequence
     * produced by multicasting the source sequence within a selector function.
     * 
     * @param source
     * @param subjectFactory
     * @param selector
     * @return
     * 
     * @see <a href='http://msdn.microsoft.com/en-us/library/hh229708(v=vs.103).aspx'>MSDN: Observable.Multicast</a>
     */
    public static <TInput, TIntermediate, TResult> Observable<TResult> multicast(
            final Observable<? extends TInput> source,
            final Func0<? extends Subject<? super TInput, ? extends TIntermediate>> subjectFactory,
            final Func1<? super Observable<TIntermediate>, ? extends Observable<TResult>> selector) {
        return Observable.create(new MulticastSubscribeFunc<TInput, TIntermediate, TResult>(source, subjectFactory, selector));
    }

    /** The multicast subscription function. */
    private static final class MulticastSubscribeFunc<TInput, TIntermediate, TResult> implements OnSubscribeFunc<TResult> {
        final Observable<? extends TInput> source;
        final Func0<? extends Subject<? super TInput, ? extends TIntermediate>> subjectFactory;
        final Func1<? super Observable<TIntermediate>, ? extends Observable<TResult>> resultSelector;

        public MulticastSubscribeFunc(Observable<? extends TInput> source,
                Func0<? extends Subject<? super TInput, ? extends TIntermediate>> subjectFactory,
                Func1<? super Observable<TIntermediate>, ? extends Observable<TResult>> resultSelector) {
            this.source = source;
            this.subjectFactory = subjectFactory;
            this.resultSelector = resultSelector;
        }

        @Override
        public Subscription onSubscribe(Observer<? super TResult> t1) {
            Observable<TResult> observable;
            ConnectableObservable<TIntermediate> connectable;
            try {
                Subject<? super TInput, ? extends TIntermediate> subject = subjectFactory.call();

                connectable = new MulticastConnectableObservable<TInput, TIntermediate>(source, subject);

                observable = resultSelector.call(connectable);
            } catch (Throwable t) {
                t1.onError(t);
                return Subscriptions.empty();
            }

            CompositeSubscription csub = new CompositeSubscription();

            csub.add(observable.subscribe(new SafeObserver<TResult>(
                    new SafeObservableSubscription(csub), t1)));
            csub.add(connectable.connect());

            return csub;
        }
    }
}
