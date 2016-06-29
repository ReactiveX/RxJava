/**
 * Copyright 2016 Netflix, Inc.
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

import static org.mockito.Matchers.any;
import static org.mockito.Mockito.*;

import org.junit.Test;
import org.mockito.Mockito;

import io.reactivex.*;
import io.reactivex.functions.Predicate;

public class ObservableFilterTest {

    @Test
    public void testFilter() {
        Observable<String> w = Observable.just("one", "two", "three");
        Observable<String> NbpObservable = w.filter(new Predicate<String>() {

            @Override
            public boolean test(String t1) {
                return t1.equals("two");
            }
        });

        Observer<String> NbpObserver = TestHelper.mockObserver();

        NbpObservable.subscribe(NbpObserver);
        
        verify(NbpObserver, Mockito.never()).onNext("one");
        verify(NbpObserver, times(1)).onNext("two");
        verify(NbpObserver, Mockito.never()).onNext("three");
        verify(NbpObserver, Mockito.never()).onError(any(Throwable.class));
        verify(NbpObserver, times(1)).onComplete();
    }

    // FIXME subscribers are not allowed to throw
//    @Test
//    public void testFatalError() {
//        try {
//            NbpObservable.just(1)
//            .filter(new Predicate<Integer>() {
//                @Override
//                public boolean test(Integer t) {
//                    return true;
//                }
//            })
//            .first()
//            .subscribe(new Consumer<Integer>() {
//                @Override
//                public void accept(Integer t) {
//                    throw new TestException();
//                }
//            });
//            Assert.fail("No exception was thrown");
//        } catch (OnErrorNotImplementedException ex) {
//            if (!(ex.getCause() instanceof TestException)) {
//                Assert.fail("Failed to report the original exception, instead: " + ex.getCause());
//            }
//        }
//    }
}