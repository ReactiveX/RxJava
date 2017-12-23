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

package io.reactivex.internal.operators.single;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import org.junit.Test;

import io.reactivex.*;
import io.reactivex.exceptions.TestException;
import io.reactivex.functions.Action;
import io.reactivex.functions.Consumer;
import io.reactivex.functions.Function;
import io.reactivex.observers.TestObserver;
import io.reactivex.schedulers.Schedulers;

public class SingleObserveOnTest {

    @Test
    public void dispose() {
        TestHelper.checkDisposed(Single.just(1).observeOn(Schedulers.single()));
    }

    @Test
    public void doubleOnSubscribe() {
        TestHelper.checkDoubleOnSubscribeSingle(new Function<Single<Object>, SingleSource<Object>>() {
            @Override
            public SingleSource<Object> apply(Single<Object> s) throws Exception {
                return s.observeOn(Schedulers.single());
            }
        });
    }

    @Test
    public void error() {
        Single.error(new TestException())
        .observeOn(Schedulers.single())
        .test()
        .awaitDone(5, TimeUnit.SECONDS)
        .assertFailure(TestException.class);
    }

    @Test
    public void race() {
        final AtomicBoolean disposed = new AtomicBoolean(false);

        final TestObserver<Long> test =
            Single.timer(1000, TimeUnit.MILLISECONDS, Schedulers.computation())
                .observeOn(Schedulers.single())
                .doOnSuccess(new Consumer<Long>() {
                    @Override
                    public void accept(Long l) throws Exception {
                        if (disposed.get()) {
                            throw new IllegalStateException("Already disposed!");
                        }
                    }
                })
                .test();

        Completable.timer(1000, TimeUnit.MILLISECONDS, Schedulers.single())
            .subscribe(new Action() {
                @Override
                public void run() throws Exception {
                    test.dispose();
                    disposed.set(true);
                }
            });

        test.awaitDone(3, TimeUnit.SECONDS)
            .assertNoErrors();
    }
}
