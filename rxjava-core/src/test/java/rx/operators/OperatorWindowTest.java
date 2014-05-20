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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import org.junit.Before;
import org.junit.Test;

import rx.Observable;
import rx.Observer;
import rx.Scheduler;
import rx.Subscriber;
import rx.exceptions.TestException;
import rx.functions.Action0;
import rx.functions.Action1;
import rx.functions.Func0;
import rx.functions.Func1;
import rx.schedulers.Schedulers;
import rx.schedulers.TestScheduler;
import rx.subjects.PublishSubject;

public class OperatorWindowTest {

    private TestScheduler scheduler;
    private Scheduler.Worker innerScheduler;

    @Before
    public void before() {
        scheduler = new TestScheduler();
        innerScheduler = scheduler.createWorker();
    }

    private static <T> List<List<T>> toLists(Observable<Observable<T>> observables) {

        final List<List<T>> lists = new ArrayList<List<T>>();
        Observable.concat(observables.map(new Func1<Observable<T>, Observable<List<T>>>() {
            @Override
            public Observable<List<T>> call(Observable<T> xs) {
                return xs.toList();
            }
        }))
                .toBlocking()
                .forEach(new Action1<List<T>>() {
                    @Override
                    public void call(List<T> xs) {
                        lists.add(xs);
                    }
                });
        return lists;
    }

    @Test
    public void testNonOverlappingWindows() {
        Observable<String> subject = Observable.from("one", "two", "three", "four", "five");
        Observable<Observable<String>> windowed = subject.window(3);

        List<List<String>> windows = toLists(windowed);

        assertEquals(2, windows.size());
        assertEquals(list("one", "two", "three"), windows.get(0));
        assertEquals(list("four", "five"), windows.get(1));
    }

    @Test
    public void testSkipAndCountGaplessWindows() {
        Observable<String> subject = Observable.from("one", "two", "three", "four", "five");
        Observable<Observable<String>> windowed = subject.window(3, 3);

        List<List<String>> windows = toLists(windowed);

        assertEquals(2, windows.size());
        assertEquals(list("one", "two", "three"), windows.get(0));
        assertEquals(list("four", "five"), windows.get(1));
    }

    @Test
    public void testOverlappingWindows() {
        Observable<String> subject = Observable.from(new String[] { "zero", "one", "two", "three", "four", "five" }, Schedulers.trampoline());
        Observable<Observable<String>> windowed = subject.window(3, 1);

        List<List<String>> windows = toLists(windowed);

        assertEquals(6, windows.size());
        assertEquals(list("zero", "one", "two"), windows.get(0));
        assertEquals(list("one", "two", "three"), windows.get(1));
        assertEquals(list("two", "three", "four"), windows.get(2));
        assertEquals(list("three", "four", "five"), windows.get(3));
        assertEquals(list("four", "five"), windows.get(4));
        assertEquals(list("five"), windows.get(5));
    }

    @Test
    public void testSkipAndCountWindowsWithGaps() {
        Observable<String> subject = Observable.from("one", "two", "three", "four", "five");
        Observable<Observable<String>> windowed = subject.window(2, 3);

        List<List<String>> windows = toLists(windowed);

        assertEquals(2, windows.size());
        assertEquals(list("one", "two"), windows.get(0));
        assertEquals(list("four", "five"), windows.get(1));
    }

    @Test
    public void testTimedAndCount() {
        final List<String> list = new ArrayList<String>();
        final List<List<String>> lists = new ArrayList<List<String>>();

        Observable<String> source = Observable.create(new Observable.OnSubscribe<String>() {
            @Override
            public void call(Subscriber<? super String> observer) {
                push(observer, "one", 10);
                push(observer, "two", 90);
                push(observer, "three", 110);
                push(observer, "four", 190);
                push(observer, "five", 210);
                complete(observer, 250);
            }
        });

        Observable<Observable<String>> windowed = source.window(100, TimeUnit.MILLISECONDS, 2, scheduler);
        windowed.subscribe(observeWindow(list, lists));

        scheduler.advanceTimeTo(100, TimeUnit.MILLISECONDS);
        assertEquals(1, lists.size());
        assertEquals(lists.get(0), list("one", "two"));

        scheduler.advanceTimeTo(200, TimeUnit.MILLISECONDS);
        assertEquals(2, lists.size());
        assertEquals(lists.get(1), list("three", "four"));

        scheduler.advanceTimeTo(300, TimeUnit.MILLISECONDS);
        assertEquals(3, lists.size());
        assertEquals(lists.get(2), list("five"));
    }

    @Test
    public void testTimed() {
        final List<String> list = new ArrayList<String>();
        final List<List<String>> lists = new ArrayList<List<String>>();

        Observable<String> source = Observable.create(new Observable.OnSubscribe<String>() {
            @Override
            public void call(Subscriber<? super String> observer) {
                push(observer, "one", 98);
                push(observer, "two", 99);
                push(observer, "three", 100);
                push(observer, "four", 101);
                push(observer, "five", 102);
                complete(observer, 150);
            }
        });

        Observable<Observable<String>> windowed = source.window(100, TimeUnit.MILLISECONDS, scheduler);
        windowed.subscribe(observeWindow(list, lists));

        scheduler.advanceTimeTo(101, TimeUnit.MILLISECONDS);
        assertEquals(1, lists.size());
        assertEquals(lists.get(0), list("one", "two", "three"));

        scheduler.advanceTimeTo(201, TimeUnit.MILLISECONDS);
        assertEquals(2, lists.size());
        assertEquals(lists.get(1), list("four", "five"));
    }

