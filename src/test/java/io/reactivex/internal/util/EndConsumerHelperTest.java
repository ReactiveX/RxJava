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

package io.reactivex.internal.util;

import static org.junit.Assert.*;

import java.util.List;

import org.junit.*;
import org.reactivestreams.Subscriber;

import io.reactivex.*;
import io.reactivex.disposables.*;
import io.reactivex.exceptions.ProtocolViolationException;
import io.reactivex.internal.disposables.DisposableHelper;
import io.reactivex.internal.subscriptions.*;
import io.reactivex.observers.*;
import io.reactivex.plugins.RxJavaPlugins;
import io.reactivex.subscribers.*;

public class EndConsumerHelperTest {

    List<Throwable> errors;

    @Before
    public void before() {
        errors = TestHelper.trackPluginErrors();
    }

    @After
    public void after() {
        RxJavaPlugins.reset();
    }

    @Test
    public void utilityClass() {
        TestHelper.checkUtilityClass(EndConsumerHelper.class);
    }

    @Test
    public void checkDoubleDefaultSubscriber() {
        Subscriber<Integer> consumer = new DefaultSubscriber<Integer>() {
            @Override
            public void onNext(Integer t) {
            }
            @Override
            public void onError(Throwable t) {
            }
            @Override
            public void onComplete() {
            }
        };

        BooleanSubscription sub1 = new BooleanSubscription();

        consumer.onSubscribe(sub1);

        assertFalse(sub1.isCancelled());

        BooleanSubscription sub2 = new BooleanSubscription();

        consumer.onSubscribe(sub2);

        assertFalse(sub1.isCancelled());

        assertTrue(sub2.isCancelled());

        TestHelper.assertError(errors, 0, ProtocolViolationException.class);
        assertEquals(EndConsumerHelper.composeMessage(consumer.getClass().getName()), errors.get(0).getMessage());
        assertEquals(errors.toString(), 1, errors.size());
    }

    static final class EndDefaultSubscriber extends DefaultSubscriber<Integer> {
        @Override
        public void onNext(Integer t) {
        }
        @Override
        public void onError(Throwable t) {
        }
        @Override
        public void onComplete() {
        }
    }

    @Test
    public void checkDoubleDefaultSubscriberNonAnonymous() {
        Subscriber<Integer> consumer = new EndDefaultSubscriber();

        BooleanSubscription sub1 = new BooleanSubscription();

        consumer.onSubscribe(sub1);

        assertFalse(sub1.isCancelled());

        BooleanSubscription sub2 = new BooleanSubscription();

        consumer.onSubscribe(sub2);

        assertFalse(sub1.isCancelled());

        assertTrue(sub2.isCancelled());

        TestHelper.assertError(errors, 0, ProtocolViolationException.class);

        // with this consumer, the class name should be predictable
        assertEquals(EndConsumerHelper.composeMessage("io.reactivex.internal.util.EndConsumerHelperTest$EndDefaultSubscriber"), errors.get(0).getMessage());
        assertEquals(errors.toString(), 1, errors.size());
    }

    @Test
    public void checkDoubleDisposableSubscriber() {
        Subscriber<Integer> consumer = new DisposableSubscriber<Integer>() {
            @Override
            public void onNext(Integer t) {
            }
            @Override
            public void onError(Throwable t) {
            }
            @Override
            public void onComplete() {
            }
        };

        BooleanSubscription sub1 = new BooleanSubscription();

        consumer.onSubscribe(sub1);

        assertFalse(sub1.isCancelled());

        BooleanSubscription sub2 = new BooleanSubscription();

        consumer.onSubscribe(sub2);

        assertFalse(sub1.isCancelled());

        assertTrue(sub2.isCancelled());

        TestHelper.assertError(errors, 0, ProtocolViolationException.class);
        assertEquals(EndConsumerHelper.composeMessage(consumer.getClass().getName()), errors.get(0).getMessage());
        assertEquals(errors.toString(), 1, errors.size());
    }

    @Test
    public void checkDoubleResourceSubscriber() {
        Subscriber<Integer> consumer = new ResourceSubscriber<Integer>() {
            @Override
            public void onNext(Integer t) {
            }
            @Override
            public void onError(Throwable t) {
            }
            @Override
            public void onComplete() {
            }
        };

        BooleanSubscription sub1 = new BooleanSubscription();

        consumer.onSubscribe(sub1);

        assertFalse(sub1.isCancelled());

        BooleanSubscription sub2 = new BooleanSubscription();

        consumer.onSubscribe(sub2);

        assertFalse(sub1.isCancelled());

        assertTrue(sub2.isCancelled());

        TestHelper.assertError(errors, 0, ProtocolViolationException.class);
        assertEquals(EndConsumerHelper.composeMessage(consumer.getClass().getName()), errors.get(0).getMessage());
        assertEquals(errors.toString(), 1, errors.size());
    }

