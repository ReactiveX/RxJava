package io.reactivex.internal.subscribers.completable;

import org.reactivestreams.*;

import io.reactivex.CompletableSubscriber;
import io.reactivex.disposables.Disposable;
import io.reactivex.internal.disposables.DisposableHelper;

public final class SubscriberCompletableSubscriber<T> implements CompletableSubscriber, Subscription {
    final Subscriber<? super T> subscriber;

    Disposable d;
    
    public SubscriberCompletableSubscriber(Subscriber<? super T> observer) {
        this.subscriber = observer;
    }

    @Override
    public void onComplete() {
        subscriber.onComplete();
    }

    @Override
    public void onError(Throwable e) {
        subscriber.onError(e);
    }

    @Override
    public void onSubscribe(Disposable d) {
        if (DisposableHelper.validate(this.d, d)) {
            this.d = d;
            
            subscriber.onSubscribe(this);
        }
    }

    @Override
    public void request(long n) {
        // ingored, no values emitted anyway
    }

    @Override
    public void cancel() {
        d.dispose();
    }
}
