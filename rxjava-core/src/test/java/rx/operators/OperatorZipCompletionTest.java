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

import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;

import org.junit.Before;
import org.junit.Test;
import org.mockito.InOrder;

import rx.Observable;
import rx.Observer;
import rx.functions.Func2;
import rx.subjects.PublishSubject;

/**
 * Systematically tests that when zipping an infinite and a finite Observable,
 * the resulting Observable is finite.
 * 
 */
public class OperatorZipCompletionTest {
    Func2<String, String, String> concat2Strings;

    PublishSubject<String> s1;
    PublishSubject<String> s2;
    Observable<String> zipped;

    Observer<String> observer;
    InOrder inOrder;

    @Before
    @SuppressWarnings("unchecked")
    public void setUp() {
        concat2Strings = new Func2<String, String, String>() {
            @Override
            public String call(String t1, String t2) {
                return t1 + "-" + t2;
            }
        };

        s1 = PublishSubject.create();
        s2 = PublishSubject.create();
        zipped = Observable.zip(s1, s2, concat2Strings);

        observer = mock(Observer.class);
        inOrder = inOrder(observer);

        zipped.subscribe(observer);
    }

    @Test
    public void testFirstCompletesThenSecondInfinite() {
        s1.onNext("a");
        s1.onNext("b");
        s1.onCompleted();
        s2.onNext("1");
        inOrder.verify(observer, times(1)).onNext("a-1");
        s2.onNext("2");
        inOrder.verify(observer, times(1)).onNext("b-2");
        inOrder.verify(observer, times(1)).onCompleted();
        inOrder.verifyNoMoreInteractions();
    }

    @Test
    public void testSecondInfiniteThenFirstCompletes() {
        s2.onNext("1");
        s2.onNext("2");
        s1.onNext("a");
        inOrder.verify(observer, times(1)).onNext("a-1");
        s1.onNext("b");
        inOrder.verify(observer, times(1)).onNext("b-2");
        s1.onCompleted();
        inOrder.verify(observer, times(1)).onCompleted();
        inOrder.verifyNoMoreInteractions();
    }

    @Test
    public void testSecondCompletesThenFirstInfinite() {
        s2.onNext("1");
        s2.onNext("2");
        s2.onCompleted();
        s1.onNext("a");
        inOrder.verify(observer, times(1)).onNext("a-1");
        s1.onNext("b");
        inOrder.verify(observer, times(1)).onNext("b-2");
        inOrder.verify(observer, times(1)).onCompleted();
        inOrder.verifyNoMoreInteractions();
    }

    @Test
    public void testFirstInfiniteThenSecondCompletes() {
        s1.onNext("a");
        s1.onNext("b");
        s2.onNext("1");
        inOrder.verify(observer, times(1)).onNext("a-1");
        s2.onNext("2");
        inOrder.verify(observer, times(1)).onNext("b-2");
        s2.onCompleted();
        inOrder.verify(observer, times(1)).onCompleted();
        inOrder.verifyNoMoreInteractions();
    }

}
