/**
 * Copyright (c) 2016-present, RxJava Contributors.
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
package io.reactivex.internal.operators.observable;

import java.util.List;
import org.junit.Test;

import io.reactivex.Observable;
import io.reactivex.TestHelper;
import io.reactivex.exceptions.TestException;
import io.reactivex.functions.Function;
import io.reactivex.observers.TestObserver;
import io.reactivex.plugins.RxJavaPlugins;

public class ObservableCreateScanTest {

    @Test
    public void take() {
        Observable.createScan(1, new Function<Integer, Integer>() {
            @Override
            public Integer apply(Integer t) throws Exception {
                return t + 1;
            }
        })
                .take(5)
                .test()
                .assertValues(1, 2, 3, 4, 5);
    }
    
    @Test
    public void disposedOnApply() {
        final TestObserver<Integer> to = new TestObserver<Integer>();
    
        Observable.createScan(1, new Function<Integer, Integer>() {
            @Override
            public Integer apply(Integer t) throws Exception {
                to.cancel();
                return t + 1;
            }
        })
                .subscribe(to);
    
        to.assertValue(1);
    }
    
    @Test
    public void disposedOnApplyThrows() {
        List<Throwable> errors = TestHelper.trackPluginErrors();
        try {
            final TestObserver<Integer> to = new TestObserver<Integer>();

            Observable.createScan(1, new Function<Integer, Integer>() {
                @Override
                public Integer apply(Integer t) throws Exception {
                    to.cancel();
                    throw new TestException();
                }
            })
                    .subscribe(to);

            to.assertValue(1);

            TestHelper.assertUndeliverable(errors, 0, TestException.class);
        } finally {
            RxJavaPlugins.reset();
        }
    }
}
