package io.reactivex.internal.operators.completable;

import java.util.concurrent.Callable;

import io.reactivex.*;
import io.reactivex.disposables.BooleanDisposable;

public final class CompletableFromCallable extends Completable {
    
    final Callable<?> callable;

    public CompletableFromCallable(Callable<?> callable) {
        this.callable = callable;
    }
    
    @Override
    protected void subscribeActual(CompletableSubscriber s) {
        BooleanDisposable bs = new BooleanDisposable();
        s.onSubscribe(bs);
        try {
            callable.call();
        } catch (Throwable e) {
            if (!bs.isDisposed()) {
                s.onError(e);
            }
            return;
        }
        if (!bs.isDisposed()) {
            s.onComplete();
        }
    }
}
