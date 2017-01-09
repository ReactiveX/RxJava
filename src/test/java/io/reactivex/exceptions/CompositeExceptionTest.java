/**
 * Copyright (c) 2016-present, RxJava Contributors.
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
package io.reactivex.exceptions;

import static org.junit.Assert.*;

import java.io.*;
import java.util.*;

import org.junit.Test;

import io.reactivex.exceptions.CompositeException.CompositeExceptionCausalChain;

public class CompositeExceptionTest {

    private final Throwable ex1 = new Throwable("Ex1");
    private final Throwable ex2 = new Throwable("Ex2", ex1);
    private final Throwable ex3 = new Throwable("Ex3", ex2);

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
        CompositeException ce = new CompositeException(e1, e2, e3);

        System.err.println("----------------------------- print composite stacktrace");
        ce.printStackTrace();
        assertEquals(3, ce.getExceptions().size());

        assertNoCircularReferences(ce);
        assertNotNull(getRootCause(ce));
        System.err.println("----------------------------- print cause stacktrace");
        ce.getCause().printStackTrace();
    }

    @Test
    public void testEmptyErrors() {
        try {
            new CompositeException();
            fail("CompositeException should fail if errors is empty");
        } catch (IllegalArgumentException e) {
            assertEquals("errors is empty", e.getMessage());
        }
        try {
            new CompositeException(new ArrayList<Throwable>());
            fail("CompositeException should fail if errors is empty");
        } catch (IllegalArgumentException e) {
            assertEquals("errors is empty", e.getMessage());
        }
    }

    @Test(timeout = 1000)
    public void testCompositeExceptionFromParentThenChild() {
        CompositeException cex = new CompositeException(ex1, ex2);

        System.err.println("----------------------------- print composite stacktrace");
        cex.printStackTrace();
        assertEquals(2, cex.getExceptions().size());

        assertNoCircularReferences(cex);
        assertNotNull(getRootCause(cex));

        System.err.println("----------------------------- print cause stacktrace");
        cex.getCause().printStackTrace();
    }

    @Test(timeout = 1000)
    public void testCompositeExceptionFromChildThenParent() {
        CompositeException cex = new CompositeException(ex2, ex1);

        System.err.println("----------------------------- print composite stacktrace");
        cex.printStackTrace();
        assertEquals(2, cex.getExceptions().size());

        assertNoCircularReferences(cex);
        assertNotNull(getRootCause(cex));

        System.err.println("----------------------------- print cause stacktrace");
        cex.getCause().printStackTrace();
    }

    @Test(timeout = 1000)
    public void testCompositeExceptionFromChildAndComposite() {
        CompositeException cex = new CompositeException(ex1, getNewCompositeExceptionWithEx123());

        System.err.println("----------------------------- print composite stacktrace");
        cex.printStackTrace();
        assertEquals(3, cex.getExceptions().size());

        assertNoCircularReferences(cex);
        assertNotNull(getRootCause(cex));

        System.err.println("----------------------------- print cause stacktrace");
        cex.getCause().printStackTrace();
    }

    @Test(timeout = 1000)
    public void testCompositeExceptionFromCompositeAndChild() {
        CompositeException cex = new CompositeException(getNewCompositeExceptionWithEx123(), ex1);

        System.err.println("----------------------------- print composite stacktrace");
        cex.printStackTrace();
        assertEquals(3, cex.getExceptions().size());

        assertNoCircularReferences(cex);
        assertNotNull(getRootCause(cex));

        System.err.println("----------------------------- print cause stacktrace");
        cex.getCause().printStackTrace();
    }

    @Test(timeout = 1000)
    public void testCompositeExceptionFromTwoDuplicateComposites() {
        List<Throwable> exs = new ArrayList<Throwable>();
        exs.add(getNewCompositeExceptionWithEx123());
        exs.add(getNewCompositeExceptionWithEx123());
        CompositeException cex = new CompositeException(exs);

        System.err.println("----------------------------- print composite stacktrace");
        cex.printStackTrace();
        assertEquals(3, cex.getExceptions().size());

        assertNoCircularReferences(cex);
        assertNotNull(getRootCause(cex));

        System.err.println("----------------------------- print cause stacktrace");
        cex.getCause().printStackTrace();
    }

    /**
     * This hijacks the Throwable.printStackTrace() output and puts it in a string, where we can look for
     * "CIRCULAR REFERENCE" (a String added by Throwable.printEnclosedStackTrace)
     */
    private static void assertNoCircularReferences(Throwable ex) {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        PrintStream printStream = new PrintStream(baos);
        ex.printStackTrace(printStream);
        assertFalse(baos.toString().contains("CIRCULAR REFERENCE"));
    }

    private static Throwable getRootCause(Throwable ex) {
        Throwable root = ex.getCause();
        if (root == null) {
            return null;
        } else {
            while (true) {
                if (root.getCause() == null) {
                    return root;
                } else {
                    root = root.getCause();
                }
            }
        }
    }

    @Test
    public void testNullCollection() {
        CompositeException composite = new CompositeException((List<Throwable>)null);
        composite.getCause();
        composite.printStackTrace();
    }
    @Test
    public void testNullElement() {
        CompositeException composite = new CompositeException(Collections.singletonList((Throwable) null));
        composite.getCause();
        composite.printStackTrace();
    }

    @Test(timeout = 1000)
    public void testCompositeExceptionWithUnsupportedInitCause() {
        Throwable t = new Throwable() {

            private static final long serialVersionUID = -3282577447436848385L;

            @Override
            public synchronized Throwable initCause(Throwable cause) {
                throw new UnsupportedOperationException();
            }
        };
        CompositeException cex = new CompositeException(t, ex1);

        System.err.println("----------------------------- print composite stacktrace");
        cex.printStackTrace();
        assertEquals(2, cex.getExceptions().size());

        assertNoCircularReferences(cex);
        assertNotNull(getRootCause(cex));

        System.err.println("----------------------------- print cause stacktrace");
        cex.getCause().printStackTrace();
    }

    @Test(timeout = 1000)
    public void testCompositeExceptionWithNullInitCause() {
        Throwable t = new Throwable("ThrowableWithNullInitCause") {

            private static final long serialVersionUID = -7984762607894527888L;

            @Override
            public synchronized Throwable initCause(Throwable cause) {
                return null;
            }
        };
        CompositeException cex = new CompositeException(t, ex1);

        System.err.println("----------------------------- print composite stacktrace");
        cex.printStackTrace();
        assertEquals(2, cex.getExceptions().size());

        assertNoCircularReferences(cex);
        assertNotNull(getRootCause(cex));

        System.err.println("----------------------------- print cause stacktrace");
        cex.getCause().printStackTrace();
    }

    @Test
    public void messageCollection() {
        CompositeException compositeException = new CompositeException(ex1, ex3);
        assertEquals("2 exceptions occurred. ", compositeException.getMessage());
    }

    @Test
    public void messageVarargs() {
        CompositeException compositeException = new CompositeException(ex1, ex2, ex3);
        assertEquals("3 exceptions occurred. ", compositeException.getMessage());
    }

    @Test
    public void complexCauses() {
        Throwable e1 = new Throwable("1");
        Throwable e2 = new Throwable("2");
        e1.initCause(e2);

        Throwable e3 = new Throwable("3");
        Throwable e4 = new Throwable("4");
        e3.initCause(e4);

        Throwable e5 = new Throwable("5");
        Throwable e6 = new Throwable("6");
        e5.initCause(e6);

        CompositeException compositeException = new CompositeException(e1, e3, e5);
        assertTrue(compositeException.getCause() instanceof CompositeExceptionCausalChain);

        List<Throwable> causeChain = new ArrayList<Throwable>();
        Throwable cause = compositeException.getCause().getCause();
        while (cause != null) {
            causeChain.add(cause);
            cause = cause.getCause();
        }
        // The original relations
        //
        // e1 -> e2
        // e3 -> e4
        // e5 -> e6
        //
        // will be set to
        //
        // e1 -> e2 -> e3 -> e4 -> e5 -> e6
        assertEquals(Arrays.asList(e1, e2, e3, e4, e5, e6), causeChain);
    }

    @Test
    public void constructorWithNull() {
        assertTrue(new CompositeException((Throwable[])null).getExceptions().get(0) instanceof NullPointerException);

        assertTrue(new CompositeException((Iterable<Throwable>)null).getExceptions().get(0) instanceof NullPointerException);

        assertTrue(new CompositeException(null, new TestException()).getExceptions().get(0) instanceof NullPointerException);
    }

    @Test
    public void printStackTrace() {
        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);

        new CompositeException(new TestException()).printStackTrace(pw);

        assertTrue(sw.toString().contains("TestException"));
    }

    @Test
    public void cyclicRootCause() {
        RuntimeException te = new RuntimeException() {

            private static final long serialVersionUID = -8492568224555229753L;
            Throwable cause;

            @Override
            public Throwable initCause(Throwable c) {
                return this;
            }

            @Override
            public Throwable getCause() {
                return cause;
            }
        };

        assertSame(te, new CompositeException(te).getCause().getCause());

        assertSame(te, new CompositeException(new RuntimeException(te)).getCause().getCause().getCause());
    }

    @Test
    public void nullRootCause() {
        RuntimeException te = new RuntimeException() {

            private static final long serialVersionUID = -8492568224555229753L;

            @Override
            public Throwable getCause() {
                return null;
            }
        };

        assertSame(te, new CompositeException(te).getCause().getCause());

        assertSame(te, new CompositeException(new RuntimeException(te)).getCause().getCause().getCause());
    }

    @Test
    public void badException() {
        Throwable e = new BadException();
        assertSame(e, new CompositeException(e).getCause().getCause());
        assertSame(e, new CompositeException(new RuntimeException(e)).getCause().getCause().getCause());
    }
}

final class BadException extends Throwable {
    private static final long serialVersionUID = 8999507293896399171L;

    @Override
    public synchronized Throwable getCause() {
        return this;
    }
}
