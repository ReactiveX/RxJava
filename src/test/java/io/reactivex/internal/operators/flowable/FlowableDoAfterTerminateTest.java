/**
 * Copyright (c) 2016-present, RxJava Contributors.
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
package io.reactivex.internal.operators.flowable;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

import org.junit.*;
import org.mockito.Mockito;
import org.reactivestreams.Subscriber;

import io.reactivex.*;
import io.reactivex.functions.Action;
import io.reactivex.internal.util.ExceptionHelper;
import io.reactivex.subscribers.TestSubscriber;

public class FlowableDoAfterTerminateTest {

    private Action aAction0;
    private Subscriber<String> observer;

    @Before
    public void before() {
        aAction0 = Mockito.mock(Action.class);
        observer = TestHelper.mockSubscriber();
    }

    private void checkActionCalled(Flowable<String> input) {
        input.doAfterTerminate(aAction0).subscribe(observer);
        try {
            verify(aAction0, times(1)).run();
        } catch (Throwable ex) {
            throw ExceptionHelper.wrapOrThrow(ex);
        }
    }

    @Test
    public void testDoAfterTerminateCalledOnComplete() {
        checkActionCalled(Flowable.fromArray("1", "2", "3"));
    }

    @Test
    public void testDoAfterTerminateCalledOnError() {
        checkActionCalled(Flowable.<String> error(new RuntimeException("expected")));
    }

    @Test
    public void nullActionShouldBeCheckedInConstructor() {
        try {
            Flowable.empty().doAfterTerminate(null);
            fail("Should have thrown NullPointerException");
        } catch (NullPointerException expected) {
            assertEquals("onAfterTerminate is null", expected.getMessage());
        }
    }

    @Test
    public void nullFinallyActionShouldBeCheckedASAP() {
        try {
            Flowable
                    .just("value")
                    .doAfterTerminate(null);

            fail();
        } catch (NullPointerException expected) {

        }
    }

    @Test
    public void ifFinallyActionThrowsExceptionShouldNotBeSwallowedAndActionShouldBeCalledOnce() throws Exception {
        Action finallyAction = Mockito.mock(Action.class);
        doThrow(new IllegalStateException()).when(finallyAction).run();

        TestSubscriber<String> testSubscriber = new TestSubscriber<String>();

        Flowable
                .just("value")
                .doAfterTerminate(finallyAction)
                .subscribe(testSubscriber);

        testSubscriber.assertValue("value");

        verify(finallyAction).run();
        // Actual result:
        // Not only IllegalStateException was swallowed
        // But finallyAction was called twice!
    }
}
