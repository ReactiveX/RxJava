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
 * Original location: https://github.com/JCTools/JCTools/blob/master/jctools-core/src/main/java/org/jctools/util/Pow2.java
 */
package rx.internal.util.unsafe;

public class Pow2 {
    public static int findNextPositivePowerOfTwo(final int value) {
        return 1 << (32 - Integer.numberOfLeadingZeros(value - 1));
    }

    public static boolean isPowerOf2(final int value) {
        return (value & (value - 1)) == 0;
    }
}