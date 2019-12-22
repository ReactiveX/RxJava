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
package io.reactivex.rxjava3.exceptions;

import static org.junit.Assert.*;

import java.io.*;
import java.util.*;

import org.junit.Test;

import io.reactivex.rxjava3.core.RxJavaTest;

public class CompositeExceptionTest extends RxJavaTest {

    private final Throwable ex1 = new Throwable("Ex1");
    private final Throwable ex2 = new Throwable("Ex2", ex1);
    private final Throwable ex3 = new Throwable("Ex3", ex2);

    private CompositeException getNewCompositeExceptionWithEx123() {
        List<Throwable> throwables = new ArrayList<>();
        throwables.add(ex1);
        throwables.add(ex2);
        throwables.add(ex3);
        return new CompositeException(throwables);
    }

    @Test
    public void multipleWithSameCause() {
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
    public void emptyErrors() {
        try {
            new CompositeException();
            fail("CompositeException should fail if errors is empty");
        } catch (IllegalArgumentException e) {
            assertEquals("errors is empty", e.getMessage());
        }
        try {
            new CompositeException(new ArrayList<>());
            fail("CompositeException should fail if errors is empty");
        } catch (IllegalArgumentException e) {
            assertEquals("errors is empty", e.getMessage());
        }
    }

    @Test
    public void compositeExceptionFromParentThenChild() {
        CompositeException cex = new CompositeException(ex1, ex2);

        System.err.println("----------------------------- print composite stacktrace");
        cex.printStackTrace();
        assertEquals(2, cex.getExceptions().size());

        assertNoCircularReferences(cex);
        assertNotNull(getRootCause(cex));

        System.err.println("----------------------------- print cause stacktrace");
        cex.getCause().printStackTrace();
    }

    @Test
    public void compositeExceptionFromChildThenParent() {
        CompositeException cex = new CompositeException(ex2, ex1);

        System.err.println("----------------------------- print composite stacktrace");
        cex.printStackTrace();
        assertEquals(2, cex.getExceptions().size());

        assertNoCircularReferences(cex);
        assertNotNull(getRootCause(cex));

        System.err.println("----------------------------- print cause stacktrace");
        cex.getCause().printStackTrace();
    }

    @Test
    public void compositeExceptionFromChildAndComposite() {
        CompositeException cex = new CompositeException(ex1, getNewCompositeExceptionWithEx123());

        System.err.println("----------------------------- print composite stacktrace");
        cex.printStackTrace();
        assertEquals(3, cex.getExceptions().size());

        assertNoCircularReferences(cex);
        assertNotNull(getRootCause(cex));

        System.err.println("----------------------------- print cause stacktrace");
        cex.getCause().printStackTrace();
    }

    @Test
    public void compositeExceptionFromCompositeAndChild() {
        CompositeException cex = new CompositeException(getNewCompositeExceptionWithEx123(), ex1);

        System.err.println("----------------------------- print composite stacktrace");
        cex.printStackTrace();
        assertEquals(3, cex.getExceptions().size());

        assertNoCircularReferences(cex);
        assertNotNull(getRootCause(cex));

        System.err.println("----------------------------- print cause stacktrace");
        cex.getCause().printStackTrace();
    }

    @Test
    public void compositeExceptionFromTwoDuplicateComposites() {
        List<Throwable> exs = new ArrayList<>();
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
    public void nullCollection() {
        CompositeException composite = new CompositeException((List<Throwable>)null);
        composite.getCause();
        composite.printStackTrace();
    }

    @Test
    public void nullElement() {
        CompositeException composite = new CompositeException(Collections.singletonList((Throwable) null));
        composite.getCause();
        composite.printStackTrace();
    }

    @Test
    public void compositeExceptionWithUnsupportedInitCause() {
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

    @Test
    public void compositeExceptionWithNullInitCause() {
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
    public void badException() {
        Throwable e = new BadException();
        assertSame(e, new CompositeException(e).getCause().getCause());
        assertSame(e, new CompositeException(new RuntimeException(e)).getCause().getCause().getCause());
    }

    @Test
    public void exceptionOverview() {
        CompositeException composite = new CompositeException(
                new TestException("ex1"),
                new TestException("ex2"),
                new TestException("ex3", new TestException("ex4"))
        );

        String overview = composite.getCause().getMessage();

        assertTrue(overview, overview.contains("Multiple exceptions (3)"));
        assertTrue(overview, overview.contains("io.reactivex.rxjava3.exceptions.TestException: ex1"));
        assertTrue(overview, overview.contains("io.reactivex.rxjava3.exceptions.TestException: ex2"));
        assertTrue(overview, overview.contains("io.reactivex.rxjava3.exceptions.TestException: ex3"));
        assertTrue(overview, overview.contains("io.reactivex.rxjava3.exceptions.TestException: ex4"));
        assertTrue(overview, overview.contains("at io.reactivex.rxjava3.exceptions.CompositeExceptionTest.exceptionOverview"));
    }

    @Test
    public void causeWithExceptionWithoutStacktrace() {
        CompositeException composite = new CompositeException(
                new TestException("ex1"),
                new CompositeException.ExceptionOverview("example")
        );

        String overview = composite.getCause().getMessage();

        assertTrue(overview, overview.contains("Multiple exceptions (2)"));
        assertTrue(overview, overview.contains("io.reactivex.rxjava3.exceptions.TestException: ex1"));
        assertTrue(overview, overview.contains("io.reactivex.rxjava3.exceptions.CompositeException.ExceptionOverview: example"));

        assertEquals(overview, 2, overview.split("at\\s").length);
    }

    @Test
    public void reoccurringException() {
        TestException ex0 = new TestException("ex0");
        TestException ex1 = new TestException("ex1", ex0);
        CompositeException composite = new CompositeException(
                ex1,
                new TestException("ex2", ex1)
        );

        String overview = composite.getCause().getMessage();
        System.err.println(overview);

        assertTrue(overview, overview.contains("Multiple exceptions (2)"));
        assertTrue(overview, overview.contains("io.reactivex.rxjava3.exceptions.TestException: ex0"));
        assertTrue(overview, overview.contains("io.reactivex.rxjava3.exceptions.TestException: ex1"));
        assertTrue(overview, overview.contains("io.reactivex.rxjava3.exceptions.TestException: ex2"));
        assertTrue(overview, overview.contains("(cause not expanded again) io.reactivex.rxjava3.exceptions.TestException: ex0"));
        assertEquals(overview, 5, overview.split("at\\s").length);
    }

    @Test
    public void nestedMultilineMessage() {
        TestException ex1 = new TestException("ex1");
        TestException ex2 = new TestException("ex2");
        CompositeException composite1 = new CompositeException(
                ex1,
                ex2
        );
        TestException ex3 = new TestException("ex3");
        TestException ex4 = new TestException("ex4", composite1);

        CompositeException composite2 = new CompositeException(
                ex3,
                ex4
        );

        String overview = composite2.getCause().getMessage();
        System.err.println(overview);

        assertTrue(overview, overview.contains("        Multiple exceptions (2)"));
        assertTrue(overview, overview.contains("        |-- io.reactivex.rxjava3.exceptions.TestException: ex1"));
        assertTrue(overview, overview.contains("        |-- io.reactivex.rxjava3.exceptions.TestException: ex2"));
    }

    @Test
    public void singleExceptionIsTheCause() {
        TestException ex = new TestException("ex1");
        CompositeException composite = new CompositeException(ex);

        assertSame(composite.getCause(), ex);
    }
}

final class BadException extends Throwable {
    private static final long serialVersionUID = 8999507293896399171L;

    @Override
    public synchronized Throwable getCause() {
        return this;
    }
}
