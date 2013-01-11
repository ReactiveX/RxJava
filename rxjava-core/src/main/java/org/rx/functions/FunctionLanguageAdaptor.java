package org.rx.functions;

public interface FunctionLanguageAdaptor {

    /**
     * Invoke the function and return the results.
     * 
     * @param function
     * @param args
     * @return Object results from function execution
     */
    Object call(Object function, Object[] args);

    /**
     * The Class of the Function that this adaptor serves.
     * <p>
     * Example: groovy.lang.Closure
     * 
     * @return Class
     */
    public Class<?> getFunctionClass();
}
