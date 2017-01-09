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

import static org.junit.Assert.*;

import org.junit.Test;

import io.reactivex.TestHelper;
import io.reactivex.disposables.*;
import io.reactivex.exceptions.TestException;
import io.reactivex.observers.TestObserver;

public class ObservableResourceWrapperTest {

    @Test
    public void disposed() {
        TestObserver<Object> to = new TestObserver<Object>();
        ObserverResourceWrapper<Object> orw = new ObserverResourceWrapper<Object>(to);

        Disposable d = Disposables.empty();

        orw.onSubscribe(d);

        assertFalse(orw.isDisposed());

        orw.dispose();

        assertTrue(orw.isDisposed());
    }

    @Test
    public void doubleOnSubscribe() {
        TestObserver<Object> to = new TestObserver<Object>();
        ObserverResourceWrapper<Object> orw = new ObserverResourceWrapper<Object>(to);

        TestHelper.doubleOnSubscribe(orw);
    }

    @Test
    public void onErrorDisposes() {
        TestObserver<Object> to = new TestObserver<Object>();
        ObserverResourceWrapper<Object> orw = new ObserverResourceWrapper<Object>(to);

        Disposable d = Disposables.empty();
        Disposable d1 = Disposables.empty();

        orw.setResource(d1);

        orw.onSubscribe(d);

        orw.onError(new TestException());

        assertTrue(d1.isDisposed());

        to.assertFailure(TestException.class);
    }
}
