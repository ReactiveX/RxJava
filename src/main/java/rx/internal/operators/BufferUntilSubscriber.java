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
import java.util.concurrent.atomic.AtomicReference;

import rx.Observer;
import rx.Subscriber;
import rx.functions.Action0;
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
 * @see <a href="https://github.com/ReactiveX/RxJava/issues/844">the Github issue describing the time gap problem</a>
 * @param <T>
 *            the type of the items to be buffered
 */
public final class BufferUntilSubscriber<T> extends Subject<T, T> {

    /**
     * Creates a default, unbounded buffering Subject instance.
     * @param <T> the value type
     * @return the instance
     */
    public static <T> BufferUntilSubscriber<T> create() {
        State<T> state = new State<T>();
        return new BufferUntilSubscriber<T>(state);
    }

    /** The common state. */
    static final class State<T> extends AtomicReference<Observer<? super T>> {
        /** */
        private static final long serialVersionUID = 8026705089538090368L;
        boolean casObserverRef(Observer<? super T>  expected, Observer<? super T>  next) {
            return compareAndSet(expected, next);
        }

        final Object guard = new Object();
        /* protected by guard */
        boolean emitting = false;

        final ConcurrentLinkedQueue<Object> buffer = new ConcurrentLinkedQueue<Object>();
        final NotificationLite<T> nl = NotificationLite.instance();
    }
    
    static final class OnSubscribeAction<T> implements OnSubscribe<T> {
        final State<T> state;

        public OnSubscribeAction(State<T> state) {
            this.state = state;
        }

        @Override
        public void call(final Subscriber<? super T> s) {
            if (state.casObserverRef(null, s)) {
                s.add(Subscriptions.create(new Action0() {
                    @SuppressWarnings("unchecked")
                    @Override
                    public void call() {
                        state.set(EMPTY_OBSERVER);
                    }
                }));
                boolean win = false;
                synchronized (state.guard) {
                    if (!state.emitting) {
                        state.emitting = true;
                        win = true;
                    }
                }
                if (win) {
                    final NotificationLite<T> nl = NotificationLite.instance();
                    while(true) {
                        Object o;
                        while ((o = state.buffer.poll()) != null) {
                            nl.accept(state.get(), o);
                        }
                        synchronized (state.guard) {
                            if (state.buffer.isEmpty()) {
                                // Although the buffer is empty, there is still a chance
                                // that further events may be put into the `buffer`.
                                // `emit(Object v)` should handle it.
                                state.emitting = false;
                                break;
                            }
                        }
                    }
                }
            } else {
                s.onError(new IllegalStateException("Only one subscriber allowed!"));
            }
        }
        
    }
    final State<T> state;

    private boolean forward = false;

    private BufferUntilSubscriber(State<T> state) {
        super(new OnSubscribeAction<T>(state));
        this.state = state;
    }

    private void emit(Object v) {
        synchronized (state.guard) {
            state.buffer.add(v);
            if (state.get() != null && !state.emitting) {
                // Have an observer and nobody is emitting,
                // should drain the `buffer`
                forward = true;
                state.emitting = true;
            }
        }
        if (forward) {
            Object o;
            while ((o = state.buffer.poll()) != null) {
                state.nl.accept(state.get(), o);
            }
            // Because `emit(Object v)` will be called in sequence,
            // no event will be put into `buffer` after we drain it.
        }
    }

    @Override
    public void onCompleted() {
        if (forward) {
            state.get().onCompleted();
        }
        else {
            emit(state.nl.completed());
        }
    }

    @Override
    public void onError(Throwable e) {
        if (forward) {
            state.get().onError(e);
        }
        else {
            emit(state.nl.error(e));
        }
    }

    @Override
    public void onNext(T t) {
        if (forward) {
            state.get().onNext(t);
        }
        else {
            emit(state.nl.next(t));
        }
    }

    @Override
    public boolean hasObservers() {
        synchronized (state.guard) {
            return state.get() != null;
        }
    }

    @SuppressWarnings("rawtypes")
    final static Observer EMPTY_OBSERVER = new Observer() {

        @Override
        public void onCompleted() {
            
        }

        @Override
        public void onError(Throwable e) {
            
        }

        @Override
        public void onNext(Object t) {
            
        }
        
    };
    
}
