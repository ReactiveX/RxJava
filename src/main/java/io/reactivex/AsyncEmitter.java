package io.reactivex;

import io.reactivex.disposables.Disposable;

/**
 * Abstraction over a RxJava Subscriber that allows associating
 * a resource with it and exposes the current number of downstream
 * requested amount.
 * <p>
 * The onNext, onError and onCompleted methods should be called 
 * in a sequential manner, just like the Observer's methods. The
 * other methods are threadsafe.
 *
 * @param <T> the value type to emit
 */
public interface AsyncEmitter<T> {

    /**
     * Signal a value.
     * @param t the value, not null
     */
    void onNext(T t);
    
    /**
     * Signal an exception.
     * @param t the exception, not null
     */
    void onError(Throwable t);
    
    /**
     * Signal the completion.
     */
    void onComplete();
    
    /**
     * Sets a Disposable on this emitter; any previous Disposable
     * or Cancellation will be unsubscribed/cancelled.
     * @param s the disposable, null is allowed
     */
    void setDisposable(Disposable s);
    
    /**
     * Sets a Cancellable on this emitter; any previous Disposable
     * or Cancellation will be unsubscribed/cancelled.
     * @param c the cancellable resource, null is allowed
     */
    void setCancellation(Cancellable c);
    /**
     * The current outstanding request amount.
     * <p>This method it threadsafe.
     * @return the current outstanding request amount
     */
    long requested();
    
    /**
     * Returns true if the downstream cancelled the sequence.
     * @return true if the downstream cancelled the sequence
     */
    boolean isCancelled();
    
    /**
     * A functional interface that has a single close method
     * that can throw.
     */
    interface Cancellable {
        
        /**
         * Cancel the action or free a resource.
         * @throws Exception on error
         */
        void cancel() throws Exception;
    }
    
    /**
     * Options to handle backpressure in the emitter.
     */
    enum BackpressureMode {
        NONE,
        
        ERROR,
        
        BUFFER,
        
        DROP,
        
        LATEST
    }
}