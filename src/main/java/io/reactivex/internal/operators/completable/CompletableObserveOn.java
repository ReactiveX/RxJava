package io.reactivex.internal.operators.completable;

import io.reactivex.*;
import io.reactivex.disposables.*;
import io.reactivex.internal.disposables.ArrayCompositeResource;

public final class CompletableObserveOn extends Completable {

    final CompletableConsumable source;
    
    final Scheduler scheduler;
    public CompletableObserveOn(CompletableConsumable source, Scheduler scheduler) {
        this.source = source;
        this.scheduler = scheduler;
    }

    @Override
    protected void subscribeActual(final CompletableSubscriber s) {

        final ArrayCompositeResource<Disposable> ad = new ArrayCompositeResource<Disposable>(2, Disposables.consumeAndDispose());
        final Scheduler.Worker w = scheduler.createWorker();
        ad.set(0, w);
        
        s.onSubscribe(ad);
        
        source.subscribe(new CompletableSubscriber() {

            @Override
            public void onComplete() {
                w.schedule(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            s.onComplete();
                        } finally {
                            ad.dispose();
                        }
                    }
                });
            }

            @Override
            public void onError(final Throwable e) {
                w.schedule(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            s.onError(e);
                        } finally {
                            ad.dispose();
                        }
                    }
                });
            }

            @Override
            public void onSubscribe(Disposable d) {
                ad.set(1, d);
            }
            
        });
    }

}
