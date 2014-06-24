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
package rx.observers;

import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import rx.Notification;
import rx.Observer;
import rx.Subscriber;

/**
 * Subscriber usable for unit testing to perform assertions, inspect received events or wrap a mocked
 * Subscriber.
 */
public class TestSubscriber<T> extends Subscriber<T> {

    private final TestObserver<T> testObserver;
    private final CountDownLatch latch = new CountDownLatch(1);
    private volatile Thread lastSeenThread;

    public TestSubscriber(Subscriber<T> delegate) {
        this.testObserver = new TestObserver<T>(delegate);
    }

    public TestSubscriber(Observer<T> delegate) {
        this.testObserver = new TestObserver<T>(delegate);
    }

    public TestSubscriber() {
        this.testObserver = new TestObserver<T>(new Observer<T>() {

            @Override
            public void onCompleted() {
                // do nothing
            }

            @Override
            public void onError(Throwable e) {
                // do nothing
            }

            @Override
            public void onNext(T t) {
                // do nothing
            }

        });
    }

    @Override
    public void onCompleted() {
        try {
            lastSeenThread = Thread.currentThread();
            testObserver.onCompleted();
        } finally {
            latch.countDown();
        }
    }

    /**
     * Get the {@link Notification}s representing each time this subscriber was notified of sequence completion
     * via {@link #onCompleted}, as a {@link List}.
     *
     * @return a list of Notifications representing calls to this subscriber's {@link #onCompleted} method
     */
    public List<Notification<T>> getOnCompletedEvents() {
        return testObserver.getOnCompletedEvents();
    }

    @Override
    public void onError(Throwable e) {
        try {
            lastSeenThread = Thread.currentThread();
            testObserver.onError(e);
        } finally {
            latch.countDown();
        }
    }

    /**
     * Get the {@link Throwable}s this subscriber was notified of via {@link #onError} as a {@link List}.
     *
     * @return a list of Throwables passed to this subscriber's {@link #onError} method
     */
    public List<Throwable> getOnErrorEvents() {
        return testObserver.getOnErrorEvents();
    }

    @Override
    public void onNext(T t) {
        lastSeenThread = Thread.currentThread();
        testObserver.onNext(t);
    }

    /**
     * Get the sequence of items observed by this subscriber, as an ordered {@link List}.
     *
     * @return a list of items observed by this subscriber, in the order in which they were observed
     */
    public List<T> getOnNextEvents() {
        return testObserver.getOnNextEvents();
    }

    /**
     * Assert that a particular sequence of items was received in order.
     *
     * @param items
     *          the sequence of items expected to have been observed
     * @throws AssertionError
     *          if the sequence of items observed does not exactly match {@code items}
     */
    public void assertReceivedOnNext(List<T> items) {
        testObserver.assertReceivedOnNext(items);
    }

    /**
     * Assert that a single terminal event occurred, either {@link #onCompleted} or {@link #onError}.
     *
     * @throws AssertionError
     *          if not exactly one terminal event notification was received
     */
    public void assertTerminalEvent() {
        testObserver.assertTerminalEvent();
    }

    /**
     * Assert that this {@code Subscriber} is unsubscribed.
     *
     * @throws AssertionError
     *          if this {@code Subscriber} is not unsubscribed
     */
    public void assertUnsubscribed() {
        if (!isUnsubscribed()) {
            throw new AssertionError("Not unsubscribed.");
        }
    }

    public void assertNoErrors() {
        if (getOnErrorEvents().size() > 0) {
            throw new AssertionError("Unexpected onError events: " + getOnErrorEvents().size(), getOnErrorEvents().get(0));
        }
    }

    /**
     * @warn javadoc missing
     */
    public void awaitTerminalEvent() {
        try {
            latch.await();
        } catch (InterruptedException e) {
            throw new RuntimeException("Interrupted", e);
        }
    }

    /**
     * @warn javadoc missing
     * @param timeout
     * @param unit
     */
    public void awaitTerminalEvent(long timeout, TimeUnit unit) {
        try {
            latch.await(timeout, unit);
        } catch (InterruptedException e) {
            throw new RuntimeException("Interrupted", e);
        }
    }

    /**
     * @warn javadoc missing
     * @param timeout
     * @param unit
     */
    public void awaitTerminalEventAndUnsubscribeOnTimeout(long timeout, TimeUnit unit) {
        try {
            awaitTerminalEvent(timeout, unit);
        } catch (RuntimeException e) {
            unsubscribe();
        }
    }

    /**
     * @warn javadoc missing
     */
    public Thread getLastSeenThread() {
        return lastSeenThread;
    }
}
