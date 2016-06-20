package io.reactivex.internal.operators.observable;

import io.reactivex.*;

public final class ObservableWrapper<T> extends Observable<T> {
    final ObservableConsumable<T> onSubscribe;

    public ObservableWrapper(ObservableConsumable<T> onSubscribe) {
        this.onSubscribe = onSubscribe;
    }
    
    @Override
    protected void subscribeActual(Observer<? super T> observer) {
        onSubscribe.subscribe(observer);
    }
}
