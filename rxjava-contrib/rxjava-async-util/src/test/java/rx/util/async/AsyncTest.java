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

package rx.util.async;

import java.util.concurrent.CountDownLatch;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import junit.framework.Assert;

import org.junit.Before;
import org.junit.Test;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

import rx.Observable;
import rx.Observer;
import rx.Subscription;
import rx.functions.Action0;
import rx.functions.Action1;
import rx.functions.Action2;
import rx.functions.Action3;
import rx.functions.Action4;
import rx.functions.Action5;
import rx.functions.Action6;
import rx.functions.Action7;
import rx.functions.Action8;
import rx.functions.Action9;
import rx.functions.ActionN;
import rx.functions.Func0;
import rx.functions.Func1;
import rx.functions.Func2;
import rx.functions.Func3;
import rx.functions.Func4;
import rx.functions.Func5;
import rx.functions.Func6;
import rx.functions.Func7;
import rx.functions.Func8;
import rx.functions.Func9;
import rx.functions.FuncN;
import rx.observers.TestObserver;
import rx.schedulers.Schedulers;
import rx.schedulers.TestScheduler;

public class AsyncTest {
    @Mock
    Observer<Object> observer;

    @Before
    public void before() {
        MockitoAnnotations.initMocks(this);
    }

    @Test
    public void testAction0() {
        final AtomicInteger value = new AtomicInteger();
        Action0 action = new Action0() {
            @Override
            public void call() {
                value.incrementAndGet();
            }
        };

        Async.toAsync(action, Schedulers.immediate())
                .call()
                .subscribe(new TestObserver<Object>(observer));

        verify(observer, never()).onError(any(Throwable.class));
        verify(observer, times(1)).onNext(null);
        verify(observer, times(1)).onCompleted();

        Assert.assertEquals(1, value.get());
    }

    @Test
    public void testAction0Error() {
        Action0 action = new Action0() {
            @Override
            public void call() {
                throw new RuntimeException("Forced failure");
            }
        };

        Async.toAsync(action, Schedulers.immediate())
                .call()
                .subscribe(new TestObserver<Object>(observer));

        verify(observer, times(1)).onError(any(Throwable.class));
        verify(observer, never()).onNext(null);
        verify(observer, never()).onCompleted();
    }

    @Test
    public void testAction1() {
        final AtomicInteger value = new AtomicInteger();
        Action1<Integer> action = new Action1<Integer>() {
            @Override
            public void call(Integer t1) {
                value.set(t1);
            }
        };

        Async.toAsync(action, Schedulers.immediate())
                .call(1)
                .subscribe(new TestObserver<Object>(observer));

        verify(observer, never()).onError(any(Throwable.class));
        verify(observer, times(1)).onNext(null);
        verify(observer, times(1)).onCompleted();

        Assert.assertEquals(1, value.get());
    }

    @Test
    public void testAction1Error() {
        Action1<Integer> action = new Action1<Integer>() {
            @Override
            public void call(Integer t1) {
                throw new RuntimeException("Forced failure");
            }
        };

        Async.toAsync(action, Schedulers.immediate())
                .call(1)
                .subscribe(new TestObserver<Object>(observer));

        verify(observer, times(1)).onError(any(Throwable.class));
        verify(observer, never()).onNext(null);
        verify(observer, never()).onCompleted();
    }

    @Test
    public void testAction2() {
        final AtomicInteger value = new AtomicInteger();
        Action2<Integer, Integer> action = new Action2<Integer, Integer>() {
            @Override
            public void call(Integer t1, Integer t2) {
                value.set(t1 | t2);
            }
        };

        Async.toAsync(action, Schedulers.immediate())
                .call(1, 2)
                .subscribe(new TestObserver<Object>(observer));

        verify(observer, never()).onError(any(Throwable.class));
        verify(observer, times(1)).onNext(null);
        verify(observer, times(1)).onCompleted();

        Assert.assertEquals(3, value.get());
    }

