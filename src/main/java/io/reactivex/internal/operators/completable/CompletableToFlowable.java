package io.reactivex.internal.operators.completable;

import org.reactivestreams.Subscriber;

import io.reactivex.*;
import io.reactivex.internal.subscribers.completable.SubscriberCompletableSubscriber;

public class CompletableToFlowable<T> extends Flowable<T> {

    final CompletableConsumable source;
    
    public CompletableToFlowable(CompletableConsumable source) {
        this.source = source;
    }

    @Override
    protected void subscribeActual(Subscriber<? super T> s) {
        SubscriberCompletableSubscriber<T> os = new SubscriberCompletableSubscriber<T>(s);
        source.subscribe(os);
    }
}
