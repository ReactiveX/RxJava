package io.reactivex.internal.disposables;

import java.util.concurrent.atomic.AtomicReference;

import io.reactivex.disposables.Disposable;

/**
 * Utility methods for working with Disposables atomically.
 */
public enum DisposableHelper {
    ;
    
    public static final Disposable DISPOSED = Disposed.INSTANCE;
    
    public static boolean isDisposed(Disposable d) {
        return d == DISPOSED;
    }
    
    public static boolean set(AtomicReference<Disposable> field, Disposable d) {
        for (;;) {
            Disposable current = field.get();
            if (current == DISPOSED) {
                if (d != null) {
                    d.dispose();
                }
                return false;
            }
            if (field.compareAndSet(current, d)) {
                if (current != null) {
                    current.dispose();
                }
                return true;
            }
        }
    }
    
    public static boolean replace(AtomicReference<Disposable> field, Disposable d) {
        for (;;) {
            Disposable current = field.get();
            if (current == DISPOSED) {
                if (d != null) {
                    d.dispose();
                }
                return false;
            }
            if (field.compareAndSet(current, d)) {
                return true;
            }
        }
    }
    
    public static boolean dispose(AtomicReference<Disposable> field) {
        Disposable current = field.get();
        if (current != DISPOSED) {
            current = field.getAndSet(DISPOSED);
            if (current != DISPOSED || current != null) {
                current.dispose();
                return true;
            }
        }
        return false;
    }
    
    static enum Disposed implements Disposable {
        INSTANCE;
        
        @Override
        public void dispose() {
            // deliberately no-op
        }
    }
}
