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

import static org.mockito.Matchers.*;
import static org.mockito.Mockito.*;

import java.util.function.Function;

import org.junit.*;
import org.mockito.InOrder;

import io.reactivex.*;
import io.reactivex.NbpObservable.NbpSubscriber;

public class NbpOperatorDistinctTest {

    NbpSubscriber<String> w;

    // nulls lead to exceptions
    final Function<String, String> TO_UPPER_WITH_EXCEPTION = new Function<String, String>() {
        @Override
        public String apply(String s) {
            if (s.equals("x")) {
                return "XX";
            }
            return s.toUpperCase();
        }
    };

    @Before
    public void before() {
        w = TestHelper.mockNbpSubscriber();
    }

    @Test
    public void testDistinctOfNone() {
        NbpObservable<String> src = NbpObservable.empty();
        src.distinct().subscribe(w);

        verify(w, never()).onNext(anyString());
        verify(w, never()).onError(any(Throwable.class));
        verify(w, times(1)).onComplete();
    }

    @Test
    public void testDistinctOfNoneWithKeySelector() {
        NbpObservable<String> src = NbpObservable.empty();
        src.distinct(TO_UPPER_WITH_EXCEPTION).subscribe(w);

        verify(w, never()).onNext(anyString());
        verify(w, never()).onError(any(Throwable.class));
        verify(w, times(1)).onComplete();
    }

    @Test
    public void testDistinctOfNormalSource() {
        NbpObservable<String> src = NbpObservable.just("a", "b", "c", "c", "c", "b", "b", "a", "e");
        src.distinct().subscribe(w);

        InOrder inOrder = inOrder(w);
        inOrder.verify(w, times(1)).onNext("a");
        inOrder.verify(w, times(1)).onNext("b");
        inOrder.verify(w, times(1)).onNext("c");
        inOrder.verify(w, times(1)).onNext("e");
        inOrder.verify(w, times(1)).onComplete();
        inOrder.verify(w, never()).onNext(anyString());
        verify(w, never()).onError(any(Throwable.class));
    }

    @Test
    public void testDistinctOfNormalSourceWithKeySelector() {
        NbpObservable<String> src = NbpObservable.just("a", "B", "c", "C", "c", "B", "b", "a", "E");
        src.distinct(TO_UPPER_WITH_EXCEPTION).subscribe(w);

        InOrder inOrder = inOrder(w);
        inOrder.verify(w, times(1)).onNext("a");
        inOrder.verify(w, times(1)).onNext("B");
        inOrder.verify(w, times(1)).onNext("c");
        inOrder.verify(w, times(1)).onNext("E");
        inOrder.verify(w, times(1)).onComplete();
        inOrder.verify(w, never()).onNext(anyString());
        verify(w, never()).onError(any(Throwable.class));
    }

    @Test
    @Ignore("Null values no longer allowed")
    public void testDistinctOfSourceWithNulls() {
        NbpObservable<String> src = NbpObservable.just(null, "a", "a", null, null, "b", null);
        src.distinct().subscribe(w);

        InOrder inOrder = inOrder(w);
        inOrder.verify(w, times(1)).onNext(null);
        inOrder.verify(w, times(1)).onNext("a");
        inOrder.verify(w, times(1)).onNext("b");
        inOrder.verify(w, times(1)).onComplete();
        inOrder.verify(w, never()).onNext(anyString());
        verify(w, never()).onError(any(Throwable.class));
    }

    @Test
    @Ignore("Null values no longer allowed")
    public void testDistinctOfSourceWithExceptionsFromKeySelector() {
        NbpObservable<String> src = NbpObservable.just("a", "b", null, "c");
        src.distinct(TO_UPPER_WITH_EXCEPTION).subscribe(w);

        InOrder inOrder = inOrder(w);
        inOrder.verify(w, times(1)).onNext("a");
        inOrder.verify(w, times(1)).onNext("b");
        inOrder.verify(w, times(1)).onError(any(NullPointerException.class));
        inOrder.verify(w, never()).onNext(anyString());
        inOrder.verify(w, never()).onComplete();
    }
}