    @Test
    public void testAction2Error() {
        Action2<Integer, Integer> action = new Action2<Integer, Integer>() {
            @Override
            public void call(Integer t1, Integer t2) {
                throw new RuntimeException("Forced failure");
            }
        };

        Async.toAsync(action, Schedulers.immediate())
                .call(1, 2)
                .subscribe(new TestObserver<Object>(observer));

        verify(observer, times(1)).onError(any(Throwable.class));
        verify(observer, never()).onNext(null);
        verify(observer, never()).onCompleted();
    }

    @Test
    public void testAction3() {
        final AtomicInteger value = new AtomicInteger();
        Action3<Integer, Integer, Integer> action = new Action3<Integer, Integer, Integer>() {
            @Override
            public void call(Integer t1, Integer t2, Integer t3) {
                value.set(t1 | t2 | t3);
            }
        };

        Async.toAsync(action, Schedulers.immediate())
                .call(1, 2, 4)
                .subscribe(new TestObserver<Object>(observer));

        verify(observer, never()).onError(any(Throwable.class));
        verify(observer, times(1)).onNext(null);
        verify(observer, times(1)).onCompleted();

        Assert.assertEquals(7, value.get());
    }

    @Test
    public void testAction3Error() {
        Action3<Integer, Integer, Integer> action = new Action3<Integer, Integer, Integer>() {
            @Override
            public void call(Integer t1, Integer t2, Integer t3) {
                throw new RuntimeException("Forced failure");
            }
        };

        Async.toAsync(action, Schedulers.immediate())
                .call(1, 2, 4)
                .subscribe(new TestObserver<Object>(observer));

        verify(observer, times(1)).onError(any(Throwable.class));
        verify(observer, never()).onNext(null);
        verify(observer, never()).onCompleted();
    }

    @Test
    public void testAction4() {
        final AtomicInteger value = new AtomicInteger();
        Action4<Integer, Integer, Integer, Integer> action = new Action4<Integer, Integer, Integer, Integer>() {
            @Override
            public void call(Integer t1, Integer t2, Integer t3, Integer t4) {
                value.set(t1 | t2 | t3 | t4);
            }
        };

        Async.toAsync(action, Schedulers.immediate())
                .call(1, 2, 4, 8)
                .subscribe(new TestObserver<Object>(observer));

        verify(observer, never()).onError(any(Throwable.class));
        verify(observer, times(1)).onNext(null);
        verify(observer, times(1)).onCompleted();

        Assert.assertEquals(15, value.get());
    }

    @Test
    public void testAction4Error() {
        Action4<Integer, Integer, Integer, Integer> action = new Action4<Integer, Integer, Integer, Integer>() {
            @Override
            public void call(Integer t1, Integer t2, Integer t3, Integer t4) {
                throw new RuntimeException("Forced failure");
            }
        };

        Async.toAsync(action, Schedulers.immediate())
                .call(1, 2, 4, 8)
                .subscribe(new TestObserver<Object>(observer));

        verify(observer, times(1)).onError(any(Throwable.class));
        verify(observer, never()).onNext(null);
        verify(observer, never()).onCompleted();
    }

    @Test
    public void testAction5() {
        final AtomicInteger value = new AtomicInteger();
        Action5<Integer, Integer, Integer, Integer, Integer> action = new Action5<Integer, Integer, Integer, Integer, Integer>() {
            @Override
            public void call(Integer t1, Integer t2, Integer t3, Integer t4, Integer t5) {
                value.set(t1 | t2 | t3 | t4 | t5);
            }
        };

        Async.toAsync(action, Schedulers.immediate())
                .call(1, 2, 4, 8, 16)
                .subscribe(new TestObserver<Object>(observer));

        verify(observer, never()).onError(any(Throwable.class));
        verify(observer, times(1)).onNext(null);
        verify(observer, times(1)).onCompleted();

        Assert.assertEquals(31, value.get());
    }

    @Test
    public void testAction5Error() {
        Action5<Integer, Integer, Integer, Integer, Integer> action = new Action5<Integer, Integer, Integer, Integer, Integer>() {
            @Override
            public void call(Integer t1, Integer t2, Integer t3, Integer t4, Integer t5) {
                throw new RuntimeException("Forced failure");
            }
        };

        Async.toAsync(action, Schedulers.immediate())
                .call(1, 2, 4, 8, 16)
                .subscribe(new TestObserver<Object>(observer));

        verify(observer, times(1)).onError(any(Throwable.class));
        verify(observer, never()).onNext(null);
        verify(observer, never()).onCompleted();
    }

