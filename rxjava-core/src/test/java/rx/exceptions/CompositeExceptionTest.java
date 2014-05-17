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
package rx.exceptions;

import static org.junit.Assert.assertEquals;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.junit.Test;

public class CompositeExceptionTest {

    private final Throwable ex1 = new Throwable("Ex1");
    private final Throwable ex2 = new Throwable("Ex2", ex1);
    private final Throwable ex3 = new Throwable("Ex3", ex2);

    public CompositeExceptionTest() {
        ex1.initCause(ex2);
    }

    private CompositeException getNewCompositeExceptionWithEx123() {
        List<Throwable> throwables = new ArrayList<Throwable>();
        throwables.add(ex1);
        throwables.add(ex2);
        throwables.add(ex3);
        return new CompositeException(throwables);
    }

    @Test(timeout = 1000)
    public void testMultipleWithSameCause() {
        Throwable rootCause = new Throwable("RootCause");
        Throwable e1 = new Throwable("1", rootCause);
        Throwable e2 = new Throwable("2", rootCause);
        Throwable e3 = new Throwable("3", rootCause);
        CompositeException ce = new CompositeException("3 failures with same root cause", Arrays.asList(e1, e2, e3));
        
        assertEquals(3, ce.getExceptions().size());
        
    }

    @Test(timeout = 1000)
    public void testAttachCallingThreadStackParentThenChild() {
        CompositeException.attachCallingThreadStack(ex1, ex2);
        assertEquals("Ex2", ex1.getCause().getMessage());
    }

    @Test(timeout = 1000)
    public void testAttachCallingThreadStackChildThenParent() {
        CompositeException.attachCallingThreadStack(ex2, ex1);
        assertEquals("Ex1", ex2.getCause().getMessage());
    }

    @Test(timeout = 1000)
    public void testAttachCallingThreadStackAddComposite() {
        CompositeException.attachCallingThreadStack(ex1, getNewCompositeExceptionWithEx123());
        assertEquals("Ex2", ex1.getCause().getMessage());
    }

    @Test(timeout = 1000)
    public void testAttachCallingThreadStackAddToComposite() {
        CompositeException compositeEx = getNewCompositeExceptionWithEx123();
        CompositeException.attachCallingThreadStack(compositeEx, ex1);
        assertEquals(CompositeException.CompositeExceptionCausalChain.MESSAGE, compositeEx.getCause().getMessage());
    }

    @Test(timeout = 1000)
    public void testAttachCallingThreadStackAddCompositeToItself() {
        CompositeException compositeEx = getNewCompositeExceptionWithEx123();
        CompositeException.attachCallingThreadStack(compositeEx, compositeEx);
        assertEquals(CompositeException.CompositeExceptionCausalChain.MESSAGE, compositeEx.getCause().getMessage());
    }

    @Test(timeout = 1000)
    public void testAttachCallingThreadStackAddExceptionsToEachOther() {
        CompositeException.attachCallingThreadStack(ex1, ex2);
        CompositeException.attachCallingThreadStack(ex2, ex1);
        assertEquals("Ex2", ex1.getCause().getMessage());
        assertEquals("Ex1", ex2.getCause().getMessage());
    }
}