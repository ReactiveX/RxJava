/**
 * Copyright 2016 Netflix, Inc.
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

package io.reactivex;

import static org.junit.Assert.*;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;

import java.lang.reflect.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

import org.mockito.Mockito;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.reactivestreams.*;

import io.reactivex.exceptions.CompositeException;
import io.reactivex.functions.Consumer;
import io.reactivex.internal.fuseable.SimpleQueue;
import io.reactivex.internal.util.ExceptionHelper;
import io.reactivex.observers.TestObserver;
import io.reactivex.plugins.RxJavaPlugins;
import io.reactivex.subscribers.TestSubscriber;

/**
 * Common methods for helping with tests from 1.x mostly.
 */
public enum TestHelper {
    ;
    /**
     * Mocks a subscriber and prepares it to request Long.MAX_VALUE.
     * @param <T> the value type
     * @return the mocked subscriber
     */
    @SuppressWarnings("unchecked")
    public static <T> Subscriber<T> mockSubscriber() {
        Subscriber<T> w = mock(Subscriber.class);
        
        Mockito.doAnswer(new Answer<Object>() {
            @Override
            public Object answer(InvocationOnMock a) throws Throwable {
                Subscription s = a.getArgumentAt(0, Subscription.class);
                s.request(Long.MAX_VALUE);
                return null;
            }
        }).when(w).onSubscribe((Subscription)any());
        
        return w;
    }
    
    /**
     * Mocks an Observer with the proper receiver type.
     * @param <T> the value type
     * @return the mocked observer
     */
    @SuppressWarnings("unchecked")
    public static <T> Observer<T> mockObserver() {
        return mock(Observer.class);
    }
    
    /**
     * Validates that the given class, when forcefully instantiated throws
     * an IllegalArgumentException("No instances!") exception.
     * @param clazz the class to test, not null
     */
    public static void checkUtilityClass(Class<?> clazz) {
        try {
            Constructor<?> c = clazz.getDeclaredConstructor();
            
            c.setAccessible(true);
            
            try {
                c.newInstance();
                fail("Should have thrown InvocationTargetException(IllegalStateException)");
            } catch (InvocationTargetException ex) {
                assertEquals("No instances!", ex.getCause().getMessage());
            }
        } catch (Exception ex) {
            AssertionError ae = new AssertionError(ex.toString());
            ae.initCause(ex);
            throw ae;
        }
    }
    
    public static List<Throwable> trackPluginErrors() {
        final List<Throwable> list = Collections.synchronizedList(new ArrayList<Throwable>());
        
        RxJavaPlugins.setErrorHandler(new Consumer<Throwable>() {
            @Override
            public void accept(Throwable t) {
                list.add(t);
            }
        });
        
        return list;
    }

    public static void assertError(List<Throwable> list, int index, Class<? extends Throwable> clazz) {
        assertTrue(list.get(index).toString(), clazz.isInstance(list.get(index)));
    }

    public static void assertError(List<Throwable> list, int index, Class<? extends Throwable> clazz, String message) {
        assertTrue(list.get(index).toString(), clazz.isInstance(list.get(index)));
        assertEquals(message, list.get(index).getMessage());
    }
    
    public static void assertError(TestObserver<?> ts, int index, Class<? extends Throwable> clazz) {
        Throwable ex = ts.errors().get(0);
        if (ex instanceof CompositeException) {
            CompositeException ce = (CompositeException) ex;
            List<Throwable> cel = ce.getExceptions();
            assertTrue(cel.get(index).toString(), clazz.isInstance(cel.get(index)));
        } else {
            fail(ex.toString() + ": not a CompositeException");
        }
    }

    public static void assertError(TestSubscriber<?> ts, int index, Class<? extends Throwable> clazz) {
        Throwable ex = ts.errors().get(0);
        if (ex instanceof CompositeException) {
            CompositeException ce = (CompositeException) ex;
            List<Throwable> cel = ce.getExceptions();
            assertTrue(cel.get(index).toString(), clazz.isInstance(cel.get(index)));
        } else {
            fail(ex.toString() + ": not a CompositeException");
        }
    }

    public static void assertError(TestObserver<?> ts, int index, Class<? extends Throwable> clazz, String message) {
        Throwable ex = ts.errors().get(0);
        if (ex instanceof CompositeException) {
            CompositeException ce = (CompositeException) ex;
            List<Throwable> cel = ce.getExceptions();
            assertTrue(cel.get(index).toString(), clazz.isInstance(cel.get(index)));
            assertEquals(message, cel.get(index).getMessage());
        } else {
            fail(ex.toString() + ": not a CompositeException");
        }
    }

    public static void assertError(TestSubscriber<?> ts, int index, Class<? extends Throwable> clazz, String message) {
        Throwable ex = ts.errors().get(0);
        if (ex instanceof CompositeException) {
            CompositeException ce = (CompositeException) ex;
            List<Throwable> cel = ce.getExceptions();
            assertTrue(cel.get(index).toString(), clazz.isInstance(cel.get(index)));
            assertEquals(message, cel.get(index).getMessage());
        } else {
            fail(ex.toString() + ": not a CompositeException");
        }
    }

