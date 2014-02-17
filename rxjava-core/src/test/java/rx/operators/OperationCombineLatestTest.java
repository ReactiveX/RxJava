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
import static rx.operators.OperationCombineLatest.*;

import java.util.Arrays;

import org.junit.Test;
import org.mockito.InOrder;
import org.mockito.Matchers;

import rx.Observable;
import rx.Observer;
import rx.Subscription;
import rx.functions.Func2;
import rx.functions.Func3;
import rx.functions.FuncN;
import rx.subjects.PublishSubject;
import rx.subscriptions.Subscriptions;

public class OperationCombineLatestTest {

    @Test
    public void testCombineLatestWithFunctionThatThrowsAnException() {
        @SuppressWarnings("unchecked")
        // mock calls don't do generics
        Observer<String> w = mock(Observer.class);

        TestObservable w1 = new TestObservable();
        TestObservable w2 = new TestObservable();

        Observable<String> combined = Observable.create(combineLatest(Observable.create(w1), Observable.create(w2), new Func2<String, String, String>() {
            @Override
            public String call(String v1, String v2) {
                throw new RuntimeException("I don't work.");
            }
        }));
        combined.subscribe(w);

        w1.observer.onNext("first value of w1");
        w2.observer.onNext("first value of w2");

        verify(w, never()).onNext(anyString());
        verify(w, never()).onCompleted();
        verify(w, times(1)).onError(Matchers.<RuntimeException> any());
    }

    @Test
    public void testCombineLatestDifferentLengthObservableSequences1() {
        @SuppressWarnings("unchecked")
        // mock calls don't do generics
        Observer<String> w = mock(Observer.class);

        TestObservable w1 = new TestObservable();
        TestObservable w2 = new TestObservable();
        TestObservable w3 = new TestObservable();

        Observable<String> combineLatestW = Observable.create(combineLatest(Observable.create(w1), Observable.create(w2), Observable.create(w3), getConcat3StringsCombineLatestFunction()));
        combineLatestW.subscribe(w);

        /* simulate sending data */
        // once for w1
        w1.observer.onNext("1a");
        w2.observer.onNext("2a");
        w3.observer.onNext("3a");
        w1.observer.onCompleted();
        // twice for w2
        w2.observer.onNext("2b");
        w2.observer.onCompleted();
        // 4 times for w3
        w3.observer.onNext("3b");
        w3.observer.onNext("3c");
        w3.observer.onNext("3d");
        w3.observer.onCompleted();

        /* we should have been called 4 times on the Observer */
        InOrder inOrder = inOrder(w);
        inOrder.verify(w).onNext("1a2a3a");
        inOrder.verify(w).onNext("1a2b3a");
        inOrder.verify(w).onNext("1a2b3b");
        inOrder.verify(w).onNext("1a2b3c");
        inOrder.verify(w).onNext("1a2b3d");
        inOrder.verify(w, never()).onNext(anyString());
        inOrder.verify(w, times(1)).onCompleted();
    }

    @Test
    public void testCombineLatestDifferentLengthObservableSequences2() {
        @SuppressWarnings("unchecked")
        Observer<String> w = mock(Observer.class);

        TestObservable w1 = new TestObservable();
        TestObservable w2 = new TestObservable();
        TestObservable w3 = new TestObservable();

        Observable<String> combineLatestW = Observable.create(combineLatest(Observable.create(w1), Observable.create(w2), Observable.create(w3), getConcat3StringsCombineLatestFunction()));
        combineLatestW.subscribe(w);

        /* simulate sending data */
        // 4 times for w1
        w1.observer.onNext("1a");
        w1.observer.onNext("1b");
        w1.observer.onNext("1c");
        w1.observer.onNext("1d");
        w1.observer.onCompleted();
        // twice for w2
        w2.observer.onNext("2a");
        w2.observer.onNext("2b");
        w2.observer.onCompleted();
        // 1 times for w3
        w3.observer.onNext("3a");
        w3.observer.onCompleted();

        /* we should have been called 1 time only on the Observer since we only combine the "latest" we don't go back and loop through others once completed */
        InOrder inOrder = inOrder(w);
        inOrder.verify(w, times(1)).onNext("1d2b3a");
        inOrder.verify(w, never()).onNext(anyString());

        inOrder.verify(w, times(1)).onCompleted();

    }

