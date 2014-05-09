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

import static org.mockito.Matchers.any;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

import junit.framework.Assert;

import org.junit.Before;
import org.junit.Test;
import org.mockito.InOrder;
import static org.mockito.Matchers.anyInt;
import org.mockito.Mock;
import static org.mockito.Mockito.mock;
import org.mockito.MockitoAnnotations;

import rx.Observable;
import rx.Observer;
import rx.Subscriber;
import rx.exceptions.TestException;
import rx.functions.Action1;
import rx.functions.Func1;
import rx.functions.Functions;
import rx.observables.GroupedObservable;

public class OperatorGroupByUntilTest {
    @Mock
    Observer<Object> observer;

    <T, R> Func1<T, R> just(final R value) {
        return new Func1<T, R>() {
            @Override
            public R call(T t1) {
                return value;
            }
        };
    }

    <T> Func1<Integer, T> fail(T dummy) {
        return new Func1<Integer, T>() {
            @Override
            public T call(Integer t1) {
                throw new RuntimeException("Forced failure");
            }
        };
    }

    <T, R> Func1<T, R> fail2(R dummy2) {
        return new Func1<T, R>() {
            @Override
            public R call(T t1) {
                throw new RuntimeException("Forced failure");
            }
        };
    }

    Func1<Integer, Integer> dbl = new Func1<Integer, Integer>() {
        @Override
        public Integer call(Integer t1) {
            return t1 * 2;
        }
    };
    Func1<Integer, Integer> identity = Functions.identity();

    @Before
    public void before() {
        MockitoAnnotations.initMocks(this);
    }

    @Test
    public void normalBehavior() {
        Observable<String> source = Observable.from(Arrays.asList(
                "  foo",
                " FoO ",
                "baR  ",
                "foO ",
                " Baz   ",
                "  qux ",
                "   bar",
                " BAR  ",
                "FOO ",
                "baz  ",
                " bAZ ",
                "    fOo    "
                ));

        Func1<GroupedObservable<String, String>, Observable<String>> duration = new Func1<GroupedObservable<String, String>, Observable<String>>() {
            @Override
            public Observable<String> call(GroupedObservable<String, String> t1) {
                return t1.skip(2);
            }
        };
        Func1<GroupedObservable<String, String>, String> getkey = new Func1<GroupedObservable<String, String>, String>() {

            @Override
            public String call(GroupedObservable<String, String> t1) {
                return t1.getKey();
            }

        };
        Func1<String, String> keysel = new Func1<String, String>() {
            @Override
            public String call(String t1) {
                return t1.trim().toLowerCase();
            }
        };
        Func1<String, String> valuesel = new Func1<String, String>() {
            @Override
            public String call(String t1) {
                return t1 + t1;
            }
        };

        Observable<String> m = source.groupByUntil(
                keysel, valuesel,
                duration).map(getkey);

        m.subscribe(observer);

        InOrder inOrder = inOrder(observer);
        inOrder.verify(observer, times(1)).onNext("foo");
        inOrder.verify(observer, times(1)).onNext("bar");
        inOrder.verify(observer, times(1)).onNext("baz");
        inOrder.verify(observer, times(1)).onNext("qux");
        inOrder.verify(observer, times(1)).onNext("foo");
        inOrder.verify(observer, times(1)).onCompleted();
        verify(observer, never()).onError(any(Throwable.class));
    }

    @Test
    public void behaveAsGroupBy() {
        Observable<Integer> source = Observable.from(0, 1, 2, 3, 4, 5, 6);

        Func1<GroupedObservable<Integer, Integer>, Observable<Object>> duration = just(Observable.never());

        Observable<GroupedObservable<Integer, Integer>> m = source.groupByUntil(
                identity, dbl,
                duration);

        final Map<Integer, Integer> actual = new HashMap<Integer, Integer>();

        m.subscribe(new Action1<GroupedObservable<Integer, Integer>>() {
            @Override
            public void call(final GroupedObservable<Integer, Integer> t1) {
                t1.subscribe(new Action1<Integer>() {
                    @Override
                    public void call(Integer t2) {
                        actual.put(t1.getKey(), t2);
                    }
                });
            }
        });

        Map<Integer, Integer> expected = new HashMap<Integer, Integer>();
        for (int i = 0; i < 7; i++) {
            expected.put(i, i * 2);
        }

        Assert.assertEquals(expected, actual);
    }

    @Test
    public void keySelectorThrows() {
        Observable<Integer> source = Observable.from(0, 1, 2, 3, 4, 5, 6);

        Func1<GroupedObservable<Integer, Integer>, Observable<Object>> duration = just(Observable.never());

        Observable<GroupedObservable<Integer, Integer>> m = source.groupByUntil(
                fail(0), dbl,
                duration);

        m.subscribe(observer);

        verify(observer, times(1)).onError(any(Throwable.class));
        verify(observer, never()).onNext(any());
        verify(observer, never()).onCompleted();

    }

    @Test
    public void valueSelectorThrows() {
        Observable<Integer> source = Observable.from(0, 1, 2, 3, 4, 5, 6);

        Func1<GroupedObservable<Integer, Integer>, Observable<Object>> duration = just(Observable.never());

        Observable<GroupedObservable<Integer, Integer>> m = source.groupByUntil(
                identity, fail(0),
                duration);

        m.subscribe(observer);

        verify(observer, times(1)).onError(any(Throwable.class));
        verify(observer, never()).onNext(any());
        verify(observer, never()).onCompleted();

    }

