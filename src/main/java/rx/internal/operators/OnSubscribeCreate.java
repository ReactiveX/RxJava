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

import java.util.Queue;
import java.util.concurrent.atomic.*;

import rx.*;
import rx.Observable.OnSubscribe;
import rx.exceptions.MissingBackpressureException;
import rx.functions.*;
import rx.internal.subscriptions.CancellableSubscription;
import rx.internal.util.RxRingBuffer;
import rx.internal.util.atomic.SpscUnboundedAtomicArrayQueue;
import rx.internal.util.unsafe.*;
import rx.plugins.RxJavaHooks;
import rx.subscriptions.SerialSubscription;

public final class OnSubscribeCreate<T> implements OnSubscribe<T> {

    final Action1<Emitter<T>> Emitter;

    final Emitter.BackpressureMode backpressure;

    public OnSubscribeCreate(Action1<Emitter<T>> Emitter, Emitter.BackpressureMode backpressure) {
        this.Emitter = Emitter;
        this.backpressure = backpressure;
    }

    @Override
    public void call(Subscriber<? super T> t) {
        BaseEmitter<T> emitter;

        switch (backpressure) {
        case NONE: {
            emitter = new NoneEmitter<T>(t);
            break;
        }
        case ERROR: {
            emitter = new ErrorEmitter<T>(t);
            break;
        }
        case DROP: {
            emitter = new DropEmitter<T>(t);
            break;
        }
        case LATEST: {
            emitter = new LatestEmitter<T>(t);
            break;
        }
        default: {
            emitter = new BufferEmitter<T>(t, RxRingBuffer.SIZE);
            break;
        }
        }

        t.add(emitter);
        t.setProducer(emitter);
        Emitter.call(emitter);

    }

    static abstract class BaseEmitter<T>
    extends AtomicLong
    implements Emitter<T>, Producer, Subscription {
        /** */
        private static final long serialVersionUID = 7326289992464377023L;

        final Subscriber<? super T> actual;

        final SerialSubscription serial;

        public BaseEmitter(Subscriber<? super T> actual) {
            this.actual = actual;
            this.serial = new SerialSubscription();
        }

        @Override
        public void onCompleted() {
            if (actual.isUnsubscribed()) {
                return;
            }
            try {
                actual.onCompleted();
            } finally {
                serial.unsubscribe();
            }
        }

        @Override
        public void onError(Throwable e) {
            if (actual.isUnsubscribed()) {
                return;
            }
            try {
                actual.onError(e);
            } finally {
                serial.unsubscribe();
            }
        }

        @Override
        public final void unsubscribe() {
            serial.unsubscribe();
            onUnsubscribed();
        }

        void onUnsubscribed() {
            // default is no-op
        }

        @Override
        public final boolean isUnsubscribed() {
            return serial.isUnsubscribed();
        }

        @Override
        public final void request(long n) {
            if (BackpressureUtils.validate(n)) {
                BackpressureUtils.getAndAddRequest(this, n);
                onRequested();
            }
        }

        void onRequested() {
            // default is no-op
        }

        @Override
        public final void setSubscription(Subscription s) {
            serial.set(s);
        }

        @Override
        public final void setCancellation(Cancellable c) {
            setSubscription(new CancellableSubscription(c));
        }

        @Override
        public final long requested() {
            return get();
        }
    }

    static final class NoneEmitter<T> extends BaseEmitter<T> {

        /** */
        private static final long serialVersionUID = 3776720187248809713L;

        public NoneEmitter(Subscriber<? super T> actual) {
            super(actual);
        }

        @Override
        public void onNext(T t) {
            if (actual.isUnsubscribed()) {
                return;
            }

            actual.onNext(t);

            for (;;) {
                long r = get();
                if (r == 0L || compareAndSet(r, r - 1)) {
                    return;
                }
            }
        }

    }

    static abstract class NoOverflowBaseEmitter<T> extends BaseEmitter<T> {

        /** */
        private static final long serialVersionUID = 4127754106204442833L;

        public NoOverflowBaseEmitter(Subscriber<? super T> actual) {
            super(actual);
        }

        @Override
        public void onNext(T t) {
            if (actual.isUnsubscribed()) {
                return;
            }

            if (get() != 0) {
                actual.onNext(t);
                BackpressureUtils.produced(this, 1);
            } else {
                onOverflow();
            }
        }

        abstract void onOverflow();
    }

    static final class DropEmitter<T> extends NoOverflowBaseEmitter<T> {

        /** */
        private static final long serialVersionUID = 8360058422307496563L;

        public DropEmitter(Subscriber<? super T> actual) {
            super(actual);
        }

        @Override
        void onOverflow() {
            // nothing to do
        }

    }

    static final class ErrorEmitter<T> extends NoOverflowBaseEmitter<T> {

        /** */
        private static final long serialVersionUID = 338953216916120960L;

        private boolean done;

        public ErrorEmitter(Subscriber<? super T> actual) {
            super(actual);
        }


        @Override
        public void onNext(T t) {
            if (done) {
                return;
            }
            super.onNext(t);
        }


