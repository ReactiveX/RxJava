/**
 * Copyright 2013 Netflix, Inc.
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
package rx.lang.groovy

import static org.mockito.Matchers.*;
import static org.mockito.Mockito.*;

import java.util.Arrays;

import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.*;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import rx.Notification;
import rx.Observable;
import rx.Observer;
import rx.Subscription;
import rx.subscriptions.Subscriptions;
import rx.util.functions.Func1;

def class ObservableTests {

    @Mock
    ScriptAssertion a;

    @Mock
    Observer<Integer> w;

    @Before
    public void before() {
        MockitoAnnotations.initMocks(this);
    }

    @Test
    public void testCreate() {
        Observable.create({it.onNext('hello');it.onCompleted();}).subscribe({ result -> a.received(result)});
        verify(a, times(1)).received("hello");
    }

    @Test
    public void testFilter() {
        Observable.filter(Observable.from(1, 2, 3), {it >= 2}).subscribe({ result -> a.received(result)});
        verify(a, times(0)).received(1);
        verify(a, times(1)).received(2);
        verify(a, times(1)).received(3);
    }

    @Test
    public void testLast() {
        assertEquals("three", Observable.from("one", "two", "three").toBlockingObservable().last())
    }

    @Test
    public void testLastWithPredicate() {
        assertEquals("two", Observable.from("one", "two", "three").toBlockingObservable().last({ x -> x.length() == 3}))
    }

    @Test
    public void testMap1() {
        Observable.from(1).map({v -> 'hello_' + v}).subscribe({ result -> a.received(result)});
        verify(a, times(1)).received("hello_1");
    }

    @Test
    public void testMap2() {
        Observable.map(Observable.from(1, 2, 3), {'hello_' + it}).subscribe({ result -> a.received(result)});
        verify(a, times(1)).received("hello_" + 1);
        verify(a, times(1)).received("hello_" + 2);
        verify(a, times(1)).received("hello_" + 3);
    }

    @Test
    public void testMaterialize() {
        Observable.materialize(Observable.from(1, 2, 3)).subscribe({ result -> a.received(result)});
        // we expect 4 onNext calls: 3 for 1, 2, 3 ObservableNotification.OnNext and 1 for ObservableNotification.OnCompleted
        verify(a, times(4)).received(any(Notification.class));
        verify(a, times(0)).error(any(Exception.class));
    }

    @Test
    public void testMergeDelayError() {
        Observable.mergeDelayError(
                Observable.from(1, 2, 3),
                Observable.merge(
                Observable.from(6),
                Observable.error(new NullPointerException()),
                Observable.from(7)),
                Observable.from(4, 5))
                .subscribe( { result -> a.received(result)}, { exception -> a.error(exception)});

        verify(a, times(1)).received(1);
        verify(a, times(1)).received(2);
        verify(a, times(1)).received(3);
        verify(a, times(1)).received(4);
        verify(a, times(1)).received(5);
        verify(a, times(1)).received(6);
        verify(a, times(0)).received(7);
        verify(a, times(1)).error(any(NullPointerException.class));
    }

    @Test
    public void testMerge() {
        Observable.merge(
                Observable.from(1, 2, 3),
                Observable.merge(
                Observable.from(6),
                Observable.error(new NullPointerException()),
                Observable.from(7)),
                Observable.from(4, 5))
                .subscribe({ result -> a.received(result)}, { exception -> a.error(exception)});

        // executing synchronously so we can deterministically know what order things will come
        verify(a, times(1)).received(1);
        verify(a, times(1)).received(2);
        verify(a, times(1)).received(3);
        verify(a, times(0)).received(4); // the NPE will cause this sequence to be skipped
        verify(a, times(0)).received(5); // the NPE will cause this sequence to be skipped
        verify(a, times(1)).received(6); // this comes before the NPE so should exist
        verify(a, times(0)).received(7);// this comes in the sequence after the NPE
        verify(a, times(1)).error(any(NullPointerException.class));
    }

    @Test
    public void testScriptWithMaterialize() {
        new TestFactory().getObservable().materialize().subscribe({ result -> a.received(result)});
        // 2 times: once for hello_1 and once for onCompleted
        verify(a, times(2)).received(any(Notification.class));
    }

    @Test
    public void testScriptWithMerge() {
        TestFactory f = new TestFactory();
        Observable.merge(f.getObservable(), f.getObservable()).subscribe({ result -> a.received(result)});
        verify(a, times(1)).received("hello_1");
        verify(a, times(1)).received("hello_2");
    }

    @Test
    public void testScriptWithOnNext() {
        new TestFactory().getObservable().subscribe({ result -> a.received(result)});
        verify(a).received("hello_1");
    }

    @Test
    public void testSkipTake() {
        Observable.skip(Observable.from(1, 2, 3), 1).take(1).subscribe({ result -> a.received(result)});
        verify(a, times(0)).received(1);
        verify(a, times(1)).received(2);
        verify(a, times(0)).received(3);
    }

    @Test
    public void testSkip() {
        Observable.skip(Observable.from(1, 2, 3), 2).subscribe({ result -> a.received(result)});
        verify(a, times(0)).received(1);
        verify(a, times(0)).received(2);
        verify(a, times(1)).received(3);
    }

    @Test
    public void testTake() {
        Observable.take(Observable.from(1, 2, 3), 2).subscribe({ result -> a.received(result)});
        verify(a, times(1)).received(1);
        verify(a, times(1)).received(2);
        verify(a, times(0)).received(3);
    }

    @Test
    public void testTakeLast() {   
        new TestFactory().getObservable().takeLast(1).subscribe({ result -> a.received(result)});
        verify(a, times(1)).received("hello_1");
    }

    @Test
    public void testTakeWhileViaGroovy() {
        Observable.takeWhile(Observable.from(1, 2, 3), { x -> x < 3}).subscribe({ result -> a.received(result)});
        verify(a, times(1)).received(1);
        verify(a, times(1)).received(2);
        verify(a, times(0)).received(3);
    }

    @Test
    public void testTakeWhileWithIndexViaGroovy() {
        Observable.takeWhileWithIndex(Observable.from(1, 2, 3), { x, i -> i < 2}).subscribe({ result -> a.received(result)});
        verify(a, times(1)).received(1);
        verify(a, times(1)).received(2);
        verify(a, times(0)).received(3);
    }

    @Test
    public void testToSortedList() {
        new TestFactory().getNumbers().toSortedList().subscribe({ result -> a.received(result)});
        verify(a, times(1)).received(Arrays.asList(1, 2, 3, 4, 5));
    }

    @Test
    public void testToSortedListStatic() {
        Observable.toSortedList(Observable.from(1, 3, 2, 5, 4)).subscribe({ result -> a.received(result)});
        verify(a, times(1)).received(Arrays.asList(1, 2, 3, 4, 5));
    }

    @Test
    public void testToSortedListWithFunction() {
        new TestFactory().getNumbers().toSortedList({a, b -> a - b}).subscribe({ result -> a.received(result)});
        verify(a, times(1)).received(Arrays.asList(1, 2, 3, 4, 5));
    }

    @Test
    public void testToSortedListWithFunctionStatic() {
        Observable.toSortedList(Observable.from(1, 3, 2, 5, 4), {a, b -> a - b}).subscribe({ result -> a.received(result)});
        verify(a, times(1)).received(Arrays.asList(1, 2, 3, 4, 5));
    }
    
    @Test
    public void testForEach() {
        Observable.create(new AsyncObservable()).toBlockingObservable().forEach({ result -> a.received(result)});
        verify(a, times(1)).received(1);
        verify(a, times(1)).received(2);
        verify(a, times(1)).received(3);
    }

    @Test
    public void testForEachWithError() {
        try {
            Observable.create(new AsyncObservable()).toBlockingObservable().forEach({ result -> throw new RuntimeException('err')});
            fail("we expect an exception to be thrown");
        }catch(Exception e) {
            // do nothing as we expect this
        }
    }

    @Test
    public void testLastOrDefault() {
        def val = Observable.from("one", "two").toBlockingObservable().lastOrDefault("default", { x -> x.length() == 3})
        assertEquals("two", val)
    }

    @Test
    public void testLastOrDefault2() {
        def val = Observable.from("one", "two").toBlockingObservable().lastOrDefault("default", { x -> x.length() > 3})
        assertEquals("default", val)
    }
    
    public void testSingle1() {
        def s = Observable.from("one").toBlockingObservable().single({ x -> x.length() == 3})
        assertEquals("one", s)
    }

    @Test(expected = IllegalStateException.class)
    public void testSingle2() {
        Observable.from("one", "two").toBlockingObservable().single({ x -> x.length() == 3})
    }

    @Test
    public void testDefer() {
        def obs = Observable.from(1, 2)
        Observable.defer({-> obs }).subscribe({ result -> a.received(result)})
        verify(a, times(1)).received(1);
        verify(a, times(1)).received(2);

    }

    @Test
    public void testAll() {
        Observable.from(1, 2, 3).all({ x -> x > 0 }).subscribe({ result -> a.received(result) });
        verify(a, times(1)).received(true);
    }

    def class AsyncObservable implements Func1<Observer<Integer>, Subscription> {

        public Subscription call(final Observer<Integer> observer) {
            new Thread(new Runnable() {
                public void run() {
                    try {
                        Thread.sleep(50)
                    }catch(Exception e) {
                        // ignore
                    }
                    observer.onNext(1);
                    observer.onNext(2);
                    observer.onNext(3);
                    observer.onCompleted();
                }
            }).start();
            return Subscriptions.empty();
        }
    }

    def class TestFactory {
        int counter = 1;

        public Observable<Integer> getNumbers() {
            return Observable.from(1, 3, 2, 5, 4);
        }

        public TestObservable getObservable() {
            return new TestObservable(counter++);
        }
    }

    def interface ScriptAssertion {
        public void error(Exception o);

        public void received(Object o);
    }

    def class TestObservable extends Observable<String> {
        private final int count;

        public TestObservable(int count) {
            this.count = count;
        }

        public Subscription subscribe(Observer<String> observer) {

            observer.onNext("hello_" + count);
            observer.onCompleted();

            return new Subscription() {

                public void unsubscribe() {
                    // unregister ... will never be called here since we are executing synchronously
                }
            };
        }
    }
}