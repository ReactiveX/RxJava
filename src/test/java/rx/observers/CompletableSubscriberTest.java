/**
 * Copyright 2016 Netflix, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package rx.observers;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.*;

import rx.CompletableSubscriber;
import rx.exceptions.*;
import rx.Subscription;
import rx.subscriptions.Subscriptions;

public class CompletableSubscriberTest {

    @Test
    public void childOnSubscribeThrows() {

        final AtomicReference<Throwable> error = new AtomicReference<Throwable>();

        SafeCompletableSubscriber safe = new SafeCompletableSubscriber(new CompletableSubscriber() {

            @Override
            public void onSubscribe(Subscription d) {
                throw new TestException();
            }

            @Override
            public void onError(Throwable e) {
                error.set(e);

            }

            @Override
            public void onCompleted() {

            }
        });

        safe.onSubscribe(Subscriptions.empty());

        Assert.assertTrue("" + error.get(), error.get() instanceof TestException);

        Assert.assertTrue(safe.isUnsubscribed());
    }

    @Test
    public void unsubscribeComposes() {

        SafeCompletableSubscriber safe = new SafeCompletableSubscriber(new CompletableSubscriber() {

            @Override
            public void onSubscribe(Subscription d) {
            }

            @Override
            public void onError(Throwable e) {
            }

            @Override
            public void onCompleted() {

            }
        });

        Subscription empty = Subscriptions.empty();
        safe.onSubscribe(empty);

        Assert.assertFalse(empty.isUnsubscribed());
        Assert.assertFalse(safe.isUnsubscribed());

        safe.unsubscribe();

        Assert.assertTrue(empty.isUnsubscribed());
        Assert.assertTrue(safe.isUnsubscribed());
    }

    @Test
    public void childOnErrorThrows() {

        SafeCompletableSubscriber safe = new SafeCompletableSubscriber(new CompletableSubscriber() {

            @Override
            public void onSubscribe(Subscription d) {
            }

            @Override
            public void onError(Throwable e) {
                throw new TestException();
            }

            @Override
            public void onCompleted() {

            }
        });

        safe.onSubscribe(Subscriptions.empty());

        try {
            safe.onError(new IOException());
            Assert.fail("Didn't throw a fatal exception");
        } catch (OnErrorFailedException ex) {
            CompositeException ce = (CompositeException)ex.getCause();

            List<Throwable> list = ce.getExceptions();
            Assert.assertEquals(2, list.size());

            Assert.assertTrue("" + list.get(0), list.get(0) instanceof IOException);
            Assert.assertTrue("" + list.get(1), list.get(1) instanceof TestException);
        }
    }

    @Test
    public void preventsCompleteError() {

        final boolean[] calls = { false, false };

        SafeCompletableSubscriber safe = new SafeCompletableSubscriber(new CompletableSubscriber() {

            @Override
            public void onSubscribe(Subscription d) {
            }

            @Override
            public void onError(Throwable e) {
                calls[0] = true;
            }

            @Override
            public void onCompleted() {
                calls[1] = true;
            }
        });

        safe.onSubscribe(Subscriptions.empty());

        safe.onCompleted();
        safe.onError(new TestException());

        Assert.assertTrue(safe.isUnsubscribed());
        Assert.assertFalse(calls[0]);
        Assert.assertTrue(calls[1]);
    }

    @Test
    public void preventsErrorComplete() {

        final boolean[] calls = { false, false };

        SafeCompletableSubscriber safe = new SafeCompletableSubscriber(new CompletableSubscriber() {

            @Override
            public void onSubscribe(Subscription d) {
            }

            @Override
            public void onError(Throwable e) {
                calls[0] = true;
            }

            @Override
            public void onCompleted() {
                calls[1] = true;
            }
        });

        safe.onSubscribe(Subscriptions.empty());

        safe.onError(new TestException());
        safe.onCompleted();

        Assert.assertTrue(safe.isUnsubscribed());
        Assert.assertTrue(calls[0]);
        Assert.assertFalse(calls[1]);
    }
}
