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

import rx.*;
import rx.Observer;
import rx.subjects.SubjectSubscriptionManager.*;
import rx.util.functions.*;

import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;

/**
 * Subject that retains events (unlimited or with given replay capacity) and will replay them to an {@link Observer} that subscribes.
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
    private static final Integer ReplaySubjectUnlimitedCapacity = Integer.MAX_VALUE;

    /**
     * @param <T>
     * @return a new replay subject with the unlimited capacity.
     */
    public static <T> ReplaySubject<T> create() {
        return create(ReplaySubjectUnlimitedCapacity);
    }

    /**
     * @param capacity Maximum element count of the replay buffer
     * @param <T>
     * @return a new replay subject with the given capacity.
     */
    public static <T> ReplaySubject<T> create(int capacity) {
        final SubjectSubscriptionManager<T> subscriptionManager = new SubjectSubscriptionManager<T>();
        final ReplayState<T> state = new ReplayState<T>(capacity);

        OnSubscribeFunc<T> onSubscribe = subscriptionManager.getOnSubscribeFunc(
                /**
                 * This function executes at beginning of subscription.
                 * We want to replay history with the subscribing thread
                 * before the Observer gets registered.
                 * 
                 * This will always run, even if Subject is in terminal state.
                 */
                new Action1<SubjectObserver<? super T>>() {

                    @Override
                    public void call(SubjectObserver<? super T> o) {
                        // replay history for this observer using the subscribing thread
                        int lastIndex = replayObserverFromIndex(state.history, 0, o);

                        // now that it is caught up add to observers
                        state.replayState.put(o, lastIndex);
                    }
                },
                /**
                 * This function executes if the Subject is terminated.
                 */
                new Action1<SubjectObserver<? super T>>() {

                    @Override
                    public void call(SubjectObserver<? super T> o) {
                        // we will finish replaying if there is anything left
                        replayObserverFromIndex(state.history, state.replayState.get(o), o);
                    }
                });

        return new ReplaySubject<T>(onSubscribe, subscriptionManager, state);
    }

    private static class ReplayState<T> {
        // single-producer, multi-consumer
        final History<T> history;
        // each Observer is tracked here for what events they have received
        final ConcurrentHashMap<Observer<? super T>, Integer> replayState;
        public ReplayState(int capacity) {
            history = new History<T>(capacity);
            replayState = new ConcurrentHashMap<Observer<? super T>, Integer>();
        }
    }

    private final SubjectSubscriptionManager<T> subscriptionManager;
    private final ReplayState<T> state;

    protected ReplaySubject(OnSubscribeFunc<T> onSubscribe, SubjectSubscriptionManager<T> subscriptionManager, ReplayState<T> state) {
        super(onSubscribe);
        this.subscriptionManager = subscriptionManager;
        this.state = state;
    }

    @Override
    public void onCompleted() {
        subscriptionManager.terminate(new Action1<Collection<SubjectObserver<? super T>>>() {

            @Override
            public void call(Collection<SubjectObserver<? super T>> observers) {
                state.history.complete(new Notification<T>());
                for (SubjectObserver<? super T> o : observers) {
                    if (caughtUp(o)) {
                        o.onCompleted();
                    }
                }
            }
        });
    }

    @Override
    public void onError(final Throwable e) {
        subscriptionManager.terminate(new Action1<Collection<SubjectObserver<? super T>>>() {

            @Override
            public void call(Collection<SubjectObserver<? super T>> observers) {
                state.history.complete(new Notification<T>(e));
                for (SubjectObserver<? super T> o : observers) {
                    if (caughtUp(o)) {
                        o.onError(e);
                    }
                }
            }
        });
    }

    @Override
    public void onNext(T v) {
        if (state.history.terminalValue.get() != null) {
            return;
        }
        state.history.next(v);
        for (SubjectObserver<? super T> o : subscriptionManager.rawSnapshot()) {
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
        if (lastEmittedLink != null) {
            int l = replayObserverFromIndex(state.history, lastEmittedLink, observer);
            state.replayState.put(observer, l);
        } else {
            throw new IllegalStateException("failed to find lastEmittedLink for: " + observer);
        }
    }

    private static <T> int replayObserverFromIndex(History<T> history, Integer l, SubjectObserver<? super T> observer) {
        while (l < history.index.get()) {
            observer.onNext(history.list.get(l));
            l++;
        }
        if (history.terminalValue.get() != null) {
            history.terminalValue.get().accept(observer);
        }

        return l;
    }

    /**
     * NOT thread-safe for multi-writer. Assumes single-writer.
     * Is thread-safe for multi-reader.
     * 
     * @param <T>
     */
    private static class History<T> {
        private final AtomicInteger index;
        private final ArrayList<T> list;
        private final AtomicReference<Notification<T>> terminalValue;
        private final int capacity;
        public History(int capacity) {
            this.capacity = capacity;
            index = new AtomicInteger(0);
            list = this.capacity == ReplaySubjectUnlimitedCapacity ? new ArrayList<T>(16) : new ArrayList<T>(capacity);
            terminalValue = new AtomicReference<Notification<T>>();
        }

        public boolean next(T n) {
            if (terminalValue.get() == null) {
                list.add(n);
                trim();
                return true;
            } else {
                return false;
            }
        }

        private void trim() {
            if (this.capacity != ReplaySubjectUnlimitedCapacity && list.size() > this.capacity ) {
                List<T> toRemove = list.subList(0, list.size() - this.capacity);
                list.removeAll(toRemove);
            }

            index.set(list.size());
        }

        public void complete(Notification<T> n) {
            terminalValue.set(n);
        }
    }

}
