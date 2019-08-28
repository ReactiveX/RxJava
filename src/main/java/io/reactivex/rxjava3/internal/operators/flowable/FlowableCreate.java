/**
 * Copyright (c) 2016-present, RxJava Contributors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in
 * compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is
 * distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See
 * the License for the specific language governing permissions and limitations under the License.
 */

package io.reactivex.rxjava3.internal.operators.flowable;

import java.util.concurrent.atomic.*;

import org.reactivestreams.*;

import io.reactivex.rxjava3.core.*;
import io.reactivex.rxjava3.disposables.Disposable;
import io.reactivex.rxjava3.exceptions.*;
import io.reactivex.rxjava3.functions.Cancellable;
import io.reactivex.rxjava3.internal.disposables.*;
import io.reactivex.rxjava3.internal.fuseable.SimplePlainQueue;
import io.reactivex.rxjava3.internal.queue.SpscLinkedArrayQueue;
import io.reactivex.rxjava3.internal.subscriptions.SubscriptionHelper;
import io.reactivex.rxjava3.internal.util.*;
import io.reactivex.rxjava3.plugins.RxJavaPlugins;

public final class FlowableCreate<T> extends Flowable<T> {

    final FlowableOnSubscribe<T> source;

    final BackpressureStrategy backpressure;

    public FlowableCreate(FlowableOnSubscribe<T> source, BackpressureStrategy backpressure) {
        this.source = source;
        this.backpressure = backpressure;
    }

    @Override
    public void subscribeActual(Subscriber<? super T> t) {
        BaseEmitter<T> emitter;

        switch (backpressure) {
        case MISSING: {
            emitter = new MissingEmitter<T>(t);
            break;
        }
        case ERROR: {
            emitter = new ErrorAsyncEmitter<T>(t);
            break;
        }
        case DROP: {
            emitter = new DropAsyncEmitter<T>(t);
            break;
        }
        case LATEST: {
            emitter = new LatestAsyncEmitter<T>(t);
            break;
        }
        default: {
            emitter = new BufferAsyncEmitter<T>(t, bufferSize());
            break;
        }
        }

        t.onSubscribe(emitter);
        try {
            source.subscribe(emitter);
        } catch (Throwable ex) {
            Exceptions.throwIfFatal(ex);
            emitter.onError(ex);
        }
    }

    /**
     * Serializes calls to onNext, onError and onComplete.
     *
     * @param <T> the value type
     */
    static final class SerializedEmitter<T>
    extends AtomicInteger
    implements FlowableEmitter<T> {

        private static final long serialVersionUID = 4883307006032401862L;

        final BaseEmitter<T> emitter;

        final AtomicThrowable errors;

        final SimplePlainQueue<T> queue;

        volatile boolean done;

        SerializedEmitter(BaseEmitter<T> emitter) {
            this.emitter = emitter;
            this.errors = new AtomicThrowable();
            this.queue = new SpscLinkedArrayQueue<T>(16);
        }

        @Override
        public void onNext(T t) {
            if (emitter.isCancelled() || done) {
                return;
            }
            if (t == null) {
                onError(ExceptionHelper.createNullPointerException("onNext called with a null value."));
                return;
            }
            if (get() == 0 && compareAndSet(0, 1)) {
                emitter.onNext(t);
                if (decrementAndGet() == 0) {
                    return;
                }
            } else {
                SimplePlainQueue<T> q = queue;
                synchronized (q) {
                    q.offer(t);
                }
                if (getAndIncrement() != 0) {
                    return;
                }
            }
            drainLoop();
        }

        @Override
        public void onError(Throwable t) {
            if (!tryOnError(t)) {
                RxJavaPlugins.onError(t);
            }
        }

        @Override
        public boolean tryOnError(Throwable t) {
           if (emitter.isCancelled() || done) {
               return false;
           }
           if (t == null) {
               t = ExceptionHelper.createNullPointerException("onError called with a null Throwable.");
           }
           if (errors.tryAddThrowable(t)) {
               done = true;
               drain();
               return true;
           }
           return false;
        }

        @Override
        public void onComplete() {
            if (emitter.isCancelled() || done) {
                return;
            }
            done = true;
            drain();
        }

        void drain() {
            if (getAndIncrement() == 0) {
                drainLoop();
            }
        }

        void drainLoop() {
            BaseEmitter<T> e = emitter;
            SimplePlainQueue<T> q = queue;
            AtomicThrowable errors = this.errors;
            int missed = 1;
            for (;;) {

                for (;;) {
                    if (e.isCancelled()) {
                        q.clear();
                        return;
                    }

                    if (errors.get() != null) {
                        q.clear();
                        errors.tryTerminateConsumer(e);
                        return;
                    }

                    boolean d = done;

                    T v = q.poll();

                    boolean empty = v == null;

                    if (d && empty) {
                        e.onComplete();
                        return;
                    }

                    if (empty) {
                        break;
                    }

                    e.onNext(v);
                }

                missed = addAndGet(-missed);
                if (missed == 0) {
                    break;
                }
            }
        }

        @Override
        public void setDisposable(Disposable d) {
            emitter.setDisposable(d);
        }

        @Override
        public void setCancellable(Cancellable c) {
            emitter.setCancellable(c);
        }

        @Override
        public long requested() {
            return emitter.requested();
        }

        @Override
        public boolean isCancelled() {
            return emitter.isCancelled();
        }

        @Override
        public FlowableEmitter<T> serialize() {
            return this;
        }

        @Override
        public String toString() {
            return emitter.toString();
        }
    }

