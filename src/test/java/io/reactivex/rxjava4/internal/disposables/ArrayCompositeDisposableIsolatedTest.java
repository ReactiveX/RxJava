/*
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

package io.reactivex.rxjava4.internal.disposables;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.Isolated;

import io.reactivex.rxjava4.core.RxJavaTest;
import io.reactivex.rxjava4.disposables.Disposable;
import io.reactivex.rxjava4.testsupport.TestHelper;

@Isolated
public class ArrayCompositeDisposableIsolatedTest extends RxJavaTest {

    @Test
    public void disposeRace() {
        for (int i = 0; i < TestHelper.RACE_DEFAULT_LOOPS; i++) {
            final ArrayCompositeDisposable acd = new ArrayCompositeDisposable(2);

            Runnable r = acd::dispose;

            TestHelper.race(r, r);
        }
    }

    @Test
    public void replaceRace() {
        for (int i = 0; i < TestHelper.RACE_DEFAULT_LOOPS; i++) {
            final ArrayCompositeDisposable acd = new ArrayCompositeDisposable(2);

            Runnable r = () -> acd.replaceResource(0, Disposable.empty());

            TestHelper.race(r, r);
        }
    }

    @Test
    public void setRace() {
        for (int i = 0; i < TestHelper.RACE_DEFAULT_LOOPS; i++) {
            final ArrayCompositeDisposable acd = new ArrayCompositeDisposable(2);

            Runnable r = () -> acd.setResource(0, Disposable.empty());

            TestHelper.race(r, r);
        }
    }
}
