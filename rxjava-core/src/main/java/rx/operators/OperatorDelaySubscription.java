/**
 * Copyright 2014 Netflix, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package rx.operators;

import java.util.concurrent.TimeUnit;
import rx.Observable;
import rx.Observable.OnSubscribe;
import rx.Scheduler;
import rx.Scheduler.Worker;
import rx.Subscriber;
import rx.functions.Action0;

/**
 * Delays the subscription to the source by the given amount, running on the given scheduler.
 * 
 * @param <T> the value type
 */
public final class OperatorDelaySubscription<T> implements OnSubscribe<T> {
    final Observable<? extends T> source;
    final long time;
    final TimeUnit unit;
    final Scheduler scheduler;

    public OperatorDelaySubscription(Observable<? extends T> source, long time, TimeUnit unit, Scheduler scheduler) {
        this.source = source;
        this.time = time;
        this.unit = unit;
        this.scheduler = scheduler;
    }

    @Override
    public void call(final Subscriber<? super T> s) {
        final Worker worker = scheduler.createWorker();
        s.add(worker);

        worker.schedule(new Action0() {
            @Override
            public void call() {
                if (!s.isUnsubscribed()) {
                    source.unsafeSubscribe(s);
                }
            }
        }, time, unit);
    }
    
}
