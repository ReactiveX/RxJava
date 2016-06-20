package io.reactivex.internal.operators.completable;

import java.util.concurrent.*;

import io.reactivex.*;
import io.reactivex.disposables.Disposable;
import io.reactivex.internal.functions.Objects;
import io.reactivex.internal.util.Exceptions;

public enum CompletableAwait {
    ;

    public static void await(CompletableConsumable cc) {
        final CountDownLatch cdl = new CountDownLatch(1);
        final Throwable[] err = new Throwable[1];
        
        cc.subscribe(new CompletableSubscriber() {

            @Override
            public void onComplete() {
                cdl.countDown();
            }

            @Override
            public void onError(Throwable e) {
                err[0] = e;
                cdl.countDown();
            }

            @Override
            public void onSubscribe(Disposable d) {
                // ignored
            }
            
        });
        
        if (cdl.getCount() == 0) {
            if (err[0] != null) {
                Exceptions.propagate(err[0]);
            }
            return;
        }
        try {
            cdl.await();
        } catch (InterruptedException ex) {
            throw Exceptions.propagate(ex);
        }
        if (err[0] != null) {
            Exceptions.propagate(err[0]);
        }
    }
    
    public static boolean await(CompletableConsumable cc, long timeout, TimeUnit unit) {
        Objects.requireNonNull(unit, "unit is null");
        
        final CountDownLatch cdl = new CountDownLatch(1);
        final Throwable[] err = new Throwable[1];
        
        cc.subscribe(new CompletableSubscriber() {

            @Override
            public void onComplete() {
                cdl.countDown();
            }

            @Override
            public void onError(Throwable e) {
                err[0] = e;
                cdl.countDown();
            }

            @Override
            public void onSubscribe(Disposable d) {
                // ignored
            }
            
        });
        
        if (cdl.getCount() == 0) {
            if (err[0] != null) {
                Exceptions.propagate(err[0]);
            }
            return true;
        }
        boolean b;
        try {
             b = cdl.await(timeout, unit);
        } catch (InterruptedException ex) {
            throw Exceptions.propagate(ex);
        }
        if (b) {
            if (err[0] != null) {
                Exceptions.propagate(err[0]);
            }
        }
        return b;
    }
    
    public static Throwable get(CompletableConsumable cc) {
        final CountDownLatch cdl = new CountDownLatch(1);
        final Throwable[] err = new Throwable[1];
        
        cc.subscribe(new CompletableSubscriber() {

            @Override
            public void onComplete() {
                cdl.countDown();
            }

            @Override
            public void onError(Throwable e) {
                err[0] = e;
                cdl.countDown();
            }

            @Override
            public void onSubscribe(Disposable d) {
                // ignored
            }
            
        });
        
        if (cdl.getCount() == 0) {
            return err[0];
        }
        try {
            cdl.await();
        } catch (InterruptedException ex) {
            throw Exceptions.propagate(ex);
        }
        return err[0];
    }
    
    public static Throwable get(CompletableConsumable cc, long timeout, TimeUnit unit) {
        Objects.requireNonNull(unit, "unit is null");
        
        final CountDownLatch cdl = new CountDownLatch(1);
        final Throwable[] err = new Throwable[1];
        
        cc.subscribe(new CompletableSubscriber() {

            @Override
            public void onComplete() {
                cdl.countDown();
            }

            @Override
            public void onError(Throwable e) {
                err[0] = e;
                cdl.countDown();
            }

            @Override
            public void onSubscribe(Disposable d) {
                // ignored
            }
            
        });
        
        if (cdl.getCount() == 0) {
            return err[0];
        }
        boolean b;
        try {
            b = cdl.await(timeout, unit);
        } catch (InterruptedException ex) {
            throw Exceptions.propagate(ex);
        }
        if (b) {
            return err[0];
        }
        Exceptions.propagate(new TimeoutException());
        return null;
    }
}