    @Test
    public void testCombineLatestWithInterleavingSequences() {
        @SuppressWarnings("unchecked")
        Observer<String> w = mock(Observer.class);

        TestObservable w1 = new TestObservable();
        TestObservable w2 = new TestObservable();
        TestObservable w3 = new TestObservable();

        Observable<String> combineLatestW = Observable.create(combineLatest(Observable.create(w1), Observable.create(w2), Observable.create(w3), getConcat3StringsCombineLatestFunction()));
        combineLatestW.subscribe(w);

        /* simulate sending data */
        w1.observer.onNext("1a");
        w2.observer.onNext("2a");
        w2.observer.onNext("2b");
        w3.observer.onNext("3a");

        w1.observer.onNext("1b");
        w2.observer.onNext("2c");
        w2.observer.onNext("2d");
        w3.observer.onNext("3b");

        w1.observer.onCompleted();
        w2.observer.onCompleted();
        w3.observer.onCompleted();

        /* we should have been called 5 times on the Observer */
        InOrder inOrder = inOrder(w);
        inOrder.verify(w).onNext("1a2b3a");
        inOrder.verify(w).onNext("1b2b3a");
        inOrder.verify(w).onNext("1b2c3a");
        inOrder.verify(w).onNext("1b2d3a");
        inOrder.verify(w).onNext("1b2d3b");

        inOrder.verify(w, never()).onNext(anyString());
        inOrder.verify(w, times(1)).onCompleted();
    }

    @SuppressWarnings("unchecked")
    /* mock calls don't do generics */
    @Test
    public void testCombineLatest2Types() {
        Func2<String, Integer, String> combineLatestFunction = getConcatStringIntegerCombineLatestFunction();

        /* define a Observer to receive aggregated events */
        Observer<String> observer = mock(Observer.class);

        Observable<String> w = Observable.create(combineLatest(Observable.from("one", "two"), Observable.from(2, 3, 4), combineLatestFunction));
        w.subscribe(observer);

        verify(observer, never()).onError(any(Throwable.class));
        verify(observer, times(1)).onCompleted();
        verify(observer, times(1)).onNext("two2");
        verify(observer, times(1)).onNext("two3");
        verify(observer, times(1)).onNext("two4");
    }

    @SuppressWarnings("unchecked")
    /* mock calls don't do generics */
    @Test
    public void testCombineLatest3TypesA() {
        Func3<String, Integer, int[], String> combineLatestFunction = getConcatStringIntegerIntArrayCombineLatestFunction();

        /* define a Observer to receive aggregated events */
        Observer<String> observer = mock(Observer.class);

        Observable<String> w = Observable.create(combineLatest(Observable.from("one", "two"), Observable.from(2), Observable.from(new int[] { 4, 5, 6 }), combineLatestFunction));
        w.subscribe(observer);

        verify(observer, never()).onError(any(Throwable.class));
        verify(observer, times(1)).onCompleted();
        verify(observer, times(1)).onNext("two2[4, 5, 6]");
    }

    @SuppressWarnings("unchecked")
    /* mock calls don't do generics */
    @Test
    public void testCombineLatest3TypesB() {
        Func3<String, Integer, int[], String> combineLatestFunction = getConcatStringIntegerIntArrayCombineLatestFunction();

        /* define a Observer to receive aggregated events */
        Observer<String> observer = mock(Observer.class);

        Observable<String> w = Observable.create(combineLatest(Observable.from("one"), Observable.from(2), Observable.from(new int[] { 4, 5, 6 }, new int[] { 7, 8 }), combineLatestFunction));
        w.subscribe(observer);

        verify(observer, never()).onError(any(Throwable.class));
        verify(observer, times(1)).onCompleted();
        verify(observer, times(1)).onNext("one2[4, 5, 6]");
        verify(observer, times(1)).onNext("one2[7, 8]");
    }

    private Func3<String, String, String, String> getConcat3StringsCombineLatestFunction() {
        Func3<String, String, String, String> combineLatestFunction = new Func3<String, String, String, String>() {

            @Override
            public String call(String a1, String a2, String a3) {
                if (a1 == null) {
                    a1 = "";
                }
                if (a2 == null) {
                    a2 = "";
                }
                if (a3 == null) {
                    a3 = "";
                }
                return a1 + a2 + a3;
            }

        };
        return combineLatestFunction;
    }

    private FuncN<String> getConcatCombineLatestFunction() {
        FuncN<String> combineLatestFunction = new FuncN<String>() {

            @Override
            public String call(Object... args) {
                String returnValue = "";
                for (Object o : args) {
                    if (o != null) {
                        returnValue += getStringValue(o);
                    }
                }
                System.out.println("returning: " + returnValue);
                return returnValue;
            }

        };
        return combineLatestFunction;
    }

    private Func2<String, Integer, String> getConcatStringIntegerCombineLatestFunction() {
        Func2<String, Integer, String> combineLatestFunction = new Func2<String, Integer, String>() {

            @Override
            public String call(String s, Integer i) {
                return getStringValue(s) + getStringValue(i);
            }

        };
        return combineLatestFunction;
    }

    private Func3<String, Integer, int[], String> getConcatStringIntegerIntArrayCombineLatestFunction() {
        Func3<String, Integer, int[], String> combineLatestFunction = new Func3<String, Integer, int[], String>() {

            @Override
            public String call(String s, Integer i, int[] iArray) {
                return getStringValue(s) + getStringValue(i) + getStringValue(iArray);
            }

        };
        return combineLatestFunction;
    }

