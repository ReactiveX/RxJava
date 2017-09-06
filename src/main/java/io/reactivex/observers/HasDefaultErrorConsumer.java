package io.reactivex.observers;

import io.reactivex.annotations.Experimental;

/**
 * An interface that indicates that the implementing type has default implementations for error consumption.
 */
@Experimental
public interface HasDefaultErrorConsumer {

    /**
     * @return {@code true} if the implementation is missing an error consumer and thus using a throwing default
     * implementation. Returns {@code false} if a concrete error consumer implementation was supplied.
     */
    @Experimental
    boolean hasMissingErrorConsumer();

}
