/**
 * Copyright (c) 2016-present, RxJava Contributors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in
 * compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is
 * distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See
 * the License for the specific language governing permissions and limitations under the License.
 */

package io.reactivex.internal.operators.observable;

import io.reactivex.internal.functions.ObjectHelper;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.*;

import io.reactivex.ObservableSource;
import io.reactivex.Observer;
import io.reactivex.disposables.Disposable;
import io.reactivex.exceptions.Exceptions;
import io.reactivex.functions.Function;
import io.reactivex.internal.disposables.*;
import io.reactivex.internal.queue.SpscLinkedArrayQueue;
import io.reactivex.observables.GroupedObservable;

public final class ObservableGroupBy<T, K, V> extends AbstractObservableWithUpstream<T, GroupedObservable<K, V>> {
    final Function<? super T, ? extends K> keySelector;
    final Function<? super T, ? extends V> valueSelector;
    final int bufferSize;
    final boolean delayError;

    public ObservableGroupBy(ObservableSource<T> source,
            Function<? super T, ? extends K> keySelector, Function<? super T, ? extends V> valueSelector,
            int bufferSize, boolean delayError) {
        super(source);
        this.keySelector = keySelector;
        this.valueSelector = valueSelector;
        this.bufferSize = bufferSize;
        this.delayError = delayError;
    }

    @Override
    public void subscribeActual(Observer<? super GroupedObservable<K, V>> t) {
        source.subscribe(new GroupByObserver<T, K, V>(t, keySelector, valueSelector, bufferSize, delayError));
    }

    public static final class GroupByObserver<T, K, V> extends AtomicInteger implements Observer<T>, Disposable {

        private static final long serialVersionUID = -3688291656102519502L;

        final Observer<? super GroupedObservable<K, V>> actual;
        final Function<? super T, ? extends K> keySelector;
        final Function<? super T, ? extends V> valueSelector;
        final int bufferSize;
        final boolean delayError;
        final Map<Object, GroupedUnicast<K, V>> groups;

        static final Object NULL_KEY = new Object();

        Disposable s;

        final AtomicBoolean cancelled = new AtomicBoolean();

        public GroupByObserver(Observer<? super GroupedObservable<K, V>> actual, Function<? super T, ? extends K> keySelector, Function<? super T, ? extends V> valueSelector, int bufferSize, boolean delayError) {
            this.actual = actual;
            this.keySelector = keySelector;
            this.valueSelector = valueSelector;
            this.bufferSize = bufferSize;
            this.delayError = delayError;
            this.groups = new ConcurrentHashMap<Object, GroupedUnicast<K, V>>();
            this.lazySet(1);
        }

        @Override
        public void onSubscribe(Disposable s) {
            if (DisposableHelper.validate(this.s, s)) {
                this.s = s;
                actual.onSubscribe(this);
            }
        }

        @Override
        public void onNext(T t) {
            K key;
            try {
                key = keySelector.apply(t);
            } catch (Throwable e) {
                Exceptions.throwIfFatal(e);
                s.dispose();
                onError(e);
                return;
            }

            Object mapKey = key != null ? key : NULL_KEY;
            GroupedUnicast<K, V> group = groups.get(mapKey);
            if (group == null) {
                // if the main has been cancelled, stop creating groups
                // and skip this value
                if (cancelled.get()) {
                    return;
                }

                group = GroupedUnicast.createWith(key, bufferSize, this, delayError);
                groups.put(mapKey, group);

                getAndIncrement();

                actual.onNext(group);
            }

            V v;
            try {
                v = ObjectHelper.requireNonNull(valueSelector.apply(t), "The value supplied is null");
            } catch (Throwable e) {
                Exceptions.throwIfFatal(e);
                s.dispose();
                onError(e);
                return;
            }

            group.onNext(v);
        }

        @Override
        public void onError(Throwable t) {
            List<GroupedUnicast<K, V>> list = new ArrayList<GroupedUnicast<K, V>>(groups.values());
            groups.clear();

            for (GroupedUnicast<K, V> e : list) {
                e.onError(t);
            }

            actual.onError(t);
        }

        @Override
        public void onComplete() {
            List<GroupedUnicast<K, V>> list = new ArrayList<GroupedUnicast<K, V>>(groups.values());
            groups.clear();

            for (GroupedUnicast<K, V> e : list) {
                e.onComplete();
            }

            actual.onComplete();
        }

