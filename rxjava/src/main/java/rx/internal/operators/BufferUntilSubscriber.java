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
package rx.internal.operators;

import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicIntegerFieldUpdater;
import java.util.concurrent.atomic.AtomicReferenceFieldUpdater;

import rx.Observer;
import rx.Subscriber;
import rx.functions.Action0;
import rx.observers.Subscribers;
import rx.subjects.Subject;
import rx.subscriptions.Subscriptions;

/**
 * A solution to the "time gap" problem that occurs with {@code groupBy} and {@code pivot}.
 * <p>
 * This currently has temporary unbounded buffers. It needs to become bounded and then do one of two things:
 * <ol>
 * <li>blow up and make the user do something about it</li>
 * <li>work with the backpressure solution ... still to be implemented (such as co-routines)</li>
 * </ol><p>
 * Generally the buffer should be very short lived (milliseconds) and then stops being involved. It can become a
 * memory leak though if a {@code GroupedObservable} backed by this class is emitted but never subscribed to
 * (such as filtered out). In that case, either a time-bomb to throw away the buffer, or just blowing up and
 * making the user do something about it is needed.
 * <p>
 * For example, to filter out {@code GroupedObservable}s, perhaps they need a silent {@code subscribe()} on them
 * to just blackhole the data.
 * <p>
 * This is an initial start at solving this problem and solves the immediate problem of {@code groupBy} and
 * {@code pivot} and trades off the possibility of memory leak for deterministic functionality.
 *
 * @see <a href="https://github.com/Netflix/RxJava/issues/844">the Github issue describing the time gap problem</a>
 * @param <T>
 *            the type of the items to be buffered
 */
public class BufferUntilSubscriber<T> extends Subject<T, T> {

    /**
     * @warn create() undescribed
     * @return
     */
    public static <T> BufferUntilSubscriber<T> create() {
        State<T> state = new State<T>();
        return new BufferUntilSubscriber<T>(state);
    }

    /** The common state. */
    static final class State<T> {
        /** The first observer or the one which buffers until the first arrives. */
        volatile Observer<? super T> observerRef = new BufferedObserver<T>();
        /** Allow a single subscriber only. */
        volatile int first;
        /** Field updater for observerRef. */
        @SuppressWarnings("rawtypes")
        static final AtomicReferenceFieldUpdater<State, Observer> OBSERVER_UPDATER
                = AtomicReferenceFieldUpdater.newUpdater(State.class, Observer.class, "observerRef");
        /** Field updater for first. */
        @SuppressWarnings("rawtypes")
        static final AtomicIntegerFieldUpdater<State> FIRST_UPDATER
                = AtomicIntegerFieldUpdater.newUpdater(State.class, "first");
        
        boolean casFirst(int expected, int next) {
            return FIRST_UPDATER.compareAndSet(this, expected, next);
        }
        void setObserverRef(Observer<? super T> o) {
            observerRef = o;
        }
    }
    
    static final class OnSubscribeAction<T> implements OnSubscribe<T> {
        final State<T> state;

        public OnSubscribeAction(State<T> state) {
            this.state = state;
        }

        @Override
        public void call(final Subscriber<? super T> s) {
            if (state.casFirst(0, 1)) {
                final NotificationLite<T> nl = NotificationLite.instance();
                // drain queued notifications before subscription
                // we do this here before PassThruObserver so the consuming thread can do this before putting itself in the line of the producer
                BufferedObserver<? super T> buffered = (BufferedObserver<? super T>)state.observerRef;
                Object o;
                while ((o = buffered.buffer.poll()) != null) {
                    nl.accept(s, o);
                }
                // register real observer for pass-thru ... and drain any further events received on first notification
                state.setObserverRef(new PassThruObserver<T>(s, buffered.buffer, state));
                s.add(Subscriptions.create(new Action0() {
                    @Override
                    public void call() {
                        state.setObserverRef(Subscribers.empty());
                    }
                }));
            } else {
                s.onError(new IllegalStateException("Only one subscriber allowed!"));
            }
        }
        
    }
    final State<T> state;
    
    private BufferUntilSubscriber(State<T> state) {
        super(new OnSubscribeAction<T>(state));
        this.state = state;
    }

    @Override
    public void onCompleted() {
        state.observerRef.onCompleted();
    }

    @Override
    public void onError(Throwable e) {
        state.observerRef.onError(e);
    }

    @Override
    public void onNext(T t) {
        state.observerRef.onNext(t);
    }

    /**
     * This is a temporary observer between buffering and the actual that gets into the line of notifications
     * from the producer and will drain the queue of any items received during the race of the initial drain and
     * switching this.
     * 
     * It will then immediately swap itself out for the actual (after a single notification), but since this is
     * now being done on the same producer thread no further buffering will occur.
     */
    private static final class PassThruObserver<T> extends Subscriber<T> {

        private final Observer<? super T> actual;
        // this assumes single threaded synchronous notifications (the Rx contract for a single Observer)
        private final ConcurrentLinkedQueue<Object> buffer;
        private final State<T> state;

        PassThruObserver(Observer<? super T> actual, ConcurrentLinkedQueue<Object> buffer, 
                State<T> state) {
            this.actual = actual;
            this.buffer = buffer;
            this.state = state;
        }

        @Override
        public void onCompleted() {
            drainIfNeededAndSwitchToActual();
            actual.onCompleted();
        }

        @Override
        public void onError(Throwable e) {
            drainIfNeededAndSwitchToActual();
            actual.onError(e);
        }

        @Override
        public void onNext(T t) {
            drainIfNeededAndSwitchToActual();
            actual.onNext(t);
        }

        private void drainIfNeededAndSwitchToActual() {
            final NotificationLite<T> nl = NotificationLite.instance();
            Object o;
            while ((o = buffer.poll()) != null) {
                nl.accept(this, o);
            }
            // now we can safely change over to the actual and get rid of the pass-thru
            // but only if not unsubscribed
            state.setObserverRef(actual);
        }

    }

    private static final class BufferedObserver<T> extends Subscriber<T> {
        private final ConcurrentLinkedQueue<Object> buffer = new ConcurrentLinkedQueue<Object>();
        private static final NotificationLite<Object> nl = NotificationLite.instance();

        @Override
        public void onCompleted() {
            buffer.add(nl.completed());
        }

        @Override
        public void onError(Throwable e) {
            buffer.add(nl.error(e));
        }

        @Override
        public void onNext(T t) {
            buffer.add(nl.next(t));
        }

    }
}
