package io.reactivex.internal.operators.completable;

import io.reactivex.*;
import io.reactivex.disposables.Disposable;

public final class CompletableUnsubscribeOn extends Completable {

    final CompletableConsumable source;
    
    final Scheduler scheduler;
    
    public CompletableUnsubscribeOn(CompletableConsumable source, Scheduler scheduler) {
        this.source = source;
        this.scheduler = scheduler;
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
                s.onError(e);
            }

            @Override
            public void onSubscribe(final Disposable d) {
                s.onSubscribe(new Disposable() {
                    @Override
                    public void dispose() {
                        scheduler.scheduleDirect(new Runnable() {
                            @Override
                            public void run() {
                                d.dispose();
                            }
                        });
                    }
                });
            }
            
        });
    }

}