    @Test
    public void testAction6() {
        final AtomicInteger value = new AtomicInteger();
        Action6<Integer, Integer, Integer, Integer, Integer, Integer> action = new Action6<Integer, Integer, Integer, Integer, Integer, Integer>() {
            @Override
            public void call(Integer t1, Integer t2, Integer t3, Integer t4, Integer t5, Integer t6) {
                value.set(t1 | t2 | t3 | t4 | t5 | t6);
            }
        };

        Async.toAsync(action, Schedulers.immediate())
                .call(1, 2, 4, 8, 16, 32)
                .subscribe(new TestObserver<Object>(observer));

        verify(observer, never()).onError(any(Throwable.class));
        verify(observer, times(1)).onNext(null);
        verify(observer, times(1)).onCompleted();

        Assert.assertEquals(63, value.get());
    }

    @Test
    public void testAction6Error() {
        Action6<Integer, Integer, Integer, Integer, Integer, Integer> action = new Action6<Integer, Integer, Integer, Integer, Integer, Integer>() {
            @Override
            public void call(Integer t1, Integer t2, Integer t3, Integer t4, Integer t5, Integer t6) {
                throw new RuntimeException("Forced failure");
            }
        };

        Async.toAsync(action, Schedulers.immediate())
                .call(1, 2, 4, 8, 16, 32)
                .subscribe(new TestObserver<Object>(observer));

        verify(observer, times(1)).onError(any(Throwable.class));
        verify(observer, never()).onNext(null);
        verify(observer, never()).onCompleted();
    }

    @Test
    public void testAction7() {
        final AtomicInteger value = new AtomicInteger();
        Action7<Integer, Integer, Integer, Integer, Integer, Integer, Integer> action = new Action7<Integer, Integer, Integer, Integer, Integer, Integer, Integer>() {
            @Override
            public void call(Integer t1, Integer t2, Integer t3, Integer t4, Integer t5, Integer t6, Integer t7) {
                value.set(t1 | t2 | t3 | t4 | t5 | t6 | t7);
            }
        };

        Async.toAsync(action, Schedulers.immediate())
                .call(1, 2, 4, 8, 16, 32, 64)
                .subscribe(new TestObserver<Object>(observer));

        verify(observer, never()).onError(any(Throwable.class));
        verify(observer, times(1)).onNext(null);
        verify(observer, times(1)).onCompleted();

        Assert.assertEquals(127, value.get());
    }

    @Test
    public void testAction7Error() {
        Action7<Integer, Integer, Integer, Integer, Integer, Integer, Integer> action = new Action7<Integer, Integer, Integer, Integer, Integer, Integer, Integer>() {
            @Override
            public void call(Integer t1, Integer t2, Integer t3, Integer t4, Integer t5, Integer t6, Integer t7) {
                throw new RuntimeException("Forced failure");
            }
        };

        Async.toAsync(action, Schedulers.immediate())
                .call(1, 2, 4, 8, 16, 32, 64)
                .subscribe(new TestObserver<Object>(observer));

        verify(observer, times(1)).onError(any(Throwable.class));
        verify(observer, never()).onNext(null);
        verify(observer, never()).onCompleted();
    }

    @Test
    public void testAction8() {
        final AtomicInteger value = new AtomicInteger();
        Action8<Integer, Integer, Integer, Integer, Integer, Integer, Integer, Integer> action = new Action8<Integer, Integer, Integer, Integer, Integer, Integer, Integer, Integer>() {
            @Override
            public void call(Integer t1, Integer t2, Integer t3, Integer t4, Integer t5, Integer t6, Integer t7, Integer t8) {
                value.set(t1 | t2 | t3 | t4 | t5 | t6 | t7 | t8);
            }
        };

        Async.toAsync(action, Schedulers.immediate())
                .call(1, 2, 4, 8, 16, 32, 64, 128)
                .subscribe(new TestObserver<Object>(observer));

        verify(observer, never()).onError(any(Throwable.class));
        verify(observer, times(1)).onNext(null);
        verify(observer, times(1)).onCompleted();

        Assert.assertEquals(255, value.get());
    }

