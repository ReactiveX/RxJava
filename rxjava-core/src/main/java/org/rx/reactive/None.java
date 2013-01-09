package org.rx.reactive;

/**
 * A common sentinel object to represent NULL as an object.
 * 
 * @ExcludeFromSDKJavadoc
 */
public class None {

    public static final None INSTANCE = new None();

    private None() {
        // prevent public instantiation
    }
}
