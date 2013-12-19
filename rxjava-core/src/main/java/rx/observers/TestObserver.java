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
package rx.observers;

import java.util.ArrayList;
import java.util.List;
import rx.Notification;
import rx.schedulers.TestScheduler;
import rx.util.Recorded;

/**
 * Observer implementation that records events received and timestamps
 * them with the help of a TestScheduler.
 * @param <T> the observed value type
 */
public class TestObserver<T> implements TestableObserver<T> {
    /** The parent test scheduler. */
    protected final TestScheduler scheduler;
    /** The list of notifications. */
    protected final List<Recorded<Notification<T>>> messages;

    public TestObserver(TestScheduler scheduler) {
        this.scheduler = scheduler;
        this.messages = new ArrayList<Recorded<Notification<T>>>();
    }

    @Override
    public void onNext(T args) {
        messages.add(Recorded.onNext(scheduler.now(), args));
    }

    @Override
    public void onError(Throwable e) {
        messages.add(Recorded.<T>onError(scheduler.now(), e));
    }

    @Override
    public void onCompleted() {
        messages.add(Recorded.<T>onCompleted(scheduler.now()));
    }

    @Override
    public List<Recorded<Notification<T>>> messages() {
        return messages;
    }
    
}
