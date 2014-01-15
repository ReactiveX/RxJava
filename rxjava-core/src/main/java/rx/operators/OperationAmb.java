/**
 * Copyright 2013 Netflix, Inc.
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

import rx.IObservable;
import rx.IObservable;
import rx.Observer;
import rx.Subscription;
import rx.subscriptions.CompositeSubscription;

/**
 * Propagates the observable sequence that reacts first.
 */
public class OperationAmb {

    public static <T> IObservable<T> amb(IObservable<? extends T> o1, IObservable<? extends T> o2) {
        List<IObservable<? extends T>> sources = new ArrayList<IObservable<? extends T>>();
        sources.add(o1);
        sources.add(o2);
        return amb(sources);
    }

    public static <T> IObservable<T> amb(IObservable<? extends T> o1, IObservable<? extends T> o2, IObservable<? extends T> o3) {
        List<IObservable<? extends T>> sources = new ArrayList<IObservable<? extends T>>();
        sources.add(o1);
        sources.add(o2);
        sources.add(o3);
        return amb(sources);
    }

    public static <T> IObservable<T> amb(IObservable<? extends T> o1, IObservable<? extends T> o2, IObservable<? extends T> o3, IObservable<? extends T> o4) {
        List<IObservable<? extends T>> sources = new ArrayList<IObservable<? extends T>>();
        sources.add(o1);
        sources.add(o2);
        sources.add(o3);
        sources.add(o4);
        return amb(sources);
    }

    public static <T> IObservable<T> amb(IObservable<? extends T> o1, IObservable<? extends T> o2, IObservable<? extends T> o3, IObservable<? extends T> o4, IObservable<? extends T> o5) {
        List<IObservable<? extends T>> sources = new ArrayList<IObservable<? extends T>>();
        sources.add(o1);
        sources.add(o2);
        sources.add(o3);
        sources.add(o4);
        sources.add(o5);
        return amb(sources);
    }

    public static <T> IObservable<T> amb(IObservable<? extends T> o1, IObservable<? extends T> o2, IObservable<? extends T> o3, IObservable<? extends T> o4, IObservable<? extends T> o5, IObservable<? extends T> o6) {
        List<IObservable<? extends T>> sources = new ArrayList<IObservable<? extends T>>();
        sources.add(o1);
        sources.add(o2);
        sources.add(o3);
        sources.add(o4);
        sources.add(o5);
        sources.add(o6);
        return amb(sources);
    }

    public static <T> IObservable<T> amb(IObservable<? extends T> o1, IObservable<? extends T> o2, IObservable<? extends T> o3, IObservable<? extends T> o4, IObservable<? extends T> o5, IObservable<? extends T> o6, IObservable<? extends T> o7) {
        List<IObservable<? extends T>> sources = new ArrayList<IObservable<? extends T>>();
        sources.add(o1);
        sources.add(o2);
        sources.add(o3);
        sources.add(o4);
        sources.add(o5);
        sources.add(o6);
        sources.add(o7);
        return amb(sources);
    }

    public static <T> IObservable<T> amb(IObservable<? extends T> o1, IObservable<? extends T> o2, IObservable<? extends T> o3, IObservable<? extends T> o4, IObservable<? extends T> o5, IObservable<? extends T> o6, IObservable<? extends T> o7, IObservable<? extends T> o8) {
        List<IObservable<? extends T>> sources = new ArrayList<IObservable<? extends T>>();
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

    public static <T> IObservable<T> amb(IObservable<? extends T> o1, IObservable<? extends T> o2, IObservable<? extends T> o3, IObservable<? extends T> o4, IObservable<? extends T> o5, IObservable<? extends T> o6, IObservable<? extends T> o7, IObservable<? extends T> o8, IObservable<? extends T> o9) {
        List<IObservable<? extends T>> sources = new ArrayList<IObservable<? extends T>>();
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

    public static <T> IObservable<T> amb(
            final Iterable<? extends IObservable<? extends T>> sources) {
        return new IObservable<T>() {

            @Override
            public Subscription subscribe(final Observer<? super T> observer) {
                AtomicInteger choice = new AtomicInteger(AmbObserver.NONE);
                int index = 0;
                CompositeSubscription parentSubscription = new CompositeSubscription();
                for (IObservable<? extends T> source : sources) {
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

    private static class AmbObserver<T> implements Observer<T> {

        private static final int NONE = -1;

        private Subscription subscription;
        private Observer<? super T> observer;
        private int index;
        private AtomicInteger choice;

        private AmbObserver(Subscription subscription,
                Observer<? super T> observer, int index, AtomicInteger choice) {
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
