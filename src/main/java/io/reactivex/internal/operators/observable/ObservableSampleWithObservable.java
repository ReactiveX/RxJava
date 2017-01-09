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

import io.reactivex.*;
import io.reactivex.disposables.Disposable;
import io.reactivex.internal.disposables.DisposableHelper;
import io.reactivex.observers.SerializedObserver;

public final class ObservableSampleWithObservable<T> extends AbstractObservableWithUpstream<T, T> {

    final ObservableSource<?> other;

    final boolean emitLast;

    public ObservableSampleWithObservable(ObservableSource<T> source, ObservableSource<?> other, boolean emitLast) {
        super(source);
        this.other = other;
        this.emitLast = emitLast;
    }

    @Override
    public void subscribeActual(Observer<? super T> t) {
        SerializedObserver<T> serial = new SerializedObserver<T>(t);
        if (emitLast) {
            source.subscribe(new SampleMainEmitLast<T>(serial, other));
        } else {
            source.subscribe(new SampleMainNoLast<T>(serial, other));
        }
    }

    abstract static class SampleMainObserver<T> extends AtomicReference<T>
    implements Observer<T>, Disposable {

        private static final long serialVersionUID = -3517602651313910099L;

        final Observer<? super T> actual;
        final ObservableSource<?> sampler;

        final AtomicReference<Disposable> other = new AtomicReference<Disposable>();

        Disposable s;

        SampleMainObserver(Observer<? super T> actual, ObservableSource<?> other) {
            this.actual = actual;
            this.sampler = other;
        }

        @Override
        public void onSubscribe(Disposable s) {
            if (DisposableHelper.validate(this.s, s)) {
                this.s = s;
                actual.onSubscribe(this);
                if (other.get() == null) {
                    sampler.subscribe(new SamplerObserver<T>(this));
                }
            }
        }

        @Override
        public void onNext(T t) {
            lazySet(t);
        }

        @Override
        public void onError(Throwable t) {
            DisposableHelper.dispose(other);
            actual.onError(t);
        }

        @Override
        public void onComplete() {
            DisposableHelper.dispose(other);
            completeMain();
        }

        boolean setOther(Disposable o) {
            return DisposableHelper.setOnce(other, o);
        }

        @Override
        public void dispose() {
            DisposableHelper.dispose(other);
            s.dispose();
        }

        @Override
        public boolean isDisposed() {
            return other.get() == DisposableHelper.DISPOSED;
        }

        public void error(Throwable e) {
            s.dispose();
            actual.onError(e);
        }

        public void complete() {
            s.dispose();
            completeOther();
        }

        void emit() {
            T value = getAndSet(null);
            if (value != null) {
                actual.onNext(value);
            }
        }

        abstract void completeMain();

        abstract void completeOther();

        abstract void run();
    }

    static final class SamplerObserver<T> implements Observer<Object> {
        final SampleMainObserver<T> parent;
        SamplerObserver(SampleMainObserver<T> parent) {
            this.parent = parent;

        }

        @Override
        public void onSubscribe(Disposable s) {
            parent.setOther(s);
        }

        @Override
        public void onNext(Object t) {
            parent.run();
        }

        @Override
        public void onError(Throwable t) {
            parent.error(t);
        }

        @Override
        public void onComplete() {
            parent.complete();
        }
    }

    static final class SampleMainNoLast<T> extends SampleMainObserver<T> {

        private static final long serialVersionUID = -3029755663834015785L;

        SampleMainNoLast(Observer<? super T> actual, ObservableSource<?> other) {
            super(actual, other);
        }

        @Override
        void completeMain() {
            actual.onComplete();
        }

        @Override
        void completeOther() {
            actual.onComplete();
        }

        @Override
        void run() {
            emit();
        }
    }

    static final class SampleMainEmitLast<T> extends SampleMainObserver<T> {

        private static final long serialVersionUID = -3029755663834015785L;

        final AtomicInteger wip;

        volatile boolean done;

        SampleMainEmitLast(Observer<? super T> actual, ObservableSource<?> other) {
            super(actual, other);
            this.wip = new AtomicInteger();
        }

        @Override
        void completeMain() {
            done = true;
            if (wip.getAndIncrement() == 0) {
                emit();
                actual.onComplete();
            }
        }

        @Override
        void completeOther() {
            done = true;
            if (wip.getAndIncrement() == 0) {
                emit();
                actual.onComplete();
            }
        }

        @Override
        void run() {
            if (wip.getAndIncrement() == 0) {
                do {
                    boolean d = done;
                    emit();
                    if (d) {
                        actual.onComplete();
                        return;
                    }
                } while (wip.decrementAndGet() != 0);
            }
        }
    }
}
