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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import rx.Notification;
import rx.Observer;

/**
 * Observer usable for unit testing to perform assertions, inspect received events or wrap a mocked Observer.
 */
public class TestObserver<T> implements Observer<T> {

    private final Observer<T> delegate;
    private final ArrayList<T> onNextEvents = new ArrayList<T>();
    private final ArrayList<Throwable> onErrorEvents = new ArrayList<Throwable>();
    private final ArrayList<Notification<T>> onCompletedEvents = new ArrayList<Notification<T>>();

    public TestObserver(Observer<T> delegate) {
        this.delegate = delegate;
    }

    @SuppressWarnings("unchecked")
    public TestObserver() {
        this.delegate = (Observer<T>) INERT;
    }

    @Override
    public void onCompleted() {
        onCompletedEvents.add(Notification.<T> createOnCompleted());
        delegate.onCompleted();
    }

    /**
     * Get the {@link Notification}s representing each time this observer was notified of sequence completion
     * via {@link #onCompleted}, as a {@link List}.
     *
     * @return a list of Notifications representing calls to this observer's {@link #onCompleted} method
     */
    public List<Notification<T>> getOnCompletedEvents() {
        return Collections.unmodifiableList(onCompletedEvents);
    }

    @Override
    public void onError(Throwable e) {
        onErrorEvents.add(e);
        delegate.onError(e);
    }

    /**
     * Get the {@link Throwable}s this observer was notified of via {@link #onError} as a {@link List}.
     *
     * @return a list of Throwables passed to this observer's {@link #onError} method
     */
    public List<Throwable> getOnErrorEvents() {
        return Collections.unmodifiableList(onErrorEvents);
    }

    @Override
    public void onNext(T t) {
        onNextEvents.add(t);
        delegate.onNext(t);
    }

    /**
     * Get the sequence of items observed by this observer, as an ordered {@link List}.
     *
     * @return a list of items observed by this observer, in the order in which they were observed
     */
    public List<T> getOnNextEvents() {
        return Collections.unmodifiableList(onNextEvents);
    }

    /**
     * Get a list containing all of the items and notifications received by this observer, where the items
     * will be given as-is, any error notifications will be represented by their {@code Throwable}s, and any
     * sequence-complete notifications will be represented by their {@code Notification} objects.
     *
     * @return a {@link List} containing one item for each item or notification received by this observer, in
     *         the order in which they were observed or received
     */
    public List<Object> getEvents() {
        ArrayList<Object> events = new ArrayList<Object>();
        events.add(onNextEvents);
        events.add(onErrorEvents);
        events.add(onCompletedEvents);
        return Collections.unmodifiableList(events);
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
        if (onNextEvents.size() != items.size()) {
            throw new AssertionError("Number of items does not match. Provided: " + items.size() + "  Actual: " + onNextEvents.size());
        }

        for (int i = 0; i < items.size(); i++) {
            if (items.get(i) == null) {
                // check for null equality
                if (onNextEvents.get(i) != null) {
                    throw new AssertionError("Value at index: " + i + " expected to be [null] but was: [" + onNextEvents.get(i) + "]");
                }
            } else if (!items.get(i).equals(onNextEvents.get(i))) {
                throw new AssertionError("Value at index: " + i + " expected to be [" + items.get(i) + "] (" + items.get(i).getClass().getSimpleName() + ") but was: [" + onNextEvents.get(i) + "] (" + onNextEvents.get(i).getClass().getSimpleName() + ")");

            }
        }

    }

    /**
     * Assert that a single terminal event occurred, either {@link #onCompleted} or {@link #onError}.
     *
     * @throws AssertionError
     *          if not exactly one terminal event notification was received
     */
    public void assertTerminalEvent() {
        if (onErrorEvents.size() > 1) {
            throw new AssertionError("Too many onError events: " + onErrorEvents.size());
        }

        if (onCompletedEvents.size() > 1) {
            throw new AssertionError("Too many onCompleted events: " + onCompletedEvents.size());
        }

        if (onCompletedEvents.size() == 1 && onErrorEvents.size() == 1) {
            throw new AssertionError("Received both an onError and onCompleted. Should be one or the other.");
        }

        if (onCompletedEvents.size() == 0 && onErrorEvents.size() == 0) {
            throw new AssertionError("No terminal events received.");
        }
    }

    // do nothing ... including swallowing errors
    private static Observer<Object> INERT = new Observer<Object>() {

        @Override
        public void onCompleted() {
            
        }

        @Override
        public void onError(Throwable e) {
            
        }

        @Override
        public void onNext(Object t) {
            
        }
        
    };
}
