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

import rx.Notification;
import rx.Observer;
import rx.Subscriber;

/**
 * Observer usable for unit testing to perform assertions, inspect received events or wrap a mocked Observer.
 */
public class TestSubscriber<T> extends Subscriber<T> {

    private final Subscriber<Object> EMPTY = Subscribers.create();

    private final TestObserver<T> testObserver;

    public TestSubscriber(Subscriber<T> delegate) {
        this.testObserver = new TestObserver<T>(delegate);
    }
    
    public TestSubscriber(Observer<T> delegate) {
        this.testObserver = new TestObserver<T>(delegate);
    }

    @SuppressWarnings("unchecked")
    public TestSubscriber() {
        this.testObserver = new TestObserver<T>((Subscriber<T>) EMPTY);
    }

    @Override
    public void onCompleted() {
        testObserver.onCompleted();
    }

    public List<Notification<T>> getOnCompletedEvents() {
        return testObserver.getOnCompletedEvents();
    }

    @Override
    public void onError(Throwable e) {
        testObserver.onError(e);
    }

    public List<Throwable> getOnErrorEvents() {
        return testObserver.getOnErrorEvents();
    }

    @Override
    public void onNext(T t) {
        testObserver.onNext(t);
    }

    public List<T> getOnNextEvents() {
        return testObserver.getOnNextEvents();
    }

    public void assertReceivedOnNext(List<T> items) {
        testObserver.assertReceivedOnNext(items);
    }

    /**
     * Assert that a single terminal event occurred, either onCompleted or onError.
     */
    public void assertTerminalEvent() {
        testObserver.assertTerminalEvent();
    }

}
