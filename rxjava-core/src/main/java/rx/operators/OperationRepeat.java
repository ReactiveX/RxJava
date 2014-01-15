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
import rx.Observable;
import rx.Observer;
import rx.Scheduler;
import rx.Subscription;
import rx.subscriptions.CompositeSubscription;
import rx.subscriptions.MultipleAssignmentSubscription;
import rx.util.functions.Action0;
import rx.util.functions.Action1;

public class OperationRepeat<T> implements IObservable<T> {

    private final IObservable<T> source;
    private final Scheduler scheduler;

    public static <T> IObservable<T> repeat(IObservable<T> source, Scheduler scheduler) {
        return new OperationRepeat<T>(source, scheduler);
    }

    private OperationRepeat(IObservable<T> source, Scheduler scheduler) {
        this.source = source;
        this.scheduler = scheduler;
    }

    @Override
    public Subscription subscribe(final Observer<? super T> observer) {
        final CompositeSubscription compositeSubscription = new CompositeSubscription();
        final MultipleAssignmentSubscription innerSubscription = new MultipleAssignmentSubscription();
        compositeSubscription.add(innerSubscription);
        compositeSubscription.add(scheduler.schedule(new Action1<Action0>() {
            @Override
            public void call(final Action0 self) {
                innerSubscription.set(source.subscribe(new Observer<T>() {

                    @Override
                    public void onCompleted() {
                        self.call();
                    }

                    @Override
                    public void onError(Throwable error) {
                        observer.onError(error);
                    }

                    @Override
                    public void onNext(T value) {
                        observer.onNext(value);
                    }
                }));
            }
        }));
        return compositeSubscription;
    }
}