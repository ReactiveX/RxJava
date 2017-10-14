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

import io.reactivex.Maybe;
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

public class ResourceMaybeObserverTest {
    static final class TestResourceMaybeObserver<T> extends ResourceMaybeObserver<T> {
        T value;

        final List<Throwable> errors = new ArrayList<Throwable>();

        int complete;

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
        TestResourceMaybeObserver<Integer> rmo = new TestResourceMaybeObserver<Integer>();
        rmo.add(null);
    }

    @Test
    public void addResources() {
        TestResourceMaybeObserver<Integer> rmo = new TestResourceMaybeObserver<Integer>();

        assertFalse(rmo.isDisposed());

        Disposable d = Disposables.empty();

        rmo.add(d);

        assertFalse(d.isDisposed());

        rmo.dispose();

        assertTrue(rmo.isDisposed());

        assertTrue(d.isDisposed());

        rmo.dispose();

        assertTrue(rmo.isDisposed());

        assertTrue(d.isDisposed());
    }

    @Test
    public void onCompleteCleansUp() {
        TestResourceMaybeObserver<Integer> rmo = new TestResourceMaybeObserver<Integer>();

        assertFalse(rmo.isDisposed());

        Disposable d = Disposables.empty();

        rmo.add(d);

        assertFalse(d.isDisposed());

        rmo.onComplete();

        assertTrue(rmo.isDisposed());

        assertTrue(d.isDisposed());
    }

    @Test
    public void onSuccessCleansUp() {
        TestResourceMaybeObserver<Integer> rmo = new TestResourceMaybeObserver<Integer>();

        assertFalse(rmo.isDisposed());

        Disposable d = Disposables.empty();

        rmo.add(d);

        assertFalse(d.isDisposed());

        rmo.onSuccess(1);

        assertTrue(rmo.isDisposed());

        assertTrue(d.isDisposed());
    }

    @Test
    public void onErrorCleansUp() {
        TestResourceMaybeObserver<Integer> rmo = new TestResourceMaybeObserver<Integer>();

        assertFalse(rmo.isDisposed());

        Disposable d = Disposables.empty();

        rmo.add(d);

        assertFalse(d.isDisposed());

        rmo.onError(new TestException());

        assertTrue(rmo.isDisposed());

        assertTrue(d.isDisposed());
    }

    @Test
    public void normal() {
        TestResourceMaybeObserver<Integer> rmo = new TestResourceMaybeObserver<Integer>();

        assertFalse(rmo.isDisposed());
        assertEquals(0, rmo.start);
        assertNull(rmo.value);
        assertTrue(rmo.errors.isEmpty());

        Maybe.just(1).subscribe(rmo);

        assertTrue(rmo.isDisposed());
        assertEquals(1, rmo.start);
        assertEquals(Integer.valueOf(1), rmo.value);
        assertEquals(0, rmo.complete);
        assertTrue(rmo.errors.isEmpty());
    }

    @Test
    public void empty() {
        TestResourceMaybeObserver<Integer> rmo = new TestResourceMaybeObserver<Integer>();

        assertFalse(rmo.isDisposed());
        assertEquals(0, rmo.start);
        assertNull(rmo.value);
        assertTrue(rmo.errors.isEmpty());

        Maybe.<Integer>empty().subscribe(rmo);

        assertTrue(rmo.isDisposed());
        assertEquals(1, rmo.start);
        assertNull(rmo.value);
        assertEquals(1, rmo.complete);
        assertTrue(rmo.errors.isEmpty());
    }

    @Test
    public void error() {
        TestResourceMaybeObserver<Integer> rmo = new TestResourceMaybeObserver<Integer>();

        assertFalse(rmo.isDisposed());
        assertEquals(0, rmo.start);
        assertNull(rmo.value);
        assertTrue(rmo.errors.isEmpty());

        final RuntimeException error = new RuntimeException("error");
        Maybe.<Integer>error(error).subscribe(rmo);

        assertTrue(rmo.isDisposed());
        assertEquals(1, rmo.start);
        assertNull(rmo.value);
        assertEquals(0, rmo.complete);
        assertEquals(1, rmo.errors.size());
        assertTrue(rmo.errors.contains(error));
    }

    @Test
    public void startOnce() {

        List<Throwable> error = TestHelper.trackPluginErrors();

        try {
            TestResourceMaybeObserver<Integer> rmo = new TestResourceMaybeObserver<Integer>();

            rmo.onSubscribe(Disposables.empty());

            Disposable d = Disposables.empty();

            rmo.onSubscribe(d);

            assertTrue(d.isDisposed());

            assertEquals(1, rmo.start);

            TestHelper.assertError(error, 0, IllegalStateException.class, EndConsumerHelper.composeMessage(rmo.getClass().getName()));
        } finally {
            RxJavaPlugins.reset();
        }
    }

    @Test
    public void dispose() {
        TestResourceMaybeObserver<Integer> rmo = new TestResourceMaybeObserver<Integer>();
        rmo.dispose();

        Disposable d = Disposables.empty();

        rmo.onSubscribe(d);

        assertTrue(d.isDisposed());

        assertEquals(0, rmo.start);
    }
}
