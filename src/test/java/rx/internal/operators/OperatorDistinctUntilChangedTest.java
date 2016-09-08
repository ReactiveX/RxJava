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
package rx.internal.operators;

import static org.junit.Assert.assertFalse;
import static org.mockito.Matchers.*;
import static org.mockito.Mockito.*;
import static org.mockito.MockitoAnnotations.initMocks;

import java.util.concurrent.atomic.AtomicBoolean;

import org.junit.*;
import org.mockito.*;

import rx.*;
import rx.exceptions.TestException;
import rx.functions.*;
import rx.observers.TestSubscriber;

public class OperatorDistinctUntilChangedTest {

    @Mock
    private Observer<String> w;
    @Mock
    private Observer<String> w2;

    // nulls lead to exceptions
    private final static Func1<String, String> TO_UPPER_WITH_EXCEPTION = new Func1<String, String>() {
        @Override
        public String call(String s) {
            if (s.equals("x")) {
                return "xx";
            }
            return s.toUpperCase();
        }
    };

    private final static Func1<String, String> THROWS_NON_FATAL = new Func1<String, String>() {
        @Override
        public String call(String s) {
            throw new RuntimeException();
        }
    };

    @Before
    public void before() {
        initMocks(this);
    }

    @Test
    public void testDistinctUntilChangedOfNone() {
        Observable<String> src = Observable.empty();
        src.distinctUntilChanged().subscribe(w);

        verify(w, never()).onNext(anyString());
        verify(w, never()).onError(any(Throwable.class));
        verify(w, times(1)).onCompleted();
    }

    @Test
    public void testDistinctUntilChangedOfNoneWithKeySelector() {
        Observable<String> src = Observable.empty();
        src.distinctUntilChanged(TO_UPPER_WITH_EXCEPTION).subscribe(w);

        verify(w, never()).onNext(anyString());
        verify(w, never()).onError(any(Throwable.class));
        verify(w, times(1)).onCompleted();
    }

    @Test
    public void testDistinctUntilChangedOfNormalSource() {
        Observable<String> src = Observable.just("a", "b", "c", "c", "c", "b", "b", "a", "e");
        src.distinctUntilChanged().subscribe(w);

        InOrder inOrder = inOrder(w);
        inOrder.verify(w, times(1)).onNext("a");
        inOrder.verify(w, times(1)).onNext("b");
        inOrder.verify(w, times(1)).onNext("c");
        inOrder.verify(w, times(1)).onNext("b");
        inOrder.verify(w, times(1)).onNext("a");
        inOrder.verify(w, times(1)).onNext("e");
        inOrder.verify(w, times(1)).onCompleted();
        inOrder.verify(w, never()).onNext(anyString());
        verify(w, never()).onError(any(Throwable.class));
    }

    @Test
    public void testDistinctUntilChangedOfNormalSourceWithKeySelector() {
        Observable<String> src = Observable.just("a", "b", "c", "C", "c", "B", "b", "a", "e");
        src.distinctUntilChanged(TO_UPPER_WITH_EXCEPTION).subscribe(w);

        InOrder inOrder = inOrder(w);
        inOrder.verify(w, times(1)).onNext("a");
        inOrder.verify(w, times(1)).onNext("b");
        inOrder.verify(w, times(1)).onNext("c");
        inOrder.verify(w, times(1)).onNext("B");
        inOrder.verify(w, times(1)).onNext("a");
        inOrder.verify(w, times(1)).onNext("e");
        inOrder.verify(w, times(1)).onCompleted();
        inOrder.verify(w, never()).onNext(anyString());
        verify(w, never()).onError(any(Throwable.class));
    }

    @Test
    public void testDistinctUntilChangedOfSourceWithNulls() {
        Observable<String> src = Observable.just(null, "a", "a", null, null, "b", null, null);
        src.distinctUntilChanged().subscribe(w);

        InOrder inOrder = inOrder(w);
        inOrder.verify(w, times(1)).onNext(null);
        inOrder.verify(w, times(1)).onNext("a");
        inOrder.verify(w, times(1)).onNext(null);
        inOrder.verify(w, times(1)).onNext("b");
        inOrder.verify(w, times(1)).onNext(null);
        inOrder.verify(w, times(1)).onCompleted();
        inOrder.verify(w, never()).onNext(anyString());
        verify(w, never()).onError(any(Throwable.class));
    }

    @Test
    public void testDistinctUntilChangedOfSourceWithExceptionsFromKeySelector() {
        Observable<String> src = Observable.just("a", "b", null, "c");
        src.distinctUntilChanged(TO_UPPER_WITH_EXCEPTION).subscribe(w);

        InOrder inOrder = inOrder(w);
        inOrder.verify(w, times(1)).onNext("a");
        inOrder.verify(w, times(1)).onNext("b");
        verify(w, times(1)).onError(any(NullPointerException.class));
        inOrder.verify(w, never()).onNext(anyString());
        inOrder.verify(w, never()).onCompleted();
    }

    @Test
    public void testDistinctUntilChangedWhenNonFatalExceptionThrownByKeySelectorIsNotReportedByUpstream() {
        Observable<String> src = Observable.just("a", "b", null, "c");
        final AtomicBoolean errorOccurred = new AtomicBoolean(false);
        src
          .doOnError(new Action1<Throwable>() {
                @Override
                public void call(Throwable t) {
                    errorOccurred.set(true);
                }
            })
          .distinctUntilChanged(THROWS_NON_FATAL)
          .subscribe(w);
        assertFalse(errorOccurred.get());
    }

    @Test
    public void customComparator() {
        Observable<String> source = Observable.just("a", "b", "B", "A","a", "C");

        TestSubscriber<String> ts = TestSubscriber.create();

        source.distinctUntilChanged(new Func2<String, String, Boolean>() {
            @Override
            public Boolean call(String a, String b) {
                return a.compareToIgnoreCase(b) == 0;
            }
        })
        .subscribe(ts);

        ts.assertValues("a", "b", "A", "C");
        ts.assertNoErrors();
        ts.assertCompleted();
    }

    @Test
    public void customComparatorThrows() {
        Observable<String> source = Observable.just("a", "b", "B", "A","a", "C");

        TestSubscriber<String> ts = TestSubscriber.create();

        source.distinctUntilChanged(new Func2<String, String, Boolean>() {
            @Override
            public Boolean call(String a, String b) {
                throw new TestException();
            }
        })
        .subscribe(ts);

        ts.assertValue("a");
        ts.assertNotCompleted();
        ts.assertError(TestException.class);
    }

}
