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

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import rx.Observable;
import rx.Observable.OnSubscribeFunc;
import rx.Subscriber;
import rx.Subscription;
import rx.subscriptions.CompositeSubscription;

/**
 * Propagates the observable sequence that reacts first.
 */
public class OperationAmb {

    public static <T> OnSubscribeFunc<T> amb(Observable<? extends T> o1, Observable<? extends T> o2) {
        List<Observable<? extends T>> sources = new ArrayList<Observable<? extends T>>();
        sources.add(o1);
        sources.add(o2);
        return amb(sources);
    }

    public static <T> OnSubscribeFunc<T> amb(Observable<? extends T> o1, Observable<? extends T> o2, Observable<? extends T> o3) {
        List<Observable<? extends T>> sources = new ArrayList<Observable<? extends T>>();
        sources.add(o1);
        sources.add(o2);
        sources.add(o3);
        return amb(sources);
    }

    public static <T> OnSubscribeFunc<T> amb(Observable<? extends T> o1, Observable<? extends T> o2, Observable<? extends T> o3, Observable<? extends T> o4) {
        List<Observable<? extends T>> sources = new ArrayList<Observable<? extends T>>();
        sources.add(o1);
        sources.add(o2);
        sources.add(o3);
        sources.add(o4);
        return amb(sources);
    }

    public static <T> OnSubscribeFunc<T> amb(Observable<? extends T> o1, Observable<? extends T> o2, Observable<? extends T> o3, Observable<? extends T> o4, Observable<? extends T> o5) {
        List<Observable<? extends T>> sources = new ArrayList<Observable<? extends T>>();
        sources.add(o1);
        sources.add(o2);
        sources.add(o3);
        sources.add(o4);
        sources.add(o5);
        return amb(sources);
    }

    public static <T> OnSubscribeFunc<T> amb(Observable<? extends T> o1, Observable<? extends T> o2, Observable<? extends T> o3, Observable<? extends T> o4, Observable<? extends T> o5, Observable<? extends T> o6) {
        List<Observable<? extends T>> sources = new ArrayList<Observable<? extends T>>();
        sources.add(o1);
        sources.add(o2);
        sources.add(o3);
        sources.add(o4);
        sources.add(o5);
        sources.add(o6);
        return amb(sources);
    }

    public static <T> OnSubscribeFunc<T> amb(Observable<? extends T> o1, Observable<? extends T> o2, Observable<? extends T> o3, Observable<? extends T> o4, Observable<? extends T> o5, Observable<? extends T> o6, Observable<? extends T> o7) {
        List<Observable<? extends T>> sources = new ArrayList<Observable<? extends T>>();
        sources.add(o1);
        sources.add(o2);
        sources.add(o3);
        sources.add(o4);
        sources.add(o5);
        sources.add(o6);
        sources.add(o7);
        return amb(sources);
    }

    public static <T> OnSubscribeFunc<T> amb(Observable<? extends T> o1, Observable<? extends T> o2, Observable<? extends T> o3, Observable<? extends T> o4, Observable<? extends T> o5, Observable<? extends T> o6, Observable<? extends T> o7, Observable<? extends T> o8) {
        List<Observable<? extends T>> sources = new ArrayList<Observable<? extends T>>();
        sources.add(o1);
        sources.add(o2);
        sources.add(o3);
        sources.add(o4);
        sources.add(o5);
        sources.add(o6);
        sources.add(o7);
        sources.add(o8);
        return amb(sources);
    }

    public static <T> OnSubscribeFunc<T> amb(Observable<? extends T> o1, Observable<? extends T> o2, Observable<? extends T> o3, Observable<? extends T> o4, Observable<? extends T> o5, Observable<? extends T> o6, Observable<? extends T> o7, Observable<? extends T> o8, Observable<? extends T> o9) {
        List<Observable<? extends T>> sources = new ArrayList<Observable<? extends T>>();
        sources.add(o1);
        sources.add(o2);
        sources.add(o3);
        sources.add(o4);
        sources.add(o5);
        sources.add(o6);
        sources.add(o7);
        sources.add(o8);
        sources.add(o9);
        return amb(sources);
    }

    public static <T> OnSubscribeFunc<T> amb(
            final Iterable<? extends Observable<? extends T>> sources) {
        return new OnSubscribeFunc<T>() {

            @Override
            public Subscription onSubscribe(final Subscriber<? super T> observer) {
                AtomicInteger choice = new AtomicInteger(AmbObserver.NONE);
                int index = 0;
                CompositeSubscription parentSubscription = new CompositeSubscription();
                for (Observable<? extends T> source : sources) {
                    SafeObservableSubscription subscription = new SafeObservableSubscription();
                    AmbObserver<T> ambObserver = new AmbObserver<T>(
                            subscription, observer, index, choice);
                    parentSubscription.add(subscription.wrap(source
                            .subscribe(ambObserver)));
                    index++;
                }
                return parentSubscription;
            }
        };
    }

    private static class AmbObserver<T> extends Subscriber<T> {

        private static final int NONE = -1;

        private Subscription subscription;
        private Subscriber<? super T> observer;
        private int index;
        private AtomicInteger choice;

        private AmbObserver(Subscription subscription,
                Subscriber<? super T> observer, int index, AtomicInteger choice) {
            this.subscription = subscription;
            this.observer = observer;
            this.choice = choice;
            this.index = index;
        }

        @Override
        public void onNext(T args) {
            if (!isSelected()) {
                subscription.unsubscribe();
                return;
            }
            observer.onNext(args);
        }

        @Override
        public void onCompleted() {
            if (!isSelected()) {
                subscription.unsubscribe();
                return;
            }
            observer.onCompleted();
        }

        @Override
        public void onError(Throwable e) {
            if (!isSelected()) {
                subscription.unsubscribe();
                return;
            }
            observer.onError(e);
        }

        private boolean isSelected() {
            if (choice.get() == NONE) {
                return choice.compareAndSet(NONE, index);
            }
            return choice.get() == index;
        }
    }

}
