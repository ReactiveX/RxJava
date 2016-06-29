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

package io.reactivex.internal.operators.flowable;

import static org.mockito.Matchers.isA;
import static org.mockito.Mockito.*;

import org.junit.*;
import org.mockito.InOrder;
import org.reactivestreams.Subscriber;

import io.reactivex.*;
import io.reactivex.exceptions.TestException;
import io.reactivex.functions.BiPredicate;

public class FlowableSequenceEqualTest {

    @Test
    public void test1() {
        Flowable<Boolean> observable = Flowable.sequenceEqual(
                Flowable.just("one", "two", "three"),
                Flowable.just("one", "two", "three"));
        verifyResult(observable, true);
    }

    @Test
    public void test2() {
        Flowable<Boolean> observable = Flowable.sequenceEqual(
                Flowable.just("one", "two", "three"),
                Flowable.just("one", "two", "three", "four"));
        verifyResult(observable, false);
    }

    @Test
    public void test3() {
        Flowable<Boolean> observable = Flowable.sequenceEqual(
                Flowable.just("one", "two", "three", "four"),
                Flowable.just("one", "two", "three"));
        verifyResult(observable, false);
    }

    @Test
    public void testWithError1() {
        Flowable<Boolean> observable = Flowable.sequenceEqual(
                Flowable.concat(Flowable.just("one"),
                        Flowable.<String> error(new TestException())),
                Flowable.just("one", "two", "three"));
        verifyError(observable);
    }

    @Test
    public void testWithError2() {
        Flowable<Boolean> observable = Flowable.sequenceEqual(
                Flowable.just("one", "two", "three"),
                Flowable.concat(Flowable.just("one"),
                        Flowable.<String> error(new TestException())));
        verifyError(observable);
    }

    @Test
    public void testWithError3() {
        Flowable<Boolean> observable = Flowable.sequenceEqual(
                Flowable.concat(Flowable.just("one"),
                        Flowable.<String> error(new TestException())),
                Flowable.concat(Flowable.just("one"),
                        Flowable.<String> error(new TestException())));
        verifyError(observable);
    }

    @Test
    public void testWithEmpty1() {
        Flowable<Boolean> observable = Flowable.sequenceEqual(
                Flowable.<String> empty(),
                Flowable.just("one", "two", "three"));
        verifyResult(observable, false);
    }

    @Test
    public void testWithEmpty2() {
        Flowable<Boolean> observable = Flowable.sequenceEqual(
                Flowable.just("one", "two", "three"),
                Flowable.<String> empty());
        verifyResult(observable, false);
    }

    @Test
    public void testWithEmpty3() {
        Flowable<Boolean> observable = Flowable.sequenceEqual(
                Flowable.<String> empty(), Flowable.<String> empty());
        verifyResult(observable, true);
    }

    @Test
    @Ignore("Null values not allowed")
    public void testWithNull1() {
        Flowable<Boolean> observable = Flowable.sequenceEqual(
                Flowable.just((String) null), Flowable.just("one"));
        verifyResult(observable, false);
    }

    @Test
    @Ignore("Null values not allowed")
    public void testWithNull2() {
        Flowable<Boolean> observable = Flowable.sequenceEqual(
                Flowable.just((String) null), Flowable.just((String) null));
        verifyResult(observable, true);
    }

    @Test
    public void testWithEqualityError() {
        Flowable<Boolean> observable = Flowable.sequenceEqual(
                Flowable.just("one"), Flowable.just("one"),
                new BiPredicate<String, String>() {
                    @Override
                    public boolean test(String t1, String t2) {
                        throw new TestException();
                    }
                });
        verifyError(observable);
    }

    private void verifyResult(Flowable<Boolean> observable, boolean result) {
        Subscriber<Boolean> observer = TestHelper.mockSubscriber();
        
        observable.subscribe(observer);

        InOrder inOrder = inOrder(observer);
        inOrder.verify(observer, times(1)).onNext(result);
        inOrder.verify(observer).onComplete();
        inOrder.verifyNoMoreInteractions();
    }

    private void verifyError(Flowable<Boolean> observable) {
        Subscriber<Boolean> observer = TestHelper.mockSubscriber();
        observable.subscribe(observer);

        InOrder inOrder = inOrder(observer);
        inOrder.verify(observer, times(1)).onError(isA(TestException.class));
        inOrder.verifyNoMoreInteractions();
    }
}