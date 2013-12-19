/**
 * Copyright 2013 Netflix, Inc.
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
package rx.observables;

import java.util.List;
import rx.Notification;
import rx.Observable;
import rx.util.Recorded;
import rx.util.RecordedSubscription;

/**
 * Observable sequence that records subscription lifetimes and timestamped
 * notification messages sent to observers
 * @param <T> the value type
 */
public abstract class TestableObservable<T> extends Observable<T> {
    /**
     * Create a TestableObservable instance.
     * @param onSubscribe the subscription function
     */
    public TestableObservable(OnSubscribeFunc<T> onSubscribe) {
        super(onSubscribe);
    }
    /**
     * Return a list of all subscriptions to the observable, including their lifetimes.
     * @return a list of all subscriptions to the observable
     */
    public abstract List<RecordedSubscription> subscriptions();
    /**
     * Return the recorded timestamped notification messages that were sent
     * by the observable to its observers.
     * @return the recorded timestamped notification messages
     */
    public abstract List<Recorded<Notification<T>>> messages();
    /**
     * Advance the parent TestScheduler so all recorded notifications are emitted.
     */
    public abstract void runToEnd();
    /**
     * Advance the parent TestScheduler so all recorded notifications up to a certain time are emitted.
     * @param time the target time to move the TestScheduler
     */
    public abstract void runTo(long time);
}
