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

import java.util.*;
import java.util.concurrent.atomic.*;

import rx.*;
import rx.Observer;
import rx.exceptions.*;
import rx.internal.operators.BackpressureUtils;

/**
 * Subject that, once an {@link Observer} has subscribed, emits all subsequently observed items to the
 * subscriber.
 * <p>
 * <img width="640" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/S.PublishSubject.png" alt="">
 * <p>
 * Example usage:
 * <p>
 * <pre> {@code

  PublishSubject<Object> subject = PublishSubject.create();
  // observer1 will receive all onNext and onCompleted events
  subject.subscribe(observer1);
  subject.onNext("one");
  subject.onNext("two");
  // observer2 will only receive "three" and onCompleted
  subject.subscribe(observer2);
  subject.onNext("three");
  subject.onCompleted();

  } </pre>
 *
 * @param <T>
 *          the type of items observed and emitted by the Subject
 */
public final class PublishSubject<T> extends Subject<T, T> {

    final PublishSubjectState<T> state;

    /**
     * Creates and returns a new {@code PublishSubject}.
     *
     * @param <T> the value type
     * @return the new {@code PublishSubject}
     */
    public static <T> PublishSubject<T> create() {
        return new PublishSubject<T>(new PublishSubjectState<T>());
    }

    protected PublishSubject(PublishSubjectState<T> state) {
        super(state);
        this.state = state;
    }

    @Override
    public void onNext(T v) {
        state.onNext(v);
    }

    @Override
    public void onError(Throwable e) {
        state.onError(e);
    }

    @Override
    public void onCompleted() {
        state.onCompleted();
    }


    @Override
    public boolean hasObservers() {
        return state.get().length != 0;
    }

    /**
     * Check if the Subject has terminated with an exception.
     * @return true if the subject has received a throwable through {@code onError}.
     * @since 1.2
     */
    public boolean hasThrowable() {
        return state.get() == PublishSubjectState.TERMINATED && state.error != null;
    }
    /**
     * Check if the Subject has terminated normally.
     * @return true if the subject completed normally via {@code onCompleted}
     * @since 1.2
     */
    public boolean hasCompleted() {
        return state.get() == PublishSubjectState.TERMINATED && state.error == null;
    }
    /**
     * Returns the Throwable that terminated the Subject.
     * @return the Throwable that terminated the Subject or {@code null} if the
     * subject hasn't terminated yet or it terminated normally.
     * @since 1.2
     */
    public Throwable getThrowable() {
        if (state.get() == PublishSubjectState.TERMINATED) {
            return state.error;
        }
        return null;
    }

    static final class PublishSubjectState<T>
    extends AtomicReference<PublishSubjectProducer<T>[]>
    implements OnSubscribe<T>, Observer<T> {

        /** */
        private static final long serialVersionUID = -7568940796666027140L;

        @SuppressWarnings("rawtypes")
        static final PublishSubjectProducer[] EMPTY = new PublishSubjectProducer[0];
        @SuppressWarnings("rawtypes")
        static final PublishSubjectProducer[] TERMINATED = new PublishSubjectProducer[0];

        Throwable error;

        @SuppressWarnings("unchecked")
        public PublishSubjectState() {
            lazySet(EMPTY);
        }

        @Override
        public void call(Subscriber<? super T> t) {
            PublishSubjectProducer<T> pp = new PublishSubjectProducer<T>(this, t);
            t.add(pp);
            t.setProducer(pp);

            if (add(pp)) {
                if (pp.isUnsubscribed()) {
                    remove(pp);
                }
            } else {
                Throwable ex = error;
                if (ex != null) {
                    t.onError(ex);
                } else {
                    t.onCompleted();
                }
            }
        }


        boolean add(PublishSubjectProducer<T> inner) {
            for (;;) {
                PublishSubjectProducer<T>[] curr = get();
                if (curr == TERMINATED) {
                    return false;
                }

                int n = curr.length;

                @SuppressWarnings("unchecked")
                PublishSubjectProducer<T>[] next = new PublishSubjectProducer[n + 1];
                System.arraycopy(curr, 0, next, 0, n);

                next[n] = inner;
                if (compareAndSet(curr, next)) {
                    return true;
                }
            }
        }

        @SuppressWarnings("unchecked")
        void remove(PublishSubjectProducer<T> inner) {
            for (;;) {
                PublishSubjectProducer<T>[] curr = get();
                if (curr == TERMINATED || curr == EMPTY) {
                    return;
                }

                int n = curr.length;
                int j = -1;
                for (int i = 0; i < n; i++) {
                    if (curr[i] == inner) {
                        j = i;
                        break;
                    }
                }

                if (j < 0) {
                    return;
                }

                PublishSubjectProducer<T>[] next;
                if (n == 1) {
                    next = EMPTY;
                } else {
                    next = new PublishSubjectProducer[n - 1];
                    System.arraycopy(curr, 0, next, 0, j);
                    System.arraycopy(curr, j + 1, next, j, n - j - 1);
                }

                if (compareAndSet(curr, next)) {
                    return;
                }
            }
        }

        @Override
        public void onNext(T t) {
            for (PublishSubjectProducer<T> pp : get()) {
                pp.onNext(t);
            }
        }

        @SuppressWarnings("unchecked")
        @Override
        public void onError(Throwable e) {
            error = e;
            List<Throwable> errors = null;
            for (PublishSubjectProducer<T> pp : getAndSet(TERMINATED)) {
                try {
                    pp.onError(e);
                } catch (Throwable ex) {
                    if (errors == null) {
                        errors = new ArrayList<Throwable>(1);
                    }
                    errors.add(ex);
                }
            }

            Exceptions.throwIfAny(errors);
        }

        @SuppressWarnings("unchecked")
        @Override
        public void onCompleted() {
            for (PublishSubjectProducer<T> pp : getAndSet(TERMINATED)) {
                pp.onCompleted();
            }
        }

    }

    static final class PublishSubjectProducer<T>
    extends AtomicLong
    implements Producer, Subscription, Observer<T> {
        /** */
        private static final long serialVersionUID = 6451806817170721536L;

        final PublishSubjectState<T> parent;

        final Subscriber<? super T> actual;

        long produced;

        public PublishSubjectProducer(PublishSubjectState<T> parent, Subscriber<? super T> actual) {
            this.parent = parent;
            this.actual = actual;
        }

        @Override
        public void request(long n) {
            if (BackpressureUtils.validate(n)) {
                for (;;) {
                    long r = get();
                    if (r == Long.MIN_VALUE) {
                        return;
                    }
                    long u = BackpressureUtils.addCap(r, n);
                    if (compareAndSet(r, u)) {
                        return;
                    }
                }
            }
        }

        @Override
        public boolean isUnsubscribed() {
            return get() == Long.MIN_VALUE;
        }

        @Override
        public void unsubscribe() {
            if (getAndSet(Long.MIN_VALUE) != Long.MIN_VALUE) {
                parent.remove(this);
            }
        }

        @Override
        public void onNext(T t) {
            long r = get();
            if (r != Long.MIN_VALUE) {
                long p = produced;
                if (r != p) {
                    produced = p + 1;
                    actual.onNext(t);
                } else {
                    unsubscribe();
                    actual.onError(new MissingBackpressureException("PublishSubject: could not emit value due to lack of requests"));
                }
            }
        }

        @Override
        public void onError(Throwable e) {
            if (get() != Long.MIN_VALUE) {
                actual.onError(e);
            }
        }

        @Override
        public void onCompleted() {
            if (get() != Long.MIN_VALUE) {
                actual.onCompleted();
            }
        }
    }
}
