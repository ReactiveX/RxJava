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

package io.reactivex.rxjava3.observers;

import static org.junit.Assert.*;

import java.util.*;

import org.junit.Test;

import io.reactivex.rxjava3.core.*;
import io.reactivex.rxjava3.disposables.*;
import io.reactivex.rxjava3.exceptions.TestException;
import io.reactivex.rxjava3.internal.util.EndConsumerHelper;
import io.reactivex.rxjava3.plugins.RxJavaPlugins;
import io.reactivex.rxjava3.testsupport.TestHelper;

public class ResourceCompletableObserverTest extends RxJavaTest {
    static final class TestResourceCompletableObserver extends ResourceCompletableObserver {
        final List<Throwable> errors = new ArrayList<>();

        int complete;

        int start;

        @Override
        protected void onStart() {
            super.onStart();

            start++;
        }

        @Override
        public void onComplete() {
            complete++;

            dispose();
        }

        @Override
        public void onError(Throwable e) {
            errors.add(e);

            dispose();
        }
    }

    @Test(expected = NullPointerException.class)
    public void nullResource() {
        TestResourceCompletableObserver rco = new TestResourceCompletableObserver();
        rco.add(null);
    }

    @Test
    public void addResources() {
        TestResourceCompletableObserver rco = new TestResourceCompletableObserver();

        assertFalse(rco.isDisposed());

        Disposable d = Disposable.empty();

        rco.add(d);

        assertFalse(d.isDisposed());

        rco.dispose();

        assertTrue(rco.isDisposed());

        assertTrue(d.isDisposed());

        rco.dispose();

        assertTrue(rco.isDisposed());

        assertTrue(d.isDisposed());
    }

    @Test
    public void onCompleteCleansUp() {
        TestResourceCompletableObserver rco = new TestResourceCompletableObserver();

        assertFalse(rco.isDisposed());

        Disposable d = Disposable.empty();

        rco.add(d);

        assertFalse(d.isDisposed());

        rco.onComplete();

        assertTrue(rco.isDisposed());

        assertTrue(d.isDisposed());
    }

    @Test
    public void onErrorCleansUp() {
        TestResourceCompletableObserver rco = new TestResourceCompletableObserver();

        assertFalse(rco.isDisposed());

        Disposable d = Disposable.empty();

        rco.add(d);

        assertFalse(d.isDisposed());

        rco.onError(new TestException());

        assertTrue(rco.isDisposed());

        assertTrue(d.isDisposed());
    }

    @Test
    public void normal() {
        TestResourceCompletableObserver rco = new TestResourceCompletableObserver();

        assertFalse(rco.isDisposed());
        assertEquals(0, rco.start);
        assertTrue(rco.errors.isEmpty());

        Completable.complete().subscribe(rco);

        assertTrue(rco.isDisposed());
        assertEquals(1, rco.start);
        assertEquals(1, rco.complete);
        assertTrue(rco.errors.isEmpty());
    }

    @Test
    public void error() {
        TestResourceCompletableObserver rco = new TestResourceCompletableObserver();

        assertFalse(rco.isDisposed());
        assertEquals(0, rco.start);
        assertTrue(rco.errors.isEmpty());

        final RuntimeException error = new RuntimeException("error");
        Completable.error(error).subscribe(rco);

        assertTrue(rco.isDisposed());
        assertEquals(1, rco.start);
        assertEquals(0, rco.complete);
        assertEquals(1, rco.errors.size());
        assertTrue(rco.errors.contains(error));
    }

    @Test
    public void startOnce() {

        List<Throwable> error = TestHelper.trackPluginErrors();

        try {
            TestResourceCompletableObserver rco = new TestResourceCompletableObserver();

            rco.onSubscribe(Disposable.empty());

            Disposable d = Disposable.empty();

            rco.onSubscribe(d);

            assertTrue(d.isDisposed());

            assertEquals(1, rco.start);

            TestHelper.assertError(error, 0, IllegalStateException.class, EndConsumerHelper.composeMessage(rco.getClass().getName()));
        } finally {
            RxJavaPlugins.reset();
        }
    }

    @Test
    public void dispose() {
        TestResourceCompletableObserver rco = new TestResourceCompletableObserver();
        rco.dispose();

        Disposable d = Disposable.empty();

        rco.onSubscribe(d);

        assertTrue(d.isDisposed());

        assertEquals(0, rco.start);
    }
}
