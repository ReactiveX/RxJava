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

public class ActionsTest {

    @Test
    public void testEmptyArities() {
        Action0 a0 = Actions.empty();
        a0.call();
        
        Action1<Integer> a1 = Actions.empty();
        a1.call(1);
        
        Action2<Integer, Integer> a2 = Actions.empty();
        a2.call(1, 2);

        Action3<Integer, Integer, Integer> a3 = Actions.empty();
        a3.call(1, 2, 3);

        Action4<Integer, Integer, Integer, Integer> a4 = Actions.empty();
        a4.call(1, 2, 3, 4);
        
        Action5<Integer, Integer, Integer, Integer, Integer> a5 = Actions.empty();
        a5.call(1, 2, 3, 4, 5);

        Action6<Integer, Integer, Integer, Integer, Integer, Integer> a6 = Actions.empty();
        a6.call(1, 2, 3, 4, 5, 6);

        Action7<Integer, Integer, Integer, Integer, Integer, Integer, Integer> a7 = Actions.empty();
        a7.call(1, 2, 3, 4, 5, 6, 7);

        Action8<Integer, Integer, Integer, Integer, Integer, Integer, Integer, Integer> a8 = Actions.empty();
        a8.call(1, 2, 3, 4, 5, 6, 7, 8);

        Action9<Integer, Integer, Integer, Integer, Integer, Integer, Integer, Integer, Integer> a9 = Actions.empty();
        a9.call(1, 2, 3, 4, 5, 6, 7, 8, 9);

        ActionN an0 = Actions.empty();
        an0.call();

        ActionN an1 = Actions.empty();
        an1.call(1);

        ActionN ann = Actions.empty();
        ann.call(1, 2, 3, 4, 5, 6, 7, 8, 9, 10);

        ActionN annn = Actions.empty();
        annn.call(1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 
                11, 12, 13, 14, 15, 16, 17, 18, 19, 20);
    }
    
    @Test
    public void testToFunc0() {
        final AtomicLong value = new AtomicLong(-1L);
        final Action0 action = new Action0() {
            @Override
            public void call() {
                value.set(0);
            }
        };
        
        assertNull(Actions.toFunc(action).call());
        assertEquals(0, value.get());
        value.set(-1L);
        assertEquals((Integer)0, Actions.toFunc(action, 0).call());
        assertEquals(0, value.get());
    }
    
    @Test
    public void testToFunc1() {
        final AtomicLong value = new AtomicLong(-1L);
        final Action1<Integer> action = new Action1<Integer>() {
            @Override
            public void call(Integer t1) {
                value.set(t1);
            }
        };
        
        assertNull(Actions.toFunc(action).call(1));
        assertEquals(1, value.get());
        value.set(-1L);
        assertEquals((Integer)0, Actions.toFunc(action, 0).call(1));
        assertEquals(1, value.get());
    }
    
    @Test
    public void testToFunc2() {
        final AtomicLong value = new AtomicLong(-1L);
        final Action2<Integer, Integer> action = new Action2<Integer, Integer>() {
            @Override
            public void call(Integer t1, Integer t2) {
                value.set(t1 | t2);
            }
        };
        
        assertNull(Actions.toFunc(action).call(1, 2));
        assertNull(Actions.toFunc(action).call(1, 2));
        assertEquals(3, value.get());
        value.set(-1L);
        assertEquals((Integer)0, Actions.toFunc(action, 0).call(1, 2));
        assertEquals(3, value.get());
    }
    
    @Test
    public void testToFunc3() {
        final AtomicLong value = new AtomicLong(-1L);
        final Action3<Integer, Integer, Integer> action = new Action3<Integer, Integer, Integer>() {
            @Override
            public void call(Integer t1, Integer t2, Integer t3) {
                value.set(t1 | t2 | t3);
            }
        };
        
        assertNull(Actions.toFunc(action).call(1, 2, 4));
        assertEquals(7, value.get());
        value.set(-1L);
        assertEquals((Integer)0, Actions.toFunc(action, 0).call(1, 2, 4));
        assertEquals(7, value.get());
    }
    
    @Test
    public void testToFunc4() {
        final AtomicLong value = new AtomicLong(-1L);
        final Action4<Integer, Integer, Integer, Integer> action = new Action4<Integer, Integer, Integer, Integer>() {
            @Override
            public void call(Integer t1, Integer t2, Integer t3, Integer t4) {
                value.set(t1 | t2 | t3 | t4);
            }
        };
        
        assertNull(Actions.toFunc(action).call(1, 2, 4, 8));
        assertEquals(15, value.get());
        value.set(-1L);
        assertEquals((Integer)0, Actions.toFunc(action, 0).call(1, 2, 4, 8));
        assertEquals(15, value.get());
    }
    
