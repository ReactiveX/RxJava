/**
 * Copyright 2016 Netflix, Inc.
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

package rx.internal.operators;

import java.util.concurrent.atomic.AtomicInteger;

import rx.Observable.Operator;
import rx.exceptions.Exceptions;
import rx.Producer;
import rx.Subscriber;

public class OperatorTakeLastOne<T> implements Operator<T, T> {

    private static class Holder {
        static final OperatorTakeLastOne<Object> INSTANCE = new OperatorTakeLastOne<Object>();
    }

    @SuppressWarnings("unchecked")
    public static <T> OperatorTakeLastOne<T> instance() {
        return (OperatorTakeLastOne<T>) Holder.INSTANCE;
    }

    OperatorTakeLastOne() {

    }

    @Override
    public Subscriber<? super T> call(Subscriber<? super T> child) {
        final ParentSubscriber<T> parent = new ParentSubscriber<T>(child);
        child.setProducer(new Producer() {

            @Override
            public void request(long n) {
                parent.requestMore(n);
            }
        });
        child.add(parent);
        return parent;
    }

    private static class ParentSubscriber<T> extends Subscriber<T> {

        private final static int NOT_REQUESTED_NOT_COMPLETED = 0;
        private final static int NOT_REQUESTED_COMPLETED = 1;
        private final static int REQUESTED_NOT_COMPLETED = 2;
        private final static int REQUESTED_COMPLETED = 3;

        /*
         * These are the expected state transitions:
         * 
         * NOT_REQUESTED_NOT_COMPLETED   -->   REQUESTED_NOT_COMPLETED 
         *            |                             | 
         *            V                             V
         * NOT_REQUESTED_COMPLETED       -->   REQUESTED_COMPLETED
         * 
         * Once at REQUESTED_COMPLETED we emit the last value if one exists
         */

        // Used as the initial value of last
        private static final Object ABSENT = new Object();

        // the downstream subscriber
        private final Subscriber<? super T> child;

        @SuppressWarnings("unchecked")
        // we can get away with this cast at runtime because of type erasure
        private T last = (T) ABSENT;

        // holds the current state of the stream so that we can make atomic
        // updates to it
        private final AtomicInteger state = new AtomicInteger(NOT_REQUESTED_NOT_COMPLETED);

        ParentSubscriber(Subscriber<? super T> child) {
            this.child = child;
        }
        
        void requestMore(long n) {
            if (n > 0) {
                // CAS loop to atomically change state given that onCompleted()
                // or another requestMore() may be acting concurrently
                while (true) {
                    // read the value of state and then try state transitions
                    // only if the value of state does not change in the
                    // meantime (in another requestMore() or onCompleted()). If
                    // the value has changed and we expect to do a transition
                    // still then we loop and try again.
                    final int s = state.get();
                    if (s == NOT_REQUESTED_NOT_COMPLETED) {
                        if (state.compareAndSet(NOT_REQUESTED_NOT_COMPLETED,
                                REQUESTED_NOT_COMPLETED)) {
                            return;
                        }
                    } else if (s == NOT_REQUESTED_COMPLETED) {
                        if (state.compareAndSet(NOT_REQUESTED_COMPLETED, REQUESTED_COMPLETED)) {
                            emit();
                            return;
                        }
                    } else
                        // already requested so we exit
                        return;
                }
            }
        }

        @Override
        public void onCompleted() {
            //shortcut if an empty stream
            if (last == ABSENT) {
                child.onCompleted();
                return;
            }
            // CAS loop to atomically change state given that requestMore()
            // may be acting concurrently
            while (true) {
                // read the value of state and then try state transitions
                // only if the value of state does not change in the meantime
                // (in another requestMore()). If the value has changed and
                // we expect to do a transition still then we loop and try
                // again.
                final int s = state.get();
                if (s == NOT_REQUESTED_NOT_COMPLETED) {
                    if (state.compareAndSet(NOT_REQUESTED_NOT_COMPLETED, NOT_REQUESTED_COMPLETED)) {
                        return;
                    }
                } else if (s == REQUESTED_NOT_COMPLETED) {
                    if (state.compareAndSet(REQUESTED_NOT_COMPLETED, REQUESTED_COMPLETED)) {
                        emit();
                        return;
                    }
                } else
                    // already completed so we exit
                    return;
            }
        }

        /**
         * If not unsubscribed then emits last value and completed to the child
         * subscriber.
         */
        private void emit() {
            if (isUnsubscribed()) {
                // release for gc
                last = null;
                return;
            }
            // Note that last is safely published despite not being volatile
            // because a CAS update must have happened in the current thread just before
            // emit() was called
            T t = last;
            // release for gc
            last = null;
            if (t != ABSENT) {
                try {
                    child.onNext(t);
                } catch (Throwable e) {
                    Exceptions.throwOrReport(e, child);
                    return;
                }
            }
            if (!isUnsubscribed())
                child.onCompleted();
        }

        @Override
        public void onError(Throwable e) {
            child.onError(e);
        }

        @Override
        public void onNext(T t) {
            last = t;
        }

    }

}
