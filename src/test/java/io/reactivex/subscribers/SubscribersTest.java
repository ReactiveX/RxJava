/**
 * Copyright 2015 Netflix, Inc.
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

package io.reactivex.subscribers;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

import java.lang.reflect.*;
import java.util.concurrent.atomic.*;
import java.util.function.Consumer;

import org.junit.*;

import io.reactivex.exceptions.*;

public class SubscribersTest {
    @Test
    public void testNotInstantiable() {
        try {
            Constructor<?> c = Subscribers.class.getDeclaredConstructor();
            c.setAccessible(true);
            Object instance = c.newInstance();
            fail("Could instantiate Actions! " + instance);
        } catch (NoSuchMethodException ex) {
            ex.printStackTrace();
        } catch (InvocationTargetException ex) {
            ex.printStackTrace();
        } catch (InstantiationException ex) {
            ex.printStackTrace();
        } catch (IllegalAccessException ex) {
            ex.printStackTrace();
        }
    }
    
    @Test
    @Ignore("Subscribers can't throw OnErrorNotImplementedException")
    public void testEmptyOnErrorNotImplemented() {
        try {
            Subscribers.empty().onError(new TestException());
            fail("OnErrorNotImplementedException not thrown!");
        } catch (OnErrorNotImplementedException ex) {
            if (!(ex.getCause() instanceof TestException)) {
                fail("TestException not wrapped, instead: " + ex.getCause());
            }
        }
    }
    @Test
    @Ignore("Subscribers can't throw OnErrorNotImplementedException")
    public void testCreate1OnErrorNotImplemented() {
        try {
            Subscribers.create(v -> { }).onError(new TestException());
            fail("OnErrorNotImplementedException not thrown!");
        } catch (OnErrorNotImplementedException ex) {
            if (!(ex.getCause() instanceof TestException)) {
                fail("TestException not wrapped, instead: " + ex.getCause());
            }
        }
    }
    @Test(expected = NullPointerException.class)
    public void testCreate1Null() {
        Subscribers.create(null);
    }
    @Test(expected = NullPointerException.class)
    public void testCreate2Null() {
        Consumer<Throwable> throwAction = e -> { };
        Subscribers.create(null, throwAction);
    }
    @Test(expected = NullPointerException.class)
    public void testCreate3Null() {
        Subscribers.create(e -> { }, null);
    }
    
    @Test(expected = NullPointerException.class)
    public void testCreate4Null() {
        Consumer<Throwable> throwAction = e -> { };
        Subscribers.create(null, throwAction, () -> { });
    }
    @Test(expected = NullPointerException.class)
    public void testCreate5Null() {
        Subscribers.create(e -> { }, null, () -> { });
    }
    @Test(expected = NullPointerException.class)
    public void testCreate6Null() {
        Consumer<Throwable> throwAction = e -> { };
        Subscribers.create(e -> { }, throwAction, null);
    }
    
    @Test
    public void testCreate1Value() {
        final AtomicInteger value = new AtomicInteger();
        Consumer<Integer> action = new Consumer<Integer>() {
            @Override
            public void accept(Integer t) {
                value.set(t);
            }
        };
        Subscribers.create(action).onNext(1);
        
        assertEquals(1, value.get());
    }
    @Test
    public void testCreate2Value() {
        final AtomicInteger value = new AtomicInteger();
        Consumer<Integer> action = new Consumer<Integer>() {
            @Override
            public void accept(Integer t) {
                value.set(t);
            }
        };
        Consumer<Throwable> throwAction = e -> { };
        Subscribers.create(action, throwAction).onNext(1);
        
        assertEquals(1, value.get());
    }
    
    @Test
    public void testCreate3Value() {
        final AtomicInteger value = new AtomicInteger();
        Consumer<Integer> action = new Consumer<Integer>() {
            @Override
            public void accept(Integer t) {
                value.set(t);
            }
        };
        Consumer<Throwable> throwAction = e -> { };
        Subscribers.create(action, throwAction, () -> { }).onNext(1);
        
        assertEquals(1, value.get());
    }
    
    @Test
    public void testError2() {
        final AtomicReference<Throwable> value = new AtomicReference<>();
        Consumer<Throwable> action = new Consumer<Throwable>() {
            @Override
            public void accept(Throwable t) {
                value.set(t);
            }
        };
        TestException exception = new TestException();
        Subscribers.create(e -> { }, action).onError(exception);
        
        assertEquals(exception, value.get());
    }
    
    @Test
    public void testError3() {
        final AtomicReference<Throwable> value = new AtomicReference<>();
        Consumer<Throwable> action = new Consumer<Throwable>() {
            @Override
            public void accept(Throwable t) {
                value.set(t);
            }
        };
        TestException exception = new TestException();
        Subscribers.create(e -> { }, action, () -> { }).onError(exception);
        
        assertEquals(exception, value.get());
    }
    
    @Test
    public void testCompleted() {
        Runnable action = mock(Runnable.class);
        
        Consumer<Throwable> throwAction = e -> { };
        Subscribers.create(e -> { }, throwAction, action).onComplete();

        verify(action).run();
    }
    @Test
    public void testEmptyCompleted() {
        Subscribers.create(e -> { }).onComplete();
        
        Consumer<Throwable> throwAction = e -> { };
        Subscribers.create(e -> { }, throwAction).onComplete();
    }
}