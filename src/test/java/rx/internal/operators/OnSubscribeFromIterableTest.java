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

import static org.junit.Assert.*;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.*;

import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;

import org.junit.Test;
import org.mockito.Mockito;

import rx.Observable;
import rx.Observer;
import rx.Subscriber;
import rx.exceptions.TestException;
import rx.internal.util.RxRingBuffer;
import rx.observers.TestSubscriber;
import rx.schedulers.Schedulers;

public class OnSubscribeFromIterableTest {

    @Test(expected = NullPointerException.class)
    public void testNull() {
        Observable.create(new OnSubscribeFromIterable<String>(null));
    }
    
    @Test
    public void testListIterable() {
        Observable<String> observable = Observable.create(new OnSubscribeFromIterable<String>(Arrays.<String> asList("one", "two", "three")));

        @SuppressWarnings("unchecked")
        Observer<String> observer = mock(Observer.class);
        observable.subscribe(observer);
        verify(observer, times(1)).onNext("one");
        verify(observer, times(1)).onNext("two");
        verify(observer, times(1)).onNext("three");
        verify(observer, Mockito.never()).onError(any(Throwable.class));
        verify(observer, times(1)).onCompleted();
    }

    /**
     * This tests the path that can not optimize based on size so must use setProducer.
     */
    @Test
    public void testRawIterable() {
        Iterable<String> it = new Iterable<String>() {

            @Override
            public Iterator<String> iterator() {
                return new Iterator<String>() {

                    int i = 0;

                    @Override
                    public boolean hasNext() {
                        return i < 3;
                    }

                    @Override
                    public String next() {
                        return String.valueOf(++i);
                    }

                    @Override
                    public void remove() {
                    }

                };
            }

        };
        Observable<String> observable = Observable.create(new OnSubscribeFromIterable<String>(it));

        @SuppressWarnings("unchecked")
        Observer<String> observer = mock(Observer.class);
        observable.subscribe(observer);
        verify(observer, times(1)).onNext("1");
        verify(observer, times(1)).onNext("2");
        verify(observer, times(1)).onNext("3");
        verify(observer, Mockito.never()).onError(any(Throwable.class));
        verify(observer, times(1)).onCompleted();
    }

    @Test
    public void testObservableFromIterable() {
        Observable<String> observable = Observable.from(Arrays.<String> asList("one", "two", "three"));

        @SuppressWarnings("unchecked")
        Observer<String> observer = mock(Observer.class);
        observable.subscribe(observer);
        verify(observer, times(1)).onNext("one");
        verify(observer, times(1)).onNext("two");
        verify(observer, times(1)).onNext("three");
        verify(observer, Mockito.never()).onError(any(Throwable.class));
        verify(observer, times(1)).onCompleted();
    }

    @Test
    public void testBackpressureViaRequest() {
        ArrayList<Integer> list = new ArrayList<Integer>(RxRingBuffer.SIZE);
        for (int i = 1; i <= RxRingBuffer.SIZE + 1; i++) {
            list.add(i);
        }
        OnSubscribeFromIterable<Integer> o = new OnSubscribeFromIterable<Integer>(list);
        TestSubscriber<Integer> ts = new TestSubscriber<Integer>();
        ts.assertReceivedOnNext(Collections.<Integer> emptyList());
        ts.requestMore(1);
        o.call(ts);
        ts.assertReceivedOnNext(Arrays.asList(1));
        ts.requestMore(2);
        ts.assertReceivedOnNext(Arrays.asList(1, 2, 3));
        ts.requestMore(3);
        ts.assertReceivedOnNext(Arrays.asList(1, 2, 3, 4, 5, 6));
        ts.requestMore(list.size());
        ts.assertTerminalEvent();
    }

    @Test
    public void testNoBackpressure() {
        OnSubscribeFromIterable<Integer> o = new OnSubscribeFromIterable<Integer>(Arrays.asList(1, 2, 3, 4, 5));
        TestSubscriber<Integer> ts = new TestSubscriber<Integer>();
        ts.assertReceivedOnNext(Collections.<Integer> emptyList());
        ts.requestMore(Long.MAX_VALUE); // infinite
        o.call(ts);
        ts.assertReceivedOnNext(Arrays.asList(1, 2, 3, 4, 5));
        ts.assertTerminalEvent();
    }

