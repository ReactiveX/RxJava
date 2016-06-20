package io.reactivex.internal.operators.completable;

import io.reactivex.*;
import io.reactivex.functions.Supplier;
import io.reactivex.internal.disposables.EmptyDisposable;

public final class CompletableErrorSupplier extends Completable {

    final Supplier<? extends Throwable> errorSupplier;
    
    public CompletableErrorSupplier(Supplier<? extends Throwable> errorSupplier) {
        this.errorSupplier = errorSupplier;
    }

    @Override
    protected void subscribeActual(CompletableSubscriber s) {
        s.onSubscribe(EmptyDisposable.INSTANCE);
        Throwable error;
        
        try {
            error = errorSupplier.get();
        } catch (Throwable e) {
            error = e;
        }
        
        if (error == null) {
            error = new NullPointerException("The error supplied is null");
        }
        s.onError(error);
    }

}
