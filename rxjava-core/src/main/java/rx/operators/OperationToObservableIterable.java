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

import rx.IObservable;
import rx.Observer;
import rx.Scheduler;
import rx.Subscription;
import rx.schedulers.ImmediateScheduler;
import rx.schedulers.Schedulers;
import rx.subscriptions.Subscriptions;
import rx.util.functions.Action0;
import rx.util.functions.Action1;

import java.util.Iterator;

/**
 * Converts an Iterable sequence into an Observable.
 * <p>
 * <img width="640" src="https://github.com/Netflix/RxJava/wiki/images/rx-operators/toObservable.png">
 * <p>
 * You can convert any object that supports the Iterable interface into an Observable that emits
 * each item in the object, with the toObservable operation.
 */
public final class OperationToObservableIterable<T> {

    public static <T> IObservable<T> toObservableIterable(Iterable<? extends T> list, Scheduler scheduler) {
        if (scheduler instanceof ImmediateScheduler) {
            return new ToObservableIterable<T>(list);
        } else {
            return new ToObservableIterableScheduled<T>(list, scheduler);
        }
    }

    public static <T> IObservable<T> toObservableIterable(Iterable<? extends T> list) {
        return new ToObservableIterable<T>(list);
    }

    private static class ToObservableIterableScheduled<T> implements IObservable<T> {

        public ToObservableIterableScheduled(Iterable<? extends T> list, Scheduler scheduler) {
            this.iterable = list;
            this.scheduler = scheduler;
        }

        Scheduler scheduler;
        final Iterable<? extends T> iterable;

        public Subscription subscribe(final Observer<? super T> observer) {
            final Iterator<? extends T> iterator = iterable.iterator();
            return scheduler.schedule(new Action1<Action0>() {
                @Override
                public void call(Action0 self) {
                    try {
                        if (iterator.hasNext()) {
                            T x = iterator.next();
                            observer.onNext(x);
                            self.call();
                        } else {
                            observer.onCompleted();
                        }
                    } catch (Exception e) {
                        observer.onError(e);
                    }
                }
            });
        }
    }

    private static class ToObservableIterable<T> implements IObservable<T> {

        public ToObservableIterable(Iterable<? extends T> list) {
            this.iterable = list;
        }

        final Iterable<? extends T> iterable;

        public Subscription subscribe(final Observer<? super T> observer) {
            try {
                for (T t : iterable) {
                    observer.onNext(t);
                }
                observer.onCompleted();
            } catch (Exception e) {
                observer.onError(e);
            }
            return Subscriptions.empty();
        }
    }
}
