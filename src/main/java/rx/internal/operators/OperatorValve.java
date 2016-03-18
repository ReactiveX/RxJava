/**
 * Copyright 2016 Netflix, Inc.
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

import rx.Observable;
import rx.Observable.Operator;
import rx.Producer;
import rx.Subscriber;

/**
 * An {@code Observable} that emits the first {@code num} items emitted by the source {@code Observable}.
 * <p>
 * <img width="640" height="305" src="https://raw.githubusercontent.com/wiki/ReactiveX/RxJava/images/rx-operators/take.png" alt="" />
 * <p>
 * You can choose to pay attention only to the first {@code num} items emitted by an {@code Observable} by using
 * the {@code take} operator. This operator returns an {@code Observable} that will invoke a subscriber's
 * {@link Subscriber#onNext onNext} function a maximum of {@code num} times before invoking
 * {@link Subscriber#onCompleted onCompleted}.
 */
public final class OperatorValve<T> implements Operator<T, T> {
    private final Observable<Boolean> onByDefault;
    private final long _granularity;

    public OperatorValve(Observable<Boolean> onByDefault, long granularity) {
        this.onByDefault = onByDefault;
        this._granularity = granularity;
    }

    @Override
    public Subscriber<? super T> call(final Subscriber<? super T> child) {
        return new Subscriber<T>(child) {
            private final long granularity = _granularity;
            private Producer p;
            private long backlog;// synchronized access on Producer p
            private long outstanding;// synchronized access on Producer p
            private boolean isOpen = true;// synchronized access on Producer p
            private AtomicBoolean terminated = new AtomicBoolean();

            @Override
            public void onCompleted() {
                if (terminated.compareAndSet(false, true))
                    child.onCompleted();
            }

            @Override
            public void onError(Throwable e) {
                if (terminated.compareAndSet(false, true))
                    child.onError(e);
            }

            @Override
            public void onNext(T t) {
                child.onNext(t);
                final long requestUp;
                synchronized (this) {
                    if (--outstanding == 0 && isOpen) {
                        // all out and still open; check to see if there is a backlog.
                        if (backlog > granularity) {
                            // don't request too much at once
                            requestUp = granularity;
                        } else if (backlog > 0) {
                            // the backlog isn't too big
                            requestUp = backlog;
                        } else {
                            // no backlog
                            requestUp = 0;
                        }
                    } else {
                        // expecting more or closed
                        requestUp = 0;
                    }
                    if (requestUp > 0) {
                        // do the last of the accounting inside the synchronized block
                        backlog -= requestUp;
                        outstanding += requestUp;
                    }
                }
                // do the request work outside the synchronized block
                if (requestUp != 0)
                    p.request(requestUp);
            }

            @Override
            public void setProducer(final Producer p) {
                this.p = p;

                onByDefault.unsafeSubscribe(new Subscriber<Boolean>() {
                    @Override
                    public void onCompleted() {
                        boolean _isOpen;
                        synchronized (this) {
                            // make sure to get the latest value of isOpen
                            _isOpen = isOpen;
                        }
                        if (!_isOpen) {
                            if (terminated.compareAndSet(false, true)) {
                                child.onError(new IllegalStateException("control signal terminated while valve was closed"));
                            }
                        }
                        unsubscribe();
                    }

                    @Override
                    public void onError(Throwable e) {
                        if (terminated.compareAndSet(false, true))
                            child.onError(e);
                        unsubscribe();
                    }

                    @Override
                    public void onNext(Boolean open) {
                        if (open) {
                            final long requestUp;
                            synchronized (this) {
                                if (!isOpen) {
                                    // opening, check backlog.
                                    if (backlog > granularity) {
                                        // don't request too much at once
                                        requestUp = granularity;
                                    } else if (backlog > 0) {
                                        // the backlog isn't too big
                                        requestUp = backlog;
                                    } else {
                                        // no backlog
                                        requestUp = 0;
                                    }
                                    isOpen = true;
                                } else {
                                    // was already open
                                    requestUp = 0;
                                }
                                if (requestUp > 0) {
                                    // do the last of the accounting inside the synchronized block
                                    backlog -= requestUp;
                                    outstanding += requestUp;
                                }
                            }
                            // do the request work outside the synchronized block
                            if (requestUp > 0)
                                p.request(requestUp);
                        } else {
                            synchronized (this) {
                                // closing
                                isOpen = false;
                            }
                        }
                    }
                });

                super.setProducer(new Producer() {
                    @Override
                    public void request(long n) {
                        if (n < 0)
                            throw new IllegalArgumentException("n >= 0 required but it was " + n);
                        final long requestUp;
                        synchronized (this) {
                            // increase backlog
                            backlog += n;
                            // now figure out if what is going to happen to it.
                            if (!isOpen) {
                                // closed; don't send
                                requestUp = 0;
                            } else {
                                if (backlog > granularity) {
                                    // don't request too much at once
                                    requestUp = granularity;
                                } else if (backlog > 0) {
                                    // the backlog isn't too big
                                    requestUp = backlog;
                                } else {
                                    // no backlog
                                    requestUp = 0;
                                }
                            }
                            if (requestUp > 0) {
                                // do the last of the accounting inside the synchronized block
                                backlog -= requestUp;
                                outstanding += requestUp;
                            }
                        }
                        // do the request work outside the synchronized block
                        if (requestUp != 0)
                            p.request(requestUp);
                    }
                });
            }
        };
    }
}
