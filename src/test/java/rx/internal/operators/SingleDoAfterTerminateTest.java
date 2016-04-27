/**
 * Copyright 2015 Netflix, Inc.
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
package rx.internal.operators;

import java.util.concurrent.atomic.AtomicInteger;

import org.junit.*;

import rx.Single;
import rx.functions.*;
import rx.observers.TestSubscriber;

public class SingleDoAfterTerminateTest {

    @Test
    public void chainedCallsOuter() {
        for (int i = 2; i <= 5; i++) {
            final AtomicInteger counter = new AtomicInteger();
            
            Single<String> source = Single.just("Test")
            .flatMap(new Func1<String, Single<String>>() {
                @Override
                public Single<String> call(String s) {
                    return Single.just("Test2")
                        .doAfterTerminate(new Action0() {
                            @Override
                            public void call() {
                                counter.getAndIncrement();
                            }
                        });
                }
            }
            );
            Single<String> result = source;

            for (int j = 1; j < i; j++) {
                result = result.doAfterTerminate(new Action0() {
                    @Override
                    public void call() {
                        counter.getAndIncrement();
                    }
                });
            }
            
            result
            .subscribe(new TestSubscriber<String>());
            
            Assert.assertEquals(i, counter.get());
        }
    }
    
    @Test
    public void chainedCallsInner() {
        for (int i = 2; i <= 5; i++) {
            final AtomicInteger counter = new AtomicInteger();
            
            final int fi = i;
            
            Single.just("Test")
            .flatMap(new Func1<String, Single<String>>() {
                @Override
                public Single<String> call(String s) {
                    Single<String> result = Single.just("Test2");
                    for (int j = 1; j < fi; j++) {
                        result = result.doAfterTerminate(new Action0() {
                            @Override
                            public void call() {
                                counter.getAndIncrement();
                            }
                        });
                    }
                    return result;
                }
            })
            .doAfterTerminate(new Action0() {
                @Override
                public void call() {
                    counter.getAndIncrement();
                }
            })
            .subscribe(new TestSubscriber<String>());
            
            Assert.assertEquals(i, counter.get());
        }
    }
}