    @Test
    public void durationSelectorThrows() {
        Observable<Integer> source = Observable.from(0, 1, 2, 3, 4, 5, 6);

        Func1<GroupedObservable<Integer, Integer>, Observable<Object>> duration = fail2((Observable<Object>) null);

        Observable<GroupedObservable<Integer, Integer>> m = source.groupByUntil(
                identity, dbl,
                duration);

        m.subscribe(observer);

        verify(observer, times(1)).onError(any(Throwable.class));
        verify(observer, never()).onNext(any());
        verify(observer, never()).onCompleted();

    }

    @Test
    public void durationThrows() {
        Observable<Integer> source = Observable.from(0, 1, 2, 3, 4, 5, 6);

        Func1<GroupedObservable<Integer, Integer>, Integer> getkey = new Func1<GroupedObservable<Integer, Integer>, Integer>() {

            @Override
            public Integer call(GroupedObservable<Integer, Integer> t1) {
                return t1.getKey();
            }

        };
        Func1<GroupedObservable<Integer, Integer>, Observable<Object>> duration = just(Observable.error(new RuntimeException("Forced failure")));

        Observable<Integer> m = source.groupByUntil(
                identity, dbl,
                duration).map(getkey);

        m.subscribe(observer);

        verify(observer, times(1)).onNext(0);
        verify(observer, times(1)).onError(any(Throwable.class));
        verify(observer, never()).onNext(1);
        verify(observer, never()).onNext(2);
        verify(observer, never()).onNext(3);
        verify(observer, never()).onNext(4);
        verify(observer, never()).onNext(5);
        verify(observer, never()).onNext(6);
        verify(observer, never()).onCompleted();

    }

    @Test
    public void innerEscapeCompleted() {
        Observable<Integer> source = Observable.from(0);

        final AtomicReference<GroupedObservable<Integer, Integer>> inner = new AtomicReference<GroupedObservable<Integer, Integer>>();

        Func1<GroupedObservable<Integer, Integer>, Observable<Object>> duration = just(Observable.never());

        Observable<GroupedObservable<Integer, Integer>> m = source.groupByUntil(identity, dbl, duration);

        m.subscribe(new Action1<GroupedObservable<Integer, Integer>>() {
            @Override
            public void call(GroupedObservable<Integer, Integer> t1) {
                inner.set(t1);
            }
        });

        inner.get().subscribe(observer);

        verify(observer).onNext(0);
        verify(observer).onCompleted();
        verify(observer, never()).onError(any(Throwable.class));
    }

    @Test
    public void innerEscapeCompletedTwice() {
        Observable<Integer> source = Observable.from(0);

        final AtomicReference<GroupedObservable<Integer, Integer>> inner = new AtomicReference<GroupedObservable<Integer, Integer>>();

        Func1<GroupedObservable<Integer, Integer>, Observable<Object>> duration = just(Observable.never());

        Observable<GroupedObservable<Integer, Integer>> m = source.groupByUntil(identity, dbl, duration);

        m.subscribe(new Action1<GroupedObservable<Integer, Integer>>() {
            @Override
            public void call(GroupedObservable<Integer, Integer> t1) {
                inner.set(t1);
            }
        });

        inner.get().subscribe(observer);
        
        @SuppressWarnings("unchecked")
        Observer<Integer> o2 = mock(Observer.class);
        
        inner.get().subscribe(o2);

        verify(o2, never()).onCompleted();
        verify(o2, never()).onNext(anyInt());
        verify(o2).onError(any(IllegalStateException.class));
    }

    @Test
    public void innerEscapeError() {
        Observable<Integer> source = Observable.concat(Observable.from(0), Observable.<Integer> error(
                new TestException("Forced failure")));

        final AtomicReference<GroupedObservable<Integer, Integer>> inner = new AtomicReference<GroupedObservable<Integer, Integer>>();

        Func1<GroupedObservable<Integer, Integer>, Observable<Object>> duration = just(Observable.never());

        Observable<GroupedObservable<Integer, Integer>> m = source.groupByUntil(identity, dbl, duration);

        m.subscribe(new Subscriber<GroupedObservable<Integer, Integer>>() {
            @Override
            public void onNext(GroupedObservable<Integer, Integer> t1) {
                inner.set(t1);
            }

            @Override
            public void onError(Throwable e) {
            }

            @Override
            public void onCompleted() {
            }

        });

        inner.get().subscribe(observer);

        verify(observer).onNext(0);
        verify(observer).onError(any(TestException.class));
        verify(observer, never()).onCompleted();
    }
    
    @Test
    public void innerEscapeErrorTwice() {
        Observable<Integer> source = Observable.concat(Observable.from(0), Observable.<Integer> error(
                new TestException("Forced failure")));

        final AtomicReference<GroupedObservable<Integer, Integer>> inner = new AtomicReference<GroupedObservable<Integer, Integer>>();

        Func1<GroupedObservable<Integer, Integer>, Observable<Object>> duration = just(Observable.never());

        Observable<GroupedObservable<Integer, Integer>> m = source.groupByUntil(identity, dbl, duration);

        m.subscribe(new Subscriber<GroupedObservable<Integer, Integer>>() {
            @Override
            public void onNext(GroupedObservable<Integer, Integer> t1) {
                inner.set(t1);
            }

            @Override
            public void onError(Throwable e) {
            }

            @Override
            public void onCompleted() {
            }

        });

        inner.get().subscribe(observer);

        @SuppressWarnings("unchecked")
        Observer<Integer> o2 = mock(Observer.class);
        
        inner.get().subscribe(o2);

        verify(o2, never()).onCompleted();
        verify(o2, never()).onNext(anyInt());
        verify(o2).onError(any(IllegalStateException.class));
    }
}