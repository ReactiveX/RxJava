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
import rx.exceptions.*;
import rx.functions.Action1;
import rx.internal.util.RxRingBuffer;
import rx.internal.util.atomic.SpscUnboundedAtomicArrayQueue;
import rx.internal.util.unsafe.*;
import rx.plugins.RxJavaHooks;
import rx.subscriptions.SerialSubscription;

public final class OnSubscribeFromAsync<T> implements OnSubscribe<T> {

    final Action1<AsyncEmitter<T>> asyncEmitter;
    
    final AsyncEmitter.BackpressureMode backpressure;
    
    public OnSubscribeFromAsync(Action1<AsyncEmitter<T>> asyncEmitter, AsyncEmitter.BackpressureMode backpressure) {
        this.asyncEmitter = asyncEmitter;
        this.backpressure = backpressure;
    }
    
    @Override
    public void call(Subscriber<? super T> t) {
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
            emitter = new BufferAsyncEmitter<T>(t, RxRingBuffer.SIZE);
            break;
        }
        }

        t.add(emitter);
        t.setProducer(emitter);
        asyncEmitter.call(emitter);
        
    }
    
    static final class CancellableSubscription 
    extends AtomicReference<AsyncEmitter.Cancellable>
    implements Subscription {
        
        /** */
        private static final long serialVersionUID = 5718521705281392066L;

        public CancellableSubscription(AsyncEmitter.Cancellable cancellable) {
            super(cancellable);
        }
        
        @Override
        public boolean isUnsubscribed() {
            return get() == null;
        }
        
        @Override
        public void unsubscribe() {
            if (get() != null) {
                AsyncEmitter.Cancellable c = getAndSet(null);
                if (c != null) {
                    try {
                        c.cancel();
                    } catch (Exception ex) {
                        Exceptions.throwIfFatal(ex);
                        RxJavaHooks.onError(ex);
                    }
                }
            }
        }
    }
    
    static abstract class BaseAsyncEmitter<T> 
    extends AtomicLong
    implements AsyncEmitter<T>, Producer, Subscription {
        /** */
        private static final long serialVersionUID = 7326289992464377023L;

        final Subscriber<? super T> actual;
        
        final SerialSubscription serial;

        public BaseAsyncEmitter(Subscriber<? super T> actual) {
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
        public final void setCancellation(AsyncEmitter.Cancellable c) {
            setSubscription(new CancellableSubscription(c));
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
    
    static abstract class NoOverflowBaseAsyncEmitter<T> extends BaseAsyncEmitter<T> {

        /** */
        private static final long serialVersionUID = 4127754106204442833L;

        public NoOverflowBaseAsyncEmitter(Subscriber<? super T> actual) {
            super(actual);
        }

        @Override
        public final void onNext(T t) {
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

        final Queue<Object> queue;
        
        Throwable error;
        volatile boolean done;
        
        final AtomicInteger wip;
        
        final NotificationLite<T> nl;
        
        public BufferAsyncEmitter(Subscriber<? super T> actual, int capacityHint) {
            super(actual);
            this.queue = UnsafeAccess.isUnsafeAvailable() 
                    ? new SpscUnboundedArrayQueue<Object>(capacityHint)
                    : new SpscUnboundedAtomicArrayQueue<Object>(capacityHint);
            this.wip = new AtomicInteger();
            this.nl = NotificationLite.instance();
        }

        @Override
        public void onNext(T t) {
            queue.offer(nl.next(t));
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
                    
                    a.onNext(nl.getValue(o));
                    
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

    static final class LatestAsyncEmitter<T> extends BaseAsyncEmitter<T> {

        /** */
        private static final long serialVersionUID = 4023437720691792495L;

        final AtomicReference<Object> queue;

        Throwable error;
        volatile boolean done;
        
        final AtomicInteger wip;
        
        final NotificationLite<T> nl;
        
        public LatestAsyncEmitter(Subscriber<? super T> actual) {
            super(actual);
            this.queue = new AtomicReference<Object>();
            this.wip = new AtomicInteger();
            this.nl = NotificationLite.instance();
        }

        @Override
        public void onNext(T t) {
            queue.set(nl.next(t));
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
                    
                    a.onNext(nl.getValue(o));
                    
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
