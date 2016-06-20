package io.reactivex.internal.subscribers.completable;

import java.util.concurrent.atomic.AtomicReference;

import io.reactivex.CompletableSubscriber;
import io.reactivex.disposables.Disposable;
import io.reactivex.plugins.RxJavaPlugins;

public final class CompletableConsumingSubscriber 
extends AtomicReference<Disposable>
implements CompletableSubscriber, Disposable {

    @Override
    public void dispose() {
        // TODO Auto-generated method stub
        
    }

    @Override
    public void onComplete() {
        // TODO Auto-generated method stub
        
    }

    @Override
    public void onError(Throwable e) {
        RxJavaPlugins.onError(e);
    }

    @Override
    public void onSubscribe(Disposable d) {
        // TODO Auto-generated method stub
        
    }

}
