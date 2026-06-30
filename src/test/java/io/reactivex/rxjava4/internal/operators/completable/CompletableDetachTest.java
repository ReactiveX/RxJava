/*
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

package io.reactivex.rxjava4.internal.operators.completable;

import static org.junit.jupiter.api.Assertions.assertNull;

import java.io.IOException;
import java.lang.ref.WeakReference;

import org.junit.jupiter.api.Test;

import io.reactivex.rxjava4.core.*;
import io.reactivex.rxjava4.disposables.Disposable;
import io.reactivex.rxjava4.exceptions.TestException;
import io.reactivex.rxjava4.observers.TestObserver;
import io.reactivex.rxjava4.processors.PublishProcessor;
import io.reactivex.rxjava4.testsupport.TestHelper;

public class CompletableDetachTest extends RxJavaTest {

    @Test
    public void doubleSubscribe() {

        TestHelper.checkDoubleOnSubscribeCompletable(Completable::onTerminateDetach);
    }

    @Test
    public void dispose() {
        TestHelper.checkDisposed(PublishProcessor.create().ignoreElements().onTerminateDetach());
    }

    @Test
    public void onError() {
        Completable.error(new TestException())
        .onTerminateDetach()
        .test()
        .assertFailure(TestException.class);
    }

    @Test
    public void onComplete() {
        Completable.complete()
        .onTerminateDetach()
        .test()
        .assertResult();
    }

    @Test
    public void cancelDetaches() throws Exception {
        Disposable d = Disposable.empty();
        final WeakReference<Disposable> wr = new WeakReference<>(d);

        TestObserver<Void> to = new Completable() /* NFI */ {
            @Override
            protected void subscribeActual(CompletableObserver observer) {
                observer.onSubscribe(wr.get());
            }
        }
        .onTerminateDetach()
        .test();

        d = null;

        to.dispose();

        System.gc();
        Thread.sleep(200);

        to.assertEmpty();

        assertNull(wr.get());
    }

    @Test
    public void completeDetaches() throws Exception {
        Disposable d = Disposable.empty();
        final WeakReference<Disposable> wr = new WeakReference<>(d);

        TestObserver<Void> to = new Completable() /* NFI */ {
            @Override
            protected void subscribeActual(CompletableObserver observer) {
                observer.onSubscribe(wr.get());
                observer.onComplete();
                observer.onComplete();
            }
        }
        .onTerminateDetach()
        .test();

        d = null;

        System.gc();
        Thread.sleep(200);

        to.assertResult();

        assertNull(wr.get());
    }

    @Test
    public void errorDetaches() throws Exception {
        Disposable d = Disposable.empty();
        final WeakReference<Disposable> wr = new WeakReference<>(d);

        TestObserver<Void> to = new Completable() /* NFI */ {
            @Override
            protected void subscribeActual(CompletableObserver observer) {
                observer.onSubscribe(wr.get());
                observer.onError(new TestException());
                observer.onError(new IOException());
            }
        }
        .onTerminateDetach()
        .test();

        d = null;

        System.gc();
        Thread.sleep(200);

        to.assertFailure(TestException.class);

        assertNull(wr.get());
    }
}
