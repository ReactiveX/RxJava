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

package io.reactivex.single;

import static org.junit.Assert.*;

import java.util.List;

import org.junit.Test;

import io.reactivex.*;
import io.reactivex.disposables.Disposable;
import io.reactivex.exceptions.*;
import io.reactivex.functions.*;
import io.reactivex.internal.functions.Functions;
import io.reactivex.plugins.RxJavaPlugins;
import io.reactivex.subjects.PublishSubject;

public class SingleSubscribeTest {

    @Test
    public void consumer() {
        final Integer[] value = { null };
        
        Single.just(1).subscribe(new Consumer<Integer>() {
            @Override
            public void accept(Integer v) throws Exception {
                value[0] = v;
            }
        });
        
        assertEquals((Integer)1, value[0]);
    }

    @Test
    public void biconsumer() {
        final Object[] value = { null, null };
        
        Single.just(1).subscribe(new BiConsumer<Integer, Throwable>() {
            @Override
            public void accept(Integer v, Throwable e) throws Exception {
                value[0] = v;
                value[1] = e;
            }
        });
        
        assertEquals(1, value[0]);
        assertNull(value[1]);
    }

    @Test
    public void biconsumerError() {
        final Object[] value = { null, null };
        
        TestException ex = new TestException();
        
        Single.error(ex).subscribe(new BiConsumer<Object, Throwable>() {
            @Override
            public void accept(Object v, Throwable e) throws Exception {
                value[0] = v;
                value[1] = e;
            }
        });
        
        assertNull(value[0]);
        assertEquals(ex, value[1]);
    }
    
    @Test
    public void subscribeThrows() {
        try {
            new Single<Integer>() {
                @Override
                protected void subscribeActual(SingleObserver<? super Integer> observer) {
                    throw new IllegalArgumentException();
                }
            }.test();
        } catch (NullPointerException ex) {
            if (!(ex.getCause() instanceof IllegalArgumentException)) {
                fail(ex.toString() + ": should have thrown NPE(IAE)");
            }
        }
    }
    
    @Test
    public void biConsumerDispose() {
        PublishSubject<Integer> ps = PublishSubject.create();
        
        Disposable d = ps.toSingle().subscribe(new BiConsumer<Object, Object>() {
            @Override
            public void accept(Object t1, Object t2) throws Exception {
                
            }
        });

        assertFalse(d.isDisposed());

        d.dispose();
        
        assertTrue(d.isDisposed());

        assertFalse(ps.hasObservers());
    }

    @Test
    public void consumerDispose() {
        PublishSubject<Integer> ps = PublishSubject.create();
        
        Disposable d = ps.toSingle().subscribe(Functions.<Integer>emptyConsumer());

        assertFalse(d.isDisposed());

        d.dispose();
        
        assertTrue(d.isDisposed());

        assertFalse(ps.hasObservers());
    }

    @Test
    public void consumerSuccessThrows() {
        List<Throwable> list = TestHelper.trackPluginErrors();
        
        try {
            Single.just(1).subscribe(new Consumer<Integer>() {
                @Override
                public void accept(Integer t) throws Exception {
                    throw new TestException();
                }
            });
            
            TestHelper.assertError(list, 0, TestException.class);
        } finally {
            RxJavaPlugins.reset();
        }
    }

    @Test
    public void consumerErrorThrows() {
        List<Throwable> list = TestHelper.trackPluginErrors();
        
        try {
            Single.<Integer>error(new TestException("Outer failure")).subscribe(
            Functions.<Integer>emptyConsumer(),
            new Consumer<Throwable>() {
                @Override
                public void accept(Throwable t) throws Exception {
                    throw new TestException("Inner failure");
                }
            });
            
            TestHelper.assertError(list, 0, CompositeException.class);
            List<Throwable> cel = TestHelper.compositeList(list.get(0));
            TestHelper.assertError(cel, 0, TestException.class, "Outer failure");
            TestHelper.assertError(cel, 1, TestException.class, "Inner failure");
        } finally {
            RxJavaPlugins.reset();
        }
    }

    @Test
    public void biConsumerThrows() {
        List<Throwable> list = TestHelper.trackPluginErrors();
        
        try {
            Single.just(1).subscribe(new BiConsumer<Integer, Throwable>() {
                @Override
                public void accept(Integer t, Throwable e) throws Exception {
                    throw new TestException();
                }
            });
            
            TestHelper.assertError(list, 0, TestException.class);
        } finally {
            RxJavaPlugins.reset();
        }
    }

    @Test
    public void biConsumerErrorThrows() {
        List<Throwable> list = TestHelper.trackPluginErrors();
        
        try {
            Single.<Integer>error(new TestException("Outer failure")).subscribe(
            new BiConsumer<Integer, Throwable>() {
                @Override
                public void accept(Integer a, Throwable t) throws Exception {
                    throw new TestException("Inner failure");
                }
            });
            
            TestHelper.assertError(list, 0, CompositeException.class);
            List<Throwable> cel = TestHelper.compositeList(list.get(0));
            TestHelper.assertError(cel, 0, TestException.class, "Outer failure");
            TestHelper.assertError(cel, 1, TestException.class, "Inner failure");
        } finally {
            RxJavaPlugins.reset();
        }
    }

    @Test
    public void methodTestNoCancel() {
        PublishSubject<Integer> ps = PublishSubject.create();
        
        ps.toSingle().test(false);
        
        assertTrue(ps.hasObservers());
    }
}
