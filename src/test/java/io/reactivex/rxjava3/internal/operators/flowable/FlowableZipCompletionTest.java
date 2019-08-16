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

package io.reactivex.rxjava3.internal.operators.flowable;

import static org.mockito.Mockito.*;

import org.junit.*;
import org.mockito.InOrder;
import org.reactivestreams.Subscriber;

import io.reactivex.rxjava3.core.*;
import io.reactivex.rxjava3.functions.BiFunction;
import io.reactivex.rxjava3.processors.PublishProcessor;
import io.reactivex.rxjava3.testsupport.TestHelper;

/**
 * Systematically tests that when zipping an infinite and a finite Observable,
 * the resulting Observable is finite.
 *
 */
public class FlowableZipCompletionTest extends RxJavaTest {
    BiFunction<String, String, String> concat2Strings;

    PublishProcessor<String> s1;
    PublishProcessor<String> s2;
    Flowable<String> zipped;

    Subscriber<String> subscriber;
    InOrder inOrder;

    @Before
    public void setUp() {
        concat2Strings = new BiFunction<String, String, String>() {
            @Override
            public String apply(String t1, String t2) {
                return t1 + "-" + t2;
            }
        };

        s1 = PublishProcessor.create();
        s2 = PublishProcessor.create();
        zipped = Flowable.zip(s1, s2, concat2Strings);

        subscriber = TestHelper.mockSubscriber();
        inOrder = inOrder(subscriber);

        zipped.subscribe(subscriber);
    }

    @Test
    public void firstCompletesThenSecondInfinite() {
        s1.onNext("a");
        s1.onNext("b");
        s1.onComplete();
        s2.onNext("1");
        inOrder.verify(subscriber, times(1)).onNext("a-1");
        s2.onNext("2");
        inOrder.verify(subscriber, times(1)).onNext("b-2");
        inOrder.verify(subscriber, times(1)).onComplete();
        inOrder.verifyNoMoreInteractions();
    }

    @Test
    public void secondInfiniteThenFirstCompletes() {
        s2.onNext("1");
        s2.onNext("2");
        s1.onNext("a");
        inOrder.verify(subscriber, times(1)).onNext("a-1");
        s1.onNext("b");
        inOrder.verify(subscriber, times(1)).onNext("b-2");
        s1.onComplete();
        inOrder.verify(subscriber, times(1)).onComplete();
        inOrder.verifyNoMoreInteractions();
    }

    @Test
    public void secondCompletesThenFirstInfinite() {
        s2.onNext("1");
        s2.onNext("2");
        s2.onComplete();
        s1.onNext("a");
        inOrder.verify(subscriber, times(1)).onNext("a-1");
        s1.onNext("b");
        inOrder.verify(subscriber, times(1)).onNext("b-2");
        inOrder.verify(subscriber, times(1)).onComplete();
        inOrder.verifyNoMoreInteractions();
    }

    @Test
    public void firstInfiniteThenSecondCompletes() {
        s1.onNext("a");
        s1.onNext("b");
        s2.onNext("1");
        inOrder.verify(subscriber, times(1)).onNext("a-1");
        s2.onNext("2");
        inOrder.verify(subscriber, times(1)).onNext("b-2");
        s2.onComplete();
        inOrder.verify(subscriber, times(1)).onComplete();
        inOrder.verifyNoMoreInteractions();
    }

}
