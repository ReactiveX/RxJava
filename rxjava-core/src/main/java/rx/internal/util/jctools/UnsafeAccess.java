/*
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
 * 
 * Original License: https://github.com/JCTools/JCTools/blob/master/LICENSE
 * Original location: https://github.com/JCTools/JCTools/blob/master/jctools-core/src/main/java/org/jctools/util/UnsafeAccess.java
 */
package rx.internal.util.jctools;

import java.lang.reflect.Field;

import sun.misc.Unsafe;

public class UnsafeAccess {
    public static final Unsafe UNSAFE;
    static {
        try {
            // This is a bit of voodoo to force the unsafe object into
            // visibility and acquire it.
            // This is not playing nice, but as an established back door it is
            // not likely to be
            // taken away.
            Field field = Unsafe.class.getDeclaredField("theUnsafe");
            field.setAccessible(true);
            UNSAFE = (Unsafe) field.get(null);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

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
}