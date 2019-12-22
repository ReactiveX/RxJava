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

package io.reactivex.rxjava3.internal.operators.observable;

import static org.junit.Assert.*;

import org.junit.Test;

import io.reactivex.rxjava3.core.RxJavaTest;
import io.reactivex.rxjava3.disposables.*;
import io.reactivex.rxjava3.exceptions.TestException;
import io.reactivex.rxjava3.observers.TestObserver;
import io.reactivex.rxjava3.testsupport.TestHelper;

public class ObservableResourceWrapperTest extends RxJavaTest {

    @Test
    public void disposed() {
        TestObserver<Object> to = new TestObserver<>();
        ObserverResourceWrapper<Object> orw = new ObserverResourceWrapper<>(to);

        Disposable d = Disposable.empty();

        orw.onSubscribe(d);

        assertFalse(orw.isDisposed());

        orw.dispose();

        assertTrue(orw.isDisposed());
    }

    @Test
    public void doubleOnSubscribe() {
        TestObserver<Object> to = new TestObserver<>();
        ObserverResourceWrapper<Object> orw = new ObserverResourceWrapper<>(to);

        TestHelper.doubleOnSubscribe(orw);
    }

    @Test
    public void onErrorDisposes() {
        TestObserver<Object> to = new TestObserver<>();
        ObserverResourceWrapper<Object> orw = new ObserverResourceWrapper<>(to);

        Disposable d = Disposable.empty();
        Disposable d1 = Disposable.empty();

        orw.setResource(d1);

        orw.onSubscribe(d);

        orw.onError(new TestException());

        assertTrue(d1.isDisposed());

        to.assertFailure(TestException.class);
    }
}
