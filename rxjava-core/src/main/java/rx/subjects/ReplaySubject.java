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
package rx.subjects;

import java.util.ArrayList;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

import rx.Observer;
import rx.functions.Action1;
import rx.operators.NotificationLite;
import rx.subjects.SubjectSubscriptionManager.SubjectObserver;

/**
 * Subject that retains all events and will replay them to an {@link Observer} that subscribes.
 * <p>
 * <img width="640" src="https://raw.github.com/wiki/Netflix/RxJava/images/rx-operators/S.ReplaySubject.png">
 * <p>
 * Example usage:
 * <p>
 * <pre> {@code

 * ReplaySubject<Object> subject = ReplaySubject.create();
  subject.onNext("one");
  subject.onNext("two");
  subject.onNext("three");
  subject.onCompleted();

  // both of the following will get the onNext/onCompleted calls from above
  subject.subscribe(observer1);
  subject.subscribe(observer2);

  } </pre>
 * 
 * @param <T>
 */
public final class ReplaySubject<T> extends Subject<T, T> {
    public static <T> ReplaySubject<T> create() {
        return create(16);
    }

    public static <T> ReplaySubject<T> create(int initialCapacity) {
        final SubjectSubscriptionManager<T> subscriptionManager = new SubjectSubscriptionManager<T>();
        final ReplayState<T> state = new ReplayState<T>(initialCapacity);
        subscriptionManager.onStart = new Action1<SubjectObserver<T>>() {
            @Override
            public void call(SubjectObserver<T> o) {
                // replay history for this observer using the subscribing thread
                int lastIndex = replayObserverFromIndex(state.history, 0, o);

                // now that it is caught up add to observers
                state.replayState.put(o, lastIndex);
            }
        };
        subscriptionManager.onTerminated = new Action1<SubjectObserver<T>>() {
            @Override
            public void call(SubjectObserver<T> o) {
                Integer idx = state.replayState.remove(o);
                if (idx == null) {
                    idx = 0;
                }
                replayObserverFromIndex(state.history, idx, o);
            }
        };
        subscriptionManager.onUnsubscribed = new Action1<SubjectObserver<T>>() {
            @Override
            public void call(SubjectObserver<T> o) {
                state.replayState.remove(o);
            }
        };
        return new ReplaySubject<T>(subscriptionManager, subscriptionManager, state);
    }

    private static class ReplayState<T> {
        // single-producer, multi-consumer
        final History<T> history;
        // each Observer is tracked here for what events they have received
        final ConcurrentHashMap<Observer<? super T>, Integer> replayState;

        public ReplayState(int initialCapacity) {
            history = new History<T>(initialCapacity);
            replayState = new ConcurrentHashMap<Observer<? super T>, Integer>();
        }
    }

    private final SubjectSubscriptionManager<T> subscriptionManager;
    private final ReplayState<T> state;

    protected ReplaySubject(OnSubscribe<T> onSubscribe, SubjectSubscriptionManager<T> subscriptionManager, ReplayState<T> state) {
        super(onSubscribe);
        this.subscriptionManager = subscriptionManager;
        this.state = state;
    }

    @Override
    public void onCompleted() {

        if (subscriptionManager.active) {
            state.history.complete();
            for (SubjectObserver<T> o : subscriptionManager.terminate(NotificationLite.instance().completed())) {
                if (caughtUp(o)) {
                    o.onCompleted();
                }
            }
        }
    }

    @Override
    public void onError(final Throwable e) {
        if (subscriptionManager.active) {
            state.history.complete(e);
            for (SubjectObserver<T> o : subscriptionManager.terminate(NotificationLite.instance().completed())) {
                if (caughtUp(o)) {
                    o.onError(e);
                }
            }
        }
    }

    @Override
    public void onNext(T v) {
        if (state.history.terminated) {
            return;
        }
        state.history.next(v);
        for (SubjectObserver<? super T> o : subscriptionManager.observers()) {
            if (caughtUp(o)) {
                o.onNext(v);
            }
        }
    }

    /*
     * This is not very elegant but resulted in non-trivial performance improvement by
     * eliminating the 'replay' code-path on the normal fast-path of emitting values.
     * 
     * With this method: 16,151,174 ops/sec
     * Without: 8,632,358 ops/sec
     */
    private boolean caughtUp(SubjectObserver<? super T> o) {
        if (!o.caughtUp) {
            o.caughtUp = true;
            replayObserver(o);
            return false;
        } else {
            // it was caught up so proceed the "raw route"
            return true;
        }
    }

    private void replayObserver(SubjectObserver<? super T> observer) {
        Integer lastEmittedLink = state.replayState.get(observer);
        if (lastEmittedLink == null) {
            lastEmittedLink = 0;
        }
        int l = replayObserverFromIndex(state.history, lastEmittedLink, observer);
        state.replayState.put(observer, l);
    }

    static <T> int replayObserverFromIndex(History<T> history, int idx, SubjectObserver<? super T> observer) {
        while (idx < history.index.get()) {
            history.accept(observer, idx);
            idx++;
        }

        return idx;
    }

    /**
     * NOT thread-safe for multi-writer. Assumes single-writer.
     * Is thread-safe for multi-reader.
     * 
     * @param <T>
     */
    private static class History<T> {
        private final NotificationLite<T> nl = NotificationLite.instance();
        private final AtomicInteger index;
        private final ArrayList<Object> list;
        private boolean terminated;

        public History(int initialCapacity) {
            index = new AtomicInteger(0);
            list = new ArrayList<Object>(initialCapacity);
        }

        public boolean next(T n) {
            if (!terminated) {
                list.add(nl.next(n));
                index.getAndIncrement();
                return true;
            } else {
                return false;
            }
        }

        public void accept(Observer<? super T> o, int idx) {
            nl.accept(o, list.get(idx));
        }
        
        public void complete() {
            if (!terminated) {
                terminated = true;
                list.add(nl.completed());
                index.getAndIncrement();
            }
        }
        public void complete(Throwable e) {
            if (!terminated) {
                terminated = true;
                list.add(nl.error(e));
                index.getAndIncrement();
            }
        }
    }

    /**
     * @return Returns the number of subscribers.
     */
    /* Support test. */int subscriberCount() {
        return state.replayState.size();
    }
}