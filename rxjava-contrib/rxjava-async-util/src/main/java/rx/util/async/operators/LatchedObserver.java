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
package rx.util.async.operators;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import rx.Observer;
import rx.functions.Action0;
import rx.functions.Action1;
import rx.functions.Action2;

/**
 * An observer implementation that calls a CountDownLatch in case
 * a terminal state has been reached.
 * @param <T> the observed value type
 */
abstract class LatchedObserver<T> implements Observer<T> {
    /** The CountDownLatch to count-down on a terminal state. */
    protected final CountDownLatch latch;
    /** Contains the error. */
    protected volatile Throwable error;
    /**
     * Indicates the completion status.
     */
    protected final AtomicBoolean done;
    /**
     * Consturcts a LatchedObserver instance.
     * @param latch the CountDownLatch to use
     */
    public LatchedObserver(CountDownLatch latch) {
        this.latch = latch;
        this.done = new AtomicBoolean();
    }
    
    /**
     * Override this method to handle an onNext event.
     * @param value 
     */
    protected abstract void onNextCore(T value);
    /**
     * Override this method to handle an onError event.
     * @param e 
     */
    protected abstract void onErrorCore(Throwable e);
    /**
     * Override this to handle th onCompleted event.
     */
    protected abstract void onCompletedCore();
    /**
     * Try to move into an error state.
     * @param e
     * @return true if succeded, false if this observable has already terminated
     */
    protected boolean fail(Throwable e) {
        if (done.compareAndSet(false, true)) {
            onErrorCore(e);
            return true;
        }
        return false;
    }

    @Override
    public final void onNext(T args) {
        if (!done.get()) {
            onNextCore(args);
        }
    }

    @Override
    public final void onError(Throwable e) {
        fail(e);
    }

    @Override
    public final void onCompleted() {
        if (done.compareAndSet(false, true)) {
            onCompletedCore();
        }
    }
    
    /**
     * Block and await the latch.
     * @throws InterruptedException if the wait is interrupted
     */
    public void await() throws InterruptedException {
        latch.await();
    }
    /**
     * Block and await the latch for a given amount of time.
     * @see CountDownLatch#await(long, java.util.concurrent.TimeUnit) 
     */
    public boolean await(long time, TimeUnit unit) throws InterruptedException {
        return latch.await(time, unit);
    }
    /**
     * Returns the observed error or null if there was none.
     * <p>
     * Should be generally called after the await() returns.
     * @return the observed error
     */
    public Throwable getThrowable() {
        return error;
    }
    
    /**
     * Create a LatchedObserver with the given callback function(s).
     */
    public static <T> LatchedObserver<T> create(Action1<? super T> onNext) {
        return create(onNext, new CountDownLatch(1));
    }

    /**
     * Create a LatchedObserver with the given callback function(s).
     */
    public static <T> LatchedObserver<T> create(Action1<? super T> onNext, Action1<? super Throwable> onError) {
        return create(onNext, onError, new CountDownLatch(1));
    }

    /**
     * Create a LatchedObserver with the given callback function(s).
     */
    public static <T> LatchedObserver<T> create(Action1<? super T> onNext, Action1<? super Throwable> onError, Action0 onCompleted) {
        return create(onNext, onError, onCompleted, new CountDownLatch(1));
    }
    
    /**
     * Create a LatchedObserver with the given callback function(s) and a shared latch.
     */
    public static <T> LatchedObserver<T> create(Action1<? super T> onNext, CountDownLatch latch) {
        return new LatchedObserverImpl<T>(onNext, Functionals.emptyThrowable(), Functionals.empty(), latch);
    }

    /**
     * Create a LatchedObserver with the given callback function(s) and a shared latch.
     */
    public static <T> LatchedObserver<T> create(Action1<? super T> onNext, Action1<? super Throwable> onError, CountDownLatch latch) {
        return new LatchedObserverImpl<T>(onNext, onError, Functionals.empty(), latch);
    }

    /**
     * Create a LatchedObserver with the given callback function(s) and a shared latch.
     */
    public static <T> LatchedObserver<T> create(Action1<? super T> onNext, Action1<? super Throwable> onError, Action0 onCompleted, CountDownLatch latch) {
        return new LatchedObserverImpl<T>(onNext, onError, onCompleted, latch);
    }
    
