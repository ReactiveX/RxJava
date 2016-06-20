package io.reactivex;

import io.reactivex.disposables.Disposable;

public interface SingleSubscriber<T> {
    
    
    void onSubscribe(Disposable d);
    
    void onSuccess(T value);

    void onError(Throwable e);
}