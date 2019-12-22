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

package io.reactivex.rxjava3.observers;

import static org.junit.Assert.*;

import java.util.*;

import org.junit.Test;

import io.reactivex.rxjava3.core.Observable;
import io.reactivex.rxjava3.core.RxJavaTest;
import io.reactivex.rxjava3.disposables.*;
import io.reactivex.rxjava3.exceptions.TestException;
import io.reactivex.rxjava3.internal.util.EndConsumerHelper;
import io.reactivex.rxjava3.plugins.RxJavaPlugins;
import io.reactivex.rxjava3.testsupport.TestHelper;

public class ResourceObserverTest extends RxJavaTest {

    static final class TestResourceObserver<T> extends ResourceObserver<T> {
        final List<T> values = new ArrayList<>();

        final List<Throwable> errors = new ArrayList<>();

        int complete;

        int start;

        @Override
        protected void onStart() {
            super.onStart();

            start++;
        }

        @Override
        public void onNext(T value) {
            values.add(value);
        }

        @Override
        public void onError(Throwable e) {
            errors.add(e);

            dispose();
        }

        @Override
        public void onComplete() {
            complete++;

            dispose();
        }
    }

    @Test(expected = NullPointerException.class)
    public void nullResource() {
        TestResourceObserver<Integer> ro = new TestResourceObserver<>();
        ro.add(null);
    }

    @Test
    public void addResources() {
        TestResourceObserver<Integer> ro = new TestResourceObserver<>();

        assertFalse(ro.isDisposed());

        Disposable d = Disposable.empty();

        ro.add(d);

        assertFalse(d.isDisposed());

        ro.dispose();

        assertTrue(ro.isDisposed());

        assertTrue(d.isDisposed());

        ro.dispose();

        assertTrue(ro.isDisposed());

        assertTrue(d.isDisposed());
    }

    @Test
    public void onCompleteCleansUp() {
        TestResourceObserver<Integer> ro = new TestResourceObserver<>();

        assertFalse(ro.isDisposed());

        Disposable d = Disposable.empty();

        ro.add(d);

        assertFalse(d.isDisposed());

        ro.onComplete();

        assertTrue(ro.isDisposed());

        assertTrue(d.isDisposed());
    }

    @Test
    public void onErrorCleansUp() {
        TestResourceObserver<Integer> ro = new TestResourceObserver<>();

        assertFalse(ro.isDisposed());

        Disposable d = Disposable.empty();

        ro.add(d);

        assertFalse(d.isDisposed());

        ro.onError(new TestException());

        assertTrue(ro.isDisposed());

        assertTrue(d.isDisposed());
    }

    @Test
    public void normal() {
        TestResourceObserver<Integer> tc = new TestResourceObserver<>();

        assertFalse(tc.isDisposed());
        assertEquals(0, tc.start);
        assertTrue(tc.values.isEmpty());
        assertTrue(tc.errors.isEmpty());

        Observable.just(1).subscribe(tc);

        assertTrue(tc.isDisposed());
        assertEquals(1, tc.start);
        assertEquals(1, tc.values.get(0).intValue());
        assertTrue(tc.errors.isEmpty());
    }

    @Test
    public void error() {
        TestResourceObserver<Integer> tc = new TestResourceObserver<>();

        assertFalse(tc.isDisposed());
        assertEquals(0, tc.start);
        assertTrue(tc.values.isEmpty());
        assertTrue(tc.errors.isEmpty());

        final RuntimeException error = new RuntimeException("error");
        Observable.<Integer>error(error).subscribe(tc);

        assertTrue(tc.isDisposed());
        assertEquals(1, tc.start);
        assertTrue(tc.values.isEmpty());
        assertEquals(1, tc.errors.size());
        assertTrue(tc.errors.contains(error));
    }

    @Test
    public void startOnce() {

        List<Throwable> error = TestHelper.trackPluginErrors();

        try {
            TestResourceObserver<Integer> tc = new TestResourceObserver<>();

            tc.onSubscribe(Disposable.empty());

            Disposable d = Disposable.empty();

            tc.onSubscribe(d);

            assertTrue(d.isDisposed());

            assertEquals(1, tc.start);

            TestHelper.assertError(error, 0, IllegalStateException.class, EndConsumerHelper.composeMessage(tc.getClass().getName()));
        } finally {
            RxJavaPlugins.reset();
        }
    }

    @Test
    public void dispose() {
        TestResourceObserver<Integer> tc = new TestResourceObserver<>();
        tc.dispose();

        Disposable d = Disposable.empty();

        tc.onSubscribe(d);

        assertTrue(d.isDisposed());

        assertEquals(0, tc.start);
    }
}
