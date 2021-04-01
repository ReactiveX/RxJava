/*
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

package io.reactivex.rxjava3.internal.operators.observable;

import java.util.concurrent.TimeUnit;

import org.junit.Test;

import io.reactivex.rxjava3.core.*;
import io.reactivex.rxjava3.internal.operators.observable.ObservableInterval.IntervalObserver;
import io.reactivex.rxjava3.observers.TestObserver;
import io.reactivex.rxjava3.schedulers.*;
import io.reactivex.rxjava3.testsupport.TestHelper;

public class ObservableIntervalTest extends RxJavaTest {

    @Test
    public void dispose() {
        TestHelper.checkDisposed(Observable.interval(1, TimeUnit.MILLISECONDS, new TestScheduler()));
    }

    @Test
    public void cancel() {
        Observable.interval(1, TimeUnit.MILLISECONDS, Schedulers.trampoline())
        .take(10)
        .test()
        .assertResult(0L, 1L, 2L, 3L, 4L, 5L, 6L, 7L, 8L, 9L);
    }

    @Test
    public void cancelledOnRun() {
        TestObserver<Long> to = new TestObserver<>();
        IntervalObserver is = new IntervalObserver(to);
        to.onSubscribe(is);

        is.dispose();

        is.run();

        to.assertEmpty();
    }
}
