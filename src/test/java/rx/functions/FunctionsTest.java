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
package rx.functions;

import static org.junit.Assert.*;

import java.lang.reflect.*;
import java.util.Arrays;
import java.util.concurrent.atomic.AtomicLong;

import org.junit.Test;

public class FunctionsTest {
    @Test
    public void testNotInstantiable() {
        try {
            Constructor<?> c = Functions.class.getDeclaredConstructor();
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
    
    @Test(expected = RuntimeException.class)
    public void testFromFunc0() {
        Func0<Integer> func = new Func0<Integer>() {
            @Override
            public Integer call() {
                return 0;
            }
        };
        
        Object[] params = new Object[0];
        assertEquals((Integer)0, Functions.fromFunc(func).call(params));
        
        Functions.fromFunc(func).call(Arrays.copyOf(params, params.length + 1));
    }
    
    @Test(expected = RuntimeException.class)
    public void testFromFunc1() {
        Func1<Integer, Integer> func = new Func1<Integer, Integer>() {
            @Override
            public Integer call(Integer t1) {
                return t1;
            }
        };
        
        Object[] params = new Object[] { 1 };
        assertEquals((Integer)1, Functions.fromFunc(func).call(params));
        
        Functions.fromFunc(func).call(Arrays.copyOf(params, params.length + 1));
    }
    
    @Test(expected = RuntimeException.class)
    public void testFromFunc2() {
        Func2<Integer, Integer, Integer> func = new Func2<Integer, Integer, Integer>() {
            @Override
            public Integer call(Integer t1, Integer t2) {
                return t1 | t2;
            }
        };
        
        Object[] params = new Object[] { 1, 2 };
        assertEquals((Integer)3, Functions.fromFunc(func).call(params));
        
        Functions.fromFunc(func).call(Arrays.copyOf(params, params.length + 1));
    }
    
    @Test(expected = RuntimeException.class)
    public void testFromFunc3() {
        Func3<Integer, Integer, Integer, Integer> func = new Func3<Integer, Integer, Integer, Integer>() {
            @Override
            public Integer call(Integer t1, Integer t2, Integer t3) {
                return t1 | t2 | t3;
            }
        };
        
        Object[] params = new Object[] { 1, 2, 4 };
        assertEquals((Integer)7, Functions.fromFunc(func).call(params));
        
        Functions.fromFunc(func).call(Arrays.copyOf(params, params.length + 1));
    }
    
    @Test(expected = RuntimeException.class)
    public void testFromFunc4() {
        Func4<Integer, Integer, Integer, Integer, Integer> func = 
                new Func4<Integer, Integer, Integer, Integer, Integer>() {
            @Override
            public Integer call(Integer t1, Integer t2, Integer t3, Integer t4) {
                return t1 | t2 | t3 | t4;
            }
        };
        
        Object[] params = new Object[] { 1, 2, 4, 8 };
        assertEquals((Integer)15, Functions.fromFunc(func).call(params));
        
        Functions.fromFunc(func).call(Arrays.copyOf(params, params.length + 1));
    }
    
    @Test(expected = RuntimeException.class)
    public void testFromFunc5() {
        Func5<Integer, Integer, Integer, Integer, Integer, Integer> func = 
                new Func5<Integer, Integer, Integer, Integer, Integer, Integer>() {
            @Override
            public Integer call(Integer t1, Integer t2, Integer t3, Integer t4, Integer t5) {
                return t1 | t2 | t3 | t4 | t5;
            }
        };
        
        Object[] params = new Object[] { 1, 2, 4, 8, 16 };
        assertEquals((Integer)31, Functions.fromFunc(func).call(params));
        
        Functions.fromFunc(func).call(Arrays.copyOf(params, params.length + 1));
    }
    
    @Test(expected = RuntimeException.class)
    public void testFromFunc6() {
        Func6<Integer, Integer, Integer, Integer, Integer, Integer, Integer> func = 
                new Func6<Integer, Integer, Integer, Integer, Integer, Integer, Integer>() {
            @Override
            public Integer call(Integer t1, Integer t2, Integer t3, Integer t4, Integer t5, Integer t6) {
                return t1 | t2 | t3 | t4 | t5 | t6;
            }
        };
        
        Object[] params = new Object[] { 1, 2, 4, 8, 16, 32 };
        assertEquals((Integer)63, Functions.fromFunc(func).call(params));
        
        Functions.fromFunc(func).call(Arrays.copyOf(params, params.length + 1));
    }
    
    @Test(expected = RuntimeException.class)
    public void testFromFunc7() {
        Func7<Integer, Integer, Integer, Integer, Integer, Integer, Integer, Integer> func = 
                new Func7<Integer, Integer, Integer, Integer, Integer, Integer, Integer, Integer>() {
            @Override
            public Integer call(Integer t1, Integer t2, Integer t3, Integer t4, Integer t5, Integer t6, Integer t7) {
                return t1 | t2 | t3 | t4 | t5 | t6 | t7;
            }
        };
        
        Object[] params = new Object[] { 1, 2, 4, 8, 16, 32, 64 };
        assertEquals((Integer)127, Functions.fromFunc(func).call(params));
        
        Functions.fromFunc(func).call(Arrays.copyOf(params, params.length + 1));
    }
    
    @Test(expected = RuntimeException.class)
    public void testFromFunc8() {
        Func8<Integer, Integer, Integer, Integer, Integer, Integer, Integer, Integer, Integer> func = 
                new Func8<Integer, Integer, Integer, Integer, Integer, Integer, Integer, Integer, Integer>() {
            @Override
            public Integer call(Integer t1, Integer t2, Integer t3, Integer t4, Integer t5, Integer t6, Integer t7, Integer t8) {
                return t1 | t2 | t3 | t4 | t5 | t6 | t7 | t8;
            }
        };
        
        Object[] params = new Object[] { 1, 2, 4, 8, 16, 32, 64, 128 };
        assertEquals((Integer)255, Functions.fromFunc(func).call(params));
        
        Functions.fromFunc(func).call(Arrays.copyOf(params, params.length + 1));
    }
    
    @Test(expected = RuntimeException.class)
    public void testFromFunc9() {
        Func9<Integer, Integer, Integer, Integer, Integer, Integer, Integer, Integer, Integer, Integer> func = 
                new Func9<Integer, Integer, Integer, Integer, Integer, Integer, Integer, Integer, Integer, Integer>() {
            @Override
            public Integer call(Integer t1, Integer t2, Integer t3, Integer t4, Integer t5, Integer t6, Integer t7, Integer t8, Integer t9) {
                return t1 | t2 | t3 | t4 | t5 | t6 | t7 | t8 | t9;
            }
        };
        
        Object[] params = new Object[] { 1, 2, 4, 8, 16, 32, 64, 128, 256 };
        assertEquals((Integer)511, Functions.fromFunc(func).call(params));
        
        Functions.fromFunc(func).call(Arrays.copyOf(params, params.length + 1));
    }
    
    @Test(expected = RuntimeException.class)
    public void testFromAction0() {
        final AtomicLong value = new AtomicLong();
        Action0 action = new Action0() {
            @Override
            public void call() {
                value.set(0);
            }
        };
        
        Object[] params = new Object[] { };
        Functions.fromAction(action).call(params);
        assertEquals(0, value.get());
        
        Functions.fromAction(action).call(Arrays.copyOf(params, params.length + 1));
    }
    
    @Test(expected = RuntimeException.class)
    public void testFromAction1() {
        final AtomicLong value = new AtomicLong();
        Action1<Integer> action = new Action1<Integer>() {
            @Override
            public void call(Integer t1) {
                value.set(t1);
            }
        };
        
        Object[] params = new Object[] { 1 };
        Functions.fromAction(action).call(params);
        assertEquals(1, value.get());
        
        Functions.fromAction(action).call(Arrays.copyOf(params, params.length + 1));
    }
    
    @Test(expected = RuntimeException.class)
    public void testFromAction2() {
        final AtomicLong value = new AtomicLong();
        Action2<Integer, Integer> action = new Action2<Integer, Integer>() {
            @Override
            public void call(Integer t1, Integer t2) {
                value.set(t1 | t2);
            }
        };
        
        Object[] params = new Object[] { 1, 2 };
        Functions.fromAction(action).call(params);
        assertEquals(3, value.get());
        
        Functions.fromAction(action).call(Arrays.copyOf(params, params.length + 1));
    }
    
    @Test(expected = RuntimeException.class)
    public void testFromAction3() {
        final AtomicLong value = new AtomicLong();
        Action3<Integer, Integer, Integer> action = new Action3<Integer, Integer, Integer>() {
            @Override
            public void call(Integer t1, Integer t2, Integer t3) {
                value.set(t1 | t2 | t3);
            }
        };
        
        Object[] params = new Object[] { 1, 2, 4 };
        Functions.fromAction(action).call(params);
        assertEquals(7, value.get());
        
        Functions.fromAction(action).call(Arrays.copyOf(params, params.length + 1));
    }
}
