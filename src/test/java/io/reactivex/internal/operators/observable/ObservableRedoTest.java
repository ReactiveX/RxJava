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

import org.junit.Test;

import io.reactivex.*;
import io.reactivex.exceptions.TestException;
import io.reactivex.functions.Function;
import io.reactivex.observers.TestObserver;

public class ObservableRedoTest {

    @Test
    public void redoCancel() {
        final TestObserver<Integer> to = new TestObserver<Integer>();

        Observable.just(1)
        .repeatWhen(new Function<Observable<Object>, ObservableSource<Object>>() {
            @Override
            public ObservableSource<Object> apply(Observable<Object> o) throws Exception {
                return o.map(new Function<Object, Object>() {
                    int count;
                    @Override
                    public Object apply(Object v) throws Exception {
                        if (++count == 1) {
                            to.cancel();
                        }
                        return v;
                    }
                });
            }
        })
        .subscribe(to);
    }

    @Test
    public void managerThrows() {
        Observable.just(1)
        .retryWhen(new Function<Observable<Throwable>, ObservableSource<Object>>() {
            @Override
            public ObservableSource<Object> apply(Observable<Throwable> v) throws Exception {
                throw new TestException();
            }
        })
        .test()
        .assertFailure(TestException.class);
    }
}