    @Test
    public void checkDoubleDefaultObserver() {
        Observer<Integer> consumer = new DefaultObserver<Integer>() {
            @Override
            public void onNext(Integer t) {
            }
            @Override
            public void onError(Throwable t) {
            }
            @Override
            public void onComplete() {
            }
        };

        Disposable sub1 = Disposables.empty();

        consumer.onSubscribe(sub1);

        assertFalse(sub1.isDisposed());

        Disposable sub2 = Disposables.empty();

        consumer.onSubscribe(sub2);

        assertFalse(sub1.isDisposed());

        assertTrue(sub2.isDisposed());

        TestHelper.assertError(errors, 0, ProtocolViolationException.class);
        assertEquals(EndConsumerHelper.composeMessage(consumer.getClass().getName()), errors.get(0).getMessage());
        assertEquals(errors.toString(), 1, errors.size());
    }

    @Test
    public void checkDoubleDisposableObserver() {
        Observer<Integer> consumer = new DisposableObserver<Integer>() {
            @Override
            public void onNext(Integer t) {
            }
            @Override
            public void onError(Throwable t) {
            }
            @Override
            public void onComplete() {
            }
        };

        Disposable sub1 = Disposables.empty();

        consumer.onSubscribe(sub1);

        assertFalse(sub1.isDisposed());

        Disposable sub2 = Disposables.empty();

        consumer.onSubscribe(sub2);

        assertFalse(sub1.isDisposed());

        assertTrue(sub2.isDisposed());

        TestHelper.assertError(errors, 0, ProtocolViolationException.class);
        assertEquals(EndConsumerHelper.composeMessage(consumer.getClass().getName()), errors.get(0).getMessage());
        assertEquals(errors.toString(), 1, errors.size());
    }

    @Test
    public void checkDoubleResourceObserver() {
        Observer<Integer> consumer = new ResourceObserver<Integer>() {
            @Override
            public void onNext(Integer t) {
            }
            @Override
            public void onError(Throwable t) {
            }
            @Override
            public void onComplete() {
            }
        };

        Disposable sub1 = Disposables.empty();

        consumer.onSubscribe(sub1);

        assertFalse(sub1.isDisposed());

        Disposable sub2 = Disposables.empty();

        consumer.onSubscribe(sub2);

        assertFalse(sub1.isDisposed());

        assertTrue(sub2.isDisposed());

        TestHelper.assertError(errors, 0, ProtocolViolationException.class);
        assertEquals(EndConsumerHelper.composeMessage(consumer.getClass().getName()), errors.get(0).getMessage());
        assertEquals(errors.toString(), 1, errors.size());
    }

    @Test
    public void checkDoubleDisposableSingleObserver() {
        SingleObserver<Integer> consumer = new DisposableSingleObserver<Integer>() {
            @Override
            public void onSuccess(Integer t) {
            }
            @Override
            public void onError(Throwable t) {
            }
        };

        Disposable sub1 = Disposables.empty();

        consumer.onSubscribe(sub1);

        assertFalse(sub1.isDisposed());

        Disposable sub2 = Disposables.empty();

        consumer.onSubscribe(sub2);

        assertFalse(sub1.isDisposed());

        assertTrue(sub2.isDisposed());

        TestHelper.assertError(errors, 0, ProtocolViolationException.class);
        assertEquals(EndConsumerHelper.composeMessage(consumer.getClass().getName()), errors.get(0).getMessage());
        assertEquals(errors.toString(), 1, errors.size());
    }

    @Test
    public void checkDoubleResourceSingleObserver() {
        SingleObserver<Integer> consumer = new ResourceSingleObserver<Integer>() {
            @Override
            public void onSuccess(Integer t) {
            }
            @Override
            public void onError(Throwable t) {
            }
        };

        Disposable sub1 = Disposables.empty();

        consumer.onSubscribe(sub1);

        assertFalse(sub1.isDisposed());

        Disposable sub2 = Disposables.empty();

        consumer.onSubscribe(sub2);

        assertFalse(sub1.isDisposed());

        assertTrue(sub2.isDisposed());

        TestHelper.assertError(errors, 0, ProtocolViolationException.class);
        assertEquals(EndConsumerHelper.composeMessage(consumer.getClass().getName()), errors.get(0).getMessage());
        assertEquals(errors.toString(), 1, errors.size());
    }

