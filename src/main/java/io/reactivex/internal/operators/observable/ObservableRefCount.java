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

import java.util.concurrent.atomic.*;
import java.util.concurrent.locks.ReentrantLock;

import io.reactivex.*;
import io.reactivex.disposables.*;
import io.reactivex.functions.Consumer;
import io.reactivex.internal.disposables.DisposableHelper;
import io.reactivex.observables.ConnectableObservable;

/**
 * Returns an observable sequence that stays connected to the source as long as
 * there is at least one subscription to the observable sequence.
 *
 * @param <T>
 *            the value type
 */
public final class ObservableRefCount<T> extends AbstractObservableWithUpstream<T, T> {

    final ConnectableObservable<? extends T> source;

    volatile CompositeDisposable baseDisposable = new CompositeDisposable();

    final AtomicInteger subscriptionCount = new AtomicInteger();

    /**
     * Use this lock for every subscription and disconnect action.
     */
    final ReentrantLock lock = new ReentrantLock();

    /**
     * Constructor.
     *
     * @param source
     *            observable to apply ref count to
     */
    public ObservableRefCount(ConnectableObservable<T> source) {
        super(source);
        this.source = source;
    }

    @Override
    public void subscribeActual(final Observer<? super T> subscriber) {

        lock.lock();
        if (subscriptionCount.incrementAndGet() == 1) {

            final AtomicBoolean writeLocked = new AtomicBoolean(true);

            try {
                // need to use this overload of connect to ensure that
                // baseDisposable is set in the case that source is a
                // synchronous Observable
                source.connect(onSubscribe(subscriber, writeLocked));
            } finally {
                // need to cover the case where the source is subscribed to
                // outside of this class thus preventing the Consumer passed
                // to source.connect above being called
                if (writeLocked.get()) {
                    // Consumer passed to source.connect was not called
                    lock.unlock();
                }
            }
        } else {
            try {
                // ready to subscribe to source so do it
                doSubscribe(subscriber, baseDisposable);
            } finally {
                // release the read lock
                lock.unlock();
            }
        }

    }

    private Consumer<Disposable> onSubscribe(final Observer<? super T> observer,
            final AtomicBoolean writeLocked) {
        return new DisposeConsumer(observer, writeLocked);
    }

    void doSubscribe(final Observer<? super T> observer, final CompositeDisposable currentBase) {
        // handle disposing from the base CompositeDisposable
        Disposable d = disconnect(currentBase);

        ConnectionObserver s = new ConnectionObserver(observer, currentBase, d);
        observer.onSubscribe(s);

        source.subscribe(s);
    }

    private Disposable disconnect(final CompositeDisposable current) {
        return Disposables.fromRunnable(new DisposeTask(current));
    }

    final class ConnectionObserver
    extends AtomicReference<Disposable>
    implements Observer<T>, Disposable {

        private static final long serialVersionUID = 3813126992133394324L;

        final Observer<? super T> subscriber;
        final CompositeDisposable currentBase;
        final Disposable resource;

        ConnectionObserver(Observer<? super T> subscriber,
                CompositeDisposable currentBase, Disposable resource) {
            this.subscriber = subscriber;
            this.currentBase = currentBase;
            this.resource = resource;
        }

        @Override
        public void onSubscribe(Disposable s) {
            DisposableHelper.setOnce(this, s);
        }

        @Override
        public void onError(Throwable e) {
            cleanup();
            subscriber.onError(e);
        }

        @Override
        public void onNext(T t) {
            subscriber.onNext(t);
        }

        @Override
        public void onComplete() {
            cleanup();
            subscriber.onComplete();
        }

        @Override
        public void dispose() {
            DisposableHelper.dispose(this);
            resource.dispose();
        }

        @Override
        public boolean isDisposed() {
            return DisposableHelper.isDisposed(get());
        }

        void cleanup() {
            // on error or completion we need to dispose the base CompositeDisposable
            // and set the subscriptionCount to 0
            lock.lock();
            try {
                if (baseDisposable == currentBase) {
                    if (source instanceof Disposable) {
                        ((Disposable)source).dispose();
                    }

                    baseDisposable.dispose();
                    baseDisposable = new CompositeDisposable();
                    subscriptionCount.set(0);
                }
            } finally {
                lock.unlock();
            }
        }
    }

    final class DisposeConsumer implements Consumer<Disposable> {
        private final Observer<? super T> observer;
        private final AtomicBoolean writeLocked;

        DisposeConsumer(Observer<? super T> observer, AtomicBoolean writeLocked) {
            this.observer = observer;
            this.writeLocked = writeLocked;
        }

        @Override
        public void accept(Disposable subscription) {
            try {
                baseDisposable.add(subscription);
                // ready to subscribe to source so do it
                doSubscribe(observer, baseDisposable);
            } finally {
                // release the write lock
                lock.unlock();
                writeLocked.set(false);
            }
        }
    }

    final class DisposeTask implements Runnable {
        private final CompositeDisposable current;

        DisposeTask(CompositeDisposable current) {
            this.current = current;
        }

        @Override
        public void run() {
            lock.lock();
            try {
                if (baseDisposable == current) {
                    if (subscriptionCount.decrementAndGet() == 0) {
                        if (source instanceof Disposable) {
                            ((Disposable)source).dispose();
                        }

                        baseDisposable.dispose();
                        // need a new baseDisposable because once
                        // disposed stays that way
                        baseDisposable = new CompositeDisposable();
                    }
                }
            } finally {
                lock.unlock();
            }
        }
    }
}