        @Override
        public void onCompleted() {
            if (done) {
                return;
            }
            done = true;
            super.onCompleted();
        }


        @Override
        public void onError(Throwable e) {
            if (done) {
                RxJavaHooks.onError(e);
                return;
            }
            done = true;
            super.onError(e);
        }


        @Override
        void onOverflow() {
            onError(new MissingBackpressureException("create: could not emit value due to lack of requests"));
        }

    }

    static final class BufferEmitter<T> extends BaseEmitter<T> {

        /** */
        private static final long serialVersionUID = 2427151001689639875L;

        final Queue<Object> queue;

        Throwable error;
        volatile boolean done;

        final AtomicInteger wip;

        public BufferEmitter(Subscriber<? super T> actual, int capacityHint) {
            super(actual);
            this.queue = UnsafeAccess.isUnsafeAvailable()
                    ? new SpscUnboundedArrayQueue<Object>(capacityHint)
                    : new SpscUnboundedAtomicArrayQueue<Object>(capacityHint);
            this.wip = new AtomicInteger();
        }

        @Override
        public void onNext(T t) {
            queue.offer(NotificationLite.next(t));
            drain();
        }

        @Override
        public void onError(Throwable e) {
            error = e;
            done = true;
            drain();
        }

        @Override
        public void onCompleted() {
            done = true;
            drain();
        }

        @Override
        void onRequested() {
            drain();
        }

        @Override
        void onUnsubscribed() {
            if (wip.getAndIncrement() == 0) {
                queue.clear();
            }
        }

        void drain() {
            if (wip.getAndIncrement() != 0) {
                return;
            }

            int missed = 1;
            final Subscriber<? super T> a = actual;
            final Queue<Object> q = queue;

            for (;;) {
                long r = get();
                long e = 0L;

                while (e != r) {
                    if (a.isUnsubscribed()) {
                        q.clear();
                        return;
                    }

                    boolean d = done;

                    Object o = q.poll();

                    boolean empty = o == null;

                    if (d && empty) {
                        Throwable ex = error;
                        if (ex != null) {
                            super.onError(ex);
                        } else {
                            super.onCompleted();
                        }
                        return;
                    }

                    if (empty) {
                        break;
                    }

                    a.onNext(NotificationLite.<T>getValue(o));

                    e++;
                }

                if (e == r) {
                    if (a.isUnsubscribed()) {
                        q.clear();
                        return;
                    }

                    boolean d = done;

                    boolean empty = q.isEmpty();

                    if (d && empty) {
                        Throwable ex = error;
                        if (ex != null) {
                            super.onError(ex);
                        } else {
                            super.onCompleted();
                        }
                        return;
                    }
                }

                if (e != 0) {
                    BackpressureUtils.produced(this, e);
                }

                missed = wip.addAndGet(-missed);
                if (missed == 0) {
                    break;
                }
            }
        }
    }

    static final class LatestEmitter<T> extends BaseEmitter<T> {

        /** */
        private static final long serialVersionUID = 4023437720691792495L;

        final AtomicReference<Object> queue;

        Throwable error;
        volatile boolean done;

        final AtomicInteger wip;

        public LatestEmitter(Subscriber<? super T> actual) {
            super(actual);
            this.queue = new AtomicReference<Object>();
            this.wip = new AtomicInteger();
        }

        @Override
        public void onNext(T t) {
            queue.set(NotificationLite.next(t));
            drain();
        }

        @Override
        public void onError(Throwable e) {
            error = e;
            done = true;
            drain();
        }

        @Override
        public void onCompleted() {
            done = true;
            drain();
        }

        @Override
        void onRequested() {
            drain();
        }

        @Override
        void onUnsubscribed() {
            if (wip.getAndIncrement() == 0) {
                queue.lazySet(null);
            }
        }

        void drain() {
            if (wip.getAndIncrement() != 0) {
                return;
            }

            int missed = 1;
            final Subscriber<? super T> a = actual;
            final AtomicReference<Object> q = queue;

            for (;;) {
                long r = get();
                long e = 0L;

                while (e != r) {
                    if (a.isUnsubscribed()) {
                        q.lazySet(null);
                        return;
                    }

                    boolean d = done;

                    Object o = q.getAndSet(null);

                    boolean empty = o == null;

                    if (d && empty) {
                        Throwable ex = error;
                        if (ex != null) {
                            super.onError(ex);
                        } else {
                            super.onCompleted();
                        }
                        return;
                    }

                    if (empty) {
                        break;
                    }

                    a.onNext(NotificationLite.<T>getValue(o));

                    e++;
                }

                if (e == r) {
                    if (a.isUnsubscribed()) {
                        q.lazySet(null);
                        return;
                    }

                    boolean d = done;

                    boolean empty = q.get() == null;

                    if (d && empty) {
                        Throwable ex = error;
                        if (ex != null) {
                            super.onError(ex);
                        } else {
                            super.onCompleted();
                        }
                        return;
                    }
                }

                if (e != 0) {
                    BackpressureUtils.produced(this, e);
                }

                missed = wip.addAndGet(-missed);
                if (missed == 0) {
                    break;
                }
            }
        }
    }

}
