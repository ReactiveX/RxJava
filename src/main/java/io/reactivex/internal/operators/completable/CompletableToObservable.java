package io.reactivex.internal.operators.completable;

import io.reactivex.*;
import io.reactivex.internal.subscribers.completable.ObserverCompletableSubscriber;

public class CompletableToObservable<T> extends Observable<T> {

    final CompletableConsumable source;
    
    public CompletableToObservable(CompletableConsumable source) {
        this.source = source;
    }

    @Override
    protected void subscribeActual(Observer<? super T> s) {
        ObserverCompletableSubscriber<T> os = new ObserverCompletableSubscriber<T>(s);
        source.subscribe(os);
    }
}
