package io.reactivex.internal.operators.completable;

import io.reactivex.*;

public final class CompletableSubscribeOn extends Completable {
    final CompletableConsumable source;
    
    final Scheduler scheduler;
    
    public CompletableSubscribeOn(CompletableConsumable source, Scheduler scheduler) {
        this.source = source;
        this.scheduler = scheduler;
    }



    @Override
    protected void subscribeActual(final CompletableSubscriber s) {
     // FIXME cancellation of this schedule
        scheduler.scheduleDirect(new Runnable() {
            @Override
            public void run() {
                source.subscribe(s);
            }
        });
    }
}
