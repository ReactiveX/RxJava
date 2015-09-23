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

import static org.mockito.Mockito.*;

import java.util.function.BiFunction;

import org.junit.*;
import org.mockito.InOrder;

import io.reactivex.*;
import io.reactivex.NbpObservable.NbpSubscriber;
import io.reactivex.subjects.nbp.NbpPublishSubject;

/**
 * Systematically tests that when zipping an infinite and a finite NbpObservable,
 * the resulting NbpObservable is finite.
 * 
 */
public class NbpOperatorZipCompletionTest {
    BiFunction<String, String, String> concat2Strings;

    NbpPublishSubject<String> s1;
    NbpPublishSubject<String> s2;
    NbpObservable<String> zipped;

    NbpSubscriber<String> NbpObserver;
    InOrder inOrder;

    @Before
    public void setUp() {
        concat2Strings = new BiFunction<String, String, String>() {
            @Override
            public String apply(String t1, String t2) {
                return t1 + "-" + t2;
            }
        };

        s1 = NbpPublishSubject.create();
        s2 = NbpPublishSubject.create();
        zipped = NbpObservable.zip(s1, s2, concat2Strings);

        NbpObserver = TestHelper.mockNbpSubscriber();
        inOrder = inOrder(NbpObserver);

        zipped.subscribe(NbpObserver);
    }

    @Test
    public void testFirstCompletesThenSecondInfinite() {
        s1.onNext("a");
        s1.onNext("b");
        s1.onComplete();
        s2.onNext("1");
        inOrder.verify(NbpObserver, times(1)).onNext("a-1");
        s2.onNext("2");
        inOrder.verify(NbpObserver, times(1)).onNext("b-2");
        inOrder.verify(NbpObserver, times(1)).onComplete();
        inOrder.verifyNoMoreInteractions();
    }

    @Test
    public void testSecondInfiniteThenFirstCompletes() {
        s2.onNext("1");
        s2.onNext("2");
        s1.onNext("a");
        inOrder.verify(NbpObserver, times(1)).onNext("a-1");
        s1.onNext("b");
        inOrder.verify(NbpObserver, times(1)).onNext("b-2");
        s1.onComplete();
        inOrder.verify(NbpObserver, times(1)).onComplete();
        inOrder.verifyNoMoreInteractions();
    }

    @Test
    public void testSecondCompletesThenFirstInfinite() {
        s2.onNext("1");
        s2.onNext("2");
        s2.onComplete();
        s1.onNext("a");
        inOrder.verify(NbpObserver, times(1)).onNext("a-1");
        s1.onNext("b");
        inOrder.verify(NbpObserver, times(1)).onNext("b-2");
        inOrder.verify(NbpObserver, times(1)).onComplete();
        inOrder.verifyNoMoreInteractions();
    }

    @Test
    public void testFirstInfiniteThenSecondCompletes() {
        s1.onNext("a");
        s1.onNext("b");
        s2.onNext("1");
        inOrder.verify(NbpObserver, times(1)).onNext("a-1");
        s2.onNext("2");
        inOrder.verify(NbpObserver, times(1)).onNext("b-2");
        s2.onComplete();
        inOrder.verify(NbpObserver, times(1)).onComplete();
        inOrder.verifyNoMoreInteractions();
    }

}