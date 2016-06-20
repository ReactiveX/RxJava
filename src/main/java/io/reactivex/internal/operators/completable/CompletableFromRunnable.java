package io.reactivex.internal.operators.completable;

import io.reactivex.*;
import io.reactivex.disposables.BooleanDisposable;

public final class CompletableFromRunnable extends Completable {

    final Runnable run;

    public CompletableFromRunnable(Runnable run) {
        this.run = run;
    }

    @Override
    protected void subscribeActual(CompletableSubscriber s) {
        BooleanDisposable bs = new BooleanDisposable();
        s.onSubscribe(bs);
        try {
            run.run();
        } catch (Throwable e) {
            if (!bs.isDisposed()) {
                s.onError(e);
            }
            return;
        }
        if (!bs.isDisposed()) {
            s.onComplete();
        }
    }

}
