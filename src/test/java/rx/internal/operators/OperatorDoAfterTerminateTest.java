/**
 * Copyright 2014 Netflix, Inc.
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
import static org.mockito.Mockito.*;

import org.junit.*;

import rx.*;
import rx.functions.Action0;
import rx.observers.TestSubscriber;

public class OperatorDoAfterTerminateTest {

    private Action0 aAction0;
    private Observer<String> observer;

    @SuppressWarnings("unchecked")
    // mocking has to be unchecked, unfortunately
    @Before
    public void before() {
        aAction0 = mock(Action0.class);
        observer = mock(Observer.class);
    }

    private void checkActionCalled(Observable<String> input) {
        input.doAfterTerminate(aAction0).subscribe(observer);
        verify(aAction0, times(1)).call();
    }

    @Test
    public void testDoAfterTerminateCalledOnComplete() {
        checkActionCalled(Observable.from(new String[] { "1", "2", "3" }));
    }

    @Test
    public void testDoAfterTerminateCalledOnError() {
        checkActionCalled(Observable.<String> error(new RuntimeException("expected")));
    }

    @Test
    public void nullActionShouldBeCheckedInConstructor() {
        try {
            new OperatorDoAfterTerminate<Object>(null);
            fail();
        } catch (NullPointerException expected) {
            assertEquals("Action can not be null", expected.getMessage());
        }
    }
    
    @Test
    public void nullFinallyActionShouldBeCheckedASAP() {
        try {
            Observable
                    .just("value")
                    .doAfterTerminate(null);

            fail();
        } catch (NullPointerException expected) {

        }
    }

    @Test
    public void ifFinallyActionThrowsExceptionShouldNotBeSwallowedAndActionShouldBeCalledOnce() {
        Action0 finallyAction = mock(Action0.class);
        doThrow(new IllegalStateException()).when(finallyAction).call();

        TestSubscriber<String> testSubscriber = new TestSubscriber<String>();

        Observable
                .just("value")
                .doAfterTerminate(finallyAction)
                .subscribe(testSubscriber);

        testSubscriber.assertValue("value");

        verify(finallyAction).call();
        // Actual result:
        // Not only IllegalStateException was swallowed
        // But finallyAction was called twice!
    }
}
