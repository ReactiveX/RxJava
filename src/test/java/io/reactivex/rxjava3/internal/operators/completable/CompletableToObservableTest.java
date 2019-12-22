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

package io.reactivex.rxjava3.internal.operators.completable;

import static org.junit.Assert.*;

import org.junit.Test;

import io.reactivex.rxjava3.core.*;
import io.reactivex.rxjava3.disposables.*;
import io.reactivex.rxjava3.functions.Function;
import io.reactivex.rxjava3.internal.fuseable.QueueFuseable;
import io.reactivex.rxjava3.internal.operators.completable.CompletableToObservable.ObserverCompletableObserver;
import io.reactivex.rxjava3.observers.TestObserver;
import io.reactivex.rxjava3.testsupport.TestHelper;

public class CompletableToObservableTest extends RxJavaTest {

    @Test
    public void doubleOnSubscribe() {
        TestHelper.checkDoubleOnSubscribeCompletableToObservable(new Function<Completable, Observable<?>>() {
            @Override
            public Observable<?> apply(Completable c) throws Exception {
                return c.toObservable();
            }
        });
    }

    @Test
    public void fusion() throws Exception {
        TestObserver<Void> to = new TestObserver<>();

        ObserverCompletableObserver co = new ObserverCompletableObserver(to);

        Disposable d = Disposable.empty();

        co.onSubscribe(d);

        assertEquals(QueueFuseable.NONE, co.requestFusion(QueueFuseable.SYNC));

        assertEquals(QueueFuseable.ASYNC, co.requestFusion(QueueFuseable.ASYNC));

        assertEquals(QueueFuseable.ASYNC, co.requestFusion(QueueFuseable.ANY));

        assertTrue(co.isEmpty());

        assertNull(co.poll());

        co.clear();

        assertFalse(co.isDisposed());

        co.dispose();

        assertTrue(d.isDisposed());

        assertTrue(co.isDisposed());

        TestHelper.assertNoOffer(co);
    }
}