    @Test
    public void testAction8Error() {
        Action8<Integer, Integer, Integer, Integer, Integer, Integer, Integer, Integer> action = new Action8<Integer, Integer, Integer, Integer, Integer, Integer, Integer, Integer>() {
            @Override
            public void call(Integer t1, Integer t2, Integer t3, Integer t4, Integer t5, Integer t6, Integer t7, Integer t8) {
                throw new RuntimeException("Forced failure");
            }
        };

        Async.toAsync(action, Schedulers.immediate())
                .call(1, 2, 4, 8, 16, 32, 64, 128)
                .subscribe(new TestObserver<Object>(observer));

        verify(observer, times(1)).onError(any(Throwable.class));
        verify(observer, never()).onNext(null);
        verify(observer, never()).onCompleted();
    }

    @Test
    public void testAction9() {
        final AtomicInteger value = new AtomicInteger();
        Action9<Integer, Integer, Integer, Integer, Integer, Integer, Integer, Integer, Integer> action = new Action9<Integer, Integer, Integer, Integer, Integer, Integer, Integer, Integer, Integer>() {
            @Override
            public void call(Integer t1, Integer t2, Integer t3, Integer t4, Integer t5, Integer t6, Integer t7, Integer t8, Integer t9) {
                value.set(t1 | t2 | t3 | t4 | t5 | t6 | t7 | t8 | t9);
            }
        };

        Async.toAsync(action, Schedulers.immediate())
                .call(1, 2, 4, 8, 16, 32, 64, 128, 256)
                .subscribe(new TestObserver<Object>(observer));

        verify(observer, never()).onError(any(Throwable.class));
        verify(observer, times(1)).onNext(null);
        verify(observer, times(1)).onCompleted();

        Assert.assertEquals(511, value.get());
    }

    @Test
    public void testAction9Error() {
        Action9<Integer, Integer, Integer, Integer, Integer, Integer, Integer, Integer, Integer> action = new Action9<Integer, Integer, Integer, Integer, Integer, Integer, Integer, Integer, Integer>() {
            @Override
            public void call(Integer t1, Integer t2, Integer t3, Integer t4, Integer t5, Integer t6, Integer t7, Integer t8, Integer t9) {
                throw new RuntimeException("Forced failure");
            }
        };

        Async.toAsync(action, Schedulers.immediate())
                .call(1, 2, 4, 8, 16, 32, 64, 128, 256)
                .subscribe(new TestObserver<Object>(observer));

        verify(observer, times(1)).onError(any(Throwable.class));
        verify(observer, never()).onNext(null);
        verify(observer, never()).onCompleted();
    }

    @Test
    public void testActionN() {
        final AtomicInteger value = new AtomicInteger();
        ActionN action = new ActionN() {
            @Override
            public void call(Object... args) {
                int i = 0;
                for (Object o : args) {
                    i = i | (Integer) o;
                }
                value.set(i);
            }
        };

        Async.toAsync(action, Schedulers.immediate())
                .call(1, 2, 4, 8, 16, 32, 64, 128, 256, 512)
                .subscribe(new TestObserver<Object>(observer));

        verify(observer, never()).onError(any(Throwable.class));
        verify(observer, times(1)).onNext(null);
        verify(observer, times(1)).onCompleted();

        Assert.assertEquals(1023, value.get());
    }

    @Test
    public void testActionNError() {
        ActionN action = new ActionN() {
            @Override
            public void call(Object... args) {
                throw new RuntimeException("Forced failure");
            }
        };

        Async.toAsync(action, Schedulers.immediate())
                .call(1, 2, 4, 8, 16, 32, 64, 128, 256, 512)
                .subscribe(new TestObserver<Object>(observer));

        verify(observer, times(1)).onError(any(Throwable.class));
        verify(observer, never()).onNext(null);
        verify(observer, never()).onCompleted();
    }

