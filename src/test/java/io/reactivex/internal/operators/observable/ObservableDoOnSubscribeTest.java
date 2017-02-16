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

import java.util.List;
import java.util.concurrent.atomic.*;

import org.junit.Test;

import io.reactivex.*;
import io.reactivex.disposables.*;
import io.reactivex.exceptions.TestException;
import io.reactivex.functions.Consumer;
import io.reactivex.plugins.RxJavaPlugins;

public class ObservableDoOnSubscribeTest {

    @Test
    public void testDoOnSubscribe() throws Exception {
        final AtomicInteger count = new AtomicInteger();
        Observable<Integer> o = Observable.just(1).doOnSubscribe(new Consumer<Disposable>() {
            @Override
            public void accept(Disposable s) {
                    count.incrementAndGet();
            }
        });

        o.subscribe();
        o.subscribe();
        o.subscribe();
        assertEquals(3, count.get());
    }

    @Test
    public void testDoOnSubscribe2() throws Exception {
        final AtomicInteger count = new AtomicInteger();
        Observable<Integer> o = Observable.just(1).doOnSubscribe(new Consumer<Disposable>() {
            @Override
            public void accept(Disposable s) {
                    count.incrementAndGet();
            }
        }).take(1).doOnSubscribe(new Consumer<Disposable>() {
            @Override
            public void accept(Disposable s) {
                    count.incrementAndGet();
            }
        });

        o.subscribe();
        assertEquals(2, count.get());
    }

    @Test
    public void testDoOnUnSubscribeWorksWithRefCount() throws Exception {
        final AtomicInteger onSubscribed = new AtomicInteger();
        final AtomicInteger countBefore = new AtomicInteger();
        final AtomicInteger countAfter = new AtomicInteger();
        final AtomicReference<Observer<? super Integer>> sref = new AtomicReference<Observer<? super Integer>>();
        Observable<Integer> o = Observable.unsafeCreate(new ObservableSource<Integer>() {

            @Override
            public void subscribe(Observer<? super Integer> s) {
                s.onSubscribe(Disposables.empty());
                onSubscribed.incrementAndGet();
                sref.set(s);
            }

        }).doOnSubscribe(new Consumer<Disposable>() {
            @Override
            public void accept(Disposable s) {
                    countBefore.incrementAndGet();
            }
        }).publish().refCount()
        .doOnSubscribe(new Consumer<Disposable>() {
            @Override
            public void accept(Disposable s) {
                    countAfter.incrementAndGet();
            }
        });

        o.subscribe();
        o.subscribe();
        o.subscribe();
        assertEquals(1, countBefore.get());
        assertEquals(1, onSubscribed.get());
        assertEquals(3, countAfter.get());
        sref.get().onComplete();
        o.subscribe();
        o.subscribe();
        o.subscribe();
        assertEquals(2, countBefore.get());
        assertEquals(2, onSubscribed.get());
        assertEquals(6, countAfter.get());
    }

    @Test
    public void onSubscribeCrash() {
        List<Throwable> errors = TestHelper.trackPluginErrors();
        try {
            final Disposable bs = Disposables.empty();

            new Observable<Integer>() {
                @Override
                protected void subscribeActual(Observer<? super Integer> s) {
                    s.onSubscribe(bs);
                    s.onError(new TestException("Second"));
                    s.onComplete();
                }
            }
            .doOnSubscribe(new Consumer<Disposable>() {
                @Override
                public void accept(Disposable s) throws Exception {
                    throw new TestException("First");
                }
            })
            .test()
            .assertFailureAndMessage(TestException.class, "First");

            assertTrue(bs.isDisposed());

            TestHelper.assertUndeliverable(errors, 0, TestException.class, "Second");
        } finally {
            RxJavaPlugins.reset();
        }
    }
}
