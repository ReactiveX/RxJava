/**
 * Copyright 2014 Netflix, Inc.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package rx.internal.util.unsafe;

import java.lang.reflect.Field;

import sun.misc.Unsafe;

/**
 * All use of this class MUST first check that UnsafeAccess.isUnsafeAvailable() == true
 * otherwise NPEs will happen in environments without "suc.misc.Unsafe" such as Android.
 * <p>
 * Note that you can force RxJava to not use Unsafe API by setting any value to System Property
 * {@code rx.unsafe-disable}.
 */
public final class UnsafeAccess {
    private UnsafeAccess() {
        throw new IllegalStateException("No instances!");
    }

    public static final Unsafe UNSAFE;

    private static final boolean DISABLED_BY_USER = System.getProperty("rx.unsafe-disable") != null;

    static {
        Unsafe u = null;
        try {
            /*
             * This mechanism for getting UNSAFE originally from:
             * 
             * Original License: https://github.com/JCTools/JCTools/blob/master/LICENSE
             * Original location: https://github.com/JCTools/JCTools/blob/master/jctools-core/src/main/java/org/jctools/util/UnsafeAccess.java
             */
            Field field = Unsafe.class.getDeclaredField("theUnsafe");
            field.setAccessible(true);
            u = (Unsafe) field.get(null);
        } catch (Throwable e) {
            // do nothing, hasUnsafe() will return false
        }
        UNSAFE = u;
    }

    public static boolean isUnsafeAvailable() {
        return UNSAFE != null && !DISABLED_BY_USER;
    }

    /*
     * Methods below are utilities to offer functionality on top of Unsafe. Several of these already exist in Java7/8 but we must support Java 6.
     */

    public static int getAndIncrementInt(Object obj, long offset) {
        for (;;) {
            int current = UNSAFE.getIntVolatile(obj, offset);
            int next = current + 1;
            if (UNSAFE.compareAndSwapInt(obj, offset, current, next))
                return current;
        }
    }

    public static int getAndAddInt(Object obj, long offset, int n) {
        for (;;) {
            int current = UNSAFE.getIntVolatile(obj, offset);
            int next = current + n;
            if (UNSAFE.compareAndSwapInt(obj, offset, current, next))
                return current;
        }
    }

    public static int getAndSetInt(Object obj, long offset, int newValue) {
        for (;;) {
            int current = UNSAFE.getIntVolatile(obj, offset);
            if (UNSAFE.compareAndSwapInt(obj, offset, current, newValue))
                return current;
        }
    }

    public static boolean compareAndSwapInt(Object obj, long offset, int expected, int newValue) {
        return UNSAFE.compareAndSwapInt(obj, offset, expected, newValue);
    }
    
    /**
     * Returns the address of the specific field on the class and
     * wraps a NoSuchFieldException into an internal error.
     * <p>
     * One can avoid using static initializers this way and just assign
     * the address directly to the target static field.
     * @param clazz the target class
     * @param fieldName the target field name
     * @return the address (offset) of the field
     */
    public static long addressOf(Class<?> clazz, String fieldName) {
        try {
            Field f = clazz.getDeclaredField(fieldName);
            return UNSAFE.objectFieldOffset(f);
        } catch (NoSuchFieldException ex) {
            InternalError ie = new InternalError();
            ie.initCause(ex);
            throw ie;
        }
    }
}