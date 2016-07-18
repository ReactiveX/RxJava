/**
 * Copyright 2016 Netflix, Inc.
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

package io.reactivex.internal.operators.flowable;

import java.util.Queue;
import java.util.concurrent.atomic.*;

import org.reactivestreams.*;

import io.reactivex.*;
import io.reactivex.disposables.*;
import io.reactivex.exceptions.MissingBackpressureException;
import io.reactivex.functions.Consumer;
import io.reactivex.internal.queue.SpscLinkedArrayQueue;
import io.reactivex.internal.subscriptions.SubscriptionHelper;
import io.reactivex.internal.util.*;
import io.reactivex.plugins.RxJavaPlugins;


public final class FlowableFromAsync<T> extends Flowable<T> {

    final Consumer<AsyncEmitter<T>> asyncEmitter;
    
    final AsyncEmitter.BackpressureMode backpressure;
    
    public FlowableFromAsync(Consumer<AsyncEmitter<T>> asyncEmitter, AsyncEmitter.BackpressureMode backpressure) {
        this.asyncEmitter = asyncEmitter;
        this.backpressure = backpressure;
    }
    
    @Override
    public void subscribeActual(Subscriber<? super T> t) {
        BaseAsyncEmitter<T> emitter;
        
        switch (backpressure) {
        case NONE: {
            emitter = new NoneAsyncEmitter<T>(t);
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
        asyncEmitter.accept(emitter);
        
    }
    
    static final class CancellableSubscription 
    extends AtomicReference<AsyncEmitter.Cancellable>
    implements Disposable {
        
        /** */
        private static final long serialVersionUID = 5718521705281392066L;

        public CancellableSubscription(AsyncEmitter.Cancellable cancellable) {
            super(cancellable);
        }
        
        @Override
        public boolean isDisposed() {
            return get() == null;
        }
        
        @Override
        public void dispose() {
            if (get() != null) {
                AsyncEmitter.Cancellable c = getAndSet(null);
                if (c != null) {
                    try {
                        c.cancel();
                    } catch (Exception ex) {
                        Exceptions.throwIfFatal(ex);
                        RxJavaPlugins.onError(ex);
                    }
                }
            }
        }
    }
    
    static abstract class BaseAsyncEmitter<T> 
    extends AtomicLong
    implements AsyncEmitter<T>, Subscription {
        /** */
        private static final long serialVersionUID = 7326289992464377023L;

        final Subscriber<? super T> actual;
        
        final SerialDisposable serial;

        public BaseAsyncEmitter(Subscriber<? super T> actual) {
            this.actual = actual;
            this.serial = new SerialDisposable();
        }

        @Override
        public void onComplete() {
            if (isCancelled()) {
                return;
            }
            try {
                actual.onComplete();
            } finally {
                serial.dispose();
            }
        }

        @Override
        public void onError(Throwable e) {
            if (isCancelled()) {
                return;
            }
            try {
                actual.onError(e);
            } finally {
                serial.dispose();
            }
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
        public final void setDisposable(Disposable s) {
            serial.set(s);
        }

        @Override
        public final void setCancellation(AsyncEmitter.Cancellable c) {
            setDisposable(new CancellableSubscription(c));
        }

        @Override
        public final long requested() {
            return get();
        }
    }
    
    static final class NoneAsyncEmitter<T> extends BaseAsyncEmitter<T> {

        /** */
        private static final long serialVersionUID = 3776720187248809713L;

        public NoneAsyncEmitter(Subscriber<? super T> actual) {
            super(actual);
        }

        @Override
        public void onNext(T t) {
            if (isCancelled()) {
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
    
    static abstract class NoOverflowBaseAsyncEmitter<T> extends BaseAsyncEmitter<T> {

        /** */
        private static final long serialVersionUID = 4127754106204442833L;

        public NoOverflowBaseAsyncEmitter(Subscriber<? super T> actual) {
            super(actual);
        }

        @Override
        public final void onNext(T t) {
            if (isCancelled()) {
                return;
            }

            if (get() != 0) {
                actual.onNext(t);
                BackpressureHelper.produced(this, 1);
            } else {
                onOverflow();
            }
        }
        
        abstract void onOverflow();
    }
    
    static final class DropAsyncEmitter<T> extends NoOverflowBaseAsyncEmitter<T> {

        /** */
        private static final long serialVersionUID = 8360058422307496563L;

        public DropAsyncEmitter(Subscriber<? super T> actual) {
            super(actual);
        }

        @Override
        void onOverflow() {
            // nothing to do
        }
        
    }

    static final class ErrorAsyncEmitter<T> extends NoOverflowBaseAsyncEmitter<T> {

        /** */
        private static final long serialVersionUID = 338953216916120960L;

        public ErrorAsyncEmitter(Subscriber<? super T> actual) {
            super(actual);
        }

        @Override
        void onOverflow() {
            onError(new MissingBackpressureException("fromAsync: could not emit value due to lack of requests"));
        }
        
    }
    
    static final class BufferAsyncEmitter<T> extends BaseAsyncEmitter<T> {

        /** */
        private static final long serialVersionUID = 2427151001689639875L;

        final Queue<T> queue;
        
        Throwable error;
        volatile boolean done;
        
        final AtomicInteger wip;
        
        public BufferAsyncEmitter(Subscriber<? super T> actual, int capacityHint) {
            super(actual);
            this.queue = new SpscLinkedArrayQueue<T>(capacityHint);
            this.wip = new AtomicInteger();
        }

        @Override
        public void onNext(T t) {
            queue.offer(t);
            drain();
        }
        
        @Override
        public void onError(Throwable e) {
            error = e;
            done = true;
            drain();
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
            final Subscriber<? super T> a = actual;
            final Queue<T> q = queue;
            
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
                            super.onError(ex);
                        } else {
                            super.onComplete();
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
                            super.onError(ex);
                        } else {
                            super.onComplete();
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

    static final class LatestAsyncEmitter<T> extends BaseAsyncEmitter<T> {

        /** */
        private static final long serialVersionUID = 4023437720691792495L;

        final AtomicReference<T> queue;

        Throwable error;
        volatile boolean done;
        
        final AtomicInteger wip;
        
        public LatestAsyncEmitter(Subscriber<? super T> actual) {
            super(actual);
            this.queue = new AtomicReference<T>();
            this.wip = new AtomicInteger();
        }

        @Override
        public void onNext(T t) {
            queue.set(t);
            drain();
        }
        
        @Override
        public void onError(Throwable e) {
            error = e;
            done = true;
            drain();
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
            final Subscriber<? super T> a = actual;
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
                            super.onError(ex);
                        } else {
                            super.onComplete();
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
                            super.onError(ex);
                        } else {
                            super.onComplete();
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