    @Test
    public void testObservableBasedOpenerAndCloser() {
        final List<String> list = new ArrayList<String>();
        final List<List<String>> lists = new ArrayList<List<String>>();

        Observable<String> source = Observable.create(new Observable.OnSubscribe<String>() {
            @Override
            public void call(Subscriber<? super String> observer) {
                push(observer, "one", 10);
                push(observer, "two", 60);
                push(observer, "three", 110);
                push(observer, "four", 160);
                push(observer, "five", 210);
                complete(observer, 500);
            }
        });

        Observable<Object> openings = Observable.create(new Observable.OnSubscribe<Object>() {
            @Override
            public void call(Subscriber<? super Object> observer) {
                push(observer, new Object(), 50);
                push(observer, new Object(), 200);
                complete(observer, 250);
            }
        });

        Func1<Object, Observable<Object>> closer = new Func1<Object, Observable<Object>>() {
            @Override
            public Observable<Object> call(Object opening) {
                return Observable.create(new Observable.OnSubscribe<Object>() {
                    @Override
                    public void call(Subscriber<? super Object> observer) {
                        push(observer, new Object(), 100);
                        complete(observer, 101);
                    }
                });
            }
        };

        Observable<Observable<String>> windowed = source.window(openings, closer);
        windowed.subscribe(observeWindow(list, lists));

        scheduler.advanceTimeTo(500, TimeUnit.MILLISECONDS);
        assertEquals(2, lists.size());
        assertEquals(lists.get(0), list("two", "three"));
        assertEquals(lists.get(1), list("five"));
    }

    @Test
    public void testObservableBasedCloser() {
        final List<String> list = new ArrayList<String>();
        final List<List<String>> lists = new ArrayList<List<String>>();

        Observable<String> source = Observable.create(new Observable.OnSubscribe<String>() {
            @Override
            public void call(Subscriber<? super String> observer) {
                push(observer, "one", 10);
                push(observer, "two", 60);
                push(observer, "three", 110);
                push(observer, "four", 160);
                push(observer, "five", 210);
                complete(observer, 250);
            }
        });

        Func0<Observable<Object>> closer = new Func0<Observable<Object>>() {
            @Override
            public Observable<Object> call() {
                return Observable.create(new Observable.OnSubscribe<Object>() {
                    @Override
                    public void call(Subscriber<? super Object> observer) {
                        push(observer, new Object(), 100);
                        push(observer, new Object(), 200);
                        complete(observer, 301);
                    }
                });
            }
        };

        Observable<Observable<String>> windowed = source.window(closer);
        windowed.subscribe(observeWindow(list, lists));

        scheduler.advanceTimeTo(500, TimeUnit.MILLISECONDS);
        assertEquals(3, lists.size());
        assertEquals(lists.get(0), list("one", "two"));
        assertEquals(lists.get(1), list("three", "four"));
        assertEquals(lists.get(2), list("five"));
    }

    private List<String> list(String... args) {
        List<String> list = new ArrayList<String>();
        for (String arg : args) {
            list.add(arg);
        }
        return list;
    }

    private <T> void push(final Observer<T> observer, final T value, int delay) {
        innerScheduler.schedule(new Action0() {
            @Override
            public void call() {
                observer.onNext(value);
            }
        }, delay, TimeUnit.MILLISECONDS);
    }

    private void complete(final Observer<?> observer, int delay) {
        innerScheduler.schedule(new Action0() {
            @Override
            public void call() {
                observer.onCompleted();
            }
        }, delay, TimeUnit.MILLISECONDS);
    }

    private Action1<Observable<String>> observeWindow(final List<String> list, final List<List<String>> lists) {
        return new Action1<Observable<String>>() {
            @Override
            public void call(Observable<String> stringObservable) {
                stringObservable.subscribe(new Observer<String>() {
                    @Override
                    public void onCompleted() {
                        lists.add(new ArrayList<String>(list));
                        list.clear();
                    }

                    @Override
                    public void onError(Throwable e) {
                        fail(e.getMessage());
                    }

                    @Override
                    public void onNext(String args) {
                        list.add(args);
                    }
                });
            }
        };
    }

