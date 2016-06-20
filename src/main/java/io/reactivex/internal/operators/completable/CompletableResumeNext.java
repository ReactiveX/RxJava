package io.reactivex.internal.operators.completable;

import io.reactivex.*;
import io.reactivex.disposables.*;
import io.reactivex.exceptions.CompositeException;
import io.reactivex.functions.Function;

public final class CompletableResumeNext extends Completable {

    final CompletableConsumable source;
    
    final Function<? super Throwable, ? extends CompletableConsumable> errorMapper;
    
    public CompletableResumeNext(CompletableConsumable source,
            Function<? super Throwable, ? extends CompletableConsumable> errorMapper) {
        this.source = source;
        this.errorMapper = errorMapper;
    }



    @Override
    protected void subscribeActual(final CompletableSubscriber s) {

        final SerialDisposable sd = new SerialDisposable();
        source.subscribe(new CompletableSubscriber() {

            @Override
            public void onComplete() {
                s.onComplete();
            }

            @Override
            public void onError(Throwable e) {
                CompletableConsumable c;
                
                try {
                    c = errorMapper.apply(e);
                } catch (Throwable ex) {
                    s.onError(new CompositeException(ex, e));
                    return;
                }
                
                if (c == null) {
                    NullPointerException npe = new NullPointerException("The CompletableConsumable returned is null");
                    npe.initCause(e);
                    s.onError(npe);
                    return;
                }
                
                c.subscribe(new CompletableSubscriber() {

                    @Override
                    public void onComplete() {
                        s.onComplete();
                    }

                    @Override
                    public void onError(Throwable e) {
                        s.onError(e);
                    }

                    @Override
                    public void onSubscribe(Disposable d) {
                        sd.set(d);
                    }
                    
                });
            }

            @Override
            public void onSubscribe(Disposable d) {
                sd.set(d);
            }
            
        });
    }

}