        @Override
        public void dispose() {
            // cancelling the main source means we don't want any more groups
            // but running groups still require new values
            if (cancelled.compareAndSet(false, true)) {
                if (decrementAndGet() == 0) {
                    s.dispose();
                }
            }
        }

        @Override
        public boolean isDisposed() {
            return cancelled.get();
        }

        public void cancel(K key) {
            Object mapKey = key != null ? key : NULL_KEY;
            groups.remove(mapKey);
            if (decrementAndGet() == 0) {
                s.dispose();
            }
        }
    }

    static final class GroupedUnicast<K, T> extends GroupedObservable<K, T> {

        final State<T, K> state;

        public static <T, K> GroupedUnicast<K, T> createWith(K key, int bufferSize, GroupByObserver<?, K, T> parent, boolean delayError) {
            State<T, K> state = new State<T, K>(bufferSize, parent, key, delayError);
            return new GroupedUnicast<K, T>(key, state);
        }

        protected GroupedUnicast(K key, State<T, K> state) {
            super(key);
            this.state = state;
        }

        @Override
        protected void subscribeActual(Observer<? super T> observer) {
            state.subscribe(observer);
        }

        public void onNext(T t) {
            state.onNext(t);
        }

        public void onError(Throwable e) {
            state.onError(e);
        }

        public void onComplete() {
            state.onComplete();
        }
    }

    static final class State<T, K> extends AtomicInteger implements Disposable, ObservableSource<T> {

        private static final long serialVersionUID = -3852313036005250360L;

        final K key;
        final SpscLinkedArrayQueue<T> queue;
        final GroupByObserver<?, K, T> parent;
        final boolean delayError;

        volatile boolean done;
        Throwable error;

        final AtomicBoolean cancelled = new AtomicBoolean();

        final AtomicBoolean once = new AtomicBoolean();

        final AtomicReference<Observer<? super T>> actual = new AtomicReference<Observer<? super T>>();

        State(int bufferSize, GroupByObserver<?, K, T> parent, K key, boolean delayError) {
            this.queue = new SpscLinkedArrayQueue<T>(bufferSize);
            this.parent = parent;
            this.key = key;
            this.delayError = delayError;
        }

        @Override
        public void dispose() {
            if (cancelled.compareAndSet(false, true)) {
                if (getAndIncrement() == 0) {
                    actual.lazySet(null);
                    parent.cancel(key);
                }
            }
        }

        @Override
        public boolean isDisposed() {
            return cancelled.get();
        }

        @Override
        public void subscribe(Observer<? super T> s) {
            if (once.compareAndSet(false, true)) {
                s.onSubscribe(this);
                actual.lazySet(s);
                if (cancelled.get()) {
                    actual.lazySet(null);
                } else {
                    drain();
                }
            } else {
                EmptyDisposable.error(new IllegalStateException("Only one Observer allowed!"), s);
            }
        }

        public void onNext(T t) {
            queue.offer(t);
            drain();
        }

        public void onError(Throwable e) {
            error = e;
            done = true;
            drain();
        }

        public void onComplete() {
            done = true;
            drain();
        }

        void drain() {
            if (getAndIncrement() != 0) {
                return;
            }
            int missed = 1;

            final SpscLinkedArrayQueue<T> q = queue;
            final boolean delayError = this.delayError;
            Observer<? super T> a = actual.get();
            for (;;) {
                if (a != null) {
                    for (;;) {
                        boolean d = done;
                        T v = q.poll();
                        boolean empty = v == null;

                        if (checkTerminated(d, empty, a, delayError)) {
                            return;
                        }

                        if (empty) {
                            break;
                        }

                        a.onNext(v);
                    }
                }

                missed = addAndGet(-missed);
                if (missed == 0) {
                    break;
                }
                if (a == null) {
                    a = actual.get();
                }
            }
        }

        boolean checkTerminated(boolean d, boolean empty, Observer<? super T> a, boolean delayError) {
            if (cancelled.get()) {
                queue.clear();
                parent.cancel(key);
                actual.lazySet(null);
                return true;
            }

            if (d) {
                if (delayError) {
                    if (empty) {
                        Throwable e = error;
                        actual.lazySet(null);
                        if (e != null) {
                            a.onError(e);
                        } else {
                            a.onComplete();
                        }
                        return true;
                    }
                } else {
                    Throwable e = error;
                    if (e != null) {
                        queue.clear();
                        actual.lazySet(null);
                        a.onError(e);
                        return true;
                    } else
                    if (empty) {
                        actual.lazySet(null);
                        a.onComplete();
                        return true;
                    }
                }
            }

            return false;
        }
    }
}
