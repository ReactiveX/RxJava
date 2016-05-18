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
package rx.internal.operators;

import java.util.concurrent.atomic.AtomicLong;

import rx.Notification;
import rx.Observable.Operator;
import rx.Producer;
import rx.Subscriber;
import rx.plugins.RxJavaPlugins;

/**
 * Turns all of the notifications from an Observable into {@code onNext} emissions, and marks
 * them with their original notification types within {@link Notification} objects.
 * <p>
 * <img width="640" src="https://github.com/ReactiveX/RxJava/wiki/images/rx-operators/materialize.png" alt="">
 * <p>
 * See <a href="http://msdn.microsoft.com/en-us/library/hh229453.aspx">here</a> for the Microsoft Rx equivalent.
 * @param <T> the value type
 */
public final class OperatorMaterialize<T> implements Operator<Notification<T>, T> {

    /** Lazy initialization via inner-class holder. */
    private static final class Holder {
        /** A singleton instance. */
        static final OperatorMaterialize<Object> INSTANCE = new OperatorMaterialize<Object>();
    }

    /**
     * @param <T> the value type
     * @return a singleton instance of this stateless operator.
     */
    @SuppressWarnings("unchecked")
    public static <T> OperatorMaterialize<T> instance() {
        return (OperatorMaterialize<T>) Holder.INSTANCE;
    }

    OperatorMaterialize() {
    }

    @Override
    public Subscriber<? super T> call(final Subscriber<? super Notification<T>> child) {
        final ParentSubscriber<T> parent = new ParentSubscriber<T>(child);
        child.add(parent);
        child.setProducer(new Producer() {
            @Override
            public void request(long n) {
                if (n > 0) {
                    parent.requestMore(n);
                }
            }
        });
        return parent;
    }

    private static class ParentSubscriber<T> extends Subscriber<T> {

        private final Subscriber<? super Notification<T>> child;

        private volatile Notification<T> terminalNotification;
        
        // guarded by this
        private boolean busy = false;
        // guarded by this
        private boolean missed = false;

        private final AtomicLong requested = new AtomicLong();

        ParentSubscriber(Subscriber<? super Notification<T>> child) {
            this.child = child;
        }

        @Override
        public void onStart() {
            request(0);
        }

        void requestMore(long n) {
            BackpressureUtils.getAndAddRequest(requested, n);
            request(n);
            drain();
        }

        @Override
        public void onCompleted() {
            terminalNotification = Notification.createOnCompleted();
            drain();
        }

        @Override
        public void onError(Throwable e) {
            terminalNotification = Notification.createOnError(e);
            RxJavaPlugins.getInstance().getErrorHandler().handleError(e);
            drain();
        }

        @Override
        public void onNext(T t) {
            child.onNext(Notification.createOnNext(t));
            decrementRequested();
        }

        private void decrementRequested() {
            // atomically decrement requested
            AtomicLong localRequested = this.requested;
            while (true) {
                long r = localRequested.get();
                if (r == Long.MAX_VALUE) {
                    // don't decrement if unlimited requested
                    return;
                } else if (localRequested.compareAndSet(r, r - 1)) {
                    return;
                }
            }
        }

        private void drain() {
            synchronized (this) {
                if (busy) {
                    // set flag to force extra loop if drain loop running
                    missed = true;
                    return;
                } 
            }
            // drain loop
            final AtomicLong localRequested = this.requested;
            while (!child.isUnsubscribed()) {
                Notification<T> tn;
                tn = terminalNotification;
                if (tn != null) {
                    if (localRequested.get() > 0) {
                        // allow tn to be GC'd after the onNext call
                        terminalNotification = null;
                        // emit the terminal notification
                        child.onNext(tn);
                        if (!child.isUnsubscribed()) {
                            child.onCompleted();
                        }
                        // note that we leave busy=true here
                        // which will prevent further drains
                        return;
                    }
                }
                // continue looping if drain() was called while in
                // this loop
                synchronized (this) {
                    if (!missed) {
                        busy = false;
                        return;
                    }
                }
            }
        }
    }
}
