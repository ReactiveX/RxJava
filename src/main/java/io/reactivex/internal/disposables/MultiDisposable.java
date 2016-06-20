package io.reactivex.internal.disposables;

import java.util.concurrent.atomic.AtomicReference;

import io.reactivex.disposables.Disposable;

/**
 * A disposable, directly extending AtomicReference, that helps manage
 * a single-element container of a disposable.
 * <p>
 * Note that due to the base class, the MultiDisposable leaks the AtomicReference
 * API. Please make sure you use the mutator method of this class only.
 */
public final class MultiDisposable extends AtomicReference<Disposable> implements Disposable {

    /** */
    private static final long serialVersionUID = 6547048646662074692L;

    public boolean update(Disposable d) {
        return DisposableHelper.set(this, d);
    }
    
    public boolean replace(Disposable d) {
        return DisposableHelper.replace(this, d);
    }
    
    @Override
    public void dispose() {
        DisposableHelper.dispose(this);
        
    }
    
    public boolean isDisposed() {
        return DisposableHelper.isDisposed(get());
    }
}

