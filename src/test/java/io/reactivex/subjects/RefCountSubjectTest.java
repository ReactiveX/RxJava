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

package io.reactivex.subjects;

import static org.junit.Assert.*;

import org.junit.Test;

import io.reactivex.*;
import io.reactivex.disposables.*;
import io.reactivex.exceptions.TestException;
import io.reactivex.observers.TestObserver;

public class RefCountSubjectTest {

    @Test
    public void normal() {
        Subject<Integer> rcp = PublishSubject.<Integer>create().refCount();

        PublishSubject<Integer> source = PublishSubject.create();

        assertFalse(source.hasObservers());

        source.subscribe(rcp);

        assertFalse(((Disposable)rcp).isDisposed());

        assertTrue(source.hasObservers());

        TestObserver<Integer> ts = rcp.test();

        source.onNext(1);

        ts.assertValue(1);

        source.onNext(2);

        ts.assertValues(1, 2);

        ts.cancel();

        assertFalse(source.hasObservers());

        assertTrue(((Disposable)rcp).isDisposed());
    }

    @Test
    public void complete() {
        Subject<Integer> rcp = PublishSubject.<Integer>create().refCount();

        PublishSubject<Integer> source = PublishSubject.create();

        assertFalse(source.hasObservers());

        source.subscribe(rcp);

        assertTrue(source.hasObservers());

        TestObserver<Integer> ts = rcp.test();

        assertFalse(rcp.hasComplete());
        assertFalse(rcp.hasThrowable());
        assertNull(rcp.getThrowable());
        assertTrue(rcp.hasObservers());

        source.onNext(1);

        ts.assertValue(1);

        source.onNext(2);

        ts.assertValues(1, 2);

        source.onComplete();

        assertFalse(source.hasObservers());

        ts.assertResult(1, 2);

        rcp.test().assertResult();

        assertTrue(rcp.hasComplete());
        assertFalse(rcp.hasThrowable());
        assertNull(rcp.getThrowable());
        assertFalse(rcp.hasObservers());
    }

    @Test
    public void error() {
        Subject<Integer> rcp = PublishSubject.<Integer>create().refCount();

        PublishSubject<Integer> source = PublishSubject.create();

        assertFalse(source.hasObservers());

        source.subscribe(rcp);

        assertTrue(source.hasObservers());

        TestObserver<Integer> ts = rcp.test();

        assertFalse(rcp.hasComplete());
        assertFalse(rcp.hasThrowable());
        assertNull(rcp.getThrowable());
        assertTrue(rcp.hasObservers());

        source.onNext(1);

        ts.assertValue(1);

        source.onNext(2);

        ts.assertValues(1, 2);

        source.onError(new TestException());

        assertFalse(source.hasObservers());

        ts.assertFailure(TestException.class, 1, 2);

        rcp.test().assertFailure(TestException.class);

        assertFalse(rcp.hasComplete());
        assertTrue(rcp.hasThrowable());
        assertNotNull(rcp.getThrowable());
        assertFalse(rcp.hasObservers());
    }

    @Test
    public void multipleSubscribers() {
        Subject<Integer> rcp = PublishSubject.<Integer>create().refCount();

        PublishSubject<Integer> source = PublishSubject.create();

        source.subscribe(rcp);

        assertTrue(source.hasObservers());

        TestObserver<Integer> ts1 = rcp.test();
        TestObserver<Integer> ts2 = rcp.test();

        ts1.cancel();

        assertTrue(source.hasObservers());

        ts2.cancel();

        assertFalse(source.hasObservers());

        rcp.test().assertFailureAndMessage(IllegalStateException.class, "RefCountSubject terminated");
    }

    @Test
    public void immediatelyCancelled() {
        Subject<Integer> rcp = PublishSubject.<Integer>create().refCount();

        PublishSubject<Integer> source = PublishSubject.create();

        source.subscribe(rcp);

        assertTrue(source.hasObservers());

        rcp.test(true);

        assertFalse(source.hasObservers());
    }

    @Test
    public void cancelTwice() {
        Subject<Integer> rcp = PublishSubject.<Integer>create().refCount();

        PublishSubject<Integer> source = PublishSubject.create();

        source.subscribe(rcp);

        assertTrue(source.hasObservers());

        final TestObserver<Integer> ts = new TestObserver<Integer>();

        rcp.subscribe(new Observer<Integer>() {

            @Override
            public void onNext(Integer t) {
                ts.onNext(t);
            }

            @Override
            public void onError(Throwable t) {
                ts.onError(t);
            }

            @Override
            public void onComplete() {
                ts.onComplete();
            }

            @Override
            public void onSubscribe(Disposable s) {
                ts.onSubscribe(s);
                assertFalse(s.isDisposed());
                s.dispose();
                assertTrue(s.isDisposed());
                s.dispose();
                assertTrue(s.isDisposed());
            }
        });

        assertFalse(source.hasObservers());

        ts.assertEmpty();
    }

    @Test
    public void cancelTwiceDontCancelUp() {
        Subject<Integer> rcp = PublishSubject.<Integer>create().refCount();

        PublishSubject<Integer> source = PublishSubject.create();

        source.subscribe(rcp);

        assertTrue(source.hasObservers());

        TestObserver<Integer> ts0 = rcp.test();

        final TestObserver<Integer> ts = new TestObserver<Integer>();

        rcp.subscribe(new Observer<Integer>() {

            @Override
            public void onNext(Integer t) {
                ts.onNext(t);
            }

            @Override
            public void onError(Throwable t) {
                ts.onError(t);
            }

            @Override
            public void onComplete() {
                ts.onComplete();
            }

            @Override
            public void onSubscribe(Disposable s) {
                ts.onSubscribe(s);
                s.dispose();
                s.dispose();
            }
        });

        assertTrue(source.hasObservers());

        ts.assertEmpty();

        source.onNext(1);
        source.onComplete();

        ts0.assertResult(1);
    }

    @Test
    public void addRemoveRace() {
        for (int i = 0; i < 1000; i++) {
            final Subject<Integer> rcp = PublishSubject.<Integer>create().refCount();

            PublishSubject<Integer> source = PublishSubject.create();

            source.subscribe(rcp);

            assertTrue(source.hasObservers());

            final TestObserver<Integer> ts1 = rcp.test();

            Runnable r1 = new Runnable() {
                @Override
                public void run() {
                    ts1.cancel();
                }
            };

            Runnable r2 = new Runnable() {
                @Override
                public void run() {
                    rcp.test().cancel();
                }
            };

            TestHelper.race(r1, r2);
        }
    }

    @Test
    public void doubleOnSubscribe() {
        final Subject<Integer> rcp = PublishSubject.<Integer>create().refCount();

        Disposable bs1 = Disposables.empty();

        rcp.onSubscribe(bs1);

        Disposable bs2 = Disposables.empty();

        rcp.onSubscribe(bs2);

        assertFalse(bs1.isDisposed());
        assertTrue(bs2.isDisposed());
    }

    @Test
    public void doubleRefCount() {
        final Subject<Integer> rcp = PublishSubject.<Integer>create().refCount();

        assertSame(rcp, rcp.refCount());
    }
}
