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

package io.reactivex.internal.operators.observable;

import static org.junit.Assert.assertEquals;

import java.util.List;
import java.util.concurrent.Callable;

import org.junit.Test;

import io.reactivex.*;
import io.reactivex.exceptions.TestException;
import io.reactivex.observers.TestObserver;
import io.reactivex.plugins.RxJavaPlugins;

public class ObservableFromCompletableTest {

    @Test
    public void disposedOnArrival() {
        final int[] count = { 0 };
        Observable.fromCallable(new Callable<Object>() {
            @Override
            public Object call() throws Exception {
                count[0]++;
                return 1;
            }
        })
        .test(true)
        .assertEmpty();

        assertEquals(0, count[0]);
    }

    @Test
    public void disposedOnCall() {
        final TestObserver<Integer> to = new TestObserver<Integer>();

        Observable.fromCallable(new Callable<Integer>() {
            @Override
            public Integer call() throws Exception {
                to.cancel();
                return 1;
            }
        })
        .subscribe(to);

        to.assertEmpty();
    }

    @Test
    public void disposedOnCallThrows() {
        List<Throwable> errors = TestHelper.trackPluginErrors();
        try {
            final TestObserver<Integer> to = new TestObserver<Integer>();

            Observable.fromCallable(new Callable<Integer>() {
                @Override
                public Integer call() throws Exception {
                    to.cancel();
                    throw new TestException();
                }
            })
            .subscribe(to);

            to.assertEmpty();

            TestHelper.assertUndeliverable(errors, 0, TestException.class);
        } finally {
            RxJavaPlugins.reset();
        }
    }

    @Test
    public void take() {
        Observable.fromCallable(new Callable<Object>() {
            @Override
            public Object call() throws Exception {
                return 1;
            }
        })
        .take(1)
        .test()
        .assertResult(1);
    }
}
