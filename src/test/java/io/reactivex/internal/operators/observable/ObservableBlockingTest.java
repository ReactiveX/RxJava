/**
 * Copyright (c) 2016-present, RxJava Contributors.
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

package io.reactivex.internal.operators.observable;

import static org.junit.Assert.*;

import java.util.*;
import java.util.concurrent.TimeUnit;

import org.junit.Test;

import io.reactivex.Observable;
import io.reactivex.Observer;
import io.reactivex.TestHelper;
import io.reactivex.disposables.*;
import io.reactivex.exceptions.TestException;
import io.reactivex.functions.*;
import io.reactivex.internal.functions.Functions;
import io.reactivex.internal.observers.BlockingFirstObserver;
import io.reactivex.observers.TestObserver;
import io.reactivex.schedulers.Schedulers;

public class ObservableBlockingTest {

    @Test
    public void blockingFirst() {
        assertEquals(1, Observable.range(1, 10)
                .subscribeOn(Schedulers.computation()).blockingFirst().intValue());
    }

    @Test
    public void blockingFirstDefault() {
        assertEquals(1, Observable.<Integer>empty()
                .subscribeOn(Schedulers.computation()).blockingFirst(1).intValue());
    }

    @Test
    public void blockingSubscribeConsumer() {
        final List<Integer> list = new ArrayList<Integer>();

        Observable.range(1, 5)
        .subscribeOn(Schedulers.computation())
        .blockingSubscribe(new Consumer<Integer>() {
            @Override
            public void accept(Integer v) throws Exception {
                list.add(v);
            }
        });

        assertEquals(Arrays.asList(1, 2, 3, 4, 5), list);
    }

    @Test
    public void blockingSubscribeConsumerConsumer() {
        final List<Object> list = new ArrayList<Object>();

        Observable.range(1, 5)
        .subscribeOn(Schedulers.computation())
        .blockingSubscribe(new Consumer<Integer>() {
            @Override
            public void accept(Integer v) throws Exception {
                list.add(v);
            }
        }, Functions.emptyConsumer());

        assertEquals(Arrays.asList(1, 2, 3, 4, 5), list);
    }

    @Test
    public void blockingSubscribeConsumerConsumerError() {
        final List<Object> list = new ArrayList<Object>();

        TestException ex = new TestException();

        Consumer<Object> cons = new Consumer<Object>() {
            @Override
            public void accept(Object v) throws Exception {
                list.add(v);
            }
        };

        Observable.range(1, 5).concatWith(Observable.<Integer>error(ex))
        .subscribeOn(Schedulers.computation())
        .blockingSubscribe(cons, cons);

        assertEquals(Arrays.asList(1, 2, 3, 4, 5, ex), list);
    }

    @Test
    public void blockingSubscribeConsumerConsumerAction() {
        final List<Object> list = new ArrayList<Object>();

        Consumer<Object> cons = new Consumer<Object>() {
            @Override
            public void accept(Object v) throws Exception {
                list.add(v);
            }
        };

        Observable.range(1, 5)
        .subscribeOn(Schedulers.computation())
        .blockingSubscribe(cons, cons, new Action() {
            @Override
            public void run() throws Exception {
                list.add(100);
            }
        });

        assertEquals(Arrays.asList(1, 2, 3, 4, 5, 100), list);
    }

    @Test
    public void blockingSubscribeObserver() {
        final List<Object> list = new ArrayList<Object>();

        Observable.range(1, 5)
        .subscribeOn(Schedulers.computation())
        .blockingSubscribe(new Observer<Object>() {

            @Override
            public void onSubscribe(Disposable d) {

            }

            @Override
            public void onNext(Object value) {
                list.add(value);
            }

            @Override
            public void onError(Throwable e) {
                list.add(e);
            }

            @Override
            public void onComplete() {
                list.add(100);
            }

        });

        assertEquals(Arrays.asList(1, 2, 3, 4, 5, 100), list);
    }

    @Test
    public void blockingSubscribeObserverError() {
        final List<Object> list = new ArrayList<Object>();

        final TestException ex = new TestException();

        Observable.range(1, 5).concatWith(Observable.<Integer>error(ex))
        .subscribeOn(Schedulers.computation())
        .blockingSubscribe(new Observer<Object>() {

            @Override
            public void onSubscribe(Disposable d) {

            }

            @Override
            public void onNext(Object value) {
                list.add(value);
            }

            @Override
            public void onError(Throwable e) {
                list.add(e);
            }

            @Override
            public void onComplete() {
                list.add(100);
            }

        });

        assertEquals(Arrays.asList(1, 2, 3, 4, 5, ex), list);
    }

    @Test(expected = TestException.class)
    public void blockingForEachThrows() {
        Observable.just(1)
        .blockingForEach(new Consumer<Integer>() {
            @Override
            public void accept(Integer e) throws Exception {
                throw new TestException();
            }
        });
    }

    @Test(expected = NoSuchElementException.class)
    public void blockingFirstEmpty() {
        Observable.empty().blockingFirst();
    }

    @Test(expected = NoSuchElementException.class)
    public void blockingLastEmpty() {
        Observable.empty().blockingLast();
    }

    @Test
    public void blockingFirstNormal() {
        assertEquals(1, Observable.just(1, 2).blockingFirst(3).intValue());
    }

    @Test
    public void blockingLastNormal() {
        assertEquals(2, Observable.just(1, 2).blockingLast(3).intValue());
    }

    @Test(expected = NoSuchElementException.class)
    public void blockingSingleEmpty() {
        Observable.empty().blockingSingle();
    }

    @Test
    public void utilityClass() {
        TestHelper.checkUtilityClass(ObservableBlockingSubscribe.class);
    }

    @Test
    public void disposeUpFront() {
        TestObserver<Object> to = new TestObserver<Object>();
        to.dispose();
        Observable.just(1).blockingSubscribe(to);

        to.assertEmpty();
    }

    @SuppressWarnings("rawtypes")
    @Test
    public void delayed() throws Exception {
        final TestObserver<Object> to = new TestObserver<Object>();
        final Observer[] s = { null };

        Schedulers.single().scheduleDirect(new Runnable() {
            @SuppressWarnings("unchecked")
            @Override
            public void run() {
                to.dispose();
                s[0].onNext(1);
            }
        }, 200, TimeUnit.MILLISECONDS);

        new Observable<Integer>() {
            @Override
            protected void subscribeActual(Observer<? super Integer> observer) {
                observer.onSubscribe(Disposables.empty());
                s[0] = observer;
            }
        }.blockingSubscribe(to);

        while (!to.isDisposed()) {
            Thread.sleep(100);
        }

        to.assertEmpty();
    }

    @Test
    public void interrupt() {
        TestObserver<Object> to = new TestObserver<Object>();
        Thread.currentThread().interrupt();
        Observable.never().blockingSubscribe(to);
    }

    @Test
    public void onCompleteDelayed() {
        TestObserver<Object> to = new TestObserver<Object>();

        Observable.empty().delay(100, TimeUnit.MILLISECONDS)
        .blockingSubscribe(to);

        to.assertResult();
    }

    @Test
    public void blockingCancelUpfront() {
        BlockingFirstObserver<Integer> o = new BlockingFirstObserver<Integer>();

        assertFalse(o.isDisposed());
        o.dispose();
        assertTrue(o.isDisposed());

        Disposable d = Disposables.empty();

        o.onSubscribe(d);

        assertTrue(d.isDisposed());

        Thread.currentThread().interrupt();
        try {
            o.blockingGet();
            fail("Should have thrown");
        } catch (RuntimeException ex) {
            assertTrue(ex.toString(), ex.getCause() instanceof InterruptedException);
        }

        Thread.interrupted();

        o.onError(new TestException());

        try {
            o.blockingGet();
            fail("Should have thrown");
        } catch (TestException ex) {
            // expected
        }
    }
}
