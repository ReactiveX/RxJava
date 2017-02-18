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

package io.reactivex.parallel;

import static org.junit.Assert.*;

import java.io.IOException;
import java.util.List;

import org.junit.Test;
import org.reactivestreams.Subscription;

import io.reactivex.*;
import io.reactivex.exceptions.TestException;
import io.reactivex.functions.*;
import io.reactivex.internal.functions.Functions;
import io.reactivex.plugins.RxJavaPlugins;

public class ParallelPeekTest {

    @Test
    public void subscriberCount() {
        ParallelFlowableTest.checkSubscriberCount(Flowable.range(1, 5).parallel()
        .doOnNext(Functions.emptyConsumer()));
    }

    @Test
    public void onSubscribeCrash() {
        Flowable.range(1, 5)
        .parallel()
        .doOnSubscribe(new Consumer<Subscription>() {
            @Override
            public void accept(Subscription s) throws Exception {
                throw new TestException();
            }
        })
        .sequential()
        .test()
        .assertFailure(TestException.class);
    }

    @Test
    public void doubleError() {
        List<Throwable> errors = TestHelper.trackPluginErrors();
        try {
            new ParallelInvalid()
            .doOnNext(Functions.emptyConsumer())
            .sequential()
            .test()
            .assertFailure(TestException.class);

            assertFalse(errors.isEmpty());
            for (Throwable ex : errors) {
                assertTrue(ex.toString(), ex.getCause() instanceof TestException);
            }
        } finally {
            RxJavaPlugins.reset();
        }
    }

    @Test
    public void requestCrash() {
        List<Throwable> errors = TestHelper.trackPluginErrors();

        try {
            Flowable.range(1, 5)
            .parallel()
            .doOnRequest(new LongConsumer() {
                @Override
                public void accept(long n) throws Exception {
                    throw new TestException();
                }
            })
            .sequential()
            .test()
            .assertResult(1, 2, 3, 4, 5);

            assertFalse(errors.isEmpty());

            for (Throwable ex : errors) {
                assertTrue(ex.toString(), ex.getCause() instanceof TestException);
            }
        } finally {
            RxJavaPlugins.reset();
        }
    }

    @Test
    public void cancelCrash() {
        List<Throwable> errors = TestHelper.trackPluginErrors();

        try {
            Flowable.<Integer>never()
            .parallel()
            .doOnCancel(new Action() {
                @Override
                public void run() throws Exception {
                    throw new TestException();
                }
            })
            .sequential()
            .test()
            .cancel();

            assertFalse(errors.isEmpty());

            for (Throwable ex : errors) {
                assertTrue(ex.toString(), ex.getCause() instanceof TestException);
            }
        } finally {
            RxJavaPlugins.reset();
        }
    }

    @Test
    public void onCompleteCrash() {
        Flowable.just(1)
        .parallel()
        .doOnComplete(new Action() {
            @Override
            public void run() throws Exception {
                throw new TestException();
            }
        })
        .sequential()
        .test()
        .assertFailure(TestException.class, 1);
    }

    @Test
    public void onAfterTerminatedCrash() {
        List<Throwable> errors = TestHelper.trackPluginErrors();

        try {
            Flowable.just(1)
            .parallel()
            .doAfterTerminated(new Action() {
                @Override
                public void run() throws Exception {
                    throw new TestException();
                }
            })
            .sequential()
            .test()
            .assertResult(1);

            assertFalse(errors.isEmpty());

            for (Throwable ex : errors) {
                assertTrue(ex.toString(), ex.getCause() instanceof TestException);
            }
        } finally {
            RxJavaPlugins.reset();
        }
    }

    @Test
    public void onAfterTerminatedCrash2() {
        List<Throwable> errors = TestHelper.trackPluginErrors();

        try {
            Flowable.<Integer>error(new IOException())
            .parallel()
            .doAfterTerminated(new Action() {
                @Override
                public void run() throws Exception {
                    throw new TestException();
                }
            })
            .sequential()
            .test()
            .assertFailure(IOException.class);

            assertFalse(errors.isEmpty());

            for (Throwable ex : errors) {
                Throwable exc = ex.getCause();
                assertTrue(ex.toString(), exc instanceof TestException
                        || exc instanceof IOException);
            }
        } finally {
            RxJavaPlugins.reset();
        }
    }
}
