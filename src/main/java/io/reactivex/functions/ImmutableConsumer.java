package io.reactivex.functions;

/**
 * A functional interface (callback) that accepts a single value.
 * @param <T> the value type
 */
public interface ImmutableConsumer<T> {
    /**
     * Consume the given value.
     * @param t the value
     * @throws Exception on error
     */
    void accept(final T t) throws Exception;
}
