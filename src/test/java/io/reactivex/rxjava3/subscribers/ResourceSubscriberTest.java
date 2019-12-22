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

package io.reactivex.rxjava3.subscribers;

import static org.junit.Assert.*;

import java.util.*;

import org.junit.Test;

import io.reactivex.rxjava3.core.*;
import io.reactivex.rxjava3.disposables.*;
import io.reactivex.rxjava3.exceptions.TestException;
import io.reactivex.rxjava3.internal.subscriptions.BooleanSubscription;
import io.reactivex.rxjava3.internal.util.EndConsumerHelper;
import io.reactivex.rxjava3.plugins.RxJavaPlugins;
import io.reactivex.rxjava3.testsupport.TestHelper;

public class ResourceSubscriberTest extends RxJavaTest {

    static class TestResourceSubscriber<T> extends ResourceSubscriber<T> {
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

        void requestMore(long n) {
            request(n);
        }
    }

    @Test(expected = NullPointerException.class)
    public void nullResource() {
        TestResourceSubscriber<Integer> ro = new TestResourceSubscriber<>();
        ro.add(null);
    }

    @Test
    public void addResources() {
        TestResourceSubscriber<Integer> ro = new TestResourceSubscriber<>();

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
        TestResourceSubscriber<Integer> ro = new TestResourceSubscriber<>();

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
        TestResourceSubscriber<Integer> ro = new TestResourceSubscriber<>();

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
        TestResourceSubscriber<Integer> tc = new TestResourceSubscriber<>();

        assertFalse(tc.isDisposed());
        assertEquals(0, tc.start);
        assertTrue(tc.values.isEmpty());
        assertTrue(tc.errors.isEmpty());

        Flowable.just(1).subscribe(tc);

        assertTrue(tc.isDisposed());
        assertEquals(1, tc.start);
        assertEquals(1, tc.values.get(0).intValue());
        assertTrue(tc.errors.isEmpty());
    }

    @Test
    public void startOnce() {

        List<Throwable> error = TestHelper.trackPluginErrors();

        try {
            TestResourceSubscriber<Integer> tc = new TestResourceSubscriber<>();

            tc.onSubscribe(new BooleanSubscription());

            BooleanSubscription bs = new BooleanSubscription();

            tc.onSubscribe(bs);

            assertTrue(bs.isCancelled());

            assertEquals(1, tc.start);

            TestHelper.assertError(error, 0, IllegalStateException.class, EndConsumerHelper.composeMessage(tc.getClass().getName()));
        } finally {
            RxJavaPlugins.reset();
        }
    }

    @Test
    public void dispose() {
        TestResourceSubscriber<Integer> tc = new TestResourceSubscriber<>();
        tc.dispose();

        BooleanSubscription bs = new BooleanSubscription();

        tc.onSubscribe(bs);

        assertTrue(bs.isCancelled());

        assertEquals(0, tc.start);
    }

    @Test
    public void request() {
        TestResourceSubscriber<Integer> tc = new TestResourceSubscriber<Integer>() {
            @Override
            protected void onStart() {
                start++;
            }
        };

        Flowable.just(1).subscribe(tc);

        assertEquals(1, tc.start);
        assertEquals(Collections.emptyList(), tc.values);
        assertTrue(tc.errors.isEmpty());
        assertEquals(0, tc.complete);

        tc.requestMore(1);

        assertEquals(1, tc.start);
        assertEquals(1, tc.values.get(0).intValue());
        assertTrue(tc.errors.isEmpty());
        assertEquals(1, tc.complete);
    }

    static final class RequestEarly extends ResourceSubscriber<Integer> {

        final List<Object> events = new ArrayList<>();

        RequestEarly() {
            request(5);
        }

        @Override
        protected void onStart() {
        }

        @Override
        public void onNext(Integer t) {
            events.add(t);
        }

        @Override
        public void onError(Throwable t) {
            events.add(t);
        }

        @Override
        public void onComplete() {
            events.add("Done");
        }

    }

    @Test
    public void requestUpfront() {
        RequestEarly sub = new RequestEarly();

        Flowable.range(1, 10).subscribe(sub);

        assertEquals(Arrays.<Object>asList(1, 2, 3, 4, 5), sub.events);
    }
}
