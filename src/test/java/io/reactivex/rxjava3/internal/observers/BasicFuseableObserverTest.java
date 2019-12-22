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

package io.reactivex.rxjava3.internal.observers;

import io.reactivex.rxjava3.disposables.Disposable;
import org.junit.Test;

import io.reactivex.rxjava3.annotations.Nullable;
import io.reactivex.rxjava3.core.RxJavaTest;
import io.reactivex.rxjava3.observers.TestObserver;
import io.reactivex.rxjava3.testsupport.TestObserverEx;

public class BasicFuseableObserverTest extends RxJavaTest {

    @Test(expected = UnsupportedOperationException.class)
    public void offer() {
        TestObserverEx<Integer> to = new TestObserverEx<>();
        BasicFuseableObserver<Integer, Integer> o = new BasicFuseableObserver<Integer, Integer>(to) {
            @Nullable
            @Override
            public Integer poll() throws Exception {
                return null;
            }

            @Override
            public int requestFusion(int mode) {
                return 0;
            }

            @Override
            public void onNext(Integer value) {
            }

            @Override
            protected boolean beforeDownstream() {
                return false;
            }
        };

        o.onSubscribe(Disposable.disposed());

        to.assertNotSubscribed();

        o.offer(1);
    }

    @Test(expected = UnsupportedOperationException.class)
    public void offer2() {
        BasicFuseableObserver<Integer, Integer> o = new BasicFuseableObserver<Integer, Integer>(new TestObserver<>()) {
            @Nullable
            @Override
            public Integer poll() throws Exception {
                return null;
            }

            @Override
            public int requestFusion(int mode) {
                return 0;
            }

            @Override
            public void onNext(Integer value) {
            }
        };

        o.offer(1, 2);
    }
}
