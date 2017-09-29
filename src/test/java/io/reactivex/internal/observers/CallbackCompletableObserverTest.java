/**
 * Copyright (c) 2016-present, RxJava Contributors.
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in
 * compliance with the License. You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software distributed under the License is
 * distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See
 * the License for the specific language governing permissions and limitations under the License.
 */

package io.reactivex.internal.observers;

import io.reactivex.*;
import io.reactivex.disposables.*;
import io.reactivex.exceptions.*;
import io.reactivex.functions.*;
import io.reactivex.internal.functions.Functions;
import io.reactivex.plugins.RxJavaPlugins;
import org.junit.Test;
import java.util.List;
import static org.junit.Assert.*;

public final class CallbackCompletableObserverTest {

    @Test
    public void emptyActionShouldReportNoCustomOnError() {
        CallbackCompletableObserver o = new CallbackCompletableObserver(Functions.EMPTY_ACTION);

        assertFalse(o.hasCustomOnError());
    }

    @Test
    public void customOnErrorShouldReportCustomOnError() {
        CallbackCompletableObserver o = new CallbackCompletableObserver(Functions.<Throwable>emptyConsumer(),
                Functions.EMPTY_ACTION);

        assertTrue(o.hasCustomOnError());
    }

    @Test
    public void onCompleteThrows() {

        List<Throwable> errors = TestHelper.trackPluginErrors();

        try {
            Completable source = new Completable() {
                @Override
                protected void subscribeActual(CompletableObserver s) {
                    Disposable s1 = Disposables.empty();
                    s.onSubscribe(s1);

                    assertFalse(s1.isDisposed());

                    s.onComplete();
                }
            };

            CallbackCompletableObserver o = new CallbackCompletableObserver(new Action() {
                @Override
                public void run() throws Exception {
                    throw new TestException();
                }
            });

            source.subscribe(o);

            assertTrue(errors.get(0) instanceof UndeliverableException);

            assertTrue(o.isDisposed());
        } finally {
            RxJavaPlugins.reset();
        }
    }

    @Test
    public void onErrorThrows() {

        List<Throwable> errors = TestHelper.trackPluginErrors();

        try {
            CallbackCompletableObserver o = new CallbackCompletableObserver(new Consumer<Throwable>() {
                @Override
                public void accept(Throwable e) throws Exception {
                    throw new TestException();
                }
            }, new Action() {
                @Override
                public void run() throws Exception {
                }
            });

            Completable source = new Completable() {
                @Override
                protected void subscribeActual(CompletableObserver s) {
                    Disposable s1 = Disposables.empty();
                    s.onSubscribe(s1);

                    assertFalse(s1.isDisposed());

                    s.onError(new TestException());
                }
            };

            source.subscribe(o);

            assertTrue(errors.get(0) instanceof UndeliverableException);

            assertTrue(o.isDisposed());
        } finally {
            RxJavaPlugins.reset();
        }
    }
}
