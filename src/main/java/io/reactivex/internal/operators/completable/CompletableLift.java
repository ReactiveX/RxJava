package io.reactivex.internal.operators.completable;

import io.reactivex.*;
import io.reactivex.internal.util.Exceptions;
import io.reactivex.plugins.RxJavaPlugins;

public final class CompletableLift extends Completable {

    final CompletableConsumable source;
    
    final CompletableOperator onLift;
    
    public CompletableLift(CompletableConsumable source, CompletableOperator onLift) {
        this.source = source;
        this.onLift = onLift;
    }

    @Override
    protected void subscribeActual(CompletableSubscriber s) {
        try {
            // TODO plugin wrapping

            CompletableSubscriber sw = onLift.apply(s);
            
            source.subscribe(sw);
        } catch (NullPointerException ex) {
            throw ex;
        } catch (Throwable ex) {
            Exceptions.throwIfFatal(ex);
            RxJavaPlugins.onError(ex);
        }
    }

}
