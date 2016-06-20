package io.reactivex.internal.operators.completable;

import org.reactivestreams.*;

import io.reactivex.*;
import io.reactivex.disposables.Disposables;

public final class CompletableFromFlowable<T> extends Completable {

    final Publisher<T> flowable;
    
    public CompletableFromFlowable(Publisher<T> flowable) {
        this.flowable = flowable;
    }

    @Override
    protected void subscribeActual(final CompletableSubscriber cs) {
        flowable.subscribe(new Subscriber<T>() {

            @Override
            public void onComplete() {
                cs.onComplete();
            }

            @Override
            public void onError(Throwable t) {
                cs.onError(t);
            }

            @Override
            public void onNext(T t) {
                // ignored
            }

            @Override
            public void onSubscribe(Subscription s) {
                cs.onSubscribe(Disposables.from(s));
                s.request(Long.MAX_VALUE);
            }
            
        });
    }

}
