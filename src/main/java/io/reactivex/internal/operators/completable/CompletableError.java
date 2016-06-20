package io.reactivex.internal.operators.completable;

import io.reactivex.*;
import io.reactivex.internal.disposables.EmptyDisposable;

public final class CompletableError extends Completable {
    
    final Throwable error;
    
    public CompletableError(Throwable error) {
        this.error = error;
    }
    
    @Override
    protected void subscribeActual(CompletableSubscriber s) {
        s.onSubscribe(EmptyDisposable.INSTANCE);
        s.onError(error);
    }
}
