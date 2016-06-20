package io.reactivex.internal.operators.completable;

import io.reactivex.*;
import io.reactivex.disposables.Disposable;
import io.reactivex.exceptions.CompositeException;
import io.reactivex.functions.Predicate;

public final class CompletableOnErrorComplete extends Completable {

    final CompletableConsumable source;
    
    final Predicate<? super Throwable> predicate;
    
    public CompletableOnErrorComplete(CompletableConsumable source, Predicate<? super Throwable> predicate) {
        this.source = source;
        this.predicate = predicate;
    }

    @Override
    protected void subscribeActual(final CompletableSubscriber s) {

        source.subscribe(new CompletableSubscriber() {

            @Override
            public void onComplete() {
                s.onComplete();
            }

            @Override
            public void onError(Throwable e) {
                boolean b;
                
                try {
                    b = predicate.test(e);
                } catch (Throwable ex) {
                    s.onError(new CompositeException(ex, e));
                    return;
                }
                
                if (b) {
                    s.onComplete();
                } else {
                    s.onError(e);
                }
            }

            @Override
            public void onSubscribe(Disposable d) {
                s.onSubscribe(d);
            }
            
        });
    }

}