    @Test
    public void testWindowViaObservableNormal1() {
        PublishSubject<Integer> source = PublishSubject.create();
        PublishSubject<Integer> boundary = PublishSubject.create();

        @SuppressWarnings("unchecked")
        final Observer<Object> o = mock(Observer.class);

        final List<Observer<Object>> values = new ArrayList<Observer<Object>>();

        Observer<Observable<Integer>> wo = new Observer<Observable<Integer>>() {
            @Override
            public void onNext(Observable<Integer> args) {
                @SuppressWarnings("unchecked")
                final Observer<Object> mo = mock(Observer.class);
                values.add(mo);

                args.subscribe(mo);
            }

            @Override
            public void onError(Throwable e) {
                o.onError(e);
            }

            @Override
            public void onCompleted() {
                o.onCompleted();
            }
        };

        source.window(boundary).subscribe(wo);

        int n = 30;
        for (int i = 0; i < n; i++) {
            source.onNext(i);
            if (i % 3 == 2 && i < n - 1) {
                boundary.onNext(i / 3);
            }
        }
        source.onCompleted();

        assertEquals(n / 3, values.size());

        int j = 0;
        for (Observer<Object> mo : values) {
            for (int i = 0; i < 3; i++) {
                verify(mo).onNext(j + i);
            }
            verify(mo).onCompleted();
            verify(mo, never()).onError(any(Throwable.class));
            j += 3;
        }

        verify(o).onCompleted();
        verify(o, never()).onError(any(Throwable.class));
    }

    @Test
    public void testWindowViaObservableBoundaryCompletes() {
        PublishSubject<Integer> source = PublishSubject.create();
        PublishSubject<Integer> boundary = PublishSubject.create();

        @SuppressWarnings("unchecked")
        final Observer<Object> o = mock(Observer.class);

        final List<Observer<Object>> values = new ArrayList<Observer<Object>>();

        Observer<Observable<Integer>> wo = new Observer<Observable<Integer>>() {
            @Override
            public void onNext(Observable<Integer> args) {
                @SuppressWarnings("unchecked")
                final Observer<Object> mo = mock(Observer.class);
                values.add(mo);

                args.subscribe(mo);
            }

            @Override
            public void onError(Throwable e) {
                o.onError(e);
            }

            @Override
            public void onCompleted() {
                o.onCompleted();
            }
        };

        source.window(boundary).subscribe(wo);

        int n = 30;
        for (int i = 0; i < n; i++) {
            source.onNext(i);
            if (i % 3 == 2 && i < n - 1) {
                boundary.onNext(i / 3);
            }
        }
        boundary.onCompleted();

        assertEquals(n / 3, values.size());

        int j = 0;
        for (Observer<Object> mo : values) {
            for (int i = 0; i < 3; i++) {
                verify(mo).onNext(j + i);
            }
            verify(mo).onCompleted();
            verify(mo, never()).onError(any(Throwable.class));
            j += 3;
        }

        verify(o).onCompleted();
        verify(o, never()).onError(any(Throwable.class));
    }

    @Test
    public void testWindowViaObservableBoundaryThrows() {
        PublishSubject<Integer> source = PublishSubject.create();
        PublishSubject<Integer> boundary = PublishSubject.create();

        @SuppressWarnings("unchecked")
        final Observer<Object> o = mock(Observer.class);

        final List<Observer<Object>> values = new ArrayList<Observer<Object>>();

        Observer<Observable<Integer>> wo = new Observer<Observable<Integer>>() {
            @Override
            public void onNext(Observable<Integer> args) {
                @SuppressWarnings("unchecked")
                final Observer<Object> mo = mock(Observer.class);
                values.add(mo);

                args.subscribe(mo);
            }

            @Override
            public void onError(Throwable e) {
                o.onError(e);
            }

            @Override
            public void onCompleted() {
                o.onCompleted();
            }
        };

        source.window(boundary).subscribe(wo);

        source.onNext(0);
        source.onNext(1);
        source.onNext(2);

        boundary.onError(new TestException());

        assertEquals(1, values.size());

        Observer<Object> mo = values.get(0);

        verify(mo).onNext(0);
        verify(mo).onNext(1);
        verify(mo).onNext(2);
        verify(mo).onError(any(TestException.class));

        verify(o, never()).onCompleted();
        verify(o).onError(any(TestException.class));
    }

    @Test
    public void testWindowViaObservableSourceThrows() {
        PublishSubject<Integer> source = PublishSubject.create();
        PublishSubject<Integer> boundary = PublishSubject.create();

        @SuppressWarnings("unchecked")
        final Observer<Object> o = mock(Observer.class);

        final List<Observer<Object>> values = new ArrayList<Observer<Object>>();

        Observer<Observable<Integer>> wo = new Observer<Observable<Integer>>() {
            @Override
            public void onNext(Observable<Integer> args) {
                @SuppressWarnings("unchecked")
                final Observer<Object> mo = mock(Observer.class);
                values.add(mo);

                args.subscribe(mo);
            }

            @Override
            public void onError(Throwable e) {
                o.onError(e);
            }

            @Override
            public void onCompleted() {
                o.onCompleted();
            }
        };

        source.window(boundary).subscribe(wo);

        source.onNext(0);
        source.onNext(1);
        source.onNext(2);

        source.onError(new TestException());

        assertEquals(1, values.size());

        Observer<Object> mo = values.get(0);

        verify(mo).onNext(0);
        verify(mo).onNext(1);
        verify(mo).onNext(2);
        verify(mo).onError(any(TestException.class));

        verify(o, never()).onCompleted();
        verify(o).onError(any(TestException.class));
    }
}