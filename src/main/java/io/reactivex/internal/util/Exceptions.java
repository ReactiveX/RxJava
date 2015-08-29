package io.reactivex.internal.util;

public enum Exceptions {
    ;
    /**
     * Convenience method to throw a {@code RuntimeException} and {@code Error} directly
     * or wrap any other exception type into a {@code RuntimeException}.
     * @param t the exception to throw directly or wrapped
     * @return because {@code propagate} itself throws an exception or error, this is a sort of phantom return
     *         value; {@code propagate} does not actually return anything
     */
    public static RuntimeException propagate(Throwable t) {
        /*
         * The return type of RuntimeException is a trick for code to be like this:
         * 
         * throw Exceptions.propagate(e);
         * 
         * Even though nothing will return and throw via that 'throw', it allows the code to look like it
         * so it's easy to read and understand that it will always result in a throw.
         */
        if (t instanceof RuntimeException) {
            throw (RuntimeException) t;
        } else if (t instanceof Error) {
            throw (Error) t;
        } else {
            throw new RuntimeException(t);
        }
    }

}