    @Test
    public void testFunc0() {
        Func0<Integer> func = new Func0<Integer>() {
            @Override
            public Integer call() {
                return 0;
            }
        };
        Async.toAsync(func, Schedulers.immediate())
                .call()
                .subscribe(new TestObserver<Object>(observer));

        verify(observer, never()).onError(any(Throwable.class));
        verify(observer, times(1)).onNext(0);
        verify(observer, times(1)).onCompleted();

    }

    @Test
    public void testFunc1() {
        Func1<Integer, Integer> func = new Func1<Integer, Integer>() {
            @Override
            public Integer call(Integer t1) {
                return t1;
            }
        };
        Async.toAsync(func, Schedulers.immediate())
                .call(1)
                .subscribe(new TestObserver<Object>(observer));

        verify(observer, never()).onError(any(Throwable.class));
        verify(observer, times(1)).onNext(1);
        verify(observer, times(1)).onCompleted();
    }

    @Test
    public void testFunc2() {
        Func2<Integer, Integer, Integer> func = new Func2<Integer, Integer, Integer>() {
            @Override
            public Integer call(Integer t1, Integer t2) {
                return t1 | t2;
            }
        };
        Async.toAsync(func, Schedulers.immediate())
                .call(1, 2)
                .subscribe(new TestObserver<Object>(observer));

        verify(observer, never()).onError(any(Throwable.class));
        verify(observer, times(1)).onNext(3);
        verify(observer, times(1)).onCompleted();
    }

    @Test
    public void testFunc3() {
        Func3<Integer, Integer, Integer, Integer> func = new Func3<Integer, Integer, Integer, Integer>() {
            @Override
            public Integer call(Integer t1, Integer t2, Integer t3) {
                return t1 | t2 | t3;
            }
        };
        Async.toAsync(func, Schedulers.immediate())
                .call(1, 2, 4)
                .subscribe(new TestObserver<Object>(observer));

        verify(observer, never()).onError(any(Throwable.class));
        verify(observer, times(1)).onNext(7);
        verify(observer, times(1)).onCompleted();
    }

    @Test
    public void testFunc4() {
        Func4<Integer, Integer, Integer, Integer, Integer> func = new Func4<Integer, Integer, Integer, Integer, Integer>() {
            @Override
            public Integer call(Integer t1, Integer t2, Integer t3, Integer t4) {
                return t1 | t2 | t3 | t4;
            }
        };
        Async.toAsync(func, Schedulers.immediate())
                .call(1, 2, 4, 8)
                .subscribe(new TestObserver<Object>(observer));

        verify(observer, never()).onError(any(Throwable.class));
        verify(observer, times(1)).onNext(15);
        verify(observer, times(1)).onCompleted();
    }

    @Test
    public void testFunc5() {
        Func5<Integer, Integer, Integer, Integer, Integer, Integer> func = new Func5<Integer, Integer, Integer, Integer, Integer, Integer>() {
            @Override
            public Integer call(Integer t1, Integer t2, Integer t3, Integer t4, Integer t5) {
                return t1 | t2 | t3 | t4 | t5;
            }
        };
        Async.toAsync(func, Schedulers.immediate())
                .call(1, 2, 4, 8, 16)
                .subscribe(new TestObserver<Object>(observer));

        verify(observer, never()).onError(any(Throwable.class));
        verify(observer, times(1)).onNext(31);
        verify(observer, times(1)).onCompleted();
    }

    @Test
    public void testFunc6() {
        Func6<Integer, Integer, Integer, Integer, Integer, Integer, Integer> func = new Func6<Integer, Integer, Integer, Integer, Integer, Integer, Integer>() {
            @Override
            public Integer call(Integer t1, Integer t2, Integer t3, Integer t4, Integer t5, Integer t6) {
                return t1 | t2 | t3 | t4 | t5 | t6;
            }
        };
        Async.toAsync(func, Schedulers.immediate())
                .call(1, 2, 4, 8, 16, 32)
                .subscribe(new TestObserver<Object>(observer));

        verify(observer, never()).onError(any(Throwable.class));
        verify(observer, times(1)).onNext(63);
        verify(observer, times(1)).onCompleted();
    }

