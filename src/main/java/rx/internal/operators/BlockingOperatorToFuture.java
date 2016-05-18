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

import java.util.concurrent.CancellationException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicReference;

import rx.Observable;
import rx.Subscriber;
import rx.Subscription;

/**
 * Returns a Future representing the single value emitted by an Observable.
 * <p>
 * <img width="640" src="https://github.com/ReactiveX/RxJava/wiki/images/rx-operators/B.toFuture.png" alt="">
 * <p>
 * The toFuture operation throws an exception if the Observable emits more than one item. If the
 * Observable may emit more than item, use <code>toList().toFuture()</code>.
 */
public final class BlockingOperatorToFuture {
    private BlockingOperatorToFuture() {
        throw new IllegalStateException("No instances!");
    }
    /**
     * Returns a Future that expects a single item from the observable.
     * 
     * @param that
     *            an observable sequence to get a Future for.
     * @param <T>
     *            the type of source.
     * @return the Future to retrieve a single elements from an Observable
     */
    public static <T> Future<T> toFuture(Observable<? extends T> that) {

        final CountDownLatch finished = new CountDownLatch(1);
        final AtomicReference<T> value = new AtomicReference<T>();
        final AtomicReference<Throwable> error = new AtomicReference<Throwable>();

        @SuppressWarnings("unchecked")
        final Subscription s = ((Observable<T>)that).single().subscribe(new Subscriber<T>() {

            @Override
            public void onCompleted() {
                finished.countDown();
            }

            @Override
            public void onError(Throwable e) {
                error.compareAndSet(null, e);
                finished.countDown();
            }

            @Override
            public void onNext(T v) {
                // "single" guarantees there is only one "onNext"
                value.set(v);
            }
        });

        return new Future<T>() {

            private volatile boolean cancelled = false;

            @Override
            public boolean cancel(boolean mayInterruptIfRunning) {
                if (finished.getCount() > 0) {
                    cancelled = true;
                    s.unsubscribe();
                    // release the latch (a race condition may have already released it by now)
                    finished.countDown();
                    return true;
                } else {
                    // can't cancel
                    return false;
                }
            }

            @Override
            public boolean isCancelled() {
                return cancelled;
            }

            @Override
            public boolean isDone() {
                return finished.getCount() == 0;
            }

            @Override
            public T get() throws InterruptedException, ExecutionException {
                finished.await();
                return getValue();
            }

            @Override
            public T get(long timeout, TimeUnit unit) throws InterruptedException, ExecutionException, TimeoutException {
                if (finished.await(timeout, unit)) {
                    return getValue();
                } else {
                    throw new TimeoutException("Timed out after " + unit.toMillis(timeout) + "ms waiting for underlying Observable.");
                }
            }

            private T getValue() throws ExecutionException {
                final Throwable throwable = error.get();

                if (throwable != null) {
                    throw new ExecutionException("Observable onError", throwable);
                } else if (cancelled) {
                    // Contract of Future.get() requires us to throw this:
                    throw new CancellationException("Subscription unsubscribed");
                } else {
                    return value.get();
                }
            }

        };

    }

}