    abstract static class BaseEmitter<T>
    extends AtomicLong
    implements FlowableEmitter<T>, Subscription {
        private static final long serialVersionUID = 7326289992464377023L;

        final Subscriber<? super T> downstream;

        final SequentialDisposable serial;

        BaseEmitter(Subscriber<? super T> downstream) {
            this.downstream = downstream;
            this.serial = new SequentialDisposable();
        }

        @Override
        public void onComplete() {
            completeDownstream();
        }

        protected void completeDownstream() {
            if (isCancelled()) {
                return;
            }
            try {
                downstream.onComplete();
            } finally {
                serial.dispose();
            }
        }

        @Override
        public final void onError(Throwable e) {
            if (e == null) {
                e = ExceptionHelper.createNullPointerException("onError called with a null Throwable.");
            }
            if (!signalError(e)) {
                RxJavaPlugins.onError(e);
            }
        }

        @Override
        public final boolean tryOnError(Throwable e) {
            if (e == null) {
                e = ExceptionHelper.createNullPointerException("tryOnError called with a null Throwable.");
            }
            return signalError(e);
        }

        public boolean signalError(Throwable e) {
            return errorDownstream(e);
        }

        protected boolean errorDownstream(Throwable e) {
            if (isCancelled()) {
                return false;
            }
            try {
                downstream.onError(e);
            } finally {
                serial.dispose();
            }
            return true;
        }

        @Override
        public final void cancel() {
            serial.dispose();
            onUnsubscribed();
        }

        void onUnsubscribed() {
            // default is no-op
        }

        @Override
        public final boolean isCancelled() {
            return serial.isDisposed();
        }

        @Override
        public final void request(long n) {
            if (SubscriptionHelper.validate(n)) {
                BackpressureHelper.add(this, n);
                onRequested();
            }
        }

        void onRequested() {
            // default is no-op
        }

        @Override
        public final void setDisposable(Disposable d) {
            serial.update(d);
        }

        @Override
        public final void setCancellable(Cancellable c) {
            setDisposable(new CancellableDisposable(c));
        }

        @Override
        public final long requested() {
            return get();
        }

        @Override
        public final FlowableEmitter<T> serialize() {
            return new SerializedEmitter<T>(this);
        }

        @Override
        public String toString() {
            return String.format("%s{%s}", getClass().getSimpleName(), super.toString());
        }
    }

    static final class MissingEmitter<T> extends BaseEmitter<T> {

        private static final long serialVersionUID = 3776720187248809713L;

        MissingEmitter(Subscriber<? super T> downstream) {
            super(downstream);
        }

        @Override
        public void onNext(T t) {
            if (isCancelled()) {
                return;
            }

            if (t != null) {
                downstream.onNext(t);
            } else {
                onError(ExceptionHelper.createNullPointerException("onNext called with a null value."));
                return;
            }

            for (;;) {
                long r = get();
                if (r == 0L || compareAndSet(r, r - 1)) {
                    return;
                }
            }
        }

    }

    abstract static class NoOverflowBaseAsyncEmitter<T> extends BaseEmitter<T> {

        private static final long serialVersionUID = 4127754106204442833L;

        NoOverflowBaseAsyncEmitter(Subscriber<? super T> downstream) {
            super(downstream);
        }

        @Override
        public final void onNext(T t) {
            if (isCancelled()) {
                return;
            }

            if (t == null) {
                onError(ExceptionHelper.createNullPointerException("onNext called with a null value."));
                return;
            }

            if (get() != 0) {
                downstream.onNext(t);
                BackpressureHelper.produced(this, 1);
            } else {
                onOverflow();
            }
        }

        abstract void onOverflow();
    }

    static final class DropAsyncEmitter<T> extends NoOverflowBaseAsyncEmitter<T> {

        private static final long serialVersionUID = 8360058422307496563L;

        DropAsyncEmitter(Subscriber<? super T> downstream) {
            super(downstream);
        }

