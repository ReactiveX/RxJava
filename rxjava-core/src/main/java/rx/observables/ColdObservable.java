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
import rx.subscriptions.CompositeSubscription;
import rx.subscriptions.Subscriptions;
import rx.util.Recorded;
import rx.util.RecordedSubscription;
import rx.util.functions.Func2;

/**
 * An observable which replays notification in a timed fashion by
 * using a TestScheduler, whenever an Observer subscribes.
 */
public final class ColdObservable<T> extends TestableObservable<T> {
    /**
     * Create a ColdObservable instance with the given TestScheduler and recorded messages.
     * @param <T> the value type
     * @param scheduler the test scheduler
     * @param messages the recorded messages
     * @return the created ColdObservable
     */
    public static <T> ColdObservable<T> create(TestScheduler scheduler, Recorded<Notification<T>>... messages) {
        ColdState<T> state = new ColdState<T>(scheduler, Arrays.asList(messages));
        return new ColdObservable<T>(new ColdObservableSubscribe<T>(state), state);
    }
    
    /**
     * Create a ColdObservable instance with the given TestScheduler and recorded messages.
     * @param <T> the value type
     * @param scheduler the test scheduler
     * @param messages the recorded messages
     * @return the created ColdObservable
     */
    public static <T> ColdObservable<T> create(TestScheduler scheduler, Iterable<? extends Recorded<Notification<T>>> messages) {
        List<Recorded<Notification<T>>> messagesList = new ArrayList<Recorded<Notification<T>>>();
        for (Recorded<Notification<T>> r : messages) {
            messagesList.add(r);
        }
        ColdState<T> state = new ColdState<T>(scheduler, messagesList);
        return new ColdObservable<T>(new ColdObservableSubscribe<T>(state), state);
    }
    
    /** The Observable's and Subscriber's state. */
    static class ColdState<T> {
        final TestScheduler scheduler;
        final List<RecordedSubscription> subscriptions = new ArrayList<RecordedSubscription>();
        final List<Recorded<Notification<T>>> messages;
        /** The latest time among the notifications. */
        long maxTime = Long.MIN_VALUE;
        public ColdState(TestScheduler scheduler, List<Recorded<Notification<T>>> messages) {
            this.scheduler = scheduler;
            this.messages = messages;
            for (Recorded<?> r : messages) {
                maxTime = Math.max(maxTime, r.time());
            }
        }
    }
    final ColdState<T> state;

    private ColdObservable(OnSubscribeFunc<T> onSubscribe, ColdState<T> state) {
        super(onSubscribe);
        this.state = state;
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
        state.scheduler.advanceTimeBy(state.maxTime, TimeUnit.MILLISECONDS);
    }
    
    /** The subscription function which replays the notifications. */
    static final class ColdObservableSubscribe<T> implements OnSubscribeFunc<T> {
        final ColdState<T> state;

        public ColdObservableSubscribe(ColdState<T> state) {
            this.state = state;
        }

        @Override
        public Subscription onSubscribe(Observer<? super T> t1) {
            int index = state.subscriptions.size();
            state.subscriptions.add(new RecordedSubscription(state.scheduler.now()));
            
            CompositeSubscription csub = new CompositeSubscription();
            
            for (Recorded<Notification<T>> rn : state.messages) {
                csub.add(state.scheduler.scheduleRelative(t1, rn.time(), new EmitMessage<T>(rn.value(), t1)));
            }
            return new RemoveSubscription<T>(state, index, csub);
        }
        
        /** Emits a notification to all currently registered observers of the state object. */
        private static final class EmitMessage<T> implements Func2<Scheduler, Object, Subscription> {
            final Notification<T> notification;
            final Observer<? super T> observer;
            public EmitMessage(Notification<T> notification, Observer<? super T> observer) {
                this.notification = notification;
                this.observer = observer;
            }

            @Override
            public Subscription call(Scheduler scheduler, Object o) {
                notification.accept(observer);
                return Subscriptions.empty();
            }
        }
        /** 
         * Remove the observer from the state. 
         * TODO FIXME extend BooleanSubscription...
         */
        static final class RemoveSubscription<T> implements Subscription {
            final ColdState<T> state;
            final int index;
            final Subscription cancel;
            final AtomicBoolean unsubscribed = new AtomicBoolean();

            public RemoveSubscription(ColdState<T> state, int index, Subscription cancel) {
                this.state = state;
                this.index = index;
                this.cancel = cancel;
            }

            @Override
            public void unsubscribe() {
                if (unsubscribed.compareAndSet(false, true)) {
                    RecordedSubscription rs = state.subscriptions.get(index);
                    state.subscriptions.set(index, new RecordedSubscription(rs.subscribed(), state.scheduler.now()));

                    cancel.unsubscribe();
                }
            }
        }
    }
}
