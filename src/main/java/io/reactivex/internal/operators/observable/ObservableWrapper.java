package io.reactivex.internal.operators.observable;

import io.reactivex.*;

public final class ObservableWrapper<T> extends Observable<T> {
    final NbpOnSubscribe<T> onSubscribe;

    public ObservableWrapper(NbpOnSubscribe<T> onSubscribe) {
        this.onSubscribe = onSubscribe;
    }
    
    @Override
    protected void subscribeActual(Observer<? super T> observer) {
        onSubscribe.accept(observer);
    }
}
