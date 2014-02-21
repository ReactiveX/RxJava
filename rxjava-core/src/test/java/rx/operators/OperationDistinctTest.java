/**
 * Copyright 2014 Netflix, Inc.
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
package rx.operators;

import static org.mockito.Matchers.*;
import static org.mockito.Mockito.*;
import static org.mockito.MockitoAnnotations.*;
import static rx.operators.OperationDistinct.*;

import java.util.Comparator;

import org.junit.Before;
import org.junit.Test;
import org.mockito.InOrder;
import org.mockito.Mock;

import rx.Observable;
import rx.Observer;
import rx.functions.Func1;

public class OperationDistinctTest {

    @Mock
    Observer<String> w;
    @Mock
    Observer<String> w2;

    // nulls lead to exceptions
    final Func1<String, String> TO_UPPER_WITH_EXCEPTION = new Func1<String, String>() {
        @Override
        public String call(String s) {
            if (s.equals("x")) {
                return "XX";
            }
            return s.toUpperCase();
        }
    };

    final Comparator<String> COMPARE_LENGTH = new Comparator<String>() {
        @Override
        public int compare(String s1, String s2) {
            return s1.length() - s2.length();
        }
    };

    @Before
    public void before() {
        initMocks(this);
    }

    @Test
    public void testDistinctOfNone() {
        Observable<String> src = Observable.empty();
        Observable.create(distinct(src)).subscribe(w);

        verify(w, never()).onNext(anyString());
        verify(w, never()).onError(any(Throwable.class));
        verify(w, times(1)).onCompleted();
    }

    @Test
    public void testDistinctOfNoneWithKeySelector() {
        Observable<String> src = Observable.empty();
        Observable.create(distinct(src, TO_UPPER_WITH_EXCEPTION)).subscribe(w);

        verify(w, never()).onNext(anyString());
        verify(w, never()).onError(any(Throwable.class));
        verify(w, times(1)).onCompleted();
    }

    @Test
    public void testDistinctOfNormalSource() {
        Observable<String> src = Observable.from("a", "b", "c", "c", "c", "b", "b", "a", "e");
        Observable.create(distinct(src)).subscribe(w);

        InOrder inOrder = inOrder(w);
        inOrder.verify(w, times(1)).onNext("a");
        inOrder.verify(w, times(1)).onNext("b");
        inOrder.verify(w, times(1)).onNext("c");
        inOrder.verify(w, times(1)).onNext("e");
        inOrder.verify(w, times(1)).onCompleted();
        inOrder.verify(w, never()).onNext(anyString());
        verify(w, never()).onError(any(Throwable.class));
    }

    @Test
    public void testDistinctOfNormalSourceWithKeySelector() {
        Observable<String> src = Observable.from("a", "B", "c", "C", "c", "B", "b", "a", "E");
        Observable.create(distinct(src, TO_UPPER_WITH_EXCEPTION)).subscribe(w);

        InOrder inOrder = inOrder(w);
        inOrder.verify(w, times(1)).onNext("a");
        inOrder.verify(w, times(1)).onNext("B");
        inOrder.verify(w, times(1)).onNext("c");
        inOrder.verify(w, times(1)).onNext("E");
        inOrder.verify(w, times(1)).onCompleted();
        inOrder.verify(w, never()).onNext(anyString());
        verify(w, never()).onError(any(Throwable.class));
    }

    @Test
    public void testDistinctOfNormalSourceWithComparator() {
        Observable<String> src = Observable.from("1", "12", "123", "aaa", "321", "12", "21", "1", "12345");
        Observable.create(distinct(src, COMPARE_LENGTH)).subscribe(w);

        InOrder inOrder = inOrder(w);
        inOrder.verify(w, times(1)).onNext("1");
        inOrder.verify(w, times(1)).onNext("12");
        inOrder.verify(w, times(1)).onNext("123");
        inOrder.verify(w, times(1)).onNext("12345");
        inOrder.verify(w, times(1)).onCompleted();
        inOrder.verify(w, never()).onNext(anyString());
        verify(w, never()).onError(any(Throwable.class));
    }

    @Test
    public void testDistinctOfNormalSourceWithKeySelectorAndComparator() {
        Observable<String> src = Observable.from("a", "x", "ab", "abc", "cba", "de", "x", "a", "abcd");
        Observable.create(distinct(src, TO_UPPER_WITH_EXCEPTION, COMPARE_LENGTH)).subscribe(w);

        InOrder inOrder = inOrder(w);
        inOrder.verify(w, times(1)).onNext("a");
        inOrder.verify(w, times(1)).onNext("x");
        inOrder.verify(w, times(1)).onNext("abc");
        inOrder.verify(w, times(1)).onNext("abcd");
        inOrder.verify(w, times(1)).onCompleted();
        inOrder.verify(w, never()).onNext(anyString());
        verify(w, never()).onError(any(Throwable.class));
    }

    @Test
    public void testDistinctOfNormalSourceWithKeySelectorAndComparatorAndTwoSubscriptions() {
        Observable<String> src = Observable.from("a", "x", "ab", "abc", "cba", "de", "x", "a", "abcd");
        Observable.create(distinct(src, TO_UPPER_WITH_EXCEPTION, COMPARE_LENGTH)).subscribe(w);

        InOrder inOrder = inOrder(w);
        inOrder.verify(w, times(1)).onNext("a");
        inOrder.verify(w, times(1)).onNext("x");
        Observable.create(distinct(src, TO_UPPER_WITH_EXCEPTION, COMPARE_LENGTH)).subscribe(w2);
        inOrder.verify(w, times(1)).onNext("abc");
        inOrder.verify(w, times(1)).onNext("abcd");
        inOrder.verify(w, times(1)).onCompleted();
        inOrder.verify(w, never()).onNext(anyString());
        verify(w, never()).onError(any(Throwable.class));

        InOrder inOrder2 = inOrder(w2);
        inOrder2.verify(w2, times(1)).onNext("a");
        inOrder2.verify(w2, times(1)).onNext("x");
        inOrder2.verify(w2, times(1)).onNext("abc");
        inOrder2.verify(w2, times(1)).onNext("abcd");
        inOrder2.verify(w2, times(1)).onCompleted();
        inOrder2.verify(w2, never()).onNext(anyString());
        verify(w2, never()).onError(any(Throwable.class));
    }

    @Test
    public void testDistinctOfSourceWithNulls() {
        Observable<String> src = Observable.from(null, "a", "a", null, null, "b", null);
        Observable.create(distinct(src)).subscribe(w);

        InOrder inOrder = inOrder(w);
        inOrder.verify(w, times(1)).onNext(null);
        inOrder.verify(w, times(1)).onNext("a");
        inOrder.verify(w, times(1)).onNext("b");
        inOrder.verify(w, times(1)).onCompleted();
        inOrder.verify(w, never()).onNext(anyString());
        verify(w, never()).onError(any(Throwable.class));
    }

    @Test
    public void testDistinctOfSourceWithExceptionsFromKeySelector() {
        Observable<String> src = Observable.from("a", "b", null, "c");
        Observable.create(distinct(src, TO_UPPER_WITH_EXCEPTION)).subscribe(w);

        InOrder inOrder = inOrder(w);
        inOrder.verify(w, times(1)).onNext("a");
        inOrder.verify(w, times(1)).onNext("b");
        inOrder.verify(w, times(1)).onError(any(NullPointerException.class));
        inOrder.verify(w, never()).onNext(anyString());
        inOrder.verify(w, never()).onCompleted();
    }
}