    @Test
    public void testToFunc5() {
        final AtomicLong value = new AtomicLong(-1L);
        final Action5<Integer, Integer, Integer, Integer, Integer> action = 
                new Action5<Integer, Integer, Integer, Integer, Integer>() {
            @Override
            public void call(Integer t1, Integer t2, Integer t3, Integer t4, Integer t5) {
                value.set(t1 | t2 | t3 | t4 | t5);
            }
        };
        
        assertNull(Actions.toFunc(action).call(1, 2, 4, 8, 16));
        assertEquals(31, value.get());
        value.set(-1L);
        assertEquals((Integer)0, Actions.toFunc(action, 0).call(1, 2, 4, 8, 16));
        assertEquals(31, value.get());
    }
    
    @Test
    public void testToFunc6() {
        final AtomicLong value = new AtomicLong(-1L);
        final Action6<Integer, Integer, Integer, Integer, Integer, Integer> action = 
                new Action6<Integer, Integer, Integer, Integer, Integer, Integer>() {
            @Override
            public void call(Integer t1, Integer t2, Integer t3, Integer t4, Integer t5, Integer t6) {
                value.set(t1 | t2 | t3 | t4 | t5 | t6);
            }
        };
        
        assertNull(Actions.toFunc(action).call(1, 2, 4, 8, 16, 32));
        assertEquals(63, value.get());
        value.set(-1L);
        assertEquals((Integer)0, Actions.toFunc(action, 0).call(1, 2, 4, 8, 16, 32));
        assertEquals(63, value.get());
    }
    
    @Test
    public void testToFunc7() {
        final AtomicLong value = new AtomicLong(-1L);
        final Action7<Integer, Integer, Integer, Integer, Integer, Integer, Integer> action = 
                new Action7<Integer, Integer, Integer, Integer, Integer, Integer, Integer>() {
            @Override
            public void call(Integer t1, Integer t2, Integer t3, Integer t4, Integer t5, Integer t6, Integer t7) {
                value.set(t1 | t2 | t3 | t4 | t5 | t6 | t7);
            }
        };
        
        assertNull(Actions.toFunc(action).call(1, 2, 4, 8, 16, 32, 64));
        assertEquals(127, value.get());
        value.set(-1L);
        assertEquals((Integer)0, Actions.toFunc(action, 0).call(1, 2, 4, 8, 16, 32, 64));
        assertEquals(127, value.get());
    }
    @Test
    public void testToFunc8() {
        final AtomicLong value = new AtomicLong(-1L);
        final Action8<Integer, Integer, Integer, Integer, Integer, Integer, Integer, Integer> action = 
                new Action8<Integer, Integer, Integer, Integer, Integer, Integer, Integer, Integer>() {
            @Override
            public void call(Integer t1, Integer t2, Integer t3, Integer t4, Integer t5, Integer t6, Integer t7, Integer t8) {
                value.set(t1 | t2 | t3 | t4 | t5 | t6 | t7 | t8);
            }
        };
        
        assertNull(Actions.toFunc(action).call(1, 2, 4, 8, 16, 32, 64, 128));
        assertEquals(255, value.get());
        value.set(-1L);
        assertEquals((Integer)0, Actions.toFunc(action, 0).call(1, 2, 4, 8, 16, 32, 64, 128));
        assertEquals(255, value.get());
    }
    @Test
    public void testToFunc9() {
        final AtomicLong value = new AtomicLong(-1L);
        final Action9<Integer, Integer, Integer, Integer, Integer, Integer, Integer, Integer, Integer> action = 
                new Action9<Integer, Integer, Integer, Integer, Integer, Integer, Integer, Integer, Integer>() {
            @Override
            public void call(Integer t1, Integer t2, Integer t3, Integer t4, Integer t5, Integer t6, Integer t7, Integer t8, Integer t9) {
                value.set(t1 | t2 | t3 | t4 | t5 | t6 | t7 | t8 | t9);
            }
        };
        
        assertNull(Actions.toFunc(action).call(1, 2, 4, 8, 16, 32, 64, 128, 256));
        assertEquals(511, value.get());
        value.set(-1L);
        assertEquals((Integer)0, Actions.toFunc(action, 0).call(1, 2, 4, 8, 16, 32, 64, 128, 256));
        assertEquals(511, value.get());
    }
    
    @Test
    public void testToFuncN() {
        for (int i = 0; i < 100; i++) {
            final AtomicLong value = new AtomicLong(-1L);
            final ActionN action = new ActionN() {
                @Override
                public void call(Object... args) {
                    int sum = 0;
                    for (Object o : args) {
                        sum += (Integer)o;
                    }
                    value.set(sum);
                }
            };
            Object[] arr = new Object[i];
            Arrays.fill(arr, 1);
            
            assertNull(Actions.toFunc(action).call(arr));
            assertEquals(i, value.get());
            value.set(-1L);
            assertEquals((Integer)0, Actions.toFunc(action, 0).call(arr));
            assertEquals(i, value.get());
        }
    }
    
    @Test
    public void testNotInstantiable() {
        try {
            Constructor<?> c = Actions.class.getDeclaredConstructor();
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
}