    @Test
    public void testSubscribeMultipleTimes() {
        OnSubscribeFromIterable<Integer> o = new OnSubscribeFromIterable<Integer>(Arrays.asList(1, 2, 3));
        TestSubscriber<Integer> ts = new TestSubscriber<Integer>();
        o.call(ts);
        ts.assertReceivedOnNext(Arrays.asList(1, 2, 3));

        ts = new TestSubscriber<Integer>();
        o.call(ts);
        ts.assertReceivedOnNext(Arrays.asList(1, 2, 3));

        ts = new TestSubscriber<Integer>();
        o.call(ts);
        ts.assertReceivedOnNext(Arrays.asList(1, 2, 3));
    }
    
    @Test
    public void testFromIterableRequestOverflow() throws InterruptedException {
        Observable<Integer> o = Observable.from(Arrays.asList(1,2,3,4));
        final int expectedCount = 4;
        final CountDownLatch latch = new CountDownLatch(expectedCount);
        o.subscribeOn(Schedulers.computation()).subscribe(new Subscriber<Integer>() {
            
            @Override
            public void onStart() {
                request(2);
            }

            @Override
            public void onCompleted() {
                //ignore
            }

            @Override
            public void onError(Throwable e) {
                throw new RuntimeException(e);
            }

            @Override
            public void onNext(Integer t) {
                latch.countDown();
                request(Long.MAX_VALUE-1);
            }});
        assertTrue(latch.await(10, TimeUnit.SECONDS));
    }

    @Test
    public void testFromEmptyIterableWhenZeroRequestedShouldStillEmitOnCompletedEagerly() {
        final AtomicBoolean completed = new AtomicBoolean(false);
        Observable.from(Collections.emptyList()).subscribe(new Subscriber<Object>() {

            @Override
            public void onStart() {
                request(0);
            }
            
            @Override
            public void onCompleted() {
                completed.set(true);
            }

            @Override
            public void onError(Throwable e) {
                
            }

            @Override
            public void onNext(Object t) {
                
            }});
        assertTrue(completed.get());
    }
    
    @Test
    public void testDoesNotCallIteratorHasNextMoreThanRequiredWithBackpressure() {
        final AtomicBoolean called = new AtomicBoolean(false);
        Iterable<Integer> iterable = new Iterable<Integer>() {

            @Override
            public Iterator<Integer> iterator() {
                return new Iterator<Integer>() {

                    int count = 1;
                    
                    @Override
                    public void remove() {
                        // ignore
                    }

                    @Override
                    public boolean hasNext() {
                        if (count > 1) {
                            called.set(true);
                            return false;
                        } else
                            return true;
                    }

                    @Override
                    public Integer next() {
                        return count++;
                    }

                };
            }
        };
        Observable.from(iterable).take(1).subscribe();
        assertFalse(called.get());
    }

    @Test
    public void testDoesNotCallIteratorHasNextMoreThanRequiredFastPath() {
        final AtomicBoolean called = new AtomicBoolean(false);
        Iterable<Integer> iterable = new Iterable<Integer>() {

            @Override
            public Iterator<Integer> iterator() {
                return new Iterator<Integer>() {

                    @Override
                    public void remove() {
                        // ignore
                    }

                    int count = 1;

                    @Override
                    public boolean hasNext() {
                        if (count > 1) {
                            called.set(true);
                            return false;
                        } else
                            return true;
                    }

                    @Override
                    public Integer next() {
                        return count++;
                    }

                };
            }
        };
        Observable.from(iterable).subscribe(new Subscriber<Integer>() {

            @Override
            public void onCompleted() {

            }

            @Override
            public void onError(Throwable e) {

            }

            @Override
            public void onNext(Integer t) {
                // unsubscribe on first emission
                unsubscribe();
            }
        });
        assertFalse(called.get());
    }

    @Test
    public void getIteratorThrows() {
        Iterable<Integer> it = new Iterable<Integer>() {
            @Override
            public Iterator<Integer> iterator() {
                throw new TestException("Forced failure");
            }
        };
        
        TestSubscriber<Integer> ts = new TestSubscriber<Integer>();
        
        Observable.from(it).unsafeSubscribe(ts);
        
        ts.assertNoValues();
        ts.assertError(TestException.class);
        ts.assertNotCompleted();
    }

