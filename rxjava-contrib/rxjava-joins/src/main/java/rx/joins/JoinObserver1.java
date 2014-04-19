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
package rx.joins;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.atomic.AtomicBoolean;

import rx.Notification;
import rx.Observable;
import rx.Subscriber;
import rx.functions.Action1;
import rx.observers.SafeSubscriber;

/**
 * Default implementation of a join observer.
 */
public final class JoinObserver1<T> extends Subscriber<Notification<T>> implements JoinObserver {
    private Object gate;
    private final Observable<T> source;
    private final Action1<Throwable> onError;
    private final List<ActivePlan0> activePlans;
    private final Queue<Notification<T>> queue;
    private final AtomicBoolean subscribed = new AtomicBoolean(false);
    private final SafeSubscriber<Notification<T>> safeObserver;

    public JoinObserver1(Observable<T> source, Action1<Throwable> onError) {
        this.source = source;
        this.onError = onError;
        queue = new LinkedList<Notification<T>>();
        activePlans = new ArrayList<ActivePlan0>();
        safeObserver = new SafeSubscriber<Notification<T>>(new InnerObserver());
        // add this subscription so it gets unsubscribed when the parent does
        add(safeObserver);
    }

    public Queue<Notification<T>> queue() {
        return queue;
    }

    public void addActivePlan(ActivePlan0 activePlan) {
        activePlans.add(activePlan);
    }

    @Override
    public void subscribe(Object gate) {
        if (subscribed.compareAndSet(false, true)) {
            this.gate = gate;
            source.materialize().unsafeSubscribe(this);
        } else {
            throw new IllegalStateException("Can only be subscribed to once.");
        }
    }

    @Override
    public void dequeue() {
        queue.remove();
    }


    @Override
    public void onNext(Notification<T> args) {
        safeObserver.onNext(args);
    }

    @Override
    public void onError(Throwable e) {
        safeObserver.onError(e);
    }

    @Override
    public void onCompleted() {
        safeObserver.onCompleted();
    }

    void removeActivePlan(ActivePlan0 activePlan) {
        activePlans.remove(activePlan);
        if (activePlans.isEmpty()) {
            unsubscribe();
        }
    }
    
    
    private final class InnerObserver extends Subscriber<Notification<T>> {

        @Override
        public void onNext(Notification<T> args) {
            synchronized (gate) {
                if (!isUnsubscribed()) {
                    if (args.isOnError()) {
                        onError.call(args.getThrowable());
                        return;
                    }
                    queue.add(args);

                    // remark: activePlans might change while iterating
                    for (ActivePlan0 a : new ArrayList<ActivePlan0>(activePlans)) {
                        a.match();
                    }
                }
            }
        }

        @Override
        public void onError(Throwable e) {
            // not expected
        }

        @Override
        public void onCompleted() {
            // not expected or ignored
        }
    }

}