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

package rx.internal.operators;

import static org.junit.Assert.*;

import java.io.IOException;

import org.junit.Test;
import org.mockito.Mockito;

import rx.*;
import rx.exceptions.TestException;
import rx.functions.*;
import rx.observers.TestSubscriber;
import rx.subscriptions.BooleanSubscription;

public class CompletableFromEmitterTest {

    @Test
    public void normal() {
        TestSubscriber<Void> ts = TestSubscriber.create();

        Completable.fromEmitter(new Action1<CompletableEmitter>() {
            @Override
            public void call(CompletableEmitter e) {
                e.onCompleted();
            }
        }).subscribe(ts);

        ts.assertNoValues();
        ts.assertNoErrors();
        ts.assertCompleted();
    }

    @Test
    public void error() {
        TestSubscriber<Void> ts = TestSubscriber.create();

        Completable.fromEmitter(new Action1<CompletableEmitter>() {
            @Override
            public void call(CompletableEmitter e) {
                e.onError(new TestException());
            }
        }).subscribe(ts);

        ts.assertNoValues();
        ts.assertError(TestException.class);
        ts.assertNotCompleted();
    }

    @Test
    public void ensureProtocol1() {
        TestSubscriber<Void> ts = TestSubscriber.create();

        Completable.fromEmitter(new Action1<CompletableEmitter>() {
            @Override
            public void call(CompletableEmitter e) {
                e.onError(new TestException());
                e.onCompleted();
                e.onError(new IOException());
            }
        }).subscribe(ts);

        ts.assertNoValues();
        ts.assertError(TestException.class);
        ts.assertNotCompleted();
    }

    @Test
    public void ensureProtocol2() {
        TestSubscriber<Void> ts = TestSubscriber.create();

        Completable.fromEmitter(new Action1<CompletableEmitter>() {
            @Override
            public void call(CompletableEmitter e) {
                e.onCompleted();
                e.onError(new TestException());
                e.onCompleted();
            }
        }).subscribe(ts);

        ts.assertNoValues();
        ts.assertNoErrors();
        ts.assertCompleted();
    }

    @Test
    public void resourceCleanupNormal() {
        TestSubscriber<Void> ts = TestSubscriber.create();

        final BooleanSubscription bs = new BooleanSubscription();

        assertFalse(bs.isUnsubscribed());

        Completable.fromEmitter(new Action1<CompletableEmitter>() {
            @Override
            public void call(CompletableEmitter e) {
                e.setSubscription(bs);
                e.onCompleted();
            }
        }).subscribe(ts);

        ts.assertNoValues();
        ts.assertNoErrors();
        ts.assertCompleted();

        assertTrue(bs.isUnsubscribed());
    }

    @Test
    public void resourceCleanupError() {
        TestSubscriber<Void> ts = TestSubscriber.create();

        final BooleanSubscription bs = new BooleanSubscription();

        assertFalse(bs.isUnsubscribed());

        Completable.fromEmitter(new Action1<CompletableEmitter>() {
            @Override
            public void call(CompletableEmitter e) {
                e.setSubscription(bs);
                e.onError(new TestException());
            }
        }).subscribe(ts);

        ts.assertNoValues();
        ts.assertError(TestException.class);
        ts.assertNotCompleted();

        assertTrue(bs.isUnsubscribed());
    }

    @Test
    public void resourceCleanupCancellable() throws Exception {
        TestSubscriber<Void> ts = TestSubscriber.create();

        final Cancellable c = Mockito.mock(Cancellable.class);

        Completable.fromEmitter(new Action1<CompletableEmitter>() {
            @Override
            public void call(CompletableEmitter e) {
                e.setCancellation(c);
                e.onCompleted();
            }
        }).subscribe(ts);

        ts.assertNoValues();
        ts.assertNoErrors();
        ts.assertCompleted();

        Mockito.verify(c).cancel();
    }

    @Test
    public void resourceCleanupUnsubscirbe() throws Exception {
        TestSubscriber<Void> ts = TestSubscriber.create();
        ts.unsubscribe();

        final Cancellable c = Mockito.mock(Cancellable.class);

        Completable.fromEmitter(new Action1<CompletableEmitter>() {
            @Override
            public void call(CompletableEmitter e) {
                e.setCancellation(c);
                e.onCompleted();
            }
        }).subscribe(ts);

        ts.assertNoValues();
        ts.assertNoErrors();
        ts.assertNotCompleted();

        Mockito.verify(c).cancel();
    }

    @Test
    public void resourceCleanupOnCompleteCrashes() throws Exception {
        final Cancellable c = Mockito.mock(Cancellable.class);

        Completable.fromEmitter(new Action1<CompletableEmitter>() {
            @Override
            public void call(CompletableEmitter e) {
                e.setCancellation(c);
                e.onCompleted();
            }
        }).unsafeSubscribe(new CompletableSubscriber() {

            @Override
            public void onCompleted() {
                throw new TestException();
            }

            @Override
            public void onError(Throwable e) {
                throw new TestException();
            }

            @Override
            public void onSubscribe(Subscription d) {

            }

        });


        Mockito.verify(c).cancel();
    }

    @Test
    public void resourceCleanupOnErrorCrashes() throws Exception {
        final Cancellable c = Mockito.mock(Cancellable.class);

        Completable.fromEmitter(new Action1<CompletableEmitter>() {
            @Override
            public void call(CompletableEmitter e) {
                e.setCancellation(c);
                e.onError(new IOException());
            }
        }).unsafeSubscribe(new CompletableSubscriber() {

            @Override
            public void onCompleted() {
                throw new TestException();
            }

            @Override
            public void onError(Throwable e) {
                throw new TestException();
            }

            @Override
            public void onSubscribe(Subscription d) {

            }

        });


        Mockito.verify(c).cancel();
    }
    @Test
    public void producerCrashes() {
        TestSubscriber<Void> ts = TestSubscriber.create();

        Completable.fromEmitter(new Action1<CompletableEmitter>() {
            @Override
            public void call(CompletableEmitter e) {
                throw new TestException();
            }
        }).subscribe(ts);

        ts.assertNoValues();
        ts.assertError(TestException.class);
        ts.assertNotCompleted();
    }

    @Test
    public void producerCrashesAfterSignal() {
        TestSubscriber<Void> ts = TestSubscriber.create();

        Completable.fromEmitter(new Action1<CompletableEmitter>() {
            @Override
            public void call(CompletableEmitter e) {
                e.onCompleted();
                throw new TestException();
            }
        }).subscribe(ts);

        ts.assertNoValues();
        ts.assertNoErrors();
        ts.assertCompleted();
    }

    @Test
    public void concurrentUse() {
        for (int i = 0; i < 500; i++) {
            TestSubscriber<Void> ts = TestSubscriber.create();

            final CompletableEmitter[] emitter = { null };

            Completable.fromEmitter(new Action1<CompletableEmitter>() {
                @Override
                public void call(CompletableEmitter e) {
                    emitter[0] = e;
                }
            }).subscribe(ts);

            final TestException ex = new TestException();
            final CompletableEmitter e = emitter[0];

            TestUtil.race(new Action0() {
                @Override
                public void call() {
                    e.onCompleted();
                }
            }, new Action0() {
                @Override
                public void call() {
                    e.onError(ex);
                }
            });

            if (ts.getCompletions() != 0) {
                ts.assertNoValues();
                ts.assertNoErrors();
                ts.assertCompleted();
            }
            if (!ts.getOnErrorEvents().isEmpty()) {
                ts.assertNoValues();
                ts.assertError(ex);
                ts.assertNotCompleted();
            }
        }
    }
}
