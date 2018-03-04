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

package io.reactivex.internal.observers;

import org.junit.Test;

import io.reactivex.TestHelper;
import io.reactivex.disposables.Disposables;
import io.reactivex.exceptions.TestException;
import io.reactivex.internal.disposables.ObserverFullArbiter;
import io.reactivex.observers.TestObserver;

public class FullArbiterObserverTest {

    @Test
    public void doubleOnSubscribe() {
        TestObserver<Integer> to = new TestObserver<Integer>();
        ObserverFullArbiter<Integer> fa = new ObserverFullArbiter<Integer>(to, null, 16);
        FullArbiterObserver<Integer> fo = new FullArbiterObserver<Integer>(fa);
        to.onSubscribe(fa);

        TestHelper.doubleOnSubscribe(fo);
    }

    @Test
    public void error() {
        TestObserver<Integer> to = new TestObserver<Integer>();
        ObserverFullArbiter<Integer> fa = new ObserverFullArbiter<Integer>(to, null, 16);
        FullArbiterObserver<Integer> fo = new FullArbiterObserver<Integer>(fa);
        to.onSubscribe(fa);

        fo.onSubscribe(Disposables.empty());
        fo.onError(new TestException());

        to.assertFailure(TestException.class);
    }
}