    @Test
    public void testFunc7() {
        Func7<Integer, Integer, Integer, Integer, Integer, Integer, Integer, Integer> func = new Func7<Integer, Integer, Integer, Integer, Integer, Integer, Integer, Integer>() {
            @Override
            public Integer call(Integer t1, Integer t2, Integer t3, Integer t4, Integer t5, Integer t6, Integer t7) {
                return t1 | t2 | t3 | t4 | t5 | t6 | t7;
            }
        };
        Async.toAsync(func, Schedulers.immediate())
                .call(1, 2, 4, 8, 16, 32, 64)
                .subscribe(new TestObserver<Object>(observer));

        verify(observer, never()).onError(any(Throwable.class));
        verify(observer, times(1)).onNext(127);
        verify(observer, times(1)).onCompleted();
    }

    @Test
    public void testFunc8() {
        Func8<Integer, Integer, Integer, Integer, Integer, Integer, Integer, Integer, Integer> func = new Func8<Integer, Integer, Integer, Integer, Integer, Integer, Integer, Integer, Integer>() {
            @Override
            public Integer call(Integer t1, Integer t2, Integer t3, Integer t4, Integer t5, Integer t6, Integer t7, Integer t8) {
                return t1 | t2 | t3 | t4 | t5 | t6 | t7 | t8;
            }
        };
        Async.toAsync(func, Schedulers.immediate())
                .call(1, 2, 4, 8, 16, 32, 64, 128)
                .subscribe(new TestObserver<Object>(observer));

        verify(observer, never()).onError(any(Throwable.class));
        verify(observer, times(1)).onNext(255);
        verify(observer, times(1)).onCompleted();
    }

    @Test
    public void testFunc9() {
        Func9<Integer, Integer, Integer, Integer, Integer, Integer, Integer, Integer, Integer, Integer> func = new Func9<Integer, Integer, Integer, Integer, Integer, Integer, Integer, Integer, Integer, Integer>() {
            @Override
            public Integer call(Integer t1, Integer t2, Integer t3, Integer t4, Integer t5, Integer t6, Integer t7, Integer t8, Integer t9) {
                return t1 | t2 | t3 | t4 | t5 | t6 | t7 | t8 | t9;
            }
        };
        Async.toAsync(func, Schedulers.immediate())
                .call(1, 2, 4, 8, 16, 32, 64, 128, 256)
                .subscribe(new TestObserver<Object>(observer));

        verify(observer, never()).onError(any(Throwable.class));
        verify(observer, times(1)).onNext(511);
        verify(observer, times(1)).onCompleted();
    }

    @Test
    public void testFuncN() {
        FuncN<Integer> func = new FuncN<Integer>() {
            @Override
            public Integer call(Object... args) {
                int i = 0;
                for (Object o : args) {
                    i = i | (Integer) o;
                }
                return i;
            }
        };
        Async.toAsync(func, Schedulers.immediate())
                .call(1, 2, 4, 8, 16, 32, 64, 128, 256, 512)
                .subscribe(new TestObserver<Object>(observer));

        verify(observer, never()).onError(any(Throwable.class));
        verify(observer, times(1)).onNext(1023);
        verify(observer, times(1)).onCompleted();
    }

    @Test
    public void testStartWithFunc() {
        Func0<String> func = new Func0<String>() {
            @Override
            public String call() {
                return "one";
            }
        };
        assertEquals("one", Async.start(func).toBlockingObservable().single());
    }

    @Test(expected = RuntimeException.class)
    public void testStartWithFuncError() {
        Func0<String> func = new Func0<String>() {
            @Override
            public String call() {
                throw new RuntimeException("Some error");
            }
        };
        Async.start(func).toBlockingObservable().single();
    }

    @Test
    public void testStartWhenSubscribeRunBeforeFunc() {
        TestScheduler scheduler = new TestScheduler();

        Func0<String> func = new Func0<String>() {
            @Override
            public String call() {
                return "one";
            }
        };

        Observable<String> observable = Async.start(func, scheduler);

        @SuppressWarnings("unchecked")
        Observer<String> observer = mock(Observer.class);
        observable.subscribe(new TestObserver<String>(observer));

        InOrder inOrder = inOrder(observer);
        inOrder.verifyNoMoreInteractions();

        // Run func
        scheduler.advanceTimeBy(100, TimeUnit.MILLISECONDS);

        inOrder.verify(observer, times(1)).onNext("one");
        inOrder.verify(observer, times(1)).onCompleted();
        inOrder.verifyNoMoreInteractions();
    }

