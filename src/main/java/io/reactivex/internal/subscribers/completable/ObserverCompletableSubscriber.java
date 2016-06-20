package io.reactivex.internal.subscribers.completable;

import io.reactivex.*;
import io.reactivex.disposables.Disposable;

public final class ObserverCompletableSubscriber<T> implements CompletableSubscriber {
    final Observer<? super T> observer;

    public ObserverCompletableSubscriber(Observer<? super T> observer) {
        this.observer = observer;
    }

    @Override
    public void onComplete() {
        observer.onComplete();
    }

    @Override
    public void onError(Throwable e) {
        observer.onError(e);
    }

    @Override
    public void onSubscribe(Disposable d) {
        observer.onSubscribe(d);
    }
    
    
}
