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
package rx.operators;

import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicReference;

import rx.Observable;
import rx.Observer;
import rx.Subscriber;

/**
 * A solution to the "time gap" problem that occurs with `groupBy` and `pivot` => https://github.com/Netflix/RxJava/issues/844
 * 
 * This currently has temporary unbounded buffers. It needs to become bounded and then do one of two things:
 * 
 * 1) blow up and make the user do something about it
 * 2) work with the backpressure solution ... still to be implemented (such as co-routines)
 * 
 * Generally the buffer should be very short lived (milliseconds) and then stops being involved.
 * It can become a memory leak though if a GroupedObservable backed by this class is emitted but never subscribed to (such as filtered out).
 * In that case, either a time-bomb to throw away the buffer, or just blowing up and making the user do something about it is needed.
 * 
 * For example, to filter out GroupedObservables, perhaps they need a silent `subscribe()` on them to just blackhole the data.
 * 
 * This is an initial start at solving this problem and solves the immediate problem of `groupBy` and `pivot` and trades off the possibility of memory leak for deterministic functionality.
 *
 * @param <T>
 */
public class BufferUntilSubscriber<T> extends Observable<T> implements Observer<T> {

    public static <T> BufferUntilSubscriber<T> create() {
        return new BufferUntilSubscriber<T>(new AtomicReference<Observer<? super T>>(new BufferedObserver<T>()));
    }

    private final AtomicReference<Observer<? super T>> observerRef;

    private BufferUntilSubscriber(final AtomicReference<Observer<? super T>> observerRef) {
        super(new OnSubscribe<T>() {

            @Override
            public void call(Subscriber<? super T> s) {
                // drain queued notifications before subscription
                // we do this here before PassThruObserver so the consuming thread can do this before putting itself in the line of the producer
                BufferedObserver<T> buffered = (BufferedObserver<T>) observerRef.get();
                Object o = null;
                while ((o = buffered.buffer.poll()) != null) {
                    emit(s, o);
                }
                // register real observer for pass-thru ... and drain any further events received on first notification
                observerRef.set(new PassThruObserver<T>(s, buffered.buffer, observerRef));
            }

        });
        this.observerRef = observerRef;
    }

    @Override
    public void onCompleted() {
        observerRef.get().onCompleted();
    }

    @Override
    public void onError(Throwable e) {
        observerRef.get().onError(e);
    }

    @Override
    public void onNext(T t) {
        observerRef.get().onNext(t);
    }

    /**
     * This is a temporary observer between buffering and the actual that gets into the line of notifications
     * from the producer and will drain the queue of any items received during the race of the initial drain and
     * switching this.
     * 
     * It will then immediately swap itself out for the actual (after a single notification), but since this is now
     * being done on the same producer thread no further buffering will occur.
     */
    private static class PassThruObserver<T> implements Observer<T> {

        private final Observer<? super T> actual;
        // this assumes single threaded synchronous notifications (the Rx contract for a single Observer)
        private final ConcurrentLinkedQueue<Object> buffer;
        private final AtomicReference<Observer<? super T>> observerRef;

        PassThruObserver(Observer<? super T> actual, ConcurrentLinkedQueue<Object> buffer, AtomicReference<Observer<? super T>> observerRef) {
            this.actual = actual;
            this.buffer = buffer;
            this.observerRef = observerRef;
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
            Object o = null;
            while ((o = buffer.poll()) != null) {
                emit(this, o);
            }
            // now we can safely change over to the actual and get rid of the pass-thru
            observerRef.set(actual);
        }

    }

    private static class BufferedObserver<T> implements Observer<T> {
        private final ConcurrentLinkedQueue<Object> buffer = new ConcurrentLinkedQueue<Object>();

        @Override
        public void onCompleted() {
            buffer.add(COMPLETE_SENTINEL);
        }

        @Override
        public void onError(Throwable e) {
            buffer.add(new ErrorSentinel(e));
        }

        @Override
        public void onNext(T t) {
            if (t == null) {
                buffer.add(NULL_SENTINEL);
            } else {
                buffer.add(t);
            }
        }

    }

    private final static <T> void emit(Observer<T> s, Object v) {
        if (v instanceof Sentinel) {
            if (v == NULL_SENTINEL) {
                s.onNext(null);
            } else if (v == COMPLETE_SENTINEL) {
                s.onCompleted();
            } else if (v instanceof ErrorSentinel) {
                s.onError(((ErrorSentinel) v).e);
            }
        } else {
            s.onNext((T) v);
        }
    }

    private static class Sentinel {

    }

    private static Sentinel NULL_SENTINEL = new Sentinel();
    private static Sentinel COMPLETE_SENTINEL = new Sentinel();

    private static class ErrorSentinel extends Sentinel {
        final Throwable e;

        ErrorSentinel(Throwable e) {
            this.e = e;
        }
    }

}