        @Override
        void onOverflow() {
            // nothing to do
        }

    }

    static final class ErrorAsyncEmitter<T> extends NoOverflowBaseAsyncEmitter<T> {

        private static final long serialVersionUID = 338953216916120960L;

        ErrorAsyncEmitter(Subscriber<? super T> downstream) {
            super(downstream);
        }

        @Override
        void onOverflow() {
            onError(new MissingBackpressureException("create: could not emit value due to lack of requests"));
        }

    }

    static final class BufferAsyncEmitter<T> extends BaseEmitter<T> {

        private static final long serialVersionUID = 2427151001689639875L;

        final SpscLinkedArrayQueue<T> queue;

        Throwable error;
        volatile boolean done;

        final AtomicInteger wip;

        BufferAsyncEmitter(Subscriber<? super T> actual, int capacityHint) {
            super(actual);
            this.queue = new SpscLinkedArrayQueue<T>(capacityHint);
            this.wip = new AtomicInteger();
        }

        @Override
        public void onNext(T t) {
            if (done || isCancelled()) {
                return;
            }

            if (t == null) {
                onError(ExceptionHelper.createNullPointerException("onNext called with a null value."));
                return;
            }
            queue.offer(t);
            drain();
        }

        @Override
        public boolean signalError(Throwable e) {
            if (done || isCancelled()) {
                return false;
            }

            error = e;
            done = true;
            drain();
            return true;
        }

        @Override
        public void onComplete() {
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
            final Subscriber<? super T> a = downstream;
            final SpscLinkedArrayQueue<T> q = queue;

            for (;;) {
                long r = get();
                long e = 0L;

                while (e != r) {
                    if (isCancelled()) {
                        q.clear();
                        return;
                    }

                    boolean d = done;

                    T o = q.poll();

                    boolean empty = o == null;

                    if (d && empty) {
                        Throwable ex = error;
                        if (ex != null) {
                            errorDownstream(ex);
                        } else {
                            completeDownstream();
                        }
                        return;
                    }

                    if (empty) {
                        break;
                    }

                    a.onNext(o);

                    e++;
                }

                if (e == r) {
                    if (isCancelled()) {
                        q.clear();
                        return;
                    }

                    boolean d = done;

                    boolean empty = q.isEmpty();

                    if (d && empty) {
                        Throwable ex = error;
                        if (ex != null) {
                            errorDownstream(ex);
                        } else {
                            completeDownstream();
                        }
                        return;
                    }
                }

                if (e != 0) {
                    BackpressureHelper.produced(this, e);
                }

                missed = wip.addAndGet(-missed);
                if (missed == 0) {
                    break;
                }
            }
        }
    }

    static final class LatestAsyncEmitter<T> extends BaseEmitter<T> {

        private static final long serialVersionUID = 4023437720691792495L;

        final AtomicReference<T> queue;

        Throwable error;
        volatile boolean done;

        final AtomicInteger wip;

        LatestAsyncEmitter(Subscriber<? super T> downstream) {
            super(downstream);
            this.queue = new AtomicReference<T>();
            this.wip = new AtomicInteger();
        }

        @Override
        public void onNext(T t) {
            if (done || isCancelled()) {
                return;
            }

            if (t == null) {
                onError(ExceptionHelper.createNullPointerException("onNext called with a null value."));
                return;
            }
            queue.set(t);
            drain();
        }

        @Override
        public boolean signalError(Throwable e) {
            if (done || isCancelled()) {
                return false;
            }
            error = e;
            done = true;
            drain();
            return true;
        }

        @Override
        public void onComplete() {
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
            final Subscriber<? super T> a = downstream;
            final AtomicReference<T> q = queue;

            for (;;) {
                long r = get();
                long e = 0L;

                while (e != r) {
                    if (isCancelled()) {
                        q.lazySet(null);
                        return;
                    }

                    boolean d = done;

                    T o = q.getAndSet(null);

                    boolean empty = o == null;

                    if (d && empty) {
                        Throwable ex = error;
                        if (ex != null) {
                            errorDownstream(ex);
                        } else {
                            completeDownstream();
                        }
                        return;
                    }

                    if (empty) {
                        break;
                    }

                    a.onNext(o);

                    e++;
                }

                if (e == r) {
                    if (isCancelled()) {
                        q.lazySet(null);
                        return;
                    }

                    boolean d = done;

                    boolean empty = q.get() == null;

                    if (d && empty) {
                        Throwable ex = error;
                        if (ex != null) {
                            errorDownstream(ex);
                        } else {
                            completeDownstream();
                        }
                        return;
                    }
                }

                if (e != 0) {
                    BackpressureHelper.produced(this, e);
                }

                missed = wip.addAndGet(-missed);
                if (missed == 0) {
                    break;
                }
            }
        }
    }

}
