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

package io.reactivex.internal.operators.completable;

import static org.junit.Assert.*;

import java.util.List;

import org.junit.Test;

import io.reactivex.*;
import io.reactivex.disposables.Disposables;
import io.reactivex.exceptions.TestException;
import io.reactivex.observers.TestObserver;
import io.reactivex.plugins.RxJavaPlugins;
import io.reactivex.processors.PublishProcessor;

public class CompletableMergeTest {
    @Test
    public void invalidPrefetch() {
        try {
            Completable.merge(Flowable.just(Completable.complete()), -99);
            fail("Should have thrown IllegalArgumentExceptio");
        } catch (IllegalArgumentException ex) {
            assertEquals("maxConcurrency > 0 required but it was -99", ex.getMessage());
        }
    }

    @Test
    public void cancelAfterFirst() {
        final TestObserver<Void> to = new TestObserver<Void>();

        Completable.mergeArray(new Completable() {
            @Override
            protected void subscribeActual(CompletableObserver s) {
                s.onSubscribe(Disposables.empty());
                s.onComplete();
                to.cancel();
            }
        }, Completable.complete())
        .subscribe(to);

        to.assertEmpty();
    }

    @Test
    public void cancelAfterFirstDelayError() {
        final TestObserver<Void> to = new TestObserver<Void>();

        Completable.mergeArrayDelayError(new Completable() {
            @Override
            protected void subscribeActual(CompletableObserver s) {
                s.onSubscribe(Disposables.empty());
                s.onComplete();
                to.cancel();
            }
        }, Completable.complete())
        .subscribe(to);

        to.assertEmpty();
    }

    @Test
    public void onErrorAfterComplete() {
        List<Throwable> errors = TestHelper.trackPluginErrors();
        try {
            final CompletableObserver[] co = { null };

            Completable.mergeArrayDelayError(Completable.complete(), new Completable() {
                @Override
                protected void subscribeActual(CompletableObserver s) {
                    s.onSubscribe(Disposables.empty());
                    s.onComplete();
                    co[0] = s;
                }
            })
            .test()
            .assertResult();

            co[0].onError(new TestException());

            TestHelper.assertError(errors, 0, TestException.class);
        } finally {
            RxJavaPlugins.reset();
        }
    }

    @Test
    public void completeAfterMain() {
        PublishProcessor<Integer> pp = PublishProcessor.create();

        TestObserver<Void> to = Completable.mergeArray(Completable.complete(), pp.ignoreElements())
        .test();

        pp.onComplete();

        to.assertResult();
    }

    @Test
    public void completeAfterMainDelayError() {
        PublishProcessor<Integer> pp = PublishProcessor.create();

        TestObserver<Void> to = Completable.mergeArrayDelayError(Completable.complete(), pp.ignoreElements())
        .test();

        pp.onComplete();

        to.assertResult();
    }

    @Test
    public void errorAfterMainDelayError() {
        PublishProcessor<Integer> pp = PublishProcessor.create();

        TestObserver<Void> to = Completable.mergeArrayDelayError(Completable.complete(), pp.ignoreElements())
        .test();

        pp.onError(new TestException());

        to.assertFailure(TestException.class);
    }
}
