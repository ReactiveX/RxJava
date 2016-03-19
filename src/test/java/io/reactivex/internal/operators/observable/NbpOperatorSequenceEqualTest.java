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

import static org.mockito.Matchers.isA;
import static org.mockito.Mockito.*;

import org.junit.*;
import org.mockito.InOrder;

import io.reactivex.*;
import io.reactivex.exceptions.TestException;
import io.reactivex.flowable.TestHelper;
import io.reactivex.functions.BiPredicate;

public class NbpOperatorSequenceEqualTest {

    @Test
    public void test1() {
        Observable<Boolean> o = Observable.sequenceEqual(
                Observable.just("one", "two", "three"),
                Observable.just("one", "two", "three"));
        verifyResult(o, true);
    }

    @Test
    public void test2() {
        Observable<Boolean> o = Observable.sequenceEqual(
                Observable.just("one", "two", "three"),
                Observable.just("one", "two", "three", "four"));
        verifyResult(o, false);
    }

    @Test
    public void test3() {
        Observable<Boolean> o = Observable.sequenceEqual(
                Observable.just("one", "two", "three", "four"),
                Observable.just("one", "two", "three"));
        verifyResult(o, false);
    }

    @Test
    public void testWithError1() {
        Observable<Boolean> o = Observable.sequenceEqual(
                Observable.concat(Observable.just("one"),
                        Observable.<String> error(new TestException())),
                Observable.just("one", "two", "three"));
        verifyError(o);
    }

    @Test
    public void testWithError2() {
        Observable<Boolean> o = Observable.sequenceEqual(
                Observable.just("one", "two", "three"),
                Observable.concat(Observable.just("one"),
                        Observable.<String> error(new TestException())));
        verifyError(o);
    }

    @Test
    public void testWithError3() {
        Observable<Boolean> o = Observable.sequenceEqual(
                Observable.concat(Observable.just("one"),
                        Observable.<String> error(new TestException())),
                Observable.concat(Observable.just("one"),
                        Observable.<String> error(new TestException())));
        verifyError(o);
    }

    @Test
    public void testWithEmpty1() {
        Observable<Boolean> o = Observable.sequenceEqual(
                Observable.<String> empty(),
                Observable.just("one", "two", "three"));
        verifyResult(o, false);
    }

    @Test
    public void testWithEmpty2() {
        Observable<Boolean> o = Observable.sequenceEqual(
                Observable.just("one", "two", "three"),
                Observable.<String> empty());
        verifyResult(o, false);
    }

    @Test
    public void testWithEmpty3() {
        Observable<Boolean> o = Observable.sequenceEqual(
                Observable.<String> empty(), Observable.<String> empty());
        verifyResult(o, true);
    }

    @Test
    @Ignore("Null values not allowed")
    public void testWithNull1() {
        Observable<Boolean> o = Observable.sequenceEqual(
                Observable.just((String) null), Observable.just("one"));
        verifyResult(o, false);
    }

    @Test
    @Ignore("Null values not allowed")
    public void testWithNull2() {
        Observable<Boolean> o = Observable.sequenceEqual(
                Observable.just((String) null), Observable.just((String) null));
        verifyResult(o, true);
    }

    @Test
    public void testWithEqualityError() {
        Observable<Boolean> o = Observable.sequenceEqual(
                Observable.just("one"), Observable.just("one"),
                new BiPredicate<String, String>() {
                    @Override
                    public boolean test(String t1, String t2) {
                        throw new TestException();
                    }
                });
        verifyError(o);
    }

    private void verifyResult(Observable<Boolean> o, boolean result) {
        Observer<Boolean> NbpObserver = TestHelper.mockNbpSubscriber();
        
        o.subscribe(NbpObserver);

        InOrder inOrder = inOrder(NbpObserver);
        inOrder.verify(NbpObserver, times(1)).onNext(result);
        inOrder.verify(NbpObserver).onComplete();
        inOrder.verifyNoMoreInteractions();
    }

    private void verifyError(Observable<Boolean> NbpObservable) {
        Observer<Boolean> NbpObserver = TestHelper.mockNbpSubscriber();
        NbpObservable.subscribe(NbpObserver);

        InOrder inOrder = inOrder(NbpObserver);
        inOrder.verify(NbpObserver, times(1)).onError(isA(TestException.class));
        inOrder.verifyNoMoreInteractions();
    }
}