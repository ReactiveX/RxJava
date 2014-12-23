/**
 * Copyright 2014 Netflix, Inc.
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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicLongFieldUpdater;

import rx.Observable;
import rx.Producer;
import rx.Subscriber;
import rx.Subscription;
import rx.exceptions.CompositeException;
import rx.exceptions.Exceptions;
import rx.exceptions.MissingBackpressureException;
import rx.functions.Action0;
import rx.functions.Action1;
import rx.functions.Func1;
import rx.internal.util.RxRingBuffer;
import rx.observables.ConnectableObservable;
import rx.subscriptions.Subscriptions;

public class OperatorPublish<T> extends ConnectableObservable<T> {
    final Observable<? extends T> source;
    private final RequestHandler<T> requestHandler;

    public static <T> ConnectableObservable<T> create(Observable<? extends T> source) {
        return new OperatorPublish<T>(source);
    }

    public static <T, R> Observable<R> create(final Observable<? extends T> source, final Func1<? super Observable<T>, ? extends Observable<R>> selector) {
        return Observable.create(new OnSubscribe<R>() {

            @Override
            public void call(final Subscriber<? super R> child) {
                OperatorPublish<T> op = new OperatorPublish<T>(source);
                selector.call(op).unsafeSubscribe(child);
                op.connect(new Action1<Subscription>() {

                    @Override
                    public void call(Subscription sub) {
                        child.add(sub);
                    }

                });
            }

        });
    }

    private OperatorPublish(Observable<? extends T> source) {
        this(source, new Object(), new RequestHandler<T>());
    }

    private OperatorPublish(Observable<? extends T> source, final Object guard, final RequestHandler<T> requestHandler) {
        super(new OnSubscribe<T>() {
            @Override
            public void call(final Subscriber<? super T> subscriber) {
                subscriber.setProducer(new Producer() {

                    @Override
                    public void request(long n) {
                        requestHandler.requestFromChildSubscriber(subscriber, n);
                    }

                });
                subscriber.add(Subscriptions.create(new Action0() {

                    @Override
                    public void call() {
                        requestHandler.state.removeSubscriber(subscriber);
                    }

                }));
            }
        });
        this.source = source;
        this.requestHandler = requestHandler;
    }

    @Override
    public void connect(Action1<? super Subscription> connection) {
        // each time we connect we create a new Subscription
        boolean shouldSubscribe = false;
        
        // subscription is the state of whether we are connected or not
        OriginSubscriber<T> origin = requestHandler.state.getOrigin();
        if (origin == null) {
            shouldSubscribe = true;
            requestHandler.state.setOrigin(new OriginSubscriber<T>(requestHandler));
        }

        // in the lock above we determined we should subscribe, do it now outside the lock
        if (shouldSubscribe) {
            // register a subscription that will shut this down
            connection.call(Subscriptions.create(new Action0() {
                @Override
                public void call() {
                    OriginSubscriber<T> s = requestHandler.state.getOrigin();
                    requestHandler.state.setOrigin(null);
                    if (s != null) {
                        s.unsubscribe();
                    }
                }
            }));

            // now that everything is hooked up let's subscribe
            // as long as the subscription is not null (which can happen if already unsubscribed)
            OriginSubscriber<T> os = requestHandler.state.getOrigin();
            if (os != null) {
                source.unsafeSubscribe(os);
            }
        }
    }

    private static class OriginSubscriber<T> extends Subscriber<T> {

        private final RequestHandler<T> requestHandler;
        private final AtomicLong originOutstanding = new AtomicLong();
        private final long THRESHOLD = RxRingBuffer.SIZE / 4;
        private final RxRingBuffer buffer = RxRingBuffer.getSpmcInstance();

        OriginSubscriber(RequestHandler<T> requestHandler) {
            this.requestHandler = requestHandler;
            add(buffer);
        }

        @Override
        public void onStart() {
            requestMore(RxRingBuffer.SIZE);
        }

        private void requestMore(long r) {
            originOutstanding.addAndGet(r);
            request(r);
        }

        @Override
        public void onCompleted() {
            try {
                requestHandler.emit(requestHandler.notifier.completed());
            } catch (MissingBackpressureException e) {
                onError(e);
            }
        }

        @Override
        public void onError(Throwable e) {
            List<Throwable> errors = null;
            for (Subscriber<? super T> subscriber : requestHandler.state.getSubscribers()) {
                try {
                    subscriber.onError(e);
                } catch (Throwable e2) {
                    if (errors == null) {
                        errors = new ArrayList<Throwable>();
                    }
                    errors.add(e2);
                }
            }
            if (errors != null) {
                if (errors.size() == 1) {
                    Exceptions.propagate(errors.get(0));
                } else {
                    throw new CompositeException("Errors while emitting onError", errors);
                }
            }
        }

        @Override
        public void onNext(T t) {
            try {
                requestHandler.emit(requestHandler.notifier.next(t));
            } catch (MissingBackpressureException e) {
                onError(e);
            }
        }

    }

    /**
     * Synchronized mutable state.
     * 
     * benjchristensen => I have not figured out a non-blocking approach to this that doesn't involve massive object allocation overhead
     * with a complicated state machine so I'm sticking with mutex locks and just trying to make sure the work done while holding the
     * lock is small (such as never emitting data).
     * 
     * This does however mean we can't rely on a reference to State being consistent. For example, it can end up with a null OriginSubscriber. 
     * 
     * @param <T>
     */
    private static class State<T> {
        private long outstandingRequests = -1;
        private long emittedSinceRequest = 0;
        private OriginSubscriber<T> origin;
        // using AtomicLong to simplify mutating it, not for thread-safety since we're synchronizing access to this class
        // using LinkedHashMap so the order of Subscribers having onNext invoked is deterministic (same each time the code is run)
        private final Map<Subscriber<? super T>, AtomicLong> ss = new LinkedHashMap<Subscriber<? super T>, AtomicLong>();
        @SuppressWarnings("unchecked")
        private Subscriber<? super T>[] subscribers = new Subscriber[0];

        public synchronized OriginSubscriber<T> getOrigin() {
            return origin;
        }

        public synchronized void setOrigin(OriginSubscriber<T> o) {
            this.origin = o;
        }

        public synchronized boolean canEmitWithDecrement() {
            if (outstandingRequests > 0) {
                outstandingRequests--;
                emittedSinceRequest++;
                return true;
            }
            return false;
        }

        public synchronized void incrementOutstandingAfterFailedEmit() {
            outstandingRequests++;
            emittedSinceRequest--;
        }

        public synchronized Subscriber<? super T>[] getSubscribers() {
            return subscribers;
        }

        /**
         * @return long outstandingRequests
         */
        public synchronized long requestFromSubscriber(Subscriber<? super T> subscriber, Long request) {
            AtomicLong r = ss.get(subscriber);
            if (r == null) {
                ss.put(subscriber, new AtomicLong(request));
            } else {
                if (r.get() != Long.MAX_VALUE) {
                    if (request == Long.MAX_VALUE) {
                        r.set(Long.MAX_VALUE);
                    } else {
                        r.addAndGet(request.longValue());
                    }
                }
            }

            return resetAfterSubscriberUpdate();
        }

        public synchronized void removeSubscriber(Subscriber<? super T> subscriber) {
            ss.remove(subscriber);
            resetAfterSubscriberUpdate();
        }

        private long resetAfterSubscriberUpdate() {
            subscribers = new Subscriber[ss.size()];
            int i = 0;
            for (Subscriber<? super T> s : ss.keySet()) {
                subscribers[i++] = s;
            }

            long lowest = -1;
            for (AtomicLong l : ss.values()) {
                // decrement all we have emitted since last request
                long c = l.addAndGet(-emittedSinceRequest);
                if (lowest == -1 || c < lowest) {
                    lowest = c;
                }
            }
            /*
             * when receiving a request from a subscriber we reset 'outstanding' to the lowest of all subscribers
             */
            outstandingRequests = lowest;
            emittedSinceRequest = 0;
            return outstandingRequests;
        }
    }

    private static class RequestHandler<T> {
        private final NotificationLite<T> notifier = NotificationLite.instance();
        
        private final State<T> state = new State<T>();
        @SuppressWarnings("unused")
        volatile long wip;
        @SuppressWarnings("rawtypes")
        static final AtomicLongFieldUpdater<RequestHandler> WIP = AtomicLongFieldUpdater.newUpdater(RequestHandler.class, "wip");

        public void requestFromChildSubscriber(Subscriber<? super T> subscriber, Long request) {
            state.requestFromSubscriber(subscriber, request);
            OriginSubscriber<T> originSubscriber = state.getOrigin();
            if(originSubscriber != null) {
                drainQueue(originSubscriber);
            }
        }

        public void emit(Object t) throws MissingBackpressureException {
            OriginSubscriber<T> originSubscriber = state.getOrigin();
            if(originSubscriber == null) {
                // unsubscribed so break ... we are done
                return;
            }
            if (notifier.isCompleted(t)) {
                originSubscriber.buffer.onCompleted();
            } else {
                originSubscriber.buffer.onNext(notifier.getValue(t));
            }
            drainQueue(originSubscriber);
        }

        private void requestMoreAfterEmission(int emitted) {
            OriginSubscriber<T> origin = state.getOrigin();
            if (emitted > 0 && origin != null) {
                long r = origin.originOutstanding.addAndGet(-emitted);
                if (r <= origin.THRESHOLD) {
                    origin.requestMore(RxRingBuffer.SIZE - origin.THRESHOLD);
                }
            }
        }

        public void drainQueue(OriginSubscriber<T> originSubscriber) {
            if (WIP.getAndIncrement(this) == 0) {
                int emitted = 0;
                do {
                    /*
                     * Set to 1 otherwise it could have grown very large while in the last poll loop
                     * and then we can end up looping all those times again here before exiting even once we've drained
                     */
                    WIP.set(this, 1);
                    /**
                     * This is done in the most inefficient possible way right now and we can revisit the approach.
                     * If we want to batch this then we need to account for new subscribers arriving with a lower request count
                     * concurrently while iterating the batch ... or accept that they won't
                     */
                    while (true) {
                        boolean shouldEmit = state.canEmitWithDecrement();
                        if (!shouldEmit) {
                            break;
                        }
                        Object o = originSubscriber.buffer.poll();
                        if (o == null) {
                            // nothing in buffer so increment outstanding back again
                            state.incrementOutstandingAfterFailedEmit();
                            break;
                        }

                        if (notifier.isCompleted(o)) {
                            for (Subscriber<? super T> s : state.getSubscribers()) {
                                notifier.accept(s, o);
                            }

                        } else {
                            for (Subscriber<? super T> s : state.getSubscribers()) {
                                notifier.accept(s, o);
                            }
                        }
                        emitted++;
                    }
                } while (WIP.decrementAndGet(this) > 0);
                requestMoreAfterEmission(emitted);
            }
        }
    }
}
