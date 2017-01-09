/**
 * Copyright (c) 2016-present, RxJava Contributors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in
 * compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is
 * distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See
 * the License for the specific language governing permissions and limitations under the License.
 */


/*
 * Original License: https://github.com/JCTools/JCTools/blob/master/LICENSE
 * Original location: https://github.com/JCTools/JCTools/blob/master/jctools-core/src/main/java/org/jctools/util/Pow2.java
 */
package io.reactivex.internal.util;

public final class Pow2 {
    private Pow2() {
        throw new IllegalStateException("No instances!");
    }

    /**
     * Find the next larger positive power of two value up from the given value. If value is a power of two then
     * this value will be returned.
     *
     * @param value from which next positive power of two will be found.
     * @return the next positive power of 2 or this value if it is a power of 2.
     */
    public static int roundToPowerOfTwo(final int value) {
        return 1 << (32 - Integer.numberOfLeadingZeros(value - 1));
    }

    /**
     * Is this value a power of two.
     *
     * @param value to be tested to see if it is a power of two.
     * @return true if the value is a power of 2 otherwise false.
     */
    public static boolean isPowerOfTwo(final int value) {
        return (value & (value - 1)) == 0;
    }
}
