/**
 * Copyright 2013 Netflix, Inc.
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
package rx.codegen;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import javassist.CtMethod;
import javassist.Modifier;

/**
 * A group of methods that, if rewritten using the normal {@code AddSpecializedDynamicMethodRewriter} rewriting strategy,
 * would conflict at the JVM level.  These methods contain parameters with types that are different in Java, but the same
 * in a dynamic language.
 *
 * The first example (more are expected) came up on the following buffer method signatures:
 * public static <T> Observable<List<T>> buffer(Observable<T> source, Func0<Observable<BufferClosing>> bufferClosingSelector)
 * public Observable<List<T>> buffer(Observable<BufferOpening> bufferOpenings, Func1<BufferOpening, Observable<BufferClosing>> bufferClosingSelector)
 * These methods both would be rewritten to buffer(Observable, groovy.lang.Closure) unless we handled the conflict
 * explicitly.
 */
public class ConflictingMethodGroup {
    private final List<CtMethod> conflictingMethods;

    /**
     * Comparator that provides a stable sort on methods.  This is important, since we add 1 method per group.
     * Sort by instance methods first, and then sort both instance and static methods by their hashes.
     */
    private static Comparator<CtMethod> methodSorter = new Comparator<CtMethod>() {
        @Override
        public int compare(CtMethod m1, CtMethod m2) {
            boolean isM1Static = Modifier.isStatic(m1.getModifiers());
            boolean isM2Static = Modifier.isStatic(m2.getModifiers());
            if (isM1Static && !isM2Static) {
                return 1;
            } else if (!isM1Static && isM2Static) {
                return -1;
            } else {
                Integer hash1 = m1.hashCode();
                Integer hash2 = m2.hashCode();
                return hash1.compareTo(hash2);
            }
        }
    };

    public ConflictingMethodGroup(List<CtMethod> conflictingMethods) {
        assert(conflictingMethods.size() >= 2);
        this.conflictingMethods = new ArrayList<CtMethod>(conflictingMethods);
        Collections.sort(this.conflictingMethods, methodSorter);
    }

    /**
     * Get all methods in the group
     * @return all methods
     */
    public List<CtMethod> getMethods() {
        return this.conflictingMethods;
    }

    /**
     * Given a method, is that method the first element in this group?
     * @param method method to check
     * @return true iff the method is the first element of this group
     */
    public boolean hasHead(CtMethod method) {
        return conflictingMethods.get(0).equals(method);
    }
}
