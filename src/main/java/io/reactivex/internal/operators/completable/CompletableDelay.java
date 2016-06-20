package io.reactivex.internal.operators.completable;

import java.util.concurrent.TimeUnit;

import io.reactivex.*;
import io.reactivex.disposables.*;

public final class CompletableDelay extends Completable {

    final CompletableConsumable source;
    
    final long delay;
    
    final TimeUnit unit;
    
    final Scheduler scheduler;
    
    final boolean delayError;
    
    public CompletableDelay(CompletableConsumable source, long delay, TimeUnit unit, Scheduler scheduler, boolean delayError) {
        this.source = source;
        this.delay = delay;
        this.unit = unit;
        this.scheduler = scheduler;
        this.delayError = delayError;
    }

    @Override
    protected void subscribeActual(final CompletableSubscriber s) {
        final CompositeDisposable set = new CompositeDisposable();
        
        source.subscribe(new CompletableSubscriber() {

            
            @Override
            public void onComplete() {
                set.add(scheduler.scheduleDirect(new Runnable() {
                    @Override
                    public void run() {
                        s.onComplete();
                    }
                }, delay, unit));
            }

            @Override
            public void onError(final Throwable e) {
                if (delayError) {
                    set.add(scheduler.scheduleDirect(new Runnable() {
                        @Override
                        public void run() {
                            s.onError(e);
                        }
                    }, delay, unit));
                } else {
                    s.onError(e);
                }
            }

            @Override
            public void onSubscribe(Disposable d) {
                set.add(d);
                s.onSubscribe(set);
            }
            
        });
    }

}
