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

package io.reactivex.observers;

import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

import io.reactivex.Single;
import io.reactivex.TestHelper;
import io.reactivex.disposables.Disposable;
import io.reactivex.disposables.Disposables;
import io.reactivex.exceptions.TestException;
import io.reactivex.internal.util.EndConsumerHelper;
import io.reactivex.plugins.RxJavaPlugins;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

public class ResourceSingleObserverTest {
    static final class TestResourceSingleObserver<T> extends ResourceSingleObserver<T> {
        T value;

        final List<Throwable> errors = new ArrayList<Throwable>();

        int start;

        @Override
        protected void onStart() {
            super.onStart();

            start++;
        }

        @Override
        public void onSuccess(final T value) {
            this.value = value;

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
        TestResourceSingleObserver<Integer> rso = new TestResourceSingleObserver<Integer>();
        rso.add(null);
    }

    @Test
    public void addResources() {
        TestResourceSingleObserver<Integer> rso = new TestResourceSingleObserver<Integer>();

        assertFalse(rso.isDisposed());

        Disposable d = Disposables.empty();

        rso.add(d);

        assertFalse(d.isDisposed());

        rso.dispose();

        assertTrue(rso.isDisposed());

        assertTrue(d.isDisposed());

        rso.dispose();

        assertTrue(rso.isDisposed());

        assertTrue(d.isDisposed());
    }

    @Test
    public void onSuccessCleansUp() {
        TestResourceSingleObserver<Integer> rso = new TestResourceSingleObserver<Integer>();

        assertFalse(rso.isDisposed());

        Disposable d = Disposables.empty();

        rso.add(d);

        assertFalse(d.isDisposed());

        rso.onSuccess(1);

        assertTrue(rso.isDisposed());

        assertTrue(d.isDisposed());
    }

    @Test
    public void onErrorCleansUp() {
        TestResourceSingleObserver<Integer> rso = new TestResourceSingleObserver<Integer>();

        assertFalse(rso.isDisposed());

        Disposable d = Disposables.empty();

        rso.add(d);

        assertFalse(d.isDisposed());

        rso.onError(new TestException());

        assertTrue(rso.isDisposed());

        assertTrue(d.isDisposed());
    }

    @Test
    public void normal() {
        TestResourceSingleObserver<Integer> rso = new TestResourceSingleObserver<Integer>();

        assertFalse(rso.isDisposed());
        assertEquals(0, rso.start);
        assertNull(rso.value);
        assertTrue(rso.errors.isEmpty());

        Single.just(1).subscribe(rso);

        assertTrue(rso.isDisposed());
        assertEquals(1, rso.start);
        assertEquals(Integer.valueOf(1), rso.value);
        assertTrue(rso.errors.isEmpty());
    }

    @Test
    public void error() {
        TestResourceSingleObserver<Integer> rso = new TestResourceSingleObserver<Integer>();

        assertFalse(rso.isDisposed());
        assertEquals(0, rso.start);
        assertNull(rso.value);
        assertTrue(rso.errors.isEmpty());

        final RuntimeException error = new RuntimeException("error");
        Single.<Integer>error(error).subscribe(rso);

        assertTrue(rso.isDisposed());
        assertEquals(1, rso.start);
        assertNull(rso.value);
        assertEquals(1, rso.errors.size());
        assertTrue(rso.errors.contains(error));
    }

    @Test
    public void startOnce() {

        List<Throwable> error = TestHelper.trackPluginErrors();

        try {
            TestResourceSingleObserver<Integer> rso = new TestResourceSingleObserver<Integer>();

            rso.onSubscribe(Disposables.empty());

            Disposable d = Disposables.empty();

            rso.onSubscribe(d);

            assertTrue(d.isDisposed());

            assertEquals(1, rso.start);

            TestHelper.assertError(error, 0, IllegalStateException.class, EndConsumerHelper.composeMessage(rso.getClass().getName()));
        } finally {
            RxJavaPlugins.reset();
        }
    }

    @Test
    public void dispose() {
        TestResourceSingleObserver<Integer> rso = new TestResourceSingleObserver<Integer>();
        rso.dispose();

        Disposable d = Disposables.empty();

        rso.onSubscribe(d);

        assertTrue(d.isDisposed());

        assertEquals(0, rso.start);
    }
}
