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

import static org.mockito.Matchers.any;
import static org.mockito.Mockito.*;

import java.util.concurrent.CountDownLatch;

import org.junit.*;
import org.mockito.Mockito;

import rx.*;
import rx.exceptions.*;
import rx.functions.*;
import rx.internal.util.*;
import rx.observers.TestSubscriber;
import rx.subjects.PublishSubject;

public class OperatorFilterTest {

    @Test
    public void testFilter() {
        Observable<String> w = Observable.just("one", "two", "three");
        Observable<String> observable = w.filter(new Func1<String, Boolean>() {

            @Override
            public Boolean call(String t1) {
                return t1.equals("two");
            }
        });

        @SuppressWarnings("unchecked")
        Observer<String> observer = mock(Observer.class);
        observable.subscribe(observer);
        verify(observer, Mockito.never()).onNext("one");
        verify(observer, times(1)).onNext("two");
        verify(observer, Mockito.never()).onNext("three");
        verify(observer, Mockito.never()).onError(any(Throwable.class));
        verify(observer, times(1)).onCompleted();
    }

    /**
     * Make sure we are adjusting subscriber.request() for filtered items
     * @throws InterruptedException on interrupt
     */
    @Test(timeout = 500)
    public void testWithBackpressure() throws InterruptedException {
        Observable<String> w = Observable.just("one", "two", "three");
        Observable<String> o = w.filter(new Func1<String, Boolean>() {

            @Override
            public Boolean call(String t1) {
                return t1.equals("three");
            }
        });

        final CountDownLatch latch = new CountDownLatch(1);
        TestSubscriber<String> ts = new TestSubscriber<String>(0L) {

            @Override
            public void onCompleted() {
                System.out.println("onCompleted");
                latch.countDown();
            }

            @Override
            public void onError(Throwable e) {
                e.printStackTrace();
                latch.countDown();
            }

            @Override
            public void onNext(String t) {
                System.out.println("Received: " + t);
                // request more each time we receive
                request(1);
            }

        };
        // this means it will only request "one" and "two", expecting to receive them before requesting more
        ts.requestMore(2);

        o.subscribe(ts);

        // this will wait forever unless OperatorTake handles the request(n) on filtered items
        latch.await();
    }

    /**
     * Make sure we are adjusting subscriber.request() for filtered items
     * @throws InterruptedException on interrupt
     */
    @Test(timeout = 500000)
    public void testWithBackpressure2() throws InterruptedException {
        Observable<Integer> w = Observable.range(1, RxRingBuffer.SIZE * 2);
        Observable<Integer> o = w.filter(new Func1<Integer, Boolean>() {

            @Override
            public Boolean call(Integer t1) {
                return t1 > 100;
            }
        });

        final CountDownLatch latch = new CountDownLatch(1);
        final TestSubscriber<Integer> ts = new TestSubscriber<Integer>(0L) {
            
            @Override
            public void onCompleted() {
                System.out.println("onCompleted");
                latch.countDown();
            }
            
            @Override
            public void onError(Throwable e) {
                e.printStackTrace();
                latch.countDown();
            }
            
            @Override
            public void onNext(Integer t) {
                System.out.println("Received: " + t);
                // request more each time we receive
                request(1);
            }
        };
        // this means it will only request 1 item and expect to receive more
        ts.requestMore(1);

        o.subscribe(ts);

        // this will wait forever unless OperatorTake handles the request(n) on filtered items
        latch.await();
    }
    
    @Test
    public void testFatalError() {
        try {
            Observable.just(1)
            .filter(new Func1<Integer, Boolean>() {
                @Override
                public Boolean call(Integer t) {
                    return true;
                }
            })
            .first()
            .subscribe(new Action1<Integer>() {
                @Override
                public void call(Integer t) {
                    throw new TestException();
                }
            });
            Assert.fail("No exception was thrown");
        } catch (OnErrorNotImplementedException ex) {
            if (!(ex.getCause() instanceof TestException)) {
                Assert.fail("Failed to report the original exception, instead: " + ex.getCause());
            }
        }
    }

    @Test
    public void functionCrashUnsubscribes() {
        
        PublishSubject<Integer> ps = PublishSubject.create();
        
        TestSubscriber<Integer> ts = new TestSubscriber<Integer>();
        
        ps.filter(new Func1<Integer, Boolean>() {
            @Override
            public Boolean call(Integer v) { 
                throw new TestException(); 
            }
        }).unsafeSubscribe(ts);
        
        Assert.assertTrue("Not subscribed?", ps.hasObservers());
        
        ps.onNext(1);
        
        Assert.assertFalse("Subscribed?", ps.hasObservers());
        
        ts.assertError(TestException.class);
    }

    @Test
    public void doesntRequestOnItsOwn() {
        TestSubscriber<Integer> ts = TestSubscriber.create(0L);
        
        Observable.range(1, 10).filter(UtilityFunctions.alwaysTrue()).unsafeSubscribe(ts);
        
        ts.assertNoValues();
        
        ts.requestMore(10);
        
        ts.assertValues(1, 2, 3, 4, 5, 6, 7, 8, 9, 10);
        ts.assertNoErrors();
        ts.assertCompleted();
    }
}
