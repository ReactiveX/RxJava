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

package io.reactivex.internal.subscribers;

import static org.junit.Assert.*;

import org.junit.Test;

import io.reactivex.disposables.*;
import io.reactivex.exceptions.TestException;
import io.reactivex.internal.subscriptions.BooleanSubscription;
import io.reactivex.subscribers.TestSubscriber;

public class SubscriberResourceWrapperTest {

    TestSubscriber<Integer> ts = new TestSubscriber<Integer>();

    SubscriberResourceWrapper<Integer> s = new SubscriberResourceWrapper<Integer>(ts);

    @Test
    public void cancel() {
        BooleanSubscription bs = new BooleanSubscription();
        Disposable d = Disposables.empty();

        s.setResource(d);

        s.onSubscribe(bs);

        assertFalse(d.isDisposed());
        assertFalse(s.isDisposed());

        ts.cancel();

        assertTrue(bs.isCancelled());
        assertTrue(d.isDisposed());
        assertTrue(s.isDisposed());
    }

    @Test
    public void error() {
        BooleanSubscription bs = new BooleanSubscription();
        Disposable d = Disposables.empty();

        s.setResource(d);

        s.onSubscribe(bs);

        s.onError(new TestException());

        assertTrue(d.isDisposed());
        assertFalse(bs.isCancelled());

        ts.assertFailure(TestException.class);
    }

    @Test
    public void complete() {
        BooleanSubscription bs = new BooleanSubscription();
        Disposable d = Disposables.empty();

        s.setResource(d);

        s.onSubscribe(bs);

        s.onComplete();

        assertTrue(d.isDisposed());
        assertFalse(bs.isCancelled());

        ts.assertResult();
    }
}
