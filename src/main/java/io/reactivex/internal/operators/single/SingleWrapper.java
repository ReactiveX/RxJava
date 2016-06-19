package io.reactivex.internal.operators.single;

import io.reactivex.Single;

public final class SingleWrapper<T> extends Single<T> {
    final SingleOnSubscribe<T> onSubscribe;

    public SingleWrapper(io.reactivex.Single.SingleOnSubscribe<T> onSubscribe) {
        this.onSubscribe = onSubscribe;
    }
    
    @Override
    protected void subscribeActual(io.reactivex.Single.SingleSubscriber<? super T> subscriber) {
        onSubscribe.accept(subscriber);
    }
}
