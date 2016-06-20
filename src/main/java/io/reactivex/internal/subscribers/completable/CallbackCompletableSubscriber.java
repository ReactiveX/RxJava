package io.reactivex.internal.subscribers.completable;

import java.util.concurrent.atomic.AtomicReference;

import io.reactivex.CompletableSubscriber;
import io.reactivex.disposables.Disposable;
import io.reactivex.functions.Consumer;
import io.reactivex.internal.disposables.DisposableHelper;
import io.reactivex.internal.util.Exceptions;
import io.reactivex.plugins.RxJavaPlugins;

public final class CallbackCompletableSubscriber 
extends AtomicReference<Disposable> implements CompletableSubscriber, Disposable {

    /** */
    private static final long serialVersionUID = -4361286194466301354L;

    final Consumer<? super Throwable> onError;
    final Runnable onComplete;
    
    static final Consumer<? super Throwable> DEFAULT_ON_ERROR = new Consumer<Throwable>() {
        @Override
        public void accept(Throwable e) {
            RxJavaPlugins.onError(e);
        }
    };

    public CallbackCompletableSubscriber(Runnable onComplete) {
        this.onError = DEFAULT_ON_ERROR;
        this.onComplete = onComplete;
    }

    public CallbackCompletableSubscriber(Consumer<? super Throwable> onError, Runnable onComplete) {
        this.onError = onError;
        this.onComplete = onComplete;
    }

    @Override
    public void onComplete() {
        try {
            onComplete.run();
        } catch (Throwable ex) {
            Exceptions.throwIfFatal(ex);
            onError(ex);
        }
    }

    @Override
    public void onError(Throwable e) {
        try {
            onError.accept(e);
        } catch (Throwable ex) {
            Exceptions.throwIfFatal(ex);
            RxJavaPlugins.onError(ex);
        }
    }

    @Override
    public void onSubscribe(Disposable d) {
        DisposableHelper.setOnce(this, d);
    }

    @Override
    public void dispose() {
        DisposableHelper.dispose(this);
    }

}
