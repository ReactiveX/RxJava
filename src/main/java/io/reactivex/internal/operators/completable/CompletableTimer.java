package io.reactivex.internal.operators.completable;

import java.util.concurrent.TimeUnit;

import io.reactivex.*;
import io.reactivex.disposables.MultipleAssignmentDisposable;

public final class CompletableTimer extends Completable {

    final long delay;
    final TimeUnit unit;
    final Scheduler scheduler;
    
    public CompletableTimer(long delay, TimeUnit unit, Scheduler scheduler) {
        this.delay = delay;
        this.unit = unit;
        this.scheduler = scheduler;
    }



    @Override
    protected void subscribeActual(final CompletableSubscriber s) {
        MultipleAssignmentDisposable mad = new MultipleAssignmentDisposable();
        s.onSubscribe(mad);
        if (!mad.isDisposed()) {
            mad.set(scheduler.scheduleDirect(new Runnable() {
                @Override
                public void run() {
                    s.onComplete();
                }
            }, delay, unit));
        }        
    }

}
