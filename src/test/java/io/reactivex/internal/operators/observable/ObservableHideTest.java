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

import static org.junit.Assert.assertFalse;
import static org.mockito.Mockito.*;

import org.junit.Test;

import io.reactivex.*;
import io.reactivex.exceptions.TestException;
import io.reactivex.subjects.PublishSubject;

public class ObservableHideTest {
    @Test
    public void testHiding() {
        PublishSubject<Integer> src = PublishSubject.create();

        Observable<Integer> dst = src.hide();

        assertFalse(dst instanceof PublishSubject);

        Observer<Object> o = TestHelper.mockObserver();

        dst.subscribe(o);

        src.onNext(1);
        src.onComplete();

        verify(o).onNext(1);
        verify(o).onComplete();
        verify(o, never()).onError(any(Throwable.class));
    }
    @Test
    public void testHidingError() {
        PublishSubject<Integer> src = PublishSubject.create();

        Observable<Integer> dst = src.hide();

        assertFalse(dst instanceof PublishSubject);

        Observer<Object> o = TestHelper.mockObserver();

        dst.subscribe(o);

        src.onError(new TestException());

        verify(o, never()).onNext(any());
        verify(o, never()).onComplete();
        verify(o).onError(any(TestException.class));
    }
}
