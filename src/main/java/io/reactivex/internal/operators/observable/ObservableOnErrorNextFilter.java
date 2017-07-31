package io.reactivex.internal.operators.observable;

import io.reactivex.ObservableSource;
import io.reactivex.Observer;
import io.reactivex.disposables.Disposable;
import io.reactivex.exceptions.CompositeException;
import io.reactivex.exceptions.Exceptions;
import io.reactivex.functions.Predicate;
import io.reactivex.internal.disposables.SequentialDisposable;
import io.reactivex.plugins.RxJavaPlugins;

public class ObservableOnErrorNextFilter<T> extends AbstractObservableWithUpstream<T, T> {
    final Predicate<? super Throwable> predicate;
    final ObservableSource<? extends T> next;
    final boolean allowFatal;

    public ObservableOnErrorNextFilter(ObservableSource<T> source, Predicate<? super Throwable> predicate, ObservableSource<? extends T> next, boolean allowFatal) {
        super(source);
        this.predicate = predicate;
        this.next = next;
        this.allowFatal = allowFatal;
    }

    @Override
    protected void subscribeActual(Observer<? super T> actual) {
        OnErrorNextFilterObserver<T> parent = new OnErrorNextFilterObserver<T>(actual, predicate, next, allowFatal);
        actual.onSubscribe(parent.arbiter);
        source.subscribe(parent);
    }

    static final class OnErrorNextFilterObserver<T> implements Observer<T> {
        final Observer<? super T> actual;
        final Predicate<? super Throwable> predicate;
        final ObservableSource<? extends T> next;
        final boolean allowFatal;
        final SequentialDisposable arbiter;

        boolean once;

        boolean done;

        OnErrorNextFilterObserver(Observer<? super T> actual, Predicate<? super Throwable> predicate, ObservableSource<? extends T> next, boolean allowFatal) {
            this.actual = actual;
            this.predicate = predicate;
            this.next = next;
            this.allowFatal = allowFatal;
            this.arbiter = new SequentialDisposable();
        }

        @Override
        public void onSubscribe(Disposable d) {
            arbiter.replace(d);
        }

        @Override
        public void onNext(T t) {
            if (done) {
                return;
            }
            actual.onNext(t);
        }

        @Override
        public void onError(Throwable t) {
            if (once) {
                if (done) {
                    RxJavaPlugins.onError(t);
                    return;
                }
                actual.onError(t);
                return;
            }
            once = true;

            if (allowFatal && !(t instanceof Exception)) {
                actual.onError(t);
                return;
            }

            boolean b;

            try {
                b = predicate.test(t);
            } catch (Exception e) {
                Exceptions.throwIfFatal(e);
                actual.onError(new CompositeException(t, e));
                return;
            }

            if (b) {
                next.subscribe(this);
            } else {
                actual.onError(t);
            }
        }

        @Override
        public void onComplete() {
            if (done) {
                return;
            }
            done = true;
            once = true;
            actual.onComplete();
        }
    }
}
