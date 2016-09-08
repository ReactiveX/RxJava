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

import java.io.IOException;
import java.util.Queue;
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;

import org.junit.*;

import rx.*;
import rx.exceptions.*;
import rx.functions.*;

public class ObserversTest {
    @Test
    public void constructorShouldBePrivate() {
        TestUtil.checkUtilityClass(Observers.class);
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

    @Test
    public void onCompleteQueues() {
        @SuppressWarnings("unchecked")
        final Observer<Integer>[] observer = new Observer[] { null };
        final boolean[] completeCalled = { false };
        SerializedObserver<Integer> so = new SerializedObserver<Integer>(new Observer<Integer>() {
            @Override
            public void onNext(Integer t) {
                observer[0].onNext(1);
                observer[0].onCompleted();
            }

            @Override
            public void onError(Throwable e) {

            }

            @Override
            public void onCompleted() {
                completeCalled[0] = true;
            }
        });
        observer[0] = so;

        so.onNext(1);

        Assert.assertTrue(completeCalled[0]);
    }

    @Test
    public void concurrentOnError() throws Exception {
        final Queue<Throwable> queue = new ConcurrentLinkedQueue<Throwable>();

        final SerializedObserver<Integer> so = new SerializedObserver<Integer>(new Observer<Integer>() {
            @Override
            public void onNext(Integer t) {
            }

            @Override
            public void onError(Throwable e) {
                queue.offer(e);
            }

            @Override
            public void onCompleted() {
            }
        });

        final CountDownLatch cdl = new CountDownLatch(1);

        synchronized (so) {
            Thread t = new Thread(new Runnable() {
                @Override
                public void run() {
                    so.onError(new IOException());
                    cdl.countDown();
                }
            });
            t.start();

            Thread.sleep(200);

            so.onError(new TestException());
        }

        if (!cdl.await(5, TimeUnit.SECONDS)) {
            fail("The wait timed out");
        }

        Assert.assertEquals(1, queue.size());
        Throwable ex = queue.poll();
        Assert.assertTrue("" + ex, ex instanceof TestException);
    }

    @Test
    public void concurrentOnComplete() throws Exception {
        final int[] completed = { 0 };
        final SerializedObserver<Integer> so = new SerializedObserver<Integer>(new Observer<Integer>() {
            @Override
            public void onNext(Integer t) {
            }

            @Override
            public void onError(Throwable e) {
            }

            @Override
            public void onCompleted() {
                completed[0]++;
            }
        });

        final CountDownLatch cdl = new CountDownLatch(1);

        synchronized (so) {
            Thread t = new Thread(new Runnable() {
                @Override
                public void run() {
                    so.onCompleted();
                    cdl.countDown();
                }
            });
            t.start();

            Thread.sleep(200);

            so.onCompleted();
        }

        if (!cdl.await(5, TimeUnit.SECONDS)) {
            fail("The wait timed out");
        }

        Assert.assertEquals(1, completed[0]);
    }
}
