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
package rx.subjects;


import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import rx.Notification;
import rx.Observer;
import rx.Subscription;
import rx.subjects.AbstractSubject.BaseState;
import rx.subjects.ReplaySubject.State.Replayer;
import rx.subscriptions.Subscriptions;

/**
 * Subject that retains all events and will replay them to an {@link Observer} that subscribes.
 * <p>
 * <img src="https://raw.github.com/wiki/Netflix/RxJava/images/rx-operators/S.ReplaySubject.png">
 * <p>
 * Example usage:
 * <p>
 * <pre> {@code

 * eplaySubject<Object> subject = ReplaySubject.create();
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
    /** The state class. */
    protected static final class State<T> extends BaseState {
        /** The values observed so far. */
        protected final List<Notification<T>> values = Collections.synchronizedList(new ArrayList<Notification<T>>());
        /** General completion indicator. */
        protected boolean done;
        /** The map of replayers. */
        protected final Map<Subscription, Replayer> replayers = new LinkedHashMap<Subscription, Replayer>();
        /**
         * Returns a live collection of the observers.
         * <p>
         * Caller should hold the lock.
         * @return 
         */
        protected Collection<Replayer> replayers() {
            return new ArrayList<Replayer>(replayers.values());
        }
        /** 
         * Add a replayer to the replayers and create a Subscription for it.
         * <p>
         * Caller should hold the lock.
         * 
         * @param obs
         * @return 
         */
        protected Subscription addReplayer(Observer<? super T> obs) {
            Subscription s = new Subscription() {
                final AtomicBoolean once = new AtomicBoolean();
                @Override
                public void unsubscribe() {
                    if (once.compareAndSet(false, true)) {
                        remove(this);
                    }
                }
                
            };
            Replayer rp = new Replayer(obs, s);
            replayers.put(s, rp);
            rp.replayTill(values.size());
            return s;
        }
        /** The replayer that holds a value where the given observer is currently at. */
        protected final class Replayer {
            protected final Observer<? super T> wrapped;
            protected int index;
            protected final Subscription cancel;
            protected Replayer(Observer<? super T> wrapped, Subscription cancel) {
                this.wrapped = wrapped;
                this.cancel = cancel;
            }
            /** 
             * Replay up to the given index
             * @param limit
             */
            protected void replayTill(int limit) {
                while (index < limit) {
                    Notification<T> not = values.get(index);
                    index++;
                    if (!not.acceptSafe(wrapped)) {
                        replayers.remove(cancel);
                        return;
                    }
                }
            }
        }
        /** 
         * Remove the subscription. 
         * @param s
         */
        protected void remove(Subscription s) {
            lock();
            try {
                replayers.remove(s);
            } finally {
                unlock();
            }
        }
    }
    /**
     * Return a subject that retains all events and will replay them to an {@link Observer} that subscribes.
     * @return a subject that retains all events and will replay them to an {@link Observer} that subscribes.
     */
    public static <T> ReplaySubject<T> create() {
        State<T> state = new State<T>();
        return new ReplaySubject<T>(new ReplaySubjectSubscribeFunc<T>(state), state);
    }
    private final State<T> state;
    private ReplaySubject(OnSubscribeFunc<T> onSubscribe, State<T> state) {
        super(onSubscribe);
        this.state = state;
    }


    @Override
    public void onCompleted() {
        state.lock();
        try {
            if (state.done) {
                return;
            }
            state.done = true;
            state.values.add(new Notification<T>());
            replayValues();
        } finally {
            state.unlock();
        }
    }

    @Override
    public void onError(Throwable e) {
        state.lock();
        try {
            if (state.done) {
                return;
            }
            state.done = true;
            state.values.add(new Notification<T>(e));
            replayValues();
        } finally {
            state.unlock();
        }
    }

    @Override
    public void onNext(T args) {
        state.lock();
        try {
            if (state.done) {
                return;
            }
            state.values.add(new Notification<T>(args));
            replayValues();
        } finally {
            state.unlock();
        }
    }
    /**
     * Replay values up to the current index.
     */
    protected void replayValues() {
        int s = state.values.size();
        for (Replayer rp : state.replayers()) {
            rp.replayTill(s);
        }
    }
    /** The subscription function. */
    protected static final class ReplaySubjectSubscribeFunc<T> implements OnSubscribeFunc<T> {
        
        private final State<T> state;
        protected ReplaySubjectSubscribeFunc(State<T> state) {
            this.state = state;
        }

        @Override
        public Subscription onSubscribe(Observer<? super T> t1) {
            List<Notification<T>> values;
            
            state.lock();
            try {
                values = state.values;
                if (!state.done) {
                    return state.addReplayer(t1);
                }
            } finally {
                state.unlock();
            }
            
            for (Notification<T> v : values) {
                if (!v.acceptSafe(t1)) {
                    break;
                }
            }
            return Subscriptions.empty();
        }
    }
}
