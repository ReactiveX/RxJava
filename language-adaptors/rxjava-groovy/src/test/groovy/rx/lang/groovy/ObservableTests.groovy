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

import static org.junit.Assert.*
import static org.mockito.Matchers.*
import static org.mockito.Mockito.*

import org.junit.Before
import org.junit.Test
import org.mockito.Mock
import org.mockito.MockitoAnnotations

import rx.Notification
import rx.Observable
import rx.Observer
import rx.Subscription
import rx.Observable.OnSubscribeFunc
import rx.subscriptions.Subscriptions

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
        Observable.from(1, 2, 3).filter({it >= 2}).subscribe({ result -> a.received(result)});
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
        Observable.from(1, 2, 3).map({'hello_' + it}).subscribe({ result -> a.received(result)});
        verify(a, times(1)).received("hello_" + 1);
        verify(a, times(1)).received("hello_" + 2);
        verify(a, times(1)).received("hello_" + 3);
    }

    @Test
    public void testMaterialize() {
        Observable.from(1, 2, 3).materialize().subscribe({ result -> a.received(result)});
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
    public void testFromWithIterable() {
        def list = [1, 2, 3, 4, 5]
        assertEquals(5, Observable.from(list).count().toBlockingObservable().single());
    }
    
    @Test
    public void testFromWithObjects() {
        def list = [1, 2, 3, 4, 5]
        // this should now treat these as 2 objects so have a count of 2
        assertEquals(2, Observable.from(list, 6).count().toBlockingObservable().single());
    }
    
    /**
     * Check that two different single arg methods are selected correctly
     */
    @Test
    public void testStartWith() {
        def list = [10, 11, 12, 13, 14]
        def startList = [1, 2, 3, 4, 5]
        assertEquals(6, Observable.from(list).startWith(0).count().toBlockingObservable().single());
        assertEquals(10, Observable.from(list).startWith(startList).count().toBlockingObservable().single());
    }
    
    @Test
    public void testScriptWithOnNext() {
        new TestFactory().getObservable().subscribe({ result -> a.received(result)});
        verify(a).received("hello_1");
    }

    @Test
    public void testSkipTake() {
        Observable.from(1, 2, 3).skip(1).take(1).subscribe({ result -> a.received(result)});
        verify(a, times(0)).received(1);
        verify(a, times(1)).received(2);
        verify(a, times(0)).received(3);
    }

    @Test
    public void testSkip() {
        Observable.from(1, 2, 3).skip(2).subscribe({ result -> a.received(result)});
        verify(a, times(0)).received(1);
        verify(a, times(0)).received(2);
        verify(a, times(1)).received(3);
    }

    @Test
    public void testTake() {
        Observable.from(1, 2, 3).take(2).subscribe({ result -> a.received(result)});
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
        Observable.from(1, 2, 3).takeWhile( { x -> x < 3}).subscribe({ result -> a.received(result)});
        verify(a, times(1)).received(1);
        verify(a, times(1)).received(2);
        verify(a, times(0)).received(3);
    }

    @Test
    public void testTakeWhileWithIndexViaGroovy() {
        Observable.from(1, 2, 3).takeWhileWithIndex({ x, i -> i < 2}).subscribe({ result -> a.received(result)});
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
    public void testToSortedListWithFunction() {
        new TestFactory().getNumbers().toSortedList({a, b -> a - b}).subscribe({ result -> a.received(result)});
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

    @Test(expected = IllegalArgumentException.class)
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
    
    
    @Test
    public void testZip() {
        Observable o1 = Observable.from(1, 2, 3);
        Observable o2 = Observable.from(4, 5, 6);
        Observable o3 = Observable.from(7, 8, 9);
        
        List values = Observable.zip(o1, o2, o3, {a, b, c -> [a, b, c] }).toList().toBlockingObservable().single();
        assertEquals([1, 4, 7], values.get(0));
        assertEquals([2, 5, 8], values.get(1));
        assertEquals([3, 6, 9], values.get(2));
    }
    
    @Test
    public void testZipWithIterable() {
        Observable o1 = Observable.from(1, 2, 3);
        Observable o2 = Observable.from(4, 5, 6);
        Observable o3 = Observable.from(7, 8, 9);
        
        List values = Observable.zip([o1, o2, o3], {a, b, c -> [a, b, c] }).toList().toBlockingObservable().single();
        assertEquals([1, 4, 7], values.get(0));
        assertEquals([2, 5, 8], values.get(1));
        assertEquals([3, 6, 9], values.get(2));
    }
    
    @Test
    public void testGroupBy() {
        int count=0;
        
        Observable.from("one", "two", "three", "four", "five", "six")
        .groupBy({String s -> s.length()})
        .flatMap({
            groupObservable ->
            
            return groupObservable.map({
                s ->
                return "Value: " + s + " Group: " + groupObservable.getKey(); 
            });
          }).toBlockingObservable().forEach({
            s ->
            println(s);
            count++;
          })
          
          assertEquals(6, count);
    }
    
    @Test
    public void testToMap1() {
        Map actual = new HashMap();
        
        Observable.from("a", "bb", "ccc", "dddd")
        .toMap({String s -> s.length()})
        .toBlockingObservable()
        .forEach({s -> actual.putAll(s); });
        
        Map expected = new HashMap();
        expected.put(1, "a");
        expected.put(2, "bb");
        expected.put(3, "ccc");
        expected.put(4, "dddd");
        
        assertEquals(expected, actual);
    }

    @Test
    public void testToMap2() {
        Map actual = new HashMap();
        
        Observable.from("a", "bb", "ccc", "dddd")
        .toMap({String s -> s.length()}, {String s -> s + s})
        .toBlockingObservable()
        .forEach({s -> actual.putAll(s); });
        
        Map expected = new HashMap();
        expected.put(1, "aa");
        expected.put(2, "bbbb");
        expected.put(3, "cccccc");
        expected.put(4, "dddddddd");
        
        assertEquals(expected, actual);
    }

    @Test
    public void testToMap3() {
        Map actual = new HashMap();
        
        LinkedHashMap last3 = new LinkedHashMap() {
            public boolean removeEldestEntry(Map.Entry e) {
                return size() > 3;
            }
        };
        
        Observable.from("a", "bb", "ccc", "dddd")
        .toMap({String s -> s.length()}, {String s -> s + s}, { last3 })
        .toBlockingObservable()
        .forEach({s -> actual.putAll(s); });
        
        Map expected = new HashMap();
        expected.put(2, "bbbb");
        expected.put(3, "cccccc");
        expected.put(4, "dddddddd");
        
        assertEquals(expected, actual);
    }
    @Test
    public void testToMultimap1() {
        Map actual = new HashMap();
        
        Observable.from("a", "b", "cc", "dd")
        .toMultimap({String s -> s.length()})
        .toBlockingObservable()
        .forEach({s -> actual.putAll(s); });
        
        Map expected = new HashMap();
        
        expected.put(1, Arrays.asList("a", "b"));
        expected.put(2, Arrays.asList("cc", "dd"));
        
        assertEquals(expected, actual);
    }

    @Test
    public void testToMultimap2() {
        Map actual = new HashMap();
        
        Observable.from("a", "b", "cc", "dd")
        .toMultimap({String s -> s.length()}, {String s -> s + s})
        .toBlockingObservable()
        .forEach({s -> actual.putAll(s); });
        
        Map expected = new HashMap();
        
        expected.put(1, Arrays.asList("aa", "bb"));
        expected.put(2, Arrays.asList("cccc", "dddd"));
        
        assertEquals(expected, actual);
    }

    @Test
    public void testToMultimap3() {
        Map actual = new HashMap();
        
        LinkedHashMap last1 = new LinkedHashMap() {
            public boolean removeEldestEntry(Map.Entry e) {
                return size() > 1;
            }
        };
        
        Observable.from("a", "b", "cc", "dd")
        .toMultimap({String s -> s.length()}, {String s -> s + s}, { last1 })
        .toBlockingObservable()
        .forEach({s -> actual.putAll(s); });
        
        Map expected = new HashMap();
        
        expected.put(2, Arrays.asList("cccc", "dddd"));
        
        assertEquals(expected, actual);
    }

    @Test
    public void testToMultimap4() {
        Map actual = new HashMap();
        
        LinkedHashMap last1 = new LinkedHashMap() {
            public boolean removeEldestEntry(Map.Entry e) {
                return size() > 2;
            }
        };
        
        Observable.from("a", "b", "cc", "dd", "eee", "eee")
        .toMultimap({String s -> s.length()}, {String s -> s + s}, { last1 }, 
            {i -> i == 2 ? new ArrayList() : new HashSet() })
        .toBlockingObservable()
        .forEach({s -> actual.putAll(s); });
        
        Map expected = new HashMap();
        
        expected.put(2, Arrays.asList("cccc", "dddd"));
        expected.put(3, new HashSet(Arrays.asList("eeeeee")));
        
        assertEquals(expected, actual);
    }

    def class AsyncObservable implements OnSubscribeFunc {

        public Subscription onSubscribe(final Observer<Integer> observer) {
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

        public TestOnSubscribe getOnSubscribe() {
            return new TestOnSubscribe(counter++);
        }
        
        public Observable getObservable() {
            return Observable.create(getOnSubscribe());
        }
    }

    def interface ScriptAssertion {
        public void error(Exception o);

        public void received(Object o);
    }

    def class TestOnSubscribe implements OnSubscribeFunc<String> {
        private final int count;

        public TestOnSubscribe(int count) {
            this.count = count;
        }

        public Subscription onSubscribe(Observer<String> observer) {

            observer.onNext("hello_" + count);
            observer.onCompleted();

            return Subscriptions.empty();
        }
    }
}