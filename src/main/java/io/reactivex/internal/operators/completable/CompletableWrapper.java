package io.reactivex.internal.operators.completable;

import io.reactivex.Completable;

public final class CompletableWrapper extends Completable {

    final CompletableOnSubscribe onSubscribe;

    public CompletableWrapper(CompletableOnSubscribe onSubscribe) {
        this.onSubscribe = onSubscribe;
    }
    
    @Override
    protected void subscribeActual(CompletableSubscriber s) {
        onSubscribe.accept(s);
    }
}
