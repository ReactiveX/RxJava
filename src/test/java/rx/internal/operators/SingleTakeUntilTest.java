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

import java.util.concurrent.CancellationException;

import static org.junit.Assert.*;
import org.junit.Test;

import rx.*;
import rx.observers.AssertableSubscriber;

public class SingleTakeUntilTest {

    @Test
    public void withSingleMessage() {
        AssertableSubscriber<Integer> ts = Single.just(1).takeUntil(Single.just(2))
        .test()
        .assertFailure(CancellationException.class);

        String message = ts.getOnErrorEvents().get(0).getMessage();

        assertTrue(message, message.startsWith("Single::takeUntil(Single)"));
    }

    @Test
    public void withCompletableMessage() {
        AssertableSubscriber<Integer> ts = Single.just(1).takeUntil(Completable.complete())
        .test()
        .assertFailure(CancellationException.class);

        String message = ts.getOnErrorEvents().get(0).getMessage();

        assertTrue(message, message.startsWith("Single::takeUntil(Completable)"));
    }

    @Test
    public void withObservableMessage() {
        AssertableSubscriber<Integer> ts = Single.just(1).takeUntil(Observable.just(1))
        .test()
        .assertFailure(CancellationException.class);

        String message = ts.getOnErrorEvents().get(0).getMessage();

        assertTrue(message, message.startsWith("Single::takeUntil(Observable)"));
    }
}
