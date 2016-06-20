package io.reactivex.internal.operators.single;

import io.reactivex.*;

public final class SingleWrapper<T> extends Single<T> {
    final SingleConsumable<T> onSubscribe;

    public SingleWrapper(SingleConsumable<T> onSubscribe) {
        this.onSubscribe = onSubscribe;
    }
    
    @Override
    protected void subscribeActual(io.reactivex.SingleSubscriber<? super T> subscriber) {
        onSubscribe.subscribe(subscriber);
    }
}
