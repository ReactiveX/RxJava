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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import rx.Notification;
import rx.Observer;
import rx.Scheduler;
import rx.Subscription;
import rx.schedulers.TestScheduler;
import rx.subscriptions.Subscriptions;
import rx.util.Recorded;
import rx.util.RecordedSubscription;
import rx.util.functions.Func2;
import rx.observables.ColdObservable.ColdState;

/**
 * An observable which replays notifications in a timed fashion by using a
 * TestScheduler, immediately on creation.
 * @param <T> the value type
 */
public class HotObservable<T> extends TestableObservable<T> {
    /** The observer's state shared with the subscriber function. */
    static final class HotState<T> extends ColdState<T> {
        final List<Observer<? super T>> observers = new ArrayList<Observer<? super T>>();

        public HotState(TestScheduler scheduler, List<Recorded<Notification<T>>> messages) {
            super (scheduler, messages);
        }
        
    }
    
    /**
     * Create a HotObservable instance with the given TestScheduler and recorded messages.
     * @param <T> the value type
     * @param scheduler the test scheduler
     * @param messages the recorded messages
     * @return the created HotObservable
     */
    public static <T> HotObservable<T> create(TestScheduler scheduler, Recorded<Notification<T>>... messages) {
        HotState<T> state = new HotState<T>(scheduler, Arrays.asList(messages));
        return new HotObservable<T>(new HotObservableSubscribe<T>(state), state);
    }
    
    /**
     * Create a HotObservable instance with the given TestScheduler and recorded messages.
     * @param <T> the value type
     * @param scheduler the test scheduler
     * @param messages the recorded messages
     * @return the created HotObservable
     */
    public static <T> HotObservable<T> create(TestScheduler scheduler, Iterable<? extends Recorded<Notification<T>>> messages) {
        List<Recorded<Notification<T>>> messagesList = new ArrayList<Recorded<Notification<T>>>();
        for (Recorded<Notification<T>> r : messages) {
            messagesList.add(r);
        }
        HotState<T> state = new HotState<T>(scheduler, messagesList);
        return new HotObservable<T>(new HotObservableSubscribe<T>(state), state);
    }
    
    final HotState<T> state;
    private HotObservable(OnSubscribeFunc<T> onSubscribeFunc, HotState<T> state) {
        super(onSubscribeFunc);
        this.state = state;
        
        for (Recorded<Notification<T>> message : state.messages) {
            state.scheduler.scheduleAbsolute(state, 
                    message.time(), new EmitMessage<T>(message.value()));
        }
    }
    /** Emits a notification to all currently registered observers of the state object. */
    private static final class EmitMessage<T> implements Func2<Scheduler, HotState<T>, Subscription> {
        final Notification<T> notification;
        public EmitMessage(Notification<T> notification) {
            this.notification = notification;
        }

        @Override
        public Subscription call(Scheduler scheduler, HotState<T> state) {
            List<Observer<? super T>> list = new ArrayList<Observer<? super T>>(state.observers);
            for (Observer<? super T> o : list) {
                notification.accept(o);
            }
            return Subscriptions.empty();
        }
    }

    @Override
    public List<Recorded<Notification<T>>> messages() {
        return state.messages;
    }

    @Override
    public List<RecordedSubscription> subscriptions() {
        return state.subscriptions;
    }

    @Override
    public void runTo(long time) {
        state.scheduler.advanceTimeTo(time, TimeUnit.MILLISECONDS);
    }

    @Override
    public void runToEnd() {
        state.scheduler.advanceTimeTo(state.maxTime, TimeUnit.MILLISECONDS);
    }
    
    /** The subscription function of the HotObservable. */
    static final class HotObservableSubscribe<T> implements OnSubscribeFunc<T> {
        final HotState<T> state;
        public HotObservableSubscribe(HotState<T> state) {
            this.state = state;
        }

        @Override
        public Subscription onSubscribe(Observer<? super T> t1) {
            state.observers.add(t1);
            int index = state.subscriptions.size();
            state.subscriptions.add(new RecordedSubscription(state.scheduler.now()));
            return new RemoveSubscription<T>(state, t1, index);
        }
    }
    
    /** 
     * Remove the observer from the state. 
     * TODO FIXME extend BooleanSubscription...
     */
    static final class RemoveSubscription<T> implements Subscription {
        final HotState<T> state;
        final Observer<? super T> observer;
        final int index;
        final AtomicBoolean unsubscribed = new AtomicBoolean();

        public RemoveSubscription(HotState<T> state, Observer<? super T> observer, int index) {
            this.state = state;
            this.observer = observer;
            this.index = index;
        }

        @Override
        public void unsubscribe() {
            if (unsubscribed.compareAndSet(false, true)) {
                state.observers.remove(observer);
                RecordedSubscription rs = state.subscriptions.get(index);
                state.subscriptions.set(index, new RecordedSubscription(rs.subscribed(), state.scheduler.now()));
            }
        }
    }
}