    private static String getStringValue(Object o) {
        if (o == null) {
            return "";
        } else {
            if (o instanceof int[]) {
                return Arrays.toString((int[]) o);
            } else {
                return String.valueOf(o);
            }
        }
    }

    private static class TestObservable implements Observable.OnSubscribeFunc<String> {

        Observer<? super String> observer;

        @Override
        public Subscription onSubscribe(Observer<? super String> observer) {
            // just store the variable where it can be accessed so we can manually trigger it
            this.observer = observer;
            return Subscriptions.empty();
        }

    }

    Func2<Integer, Integer, Integer> or = new Func2<Integer, Integer, Integer>() {
        @Override
        public Integer call(Integer t1, Integer t2) {
            return t1 | t2;
        }
    };

    @Test
    public void combineSimple() {
        PublishSubject<Integer> a = PublishSubject.create();
        PublishSubject<Integer> b = PublishSubject.create();

        Observable<Integer> source = Observable.combineLatest(a, b, or);

        Observer<Object> observer = mock(Observer.class);

        source.subscribe(observer);

        InOrder inOrder = inOrder(observer);

        a.onNext(1);

        inOrder.verify(observer, never()).onNext(any());

        a.onNext(2);

        inOrder.verify(observer, never()).onNext(any());

        b.onNext(0x10);

        inOrder.verify(observer, times(1)).onNext(0x12);

        b.onNext(0x20);
        inOrder.verify(observer, times(1)).onNext(0x22);

        b.onCompleted();

        inOrder.verify(observer, never()).onCompleted();

        a.onCompleted();

        inOrder.verify(observer, times(1)).onCompleted();

        a.onNext(3);
        b.onNext(0x30);
        a.onCompleted();
        b.onCompleted();

        inOrder.verifyNoMoreInteractions();
        verify(observer, never()).onError(any(Throwable.class));
    }

    @Test
    public void combineMultipleObservers() {
        PublishSubject<Integer> a = PublishSubject.create();
        PublishSubject<Integer> b = PublishSubject.create();

        Observable<Integer> source = Observable.combineLatest(a, b, or);

        Observer<Object> observer1 = mock(Observer.class);
        Observer<Object> observer2 = mock(Observer.class);

        source.subscribe(observer1);
        source.subscribe(observer2);

        InOrder inOrder1 = inOrder(observer1);
        InOrder inOrder2 = inOrder(observer2);

        a.onNext(1);

        inOrder1.verify(observer1, never()).onNext(any());
        inOrder2.verify(observer2, never()).onNext(any());

        a.onNext(2);

        inOrder1.verify(observer1, never()).onNext(any());
        inOrder2.verify(observer2, never()).onNext(any());

        b.onNext(0x10);

        inOrder1.verify(observer1, times(1)).onNext(0x12);
        inOrder2.verify(observer2, times(1)).onNext(0x12);

        b.onNext(0x20);
        inOrder1.verify(observer1, times(1)).onNext(0x22);
        inOrder2.verify(observer2, times(1)).onNext(0x22);

        b.onCompleted();

        inOrder1.verify(observer1, never()).onCompleted();
        inOrder2.verify(observer2, never()).onCompleted();

        a.onCompleted();

        inOrder1.verify(observer1, times(1)).onCompleted();
        inOrder2.verify(observer2, times(1)).onCompleted();

        a.onNext(3);
        b.onNext(0x30);
        a.onCompleted();
        b.onCompleted();

        inOrder1.verifyNoMoreInteractions();
        inOrder2.verifyNoMoreInteractions();
        verify(observer1, never()).onError(any(Throwable.class));
        verify(observer2, never()).onError(any(Throwable.class));
    }

    @Test
    public void testFirstNeverProduces() {
        PublishSubject<Integer> a = PublishSubject.create();
        PublishSubject<Integer> b = PublishSubject.create();

        Observable<Integer> source = Observable.combineLatest(a, b, or);

        Observer<Object> observer = mock(Observer.class);

        source.subscribe(observer);

        InOrder inOrder = inOrder(observer);

        b.onNext(0x10);
        b.onNext(0x20);

        a.onCompleted();

        inOrder.verify(observer, times(1)).onCompleted();
        verify(observer, never()).onNext(any());
        verify(observer, never()).onError(any(Throwable.class));
    }

    @Test
    public void testSecondNeverProduces() {
        PublishSubject<Integer> a = PublishSubject.create();
        PublishSubject<Integer> b = PublishSubject.create();

        Observable<Integer> source = Observable.combineLatest(a, b, or);

        Observer<Object> observer = mock(Observer.class);

        source.subscribe(observer);

        InOrder inOrder = inOrder(observer);

        a.onNext(0x1);
        a.onNext(0x2);

        b.onCompleted();
        a.onCompleted();

        inOrder.verify(observer, times(1)).onCompleted();
        verify(observer, never()).onNext(any());
        verify(observer, never()).onError(any(Throwable.class));
    }
}
