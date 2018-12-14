package io.reactivex.internal.operators.observable;

import io.reactivex.ObservableSource;
import io.reactivex.Observer;
import io.reactivex.annotations.Nullable;
import io.reactivex.functions.ImmutableConsumer;
import io.reactivex.internal.observers.BasicFuseableObserver;

public final class ObservablePeek<T> extends AbstractObservableWithUpstream<T, T> {
    final ImmutableConsumer<T> function;

    public ObservablePeek(ObservableSource<T> source, ImmutableConsumer<T> function) {
        super(source);
        this.function = function;
    }

    @Override
    protected void subscribeActual(Observer<? super T> t) {
        source.subscribe(new PeekObserver<T>(t, function));
    }

    static final class PeekObserver<T> extends BasicFuseableObserver<T, T> {
        final ImmutableConsumer<T> function;

        PeekObserver(Observer<? super T> actual, ImmutableConsumer<T> function) {
            super(actual);
            this.function = function;
        }

        @Override
        public void onNext(final T t) {
            if (done) {
                return;
            }

            if (sourceMode != NONE) {
                downstream.onNext(null);
                return;
            }

            try {
                function.accept(t);
            } catch (Throwable ex) {
                fail(ex);
                return;
            }
            downstream.onNext(t);
        }

        @Override
        public int requestFusion(int mode) {
            return transitiveBoundaryFusion(mode);
        }

        @Nullable
        @Override
        public T poll() throws Exception {
            final T t = qd.poll();
            if (t != null) {
                function.accept(t);
            }
            return t;
        }
    }
}