    @Test
    public void checkDoubleDisposableMaybeObserver() {
        MaybeObserver<Integer> consumer = new DisposableMaybeObserver<Integer>() {
            @Override
            public void onSuccess(Integer t) {
            }
            @Override
            public void onError(Throwable t) {
            }
            @Override
            public void onComplete() {
            }
        };

        Disposable sub1 = Disposables.empty();

        consumer.onSubscribe(sub1);

        assertFalse(sub1.isDisposed());

        Disposable sub2 = Disposables.empty();

        consumer.onSubscribe(sub2);

        assertFalse(sub1.isDisposed());

        assertTrue(sub2.isDisposed());

        TestHelper.assertError(errors, 0, ProtocolViolationException.class);
        assertEquals(EndConsumerHelper.composeMessage(consumer.getClass().getName()), errors.get(0).getMessage());
        assertEquals(errors.toString(), 1, errors.size());
    }

    @Test
    public void checkDoubleResourceMaybeObserver() {
        MaybeObserver<Integer> consumer = new ResourceMaybeObserver<Integer>() {
            @Override
            public void onSuccess(Integer t) {
            }
            @Override
            public void onError(Throwable t) {
            }
            @Override
            public void onComplete() {
            }
        };

        Disposable sub1 = Disposables.empty();

        consumer.onSubscribe(sub1);

        assertFalse(sub1.isDisposed());

        Disposable sub2 = Disposables.empty();

        consumer.onSubscribe(sub2);

        assertFalse(sub1.isDisposed());

        assertTrue(sub2.isDisposed());

        TestHelper.assertError(errors, 0, ProtocolViolationException.class);
        assertEquals(EndConsumerHelper.composeMessage(consumer.getClass().getName()), errors.get(0).getMessage());
        assertEquals(errors.toString(), 1, errors.size());
    }

    @Test
    public void checkDoubleDisposableCompletableObserver() {
        CompletableObserver consumer = new DisposableCompletableObserver() {
            @Override
            public void onError(Throwable t) {
            }
            @Override
            public void onComplete() {
            }
        };

        Disposable sub1 = Disposables.empty();

        consumer.onSubscribe(sub1);

        assertFalse(sub1.isDisposed());

        Disposable sub2 = Disposables.empty();

        consumer.onSubscribe(sub2);

        assertFalse(sub1.isDisposed());

        assertTrue(sub2.isDisposed());

        TestHelper.assertError(errors, 0, ProtocolViolationException.class);
        assertEquals(EndConsumerHelper.composeMessage(consumer.getClass().getName()), errors.get(0).getMessage());
        assertEquals(errors.toString(), 1, errors.size());
    }

    @Test
    public void checkDoubleResourceCompletableObserver() {
        CompletableObserver consumer = new ResourceCompletableObserver() {
            @Override
            public void onError(Throwable t) {
            }
            @Override
            public void onComplete() {
            }
        };

        Disposable sub1 = Disposables.empty();

        consumer.onSubscribe(sub1);

        assertFalse(sub1.isDisposed());

        Disposable sub2 = Disposables.empty();

        consumer.onSubscribe(sub2);

        assertFalse(sub1.isDisposed());

        assertTrue(sub2.isDisposed());

        TestHelper.assertError(errors, 0, ProtocolViolationException.class);
        assertEquals(EndConsumerHelper.composeMessage(consumer.getClass().getName()), errors.get(0).getMessage());
        assertEquals(errors.toString(), 1, errors.size());
    }

    @Test
    public void validateDisposable() {
        Disposable d1 = Disposables.empty();

        assertFalse(EndConsumerHelper.validate(DisposableHelper.DISPOSED, d1, getClass()));

        assertTrue(d1.isDisposed());

        assertTrue(errors.toString(), errors.isEmpty());
    }

    @Test
    public void validateSubscription() {
        BooleanSubscription d1 = new BooleanSubscription();

        assertFalse(EndConsumerHelper.validate(SubscriptionHelper.CANCELLED, d1, getClass()));

        assertTrue(d1.isCancelled());

        assertTrue(errors.toString(), errors.isEmpty());
    }
}
