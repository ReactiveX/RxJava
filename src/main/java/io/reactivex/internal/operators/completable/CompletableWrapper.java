package io.reactivex.internal.operators.completable;

import io.reactivex.*;

public final class CompletableWrapper extends Completable {

    final CompletableConsumable onSubscribe;

    public CompletableWrapper(CompletableConsumable onSubscribe) {
        this.onSubscribe = onSubscribe;
    }
    
    @Override
    protected void subscribeActual(CompletableSubscriber s) {
        onSubscribe.subscribe(s);
    }
}
