package io.reactivex.internal.operators.completable;

import java.util.concurrent.atomic.AtomicBoolean;

import io.reactivex.*;
import io.reactivex.disposables.Disposable;
import io.reactivex.exceptions.CompositeException;
import io.reactivex.functions.*;
import io.reactivex.internal.disposables.EmptyDisposable;
import io.reactivex.plugins.RxJavaPlugins;

public final class CompletableUsing<R> extends Completable {

    final Supplier<R> resourceSupplier;
    final Function<? super R, ? extends CompletableConsumable> completableFunction;
    final Consumer<? super R> disposer;
    final boolean eager;
    
    public CompletableUsing(Supplier<R> resourceSupplier,
            Function<? super R, ? extends CompletableConsumable> completableFunction, Consumer<? super R> disposer,
            boolean eager) {
        this.resourceSupplier = resourceSupplier;
        this.completableFunction = completableFunction;
        this.disposer = disposer;
        this.eager = eager;
    }



    @Override
    protected void subscribeActual(final CompletableSubscriber s) {
        final R resource;
        
        try {
            resource = resourceSupplier.get();
        } catch (Throwable e) {
            s.onSubscribe(EmptyDisposable.INSTANCE);
            s.onError(e);
            return;
        }
        
        CompletableConsumable cs;
        
        try {
            cs = completableFunction.apply(resource);
        } catch (Throwable e) {
            s.onSubscribe(EmptyDisposable.INSTANCE);
            s.onError(e);
            return;
        }
        
        if (cs == null) {
            s.onSubscribe(EmptyDisposable.INSTANCE);
            s.onError(new NullPointerException("The completable supplied is null"));
            return;
        }
        
        final AtomicBoolean once = new AtomicBoolean();
        
        cs.subscribe(new CompletableSubscriber() {
            Disposable d;
            void disposeThis() {
                d.dispose();
                if (once.compareAndSet(false, true)) {
                    try {
                        disposer.accept(resource);
                    } catch (Throwable ex) {
                        RxJavaPlugins.onError(ex);
                    }
                }
            }

            @Override
            public void onComplete() {
                if (eager) {
                    if (once.compareAndSet(false, true)) {
                        try {
                            disposer.accept(resource);
                        } catch (Throwable ex) {
                            s.onError(ex);
                            return;
                        }
                    }
                }
                
                s.onComplete();
                
                if (!eager) {
                    disposeThis();
                }
            }

            @Override
            public void onError(Throwable e) {
                if (eager) {
                    if (once.compareAndSet(false, true)) {
                        try {
                            disposer.accept(resource);
                        } catch (Throwable ex) {
                            e = new CompositeException(ex, e);
                        }
                    }
                }
                
                s.onError(e);
                
                if (!eager) {
                    disposeThis();
                }
            }
            
            @Override
            public void onSubscribe(Disposable d) {
                this.d = d;
                s.onSubscribe(new Disposable() {
                    @Override
                    public void dispose() {
                        disposeThis();
                    }
                });
            }
        });
    }

}
