/*
 * ORACLE PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 * 
 * Written by Doug Lea with assistance from members of JCP JSR-166
 * Expert Group and released to the public domain, as explained at
 * http://creativecommons.org/publicdomain/zero/1.0/
 * 
 * Originally from AtomicReferenceArray and modified to support primitive ints.
 * 
 * Copyright 2014 Netflix, Inc.
 */
package rx.internal.util;

import sun.misc.Unsafe;

public class AtomicIntReferenceArray {
    private static final Unsafe unsafe;
    private static final int base;
    private static final int shift;
    private final int[] array;

    static {
        try {
            unsafe = UnsafeAccess.UNSAFE;
            base = unsafe.arrayBaseOffset(Object[].class);
            int scale = unsafe.arrayIndexScale(Object[].class);
            if ((scale & (scale - 1)) != 0)
                throw new Error("data type scale not a power of two");
            shift = 31 - Integer.numberOfLeadingZeros(scale);
        } catch (Exception e) {
            throw new Error(e);
        }
    }

    private long checkedByteOffset(int i) {
        if (i < 0 || i >= array.length)
            throw new IndexOutOfBoundsException("index " + i);

        return byteOffset(i);
    }

    private static long byteOffset(int i) {
        return ((long) i << shift) + base;
    }

    /**
     * Creates a new AtomicReferenceArray of the given length, with all
     * elements initially null.
     *
     * @param length
     *            the length of the array
     */
    public AtomicIntReferenceArray(int length) {
        array = new int[length];
    }

    /**
     * Returns the length of the array.
     *
     * @return the length of the array
     */
    public final int length() {
        return array.length;
    }

    /**
     * Gets the current value at position {@code i}.
     *
     * @param i
     *            the index
     * @return the current value
     */
    public final int get(int i) {
        return getRaw(checkedByteOffset(i));
    }

    private int getRaw(long offset) {
        return unsafe.getIntVolatile(array, offset);
    }

    /**
     * Sets the element at position {@code i} to the given value.
     *
     * @param i
     *            the index
     * @param newValue
     *            the new value
     */
    public final void set(int i, int newValue) {
        unsafe.putIntVolatile(array, checkedByteOffset(i), newValue);
    }

    /**
     * Eventually sets the element at position {@code i} to the given value.
     *
     * @param i
     *            the index
     * @param newValue
     *            the new value
     * @since 1.6
     */
    public final void lazySet(int i, int newValue) {
        unsafe.putOrderedInt(array, checkedByteOffset(i), newValue);
    }

    /**
     * Atomically sets the element at position {@code i} to the given
     * value and returns the old value.
     *
     * @param i
     *            the index
     * @param newValue
     *            the new value
     * @return the previous value
     */
    public final int getAndSet(int i, int newValue) {
        return UnsafeAccess.getAndSetInt(array, checkedByteOffset(i), newValue);
    }

    /**
     * Atomically sets the element at position {@code i} to the given
     * updated value if the current value {@code ==} the expected value.
     *
     * @param i
     *            the index
     * @param expect
     *            the expected value
     * @param update
     *            the new value
     * @return {@code true} if successful. False return indicates that
     *         the actual value was not equal to the expected value.
     */
    public final boolean compareAndSet(int i, int expect, int update) {
        return compareAndSetRaw(checkedByteOffset(i), expect, update);
    }

    private boolean compareAndSetRaw(long offset, int expect, int update) {
        return unsafe.compareAndSwapInt(array, offset, expect, update);
    }

}
