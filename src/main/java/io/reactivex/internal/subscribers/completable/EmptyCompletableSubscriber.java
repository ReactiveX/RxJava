package io.reactivex.internal.subscribers.completable;

import java.util.concurrent.atomic.AtomicReference;

import io.reactivex.CompletableSubscriber;
import io.reactivex.disposables.Disposable;
import io.reactivex.internal.disposables.DisposableHelper;
import io.reactivex.plugins.RxJavaPlugins;

public final class EmptyCompletableSubscriber 
extends AtomicReference<Disposable>
implements CompletableSubscriber, Disposable {

    /** */
    private static final long serialVersionUID = -7545121636549663526L;

    @Override
    public void dispose() {
        DisposableHelper.dispose(this);
    }

    @Override
    public void onComplete() {
        // no-op
    }

    @Override
    public void onError(Throwable e) {
        RxJavaPlugins.onError(e);
    }

    @Override
    public void onSubscribe(Disposable d) {
        DisposableHelper.setOnce(this, d);
    }

}