    @Test
    public void hasNextThrowsImmediately() {
        Iterable<Integer> it = new Iterable<Integer>() {
            @Override
            public Iterator<Integer> iterator() {
                return new Iterator<Integer>() {
                    @Override
                    public boolean hasNext() {
                        throw new TestException("Forced failure");
                    }
                    
                    @Override
                    public Integer next() {
                        return null;
                    }
                    
                    @Override
                    public void remove() {
                        // ignored
                    }
                };
            }
        };
        
        TestSubscriber<Integer> ts = new TestSubscriber<Integer>();
        
        Observable.from(it).unsafeSubscribe(ts);
        
        ts.assertNoValues();
        ts.assertError(TestException.class);
        ts.assertNotCompleted();
    }

    @Test
    public void hasNextThrowsSecondTimeFastpath() {
        Iterable<Integer> it = new Iterable<Integer>() {
            @Override
            public Iterator<Integer> iterator() {
                return new Iterator<Integer>() {
                    int count;
                    @Override
                    public boolean hasNext() {
                        if (++count >= 2) {
                            throw new TestException("Forced failure");
                        }
                        return true;
                    }
                    
                    @Override
                    public Integer next() {
                        return 1;
                    }
                    
                    @Override
                    public void remove() {
                        // ignored
                    }
                };
            }
        };
        
        TestSubscriber<Integer> ts = new TestSubscriber<Integer>();
        
        Observable.from(it).unsafeSubscribe(ts);
        
        ts.assertValues(1);
        ts.assertError(TestException.class);
        ts.assertNotCompleted();
    }

    @Test
    public void hasNextThrowsSecondTimeSlowpath() {
        Iterable<Integer> it = new Iterable<Integer>() {
            @Override
            public Iterator<Integer> iterator() {
                return new Iterator<Integer>() {
                    int count;
                    @Override
                    public boolean hasNext() {
                        if (++count >= 2) {
                            throw new TestException("Forced failure");
                        }
                        return true;
                    }
                    
                    @Override
                    public Integer next() {
                        return 1;
                    }
                    
                    @Override
                    public void remove() {
                        // ignored
                    }
                };
            }
        };
        
        TestSubscriber<Integer> ts = new TestSubscriber<Integer>(5);
        
        Observable.from(it).unsafeSubscribe(ts);
        
        ts.assertValues(1);
        ts.assertError(TestException.class);
        ts.assertNotCompleted();
    }
    
    @Test
    public void nextThrowsFastpath() {
        Iterable<Integer> it = new Iterable<Integer>() {
            @Override
            public Iterator<Integer> iterator() {
                return new Iterator<Integer>() {
                    @Override
                    public boolean hasNext() {
                        return true;
                    }
                    
                    @Override
                    public Integer next() {
                        throw new TestException("Forced failure");
                    }
                    
                    @Override
                    public void remove() {
                        // ignored
                    }
                };
            }
        };
        
        TestSubscriber<Integer> ts = new TestSubscriber<Integer>();
        
        Observable.from(it).unsafeSubscribe(ts);
        
        ts.assertNoValues();
        ts.assertError(TestException.class);
        ts.assertNotCompleted();
    }

    @Test
    public void nextThrowsSlowpath() {
        Iterable<Integer> it = new Iterable<Integer>() {
            @Override
            public Iterator<Integer> iterator() {
                return new Iterator<Integer>() {
                    @Override
                    public boolean hasNext() {
                        return true;
                    }
                    
                    @Override
                    public Integer next() {
                        throw new TestException("Forced failure");
                    }
                    
                    @Override
                    public void remove() {
                        // ignored
                    }
                };
            }
        };
        
        TestSubscriber<Integer> ts = new TestSubscriber<Integer>(5);
        
        Observable.from(it).unsafeSubscribe(ts);
        
        ts.assertNoValues();
        ts.assertError(TestException.class);
        ts.assertNotCompleted();
    }

    @Test
    public void deadOnArrival() {
        Iterable<Integer> it = new Iterable<Integer>() {
            @Override
            public Iterator<Integer> iterator() {
                return new Iterator<Integer>() {
                    @Override
                    public boolean hasNext() {
                        return false;
                    }
                    
                    @Override
                    public Integer next() {
                        throw new NoSuchElementException();
                    }
                    
                    @Override
                    public void remove() {
                        // ignored
                    }
                };
            }
        };
        
        TestSubscriber<Integer> ts = new TestSubscriber<Integer>(5);
        ts.unsubscribe();
        
        Observable.from(it).unsafeSubscribe(ts);
        
        ts.assertNoValues();
        ts.assertNoErrors();
        ts.assertNotCompleted();
        
    }
}
