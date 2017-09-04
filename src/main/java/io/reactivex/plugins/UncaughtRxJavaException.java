package io.reactivex.plugins;

import io.reactivex.annotations.Experimental;

/**
 * An exception thrown when an uncaught throwable was received and the {@link RxJavaPlugins#UNCAUGHT_THROW} property was
 * set. This guarantees the existence of an underlying cause retrievable via {@link #getCause()}.
 */
@Experimental
public final class UncaughtRxJavaException extends RuntimeException {

    public UncaughtRxJavaException(Throwable cause) {
        super(cause);
    }

}
