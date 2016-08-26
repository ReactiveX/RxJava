/**
 * Copyright 2016 Netflix, Inc.
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

package io.reactivex.subjects;

import java.util.concurrent.atomic.*;

import io.reactivex.*;
import io.reactivex.disposables.Disposable;
import io.reactivex.internal.disposables.EmptyDisposable;
import io.reactivex.internal.util.NotificationLite;
import io.reactivex.plugins.RxJavaPlugins;

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
public final class PublishSubject<T> extends Subject<T> {
    final State<T> state;
    
    /**
     * Creates and returns a new {@code PublishSubject}.
     *
     * @param <T> the value type
     * @return the new {@code PublishSubject}
     */
    public static <T> PublishSubject<T> create() {
        return new PublishSubject<T>();
    }
    
    protected PublishSubject() {
        this.state = new State<T>();
    }

    @Override
    protected void subscribeActual(Observer<? super T> observer) {
        state.subscribe(observer);
    }

    @Override
    public void onSubscribe(Disposable d) {
        if (state.done) {
            d.dispose();
        }
    }
    
    @Override
    public void onNext(T value) {
        state.onNext(value);
    }
    
    @Override
    public void onError(Throwable e) {
        state.onError(e);
    }
    
    @Override
    public void onComplete() {
        state.onComplete();
    }
    
    @Override
    public boolean hasObservers() {
        return state.subscribers.get().length != 0;
    }
    
    @Override
    public Throwable getThrowable() {
        Object o = state.get();
        if (NotificationLite.isError(o)) {
            return NotificationLite.getError(o);
        }
        return null;
    }
    
    @Override
    public boolean hasComplete() {
        Object o = state.get();
        return o != null && !NotificationLite.isError(o);
    }
    
    @Override
    public boolean hasThrowable() {
        return NotificationLite.isError(state.get());
    }
    
    static final class State<T> extends AtomicReference<Object> 
    implements ObservableSource<T> {
        /** */
        private static final long serialVersionUID = 4876574210612691772L;

        final AtomicReference<PublishDisposable<? super T>[]> subscribers = new AtomicReference<PublishDisposable<? super T>[]>();
        
        @SuppressWarnings("rawtypes")
        static final PublishDisposable[] EMPTY = new PublishDisposable[0];
        @SuppressWarnings("rawtypes")
        static final PublishDisposable[] TERMINATED = new PublishDisposable[0];

        volatile boolean done;
        
        @SuppressWarnings("unchecked")
        public State() {
            subscribers.lazySet(EMPTY);
        }
        
        boolean add(PublishDisposable<? super T> s) {
            for (;;) {
                PublishDisposable<? super T>[] a = subscribers.get();
                if (a == TERMINATED) {
                    return false;
                }
                int n = a.length;
                
                @SuppressWarnings("unchecked")
                PublishDisposable<? super T>[] b = new PublishDisposable[n + 1];
                System.arraycopy(a, 0, b, 0, n);
                b[n] = s;
                
                if (subscribers.compareAndSet(a, b)) {
                    return true;
                }
            }
        }
        
        @SuppressWarnings("unchecked")
        void remove(PublishDisposable<? super T> s) {
            for (;;) {
                PublishDisposable<? super T>[] a = subscribers.get();
                if (a == TERMINATED || a == EMPTY) {
                    return;
                }
                int n = a.length;
                int j = -1;
                for (int i = 0; i < n; i++) {
                    PublishDisposable<? super T> e = a[i];
                    if (e.equals(s)) {
                        j = i;
                        break;
                    }
                }
                if (j < 0) {
                    return;
                }
                PublishDisposable<? super T>[] b;
                if (n == 1) {
                    b = EMPTY;
                } else {
                    b = new PublishDisposable[n - 1];
                    System.arraycopy(a, 0, b, 0, j);
                    System.arraycopy(a, j + 1, b, j, n - j - 1);
                }
                if (subscribers.compareAndSet(a, b)) {
                    return;
                }
            }
        }
        
        @SuppressWarnings("unchecked")
        PublishDisposable<? super T>[] terminate(Object notification) {
            if (compareAndSet(null, notification)) {
                return subscribers.getAndSet(TERMINATED);
            }
            return TERMINATED;
        }
        
        void emit(Observer<? super T> t, Object v) {
            if (NotificationLite.isComplete(v)) {
                t.onComplete();
            } else {
                t.onError(NotificationLite.getError(v));
            }
        }
        
        @Override
        public void subscribe(final Observer<? super T> t) {
            Object v = get();
            if (v != null) {
                t.onSubscribe(EmptyDisposable.INSTANCE);
                emit(t, v);
                return;
            }
            PublishDisposable<T> d = new PublishDisposable<T>(t, this);
            t.onSubscribe(d);
            if (add(d)) {
                if (d.isDisposed()) {
                    remove(d);
                }
                return;
            }
            v = get();
            emit(t, v);
        }
        
        public void onNext(T value) {
            if (done) {
                return;
            }
            if (value == null) {
                onError(new NullPointerException("The value is null"));
                return;
            }
            for (PublishDisposable<? super T> v : subscribers.get()) {
                if (!v.get()) {
                    v.actual.onNext(value);
                }
            }
        }
        
        public void onError(Throwable e) {
            if (done) {
                RxJavaPlugins.onError(e);
                return;
            }
            done = true;
            if (e == null) {
                e = new NullPointerException();
            }

            for (PublishDisposable<? super T> v : terminate(NotificationLite.error(e))) {
                if (!v.get()) {
                    v.actual.onError(e);
                }
            }
        }
        
        public void onComplete() {
            if (done) {
                return;
            }
            done = true;
            for (PublishDisposable<? super T> v : terminate(NotificationLite.complete())) {
                if (!v.get()) {
                    v.actual.onComplete();
                }
            }
        }
    }
    
    static final class PublishDisposable<T> extends AtomicBoolean implements Disposable {
        /** */
        private static final long serialVersionUID = 2734660924929727368L;

        final Observer<? super T> actual;

        final State<T> parent;
        
        public PublishDisposable(Observer<? super T> actual, State<T> parent) {
            this.actual = actual;
            this.parent = parent;
        }
        
        @Override
        public boolean isDisposed() {
            return get();
        }
        
        @Override
        public void dispose() {
            if (compareAndSet(false, true)) {
                parent.remove(this);
            }
        }
    }
}
