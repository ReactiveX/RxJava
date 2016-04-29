/**
 * Copyright 2016 Netflix, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */

package rx.internal.operators;

import rx.Single;
import rx.SingleSubscriber;
import rx.exceptions.Exceptions;
import rx.functions.FuncN;
import rx.plugins.RxJavaPlugins;
import rx.subscriptions.CompositeSubscription;

import java.util.NoSuchElementException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

public class SingleOperatorZip {

    public static <T, R> Single<R> zip(final Single<? extends T>[] singles, final FuncN<? extends R> zipper) {
        return Single.create(new Single.OnSubscribe<R>() {
            @Override
            public void call(final SingleSubscriber<? super R> subscriber) {
                if (singles.length == 0) {
                    subscriber.onError(new NoSuchElementException("Can't zip 0 Singles."));
                    return;
                }

                final AtomicInteger wip = new AtomicInteger(singles.length);
                final AtomicBoolean once = new AtomicBoolean();
                final Object[] values = new Object[singles.length];

                CompositeSubscription compositeSubscription = new CompositeSubscription();
                subscriber.add(compositeSubscription);

                for (int i = 0; i < singles.length; i++) {
                    if (compositeSubscription.isUnsubscribed() || once.get()) {
                        break;
                    }

                    final int j = i;
                    SingleSubscriber<T> singleSubscriber = new SingleSubscriber<T>() {
                        @Override
                        public void onSuccess(T value) {
                            values[j] = value;
                            if (wip.decrementAndGet() == 0) {
                                R r;

                                try {
                                    r = zipper.call(values);
                                } catch (Throwable e) {
                                    Exceptions.throwIfFatal(e);
                                    onError(e);
                                    return;
                                }

                                subscriber.onSuccess(r);
                            }
                        }

                        @Override
                        public void onError(Throwable error) {
                            if (once.compareAndSet(false, true)) {
                                subscriber.onError(error);
                            } else {
                                RxJavaPlugins.getInstance().getErrorHandler().handleError(error);
                            }
                        }
                    };

                    compositeSubscription.add(singleSubscriber);

                    if (compositeSubscription.isUnsubscribed() || once.get()) {
                        break;
                    }

                    singles[i].subscribe(singleSubscriber);
                }
            }
        });
    }
}