    /**
     * Create a LatchedObserver with the given indexed callback function(s).
     */
    public static <T> LatchedObserver<T> createIndexed(Action2<? super T, ? super Integer> onNext) {
        return createIndexed(onNext, new CountDownLatch(1));
    }

    /**
     * Create a LatchedObserver with the given indexed callback function(s).
     */
    public static <T> LatchedObserver<T> createIndexed(Action2<? super T, ? super Integer> onNext, Action1<? super Throwable> onError) {
        return createIndexed(onNext, onError, new CountDownLatch(1));
    }

    /**
     * Create a LatchedObserver with the given indexed callback function(s).
     */
    public static <T> LatchedObserver<T> createIndexed(Action2<? super T, ? super Integer> onNext, Action1<? super Throwable> onError, Action0 onCompleted) {
        return createIndexed(onNext, onError, onCompleted, new CountDownLatch(1));
    }
    
    /**
     * Create a LatchedObserver with the given indexed callback function(s) and a shared latch.
     */
    public static <T> LatchedObserver<T> createIndexed(Action2<? super T, ? super Integer> onNext, CountDownLatch latch) {
        return new LatchedObserverIndexedImpl<T>(onNext, Functionals.emptyThrowable(), Functionals.empty(), latch);
    }

    /**
     * Create a LatchedObserver with the given indexed callback function(s) and a shared latch.
     */
    public static <T> LatchedObserver<T> createIndexed(Action2<? super T, ? super Integer> onNext, Action1<? super Throwable> onError, CountDownLatch latch) {
        return new LatchedObserverIndexedImpl<T>(onNext, onError, Functionals.empty(), latch);
    }

    /**
     * Create a LatchedObserver with the given indexed callback function(s) and a shared latch.
     */
    public static <T> LatchedObserver<T> createIndexed(Action2<? super T, ? super Integer> onNext, Action1<? super Throwable> onError, Action0 onCompleted, CountDownLatch latch) {
        return new LatchedObserverIndexedImpl<T>(onNext, onError, onCompleted, latch);
    }

    /**
     * A latched observer which calls an action for each observed value
     * and checks if a cancellation token is not unsubscribed.
     * @param <T> the observed value type
     */
    private static final class LatchedObserverImpl<T> extends LatchedObserver<T> {
        final Action1<? super T> onNext;
        final Action1<? super Throwable> onError;
        final Action0 onCompleted;

        public LatchedObserverImpl(Action1<? super T> onNext, 
                Action1<? super Throwable> onError, 
                Action0 onCompleted, 
                CountDownLatch latch) {
            super(latch);
            this.onNext = onNext;
            this.onError = onError;
            this.onCompleted = onCompleted;
        }
        
        @Override
        protected void onNextCore(T args) {
            try {
                onNext.call(args);
            } catch (Throwable t) {
                fail(t);
            }
        }

        @Override
        protected void onErrorCore(Throwable e) {
            try {
                error = e;
                onError.call(e);
            } finally {
                latch.countDown();
            }
        }

        @Override
        protected void onCompletedCore() {
            try {
                onCompleted.call();
            } finally {
                latch.countDown();
            }
        }
    }
    /**
     * A latched observer which calls an action for each observed value
     * and checks if a cancellation token is not unsubscribed.
     * @param <T> the observed value type
     */
    private static final class LatchedObserverIndexedImpl<T> extends LatchedObserver<T> {
        final Action2<? super T, ? super Integer> onNext;
        final Action1<? super Throwable> onError;
        final Action0 onCompleted;
        int index;

        public LatchedObserverIndexedImpl(Action2<? super T, ? super Integer> onNext, 
                Action1<? super Throwable> onError, 
                Action0 onCompleted, 
                CountDownLatch latch) {
            super(latch);
            this.onNext = onNext;
            this.onError = onError;
            this.onCompleted = onCompleted;
        }
        
        @Override
        protected void onNextCore(T args) {
            if (index == Integer.MAX_VALUE) {
                fail(new ArithmeticException("index overflow"));
                return;
            }
            try {
                onNext.call(args, index++);
            } catch (Throwable t) {
                fail(t);
            }
        }

        @Override
        protected void onErrorCore(Throwable e) {
            try {
                error = e;
                onError.call(e);
            } finally {
                latch.countDown();
            }
        }

        @Override
        protected void onCompletedCore() {
            try {
                onCompleted.call();
            } finally {
                latch.countDown();
            }
        }
    }
}
