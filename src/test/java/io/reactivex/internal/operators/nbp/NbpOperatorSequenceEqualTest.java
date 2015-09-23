/**
 * Copyright 2015 Netflix, Inc.
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

package io.reactivex.internal.operators.nbp;

import static org.mockito.Matchers.isA;
import static org.mockito.Mockito.*;

import java.util.function.BiPredicate;

import org.junit.*;
import org.mockito.InOrder;

import io.reactivex.*;
import io.reactivex.NbpObservable.NbpSubscriber;
import io.reactivex.exceptions.TestException;

public class NbpOperatorSequenceEqualTest {

    @Test
    public void test1() {
        NbpObservable<Boolean> o = NbpObservable.sequenceEqual(
                NbpObservable.just("one", "two", "three"),
                NbpObservable.just("one", "two", "three"));
        verifyResult(o, true);
    }

    @Test
    public void test2() {
        NbpObservable<Boolean> o = NbpObservable.sequenceEqual(
                NbpObservable.just("one", "two", "three"),
                NbpObservable.just("one", "two", "three", "four"));
        verifyResult(o, false);
    }

    @Test
    public void test3() {
        NbpObservable<Boolean> o = NbpObservable.sequenceEqual(
                NbpObservable.just("one", "two", "three", "four"),
                NbpObservable.just("one", "two", "three"));
        verifyResult(o, false);
    }

    @Test
    public void testWithError1() {
        NbpObservable<Boolean> o = NbpObservable.sequenceEqual(
                NbpObservable.concat(NbpObservable.just("one"),
                        NbpObservable.<String> error(new TestException())),
                NbpObservable.just("one", "two", "three"));
        verifyError(o);
    }

    @Test
    public void testWithError2() {
        NbpObservable<Boolean> o = NbpObservable.sequenceEqual(
                NbpObservable.just("one", "two", "three"),
                NbpObservable.concat(NbpObservable.just("one"),
                        NbpObservable.<String> error(new TestException())));
        verifyError(o);
    }

    @Test
    public void testWithError3() {
        NbpObservable<Boolean> o = NbpObservable.sequenceEqual(
                NbpObservable.concat(NbpObservable.just("one"),
                        NbpObservable.<String> error(new TestException())),
                NbpObservable.concat(NbpObservable.just("one"),
                        NbpObservable.<String> error(new TestException())));
        verifyError(o);
    }

    @Test
    public void testWithEmpty1() {
        NbpObservable<Boolean> o = NbpObservable.sequenceEqual(
                NbpObservable.<String> empty(),
                NbpObservable.just("one", "two", "three"));
        verifyResult(o, false);
    }

    @Test
    public void testWithEmpty2() {
        NbpObservable<Boolean> o = NbpObservable.sequenceEqual(
                NbpObservable.just("one", "two", "three"),
                NbpObservable.<String> empty());
        verifyResult(o, false);
    }

    @Test
    public void testWithEmpty3() {
        NbpObservable<Boolean> o = NbpObservable.sequenceEqual(
                NbpObservable.<String> empty(), NbpObservable.<String> empty());
        verifyResult(o, true);
    }

    @Test
    @Ignore("Null values not allowed")
    public void testWithNull1() {
        NbpObservable<Boolean> o = NbpObservable.sequenceEqual(
                NbpObservable.just((String) null), NbpObservable.just("one"));
        verifyResult(o, false);
    }

    @Test
    @Ignore("Null values not allowed")
    public void testWithNull2() {
        NbpObservable<Boolean> o = NbpObservable.sequenceEqual(
                NbpObservable.just((String) null), NbpObservable.just((String) null));
        verifyResult(o, true);
    }

    @Test
    public void testWithEqualityError() {
        NbpObservable<Boolean> o = NbpObservable.sequenceEqual(
                NbpObservable.just("one"), NbpObservable.just("one"),
                new BiPredicate<String, String>() {
                    @Override
                    public boolean test(String t1, String t2) {
                        throw new TestException();
                    }
                });
        verifyError(o);
    }

    private void verifyResult(NbpObservable<Boolean> o, boolean result) {
        NbpSubscriber<Boolean> NbpObserver = TestHelper.mockNbpSubscriber();
        
        o.subscribe(NbpObserver);

        InOrder inOrder = inOrder(NbpObserver);
        inOrder.verify(NbpObserver, times(1)).onNext(result);
        inOrder.verify(NbpObserver).onComplete();
        inOrder.verifyNoMoreInteractions();
    }

    private void verifyError(NbpObservable<Boolean> NbpObservable) {
        NbpSubscriber<Boolean> NbpObserver = TestHelper.mockNbpSubscriber();
        NbpObservable.subscribe(NbpObserver);

        InOrder inOrder = inOrder(NbpObserver);
        inOrder.verify(NbpObserver, times(1)).onError(isA(TestException.class));
        inOrder.verifyNoMoreInteractions();
    }
}