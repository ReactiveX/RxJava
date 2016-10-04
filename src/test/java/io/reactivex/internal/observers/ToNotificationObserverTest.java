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

package io.reactivex.internal.observers;

import static org.junit.Assert.*;

import java.util.*;

import org.junit.Test;

import io.reactivex.*;
import io.reactivex.disposables.*;
import io.reactivex.exceptions.*;
import io.reactivex.functions.Consumer;
import io.reactivex.internal.functions.Functions;
import io.reactivex.plugins.RxJavaPlugins;

public class ToNotificationObserverTest {

    @Test
    public void doubleOnSubscribe() {
        ToNotificationObserver<Integer> o = new ToNotificationObserver<Integer>(Functions.emptyConsumer());

        Disposable d1 = Disposables.empty();

        o.onSubscribe(d1);

        Disposable d2 = Disposables.empty();

        o.onSubscribe(d2);

        assertFalse(d1.isDisposed());

        assertTrue(d2.isDisposed());
    }

    @Test
    public void nullOnNext() {
        final List<Notification<Object>> list = new ArrayList<Notification<Object>>();

        ToNotificationObserver<Integer> o = new ToNotificationObserver<Integer>(new Consumer<Notification<Object>>() {
            @Override
            public void accept(Notification<Object> e) throws Exception {
                list.add(e);
            }
        });

        Disposable d1 = Disposables.empty();

        o.onSubscribe(d1);

        o.onNext(null);

        assertTrue(d1.isDisposed());

        assertTrue(list.toString(), list.get(0).getError() instanceof NullPointerException);
    }

    @Test
    public void onNextCrash() {
        final List<Notification<Object>> list = new ArrayList<Notification<Object>>();

        ToNotificationObserver<Integer> o = new ToNotificationObserver<Integer>(new Consumer<Notification<Object>>() {
            @Override
            public void accept(Notification<Object> e) throws Exception {
                if (e.isOnNext()) {
                    throw new TestException();
                } else {
                    list.add(e);
                }
            }
        });

        Disposable d1 = Disposables.empty();

        o.onSubscribe(d1);

        o.onNext(1);

        assertTrue(d1.isDisposed());

        assertTrue(list.toString(), list.get(0).getError() instanceof TestException);
    }

    @Test
    public void onErrorCrash() {
        final List<Throwable> list = TestHelper.trackPluginErrors();

        try {
            ToNotificationObserver<Integer> o = new ToNotificationObserver<Integer>(new Consumer<Notification<Object>>() {
                @Override
                public void accept(Notification<Object> e) throws Exception {
                    throw new TestException("Inner");
                }
            });

            Disposable d1 = Disposables.empty();

            o.onSubscribe(d1);

            o.onError(new TestException("Outer"));

            TestHelper.assertError(list, 0, CompositeException.class);

            List<Throwable> ce = TestHelper.compositeList(list.get(0));

            TestHelper.assertError(ce, 0, TestException.class, "Outer");
            TestHelper.assertError(ce, 1, TestException.class, "Inner");
        } finally {
            RxJavaPlugins.reset();
        }
    }

    @Test
    public void onCompleteCrash() {
        final List<Throwable> list = TestHelper.trackPluginErrors();

        try {
            ToNotificationObserver<Integer> o = new ToNotificationObserver<Integer>(new Consumer<Notification<Object>>() {
                @Override
                public void accept(Notification<Object> e) throws Exception {
                    throw new TestException("Inner");
                }
            });

            Disposable d1 = Disposables.empty();

            o.onSubscribe(d1);

            o.onComplete();

            TestHelper.assertError(list, 0, TestException.class, "Inner");
        } finally {
            RxJavaPlugins.reset();
        }
    }
}