    @Test
    public void testStartWhenSubscribeRunAfterFunc() {
        TestScheduler scheduler = new TestScheduler();

        Func0<String> func = new Func0<String>() {
            @Override
            public String call() {
                return "one";
            }
        };

        Observable<String> observable = Async.start(func, scheduler);

        // Run func
        scheduler.advanceTimeBy(100, TimeUnit.MILLISECONDS);

        @SuppressWarnings("unchecked")
        Observer<String> observer = mock(Observer.class);
        observable.subscribe(new TestObserver<String>(observer));

        InOrder inOrder = inOrder(observer);
        inOrder.verify(observer, times(1)).onNext("one");
        inOrder.verify(observer, times(1)).onCompleted();
        inOrder.verifyNoMoreInteractions();
    }

    @Test
    public void testStartWithFuncAndMultipleObservers() {
        TestScheduler scheduler = new TestScheduler();

        @SuppressWarnings("unchecked")
        Func0<String> func = (Func0<String>) mock(Func0.class);
        doAnswer(new Answer<String>() {
            @Override
            public String answer(InvocationOnMock invocation) throws Throwable {
                return "one";
            }
        }).when(func).call();

        Observable<String> observable = Async.start(func, scheduler);

        scheduler.advanceTimeBy(100, TimeUnit.MILLISECONDS);

        @SuppressWarnings("unchecked")
        Observer<String> observer1 = mock(Observer.class);
        @SuppressWarnings("unchecked")
        Observer<String> observer2 = mock(Observer.class);
        @SuppressWarnings("unchecked")
        Observer<String> observer3 = mock(Observer.class);

        observable.subscribe(new TestObserver<String>(observer1));
        observable.subscribe(new TestObserver<String>(observer2));
        observable.subscribe(new TestObserver<String>(observer3));

        InOrder inOrder;
        inOrder = inOrder(observer1);
        inOrder.verify(observer1, times(1)).onNext("one");
        inOrder.verify(observer1, times(1)).onCompleted();
        inOrder.verifyNoMoreInteractions();

        inOrder = inOrder(observer2);
        inOrder.verify(observer2, times(1)).onNext("one");
        inOrder.verify(observer2, times(1)).onCompleted();
        inOrder.verifyNoMoreInteractions();

        inOrder = inOrder(observer3);
        inOrder.verify(observer3, times(1)).onNext("one");
        inOrder.verify(observer3, times(1)).onCompleted();
        inOrder.verifyNoMoreInteractions();

        verify(func, times(1)).call();
    }

    @Test
    public void testRunAsync() throws InterruptedException {
        final CountDownLatch cdl = new CountDownLatch(1);
        final CountDownLatch cdl2 = new CountDownLatch(1);
        Action2<Observer<? super Integer>, Subscription> action = new Action2<Observer<? super Integer>, Subscription>() {
            @Override
            public void call(Observer<? super Integer> t1, Subscription t2) {
                try {
                    cdl.await();
                } catch (InterruptedException ex) {
                    Thread.currentThread().interrupt();
                    return;
                }
                for (int i = 0; i < 10 && !t2.isUnsubscribed(); i++) {
                    t1.onNext(i);
                }
                t1.onCompleted();
                cdl2.countDown();
            }
        };
        
        @SuppressWarnings("unchecked")
        Observer<Object> o = mock(Observer.class);
        InOrder inOrder = inOrder(o);
        
        StoppableObservable<Integer> so = Async.<Integer>runAsync(Schedulers.io(), action);
        
        so.subscribe(o);
        
        cdl.countDown();
        
        if (!cdl2.await(2, TimeUnit.SECONDS)) {
            fail("Didn't complete");
        }
        
        for (int i = 0; i < 10; i++) {
            inOrder.verify(o).onNext(i);
        }
        inOrder.verify(o).onCompleted();
        inOrder.verifyNoMoreInteractions();
        verify(o, never()).onError(any(Throwable.class));
        
    }
}
