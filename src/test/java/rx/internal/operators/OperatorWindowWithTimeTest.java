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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import org.junit.Before;
import org.junit.Test;

import rx.Observable;
import rx.Observer;
import rx.Scheduler;
import rx.Subscriber;
import rx.functions.Action0;
import rx.functions.Action1;
import rx.schedulers.TestScheduler;

public class OperatorWindowWithTimeTest {

    private TestScheduler scheduler;
    private Scheduler.Worker innerScheduler;

    @Before
    public void before() {
        scheduler = new TestScheduler();
        innerScheduler = scheduler.createWorker();
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
}