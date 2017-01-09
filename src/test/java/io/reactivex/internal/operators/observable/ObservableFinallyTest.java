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

import static org.mockito.Mockito.*;

import org.junit.*;

import io.reactivex.*;
import io.reactivex.functions.Action;
import io.reactivex.internal.util.ExceptionHelper;

public class ObservableFinallyTest {

    private Action aAction0;
    private Observer<String> observer;

    // mocking has to be unchecked, unfortunately
    @Before
    public void before() {
        aAction0 = mock(Action.class);
        observer = TestHelper.mockObserver();
    }

    private void checkActionCalled(Observable<String> input) {
        input.doAfterTerminate(aAction0).subscribe(observer);
        try {
            verify(aAction0, times(1)).run();
        } catch (Throwable e) {
            throw ExceptionHelper.wrapOrThrow(e);
        }
    }

    @Test
    public void testFinallyCalledOnComplete() {
        checkActionCalled(Observable.fromArray("1", "2", "3"));
    }

    @Test
    public void testFinallyCalledOnError() {
        checkActionCalled(Observable.<String> error(new RuntimeException("expected")));
    }
}
