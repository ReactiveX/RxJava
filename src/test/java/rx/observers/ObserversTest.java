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
package rx.observers;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

import java.lang.reflect.*;
import java.util.concurrent.atomic.*;

import org.junit.Test;

import rx.exceptions.*;
import rx.functions.*;

public class ObserversTest {
    @Test
    public void testNotInstantiable() {
        try {
            Constructor<?> c = Observers.class.getDeclaredConstructor();
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
    public void testEmptyOnErrorNotImplemented() {
        try {
            Observers.empty().onError(new TestException());
            fail("OnErrorNotImplementedException not thrown!");
        } catch (OnErrorNotImplementedException ex) {
            if (!(ex.getCause() instanceof TestException)) {
                fail("TestException not wrapped, instead: " + ex.getCause());
            }
        }
    }
    @Test
    public void testCreate1OnErrorNotImplemented() {
        try {
            Observers.create(Actions.empty()).onError(new TestException());
            fail("OnErrorNotImplementedException not thrown!");
        } catch (OnErrorNotImplementedException ex) {
            if (!(ex.getCause() instanceof TestException)) {
                fail("TestException not wrapped, instead: " + ex.getCause());
            }
        }
    }
    @Test(expected = IllegalArgumentException.class)
    public void testCreate1Null() {
        Observers.create(null);
    }
    @Test(expected = IllegalArgumentException.class)
    public void testCreate2Null() {
        Action1<Throwable> throwAction = Actions.empty();
        Observers.create(null, throwAction);
    }
    @Test(expected = IllegalArgumentException.class)
    public void testCreate3Null() {
        Observers.create(Actions.empty(), null);
    }
    
    @Test(expected = IllegalArgumentException.class)
    public void testCreate4Null() {
        Action1<Throwable> throwAction = Actions.empty();
        Observers.create(null, throwAction, Actions.empty());
    }
    @Test(expected = IllegalArgumentException.class)
    public void testCreate5Null() {
        Observers.create(Actions.empty(), null, Actions.empty());
    }
    @Test(expected = IllegalArgumentException.class)
    public void testCreate6Null() {
        Action1<Throwable> throwAction = Actions.empty();
        Observers.create(Actions.empty(), throwAction, null);
    }
    
    @Test
    public void testCreate1Value() {
        final AtomicInteger value = new AtomicInteger();
        Action1<Integer> action = new Action1<Integer>() {
            @Override
            public void call(Integer t) {
                value.set(t);
            }
        };
        Observers.create(action).onNext(1);
        
        assertEquals(1, value.get());
    }
    @Test
    public void testCreate2Value() {
        final AtomicInteger value = new AtomicInteger();
        Action1<Integer> action = new Action1<Integer>() {
            @Override
            public void call(Integer t) {
                value.set(t);
            }
        };
        Action1<Throwable> throwAction = Actions.empty();
        Observers.create(action, throwAction).onNext(1);
        
        assertEquals(1, value.get());
    }
    
    @Test
    public void testCreate3Value() {
        final AtomicInteger value = new AtomicInteger();
        Action1<Integer> action = new Action1<Integer>() {
            @Override
            public void call(Integer t) {
                value.set(t);
            }
        };
        Action1<Throwable> throwAction = Actions.empty();
        Observers.create(action, throwAction, Actions.empty()).onNext(1);
        
        assertEquals(1, value.get());
    }
    
    @Test
    public void testError2() {
        final AtomicReference<Throwable> value = new AtomicReference<Throwable>();
        Action1<Throwable> action = new Action1<Throwable>() {
            @Override
            public void call(Throwable t) {
                value.set(t);
            }
        };
        TestException exception = new TestException();
        Observers.create(Actions.empty(), action).onError(exception);
        
        assertEquals(exception, value.get());
    }
    
    @Test
    public void testError3() {
        final AtomicReference<Throwable> value = new AtomicReference<Throwable>();
        Action1<Throwable> action = new Action1<Throwable>() {
            @Override
            public void call(Throwable t) {
                value.set(t);
            }
        };
        TestException exception = new TestException();
        Observers.create(Actions.empty(), action, Actions.empty()).onError(exception);
        
        assertEquals(exception, value.get());
    }
    
    @Test
    public void testCompleted() {
        Action0 action = mock(Action0.class);
        
        Action1<Throwable> throwAction = Actions.empty();
        Observers.create(Actions.empty(), throwAction, action).onCompleted();

        verify(action).call();
    }
    
    @Test
    public void testEmptyCompleted() {
        Observers.create(Actions.empty()).onCompleted();
        
        Action1<Throwable> throwAction = Actions.empty();
        Observers.create(Actions.empty(), throwAction).onCompleted();
    }
}
