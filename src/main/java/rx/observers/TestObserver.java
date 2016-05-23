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

import java.util.*;

import rx.Notification;
import rx.Observer;
import rx.exceptions.CompositeException;

/**
 * Observer usable for unit testing to perform assertions, inspect received events or wrap a mocked Observer.
 * @param <T> the observed value type
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
            assertionError("Number of items does not match. Provided: " + items.size() + "  Actual: " + onNextEvents.size()
            + ".\n"
            + "Provided values: " + items
            + "\n"
            + "Actual values: " + onNextEvents
            + "\n");
        }

        for (int i = 0; i < items.size(); i++) {
            T expected = items.get(i);
            T actual = onNextEvents.get(i);
            if (expected == null) {
                // check for null equality
                if (actual != null) {
                    assertionError("Value at index: " + i + " expected to be [null] but was: [" + actual + "]\n");
                }
            } else if (!expected.equals(actual)) {
                assertionError("Value at index: " + i 
                        + " expected to be [" + expected + "] (" + expected.getClass().getSimpleName() 
                        + ") but was: [" + actual + "] (" + (actual != null ? actual.getClass().getSimpleName() : "null") + ")\n");

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
            assertionError("Too many onError events: " + onErrorEvents.size());
        }

        if (onCompletedEvents.size() > 1) {
            assertionError("Too many onCompleted events: " + onCompletedEvents.size());
        }

        if (onCompletedEvents.size() == 1 && onErrorEvents.size() == 1) {
            assertionError("Received both an onError and onCompleted. Should be one or the other.");
        }

        if (onCompletedEvents.size() == 0 && onErrorEvents.size() == 0) {
            assertionError("No terminal events received.");
        }
    }

    /**
     * Combines an assertion error message with the current completion and error state of this
     * TestSubscriber, giving more information when some assertXXX check fails.
     * @param message the message to use for the error
     */
    final void assertionError(String message) {
        StringBuilder b = new StringBuilder(message.length() + 32);
        
        b.append(message);
        
        
        b.append(" (");
        int c = onCompletedEvents.size();
        b.append(c);
        b.append(" completion");
        if (c != 1) {
            b.append("s");
        }
        b.append(")");
        
        if (!onErrorEvents.isEmpty()) {
            int size = onErrorEvents.size();
            b.append(" (+")
            .append(size)
            .append(" error");
            if (size != 1) {
                b.append("s");
            }
            b.append(")");
        }
        
        AssertionError ae = new AssertionError(b.toString());
        if (!onErrorEvents.isEmpty()) {
            if (onErrorEvents.size() == 1) {
                ae.initCause(onErrorEvents.get(0));
            } else {
                ae.initCause(new CompositeException(onErrorEvents));
            }
        }
        throw ae;
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
