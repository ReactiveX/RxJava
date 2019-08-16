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

package io.reactivex.rxjava3.internal.operators.single;

import org.junit.Test;

import io.reactivex.rxjava3.core.*;
import io.reactivex.rxjava3.disposables.Disposable;

public class SingleLiftTest extends RxJavaTest {

    @Test
    public void normal() {

        Single.just(1).lift(new SingleOperator<Integer, Integer>() {
            @Override
            public SingleObserver<Integer> apply(final SingleObserver<? super Integer> observer) throws Exception {
                return new SingleObserver<Integer>() {

                    @Override
                    public void onSubscribe(Disposable d) {
                        observer.onSubscribe(d);
                    }

                    @Override
                    public void onSuccess(Integer value) {
                        observer.onSuccess(value + 1);
                    }

                    @Override
                    public void onError(Throwable e) {
                        observer.onError(e);
                    }
                };
            }
        })
        .test()
        .assertResult(2);
    }
}
