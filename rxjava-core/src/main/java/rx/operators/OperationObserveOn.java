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

import rx.Observable;
import rx.Observer;
import rx.Scheduler;
import rx.Subscription;
import rx.util.functions.Action0;
import rx.util.functions.Func0;
import rx.util.functions.Func1;

public class OperationObserveOn {

    public static <T> Func1<Observer<T>, Subscription> observeOn(Observable<T> source, Scheduler scheduler) {
        return new ObserveOn<T>(source, scheduler);
    }

    private static class ObserveOn<T> implements Func1<Observer<T>, Subscription> {
        private final Observable<T> source;
        private final Scheduler scheduler;

        public ObserveOn(Observable<T> source, Scheduler scheduler) {
            this.source = source;
            this.scheduler = scheduler;
        }

        @Override
        public Subscription call(final Observer<T> observer) {
            return source.subscribe(new ScheduledObserver<T>(observer, scheduler));
        }
    }

    private static class ScheduledObserver<T> implements Observer<T> {
        private final Observer<T> underlying;
        private final Scheduler scheduler;

        public ScheduledObserver(Observer<T> underlying, Scheduler scheduler) {
            this.underlying = underlying;
            this.scheduler = scheduler;
        }

        @Override
        public void onCompleted() {
            scheduler.schedule(new Action0() {
                @Override
                public void call() {
                    underlying.onCompleted();
                }
            });
        }

        @Override
        public void onError(Exception e) {
            scheduler.schedule(new Action0() {
                @Override
                public void call() {
                    underlying.onCompleted();
                }
            });
        }

        @Override
        public void onNext(T args) {
            scheduler.schedule(new Action0() {
                @Override
                public void call() {
                    underlying.onCompleted();
                }
            });
        }
    }
}
