package io.reactivex.observers;

import io.reactivex.annotations.Experimental;

/**
 * An interface that indicates that the implementing type is composed of individual components and exposes information
 * about their behavior.
 *
 * <p><em>NOTE:</em> This is considered a read-only public API and is not intended to be implemented by consumers.
 *
 * @since 2.1.4 - experimental
 */
@Experimental
public interface CompositeObserver {

    /**
     * @return {@code true} if a concrete error consumer implementation was supplied. Returns {@code false} if the
     * implementation is missing an error consumer and thus using a throwing default implementation.
     */
    @Experimental
    boolean onErrorImplemented();

}
