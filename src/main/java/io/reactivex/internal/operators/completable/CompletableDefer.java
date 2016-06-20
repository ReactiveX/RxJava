package io.reactivex.internal.operators.completable;

import io.reactivex.*;
import io.reactivex.functions.Supplier;
import io.reactivex.internal.disposables.EmptyDisposable;

public final class CompletableDefer extends Completable {

    final Supplier<? extends CompletableConsumable> completableSupplier;
    
    public CompletableDefer(Supplier<? extends CompletableConsumable> completableSupplier) {
        this.completableSupplier = completableSupplier;
    }

    @Override
    protected void subscribeActual(CompletableSubscriber s) {
        CompletableConsumable c;
        
        try {
            c = completableSupplier.get();
        } catch (Throwable e) {
            s.onSubscribe(EmptyDisposable.INSTANCE);
            s.onError(e);
            return;
        }
        
        if (c == null) {
            s.onSubscribe(EmptyDisposable.INSTANCE);
            s.onError(new NullPointerException("The completable returned is null"));
            return;
        }
        
        c.subscribe(s);
    }

}