    /**
     * Verify that a specific enum type has no enum constants.
     * @param <E> the enum type
     * @param e the enum class instance
     */
    public static <E extends Enum<E>> void assertEmptyEnum(Class<E> e) {
        assertEquals(0, e.getEnumConstants().length);
        
        try {
            try {
                Method m0 = e.getDeclaredMethod("values");

                Object[] a = (Object[])m0.invoke(null);
                assertEquals(0, a.length);
                
                Method m = e.getDeclaredMethod("valueOf", String.class);
            
                m.invoke("INSTANCE");
                fail("Should have thrown!");
            } catch (InvocationTargetException ex) {
                fail(ex.toString());
            } catch (IllegalAccessException ex) {
                fail(ex.toString());
            } catch (IllegalArgumentException ex) {
                // we expected this
            }
        } catch (NoSuchMethodException ex) {
            fail(ex.toString());
        }
    }
    
    public static void assertBadRequestReported(Publisher<?> source) {
        List<Throwable> list = trackPluginErrors();
        try {
            final CountDownLatch cdl = new CountDownLatch(1);
            
            source.subscribe(new Subscriber<Object>() {

                @Override
                public void onSubscribe(Subscription s) {
                    try {
                        s.request(-99);
                        s.cancel();
                        s.cancel();
                    } finally {
                        cdl.countDown();
                    }
                }

                @Override
                public void onNext(Object t) {
                    
                }

                @Override
                public void onError(Throwable t) {
                    
                }

                @Override
                public void onComplete() {
                    
                }
                
            });
            
            try {
                assertTrue(cdl.await(5, TimeUnit.SECONDS));
            } catch (InterruptedException ex) {
                throw new AssertionError(ex.getMessage());
            }
            
            assertTrue(list.toString(), list.get(0) instanceof IllegalArgumentException);
            assertEquals("n > 0 required but it was -99", list.get(0).getMessage());
        } finally {
            RxJavaPlugins.setErrorHandler(null);
        }
    }
    
    /**
     * Synchronizes the execution of two runnables (as much as possible)
     * to test race conditions.
     * <p>The method blocks until both have run to completion.
     * @param r1 the first runnable
     * @param r2 the second runnable
     * @param s the scheduler to use
     */
    public static void race(final Runnable r1, final Runnable r2, Scheduler s) {
        final AtomicInteger count = new AtomicInteger(2);
        final CountDownLatch cdl = new CountDownLatch(2);
        
        final Throwable[] errors = { null, null };
        
        s.scheduleDirect(new Runnable() {
            @Override
            public void run() {
                if (count.decrementAndGet() != 0) {
                    while (count.get() != 0);
                }
                
                try {
                    try {
                        r1.run();
                    } catch (Throwable ex) {
                        errors[0] = ex;
                    }
                } finally {
                    cdl.countDown();
                }
            }
        });
        
        if (count.decrementAndGet() != 0) {
            while (count.get() != 0);
        }
        
        try {
            try {
                r2.run();
            } catch (Throwable ex) {
                errors[1] = ex;
            }
        } finally {
            cdl.countDown();
        }
        
        try {
            if (!cdl.await(5, TimeUnit.SECONDS)) {
                throw new AssertionError("The wait timed out!");
            }
        } catch (InterruptedException ex) {
            throw new RuntimeException(ex);
        }
        if (errors[0] != null && errors[1] == null) {
            throw ExceptionHelper.wrapOrThrow(errors[0]);
        }
        
        if (errors[0] == null && errors[1] != null) {
            throw ExceptionHelper.wrapOrThrow(errors[1]);
        }
        
        if (errors[0] != null && errors[1] != null) {
            throw new CompositeException(errors);
        }
    }
    
    public static List<Throwable> compositeList(Throwable ex) {
        return ((CompositeException)ex).getExceptions();
    }
    
    /**
     * Assert that the offer methods throw UnsupportedOperationExcetpion.
     * @param q the queue implementation
     */
    public static void assertNoOffer(SimpleQueue<?> q) {
        try {
            q.offer(null);
            fail("Should have thrown!");
        } catch (UnsupportedOperationException ex) {
            // expected
        }
        try {
            q.offer(null, null);
            fail("Should have thrown!");
        } catch (UnsupportedOperationException ex) {
            // expected
        }
    }
    
    @SuppressWarnings("unchecked")
    public static <E extends Enum<E>> void checkEnum(Class<E> enumClass) {
        try {
            Method m = enumClass.getMethod("values");
            m.setAccessible(true);
            Method e = enumClass.getMethod("valueOf", String.class);
            m.setAccessible(true);
            
            for (Enum<E> o : (Enum<E>[])m.invoke(null)) {
                assertSame(o, e.invoke(null, o.name()));
            }
            
        } catch (Throwable ex) {
            throw ExceptionHelper.wrapOrThrow(ex);
        }
    }
}
