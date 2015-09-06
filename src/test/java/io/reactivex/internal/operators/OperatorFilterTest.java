/**
 * Copyright 2015 Netflix, Inc.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in
 * compliance with the License. You may obtain a copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software distributed under the License is
 * distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See
 * the License for the specific language governing permissions and limitations under the License.
 */

package io.reactivex.internal.operators;

import static org.mockito.Matchers.any;
import static org.mockito.Mockito.*;

import java.util.concurrent.CountDownLatch;
import java.util.function.Predicate;

import org.junit.Test;
import org.mockito.Mockito;
import org.reactivestreams.Subscriber;

import io.reactivex.*;
import io.reactivex.subscribers.TestSubscriber;

public class OperatorFilterTest {

    @Test
    public void testFilter() {
        Observable<String> w = Observable.just("one", "two", "three");
        Observable<String> observable = w.filter(new Predicate<String>() {

            @Override
            public boolean test(String t1) {
                return t1.equals("two");
            }
        });

        Subscriber<String> observer = TestHelper.mockSubscriber();

        observable.subscribe(observer);
        
        verify(observer, Mockito.never()).onNext("one");
        verify(observer, times(1)).onNext("two");
        verify(observer, Mockito.never()).onNext("three");
        verify(observer, Mockito.never()).onError(any(Throwable.class));
        verify(observer, times(1)).onComplete();
    }

    /**
     * Make sure we are adjusting subscriber.request() for filtered items
     */
    @Test(timeout = 500)
    public void testWithBackpressure() throws InterruptedException {
        Observable<String> w = Observable.just("one", "two", "three");
        Observable<String> o = w.filter(new Predicate<String>() {

            @Override
            public boolean test(String t1) {
                return t1.equals("three");
            }
        });

        final CountDownLatch latch = new CountDownLatch(1);
        TestSubscriber<String> ts = new TestSubscriber<String>() {

            @Override
            public void onComplete() {
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
        ts.request(2);

        o.subscribe(ts);

        // this will wait forever unless OperatorTake handles the request(n) on filtered items
        latch.await();
    }

    /**
     * Make sure we are adjusting subscriber.request() for filtered items
     */
    @Test(timeout = 500000)
    public void testWithBackpressure2() throws InterruptedException {
        Observable<Integer> w = Observable.range(1, Observable.bufferSize() * 2);
        Observable<Integer> o = w.filter(new Predicate<Integer>() {

            @Override
            public boolean test(Integer t1) {
                return t1 > 100;
            }
        });

        final CountDownLatch latch = new CountDownLatch(1);
        final TestSubscriber<Integer> ts = new TestSubscriber<Integer>() {
            
            @Override
            public void onComplete() {
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
        ts.request(1);

        o.subscribe(ts);

        // this will wait forever unless OperatorTake handles the request(n) on filtered items
        latch.await();
    }
    
    // FIXME subscribers are not allowed to throw
//    @Test
//    public void testFatalError() {
//        try {
//            Observable.just(1)
//            .filter(new Predicate<Integer>() {
//                @Override
//                public boolean test(Integer t) {
//                    return true;
//                }
//            })
//            .first()
//            .subscribe(new Consumer<Integer>() {
//                @Override
//                public void accept(Integer t) {
//                    throw new TestException();
//                }
//            });
//            Assert.fail("No exception was thrown");
//        } catch (OnErrorNotImplementedException ex) {
//            if (!(ex.getCause() instanceof TestException)) {
//                Assert.fail("Failed to report the original exception, instead: " + ex.getCause());
//            }
//        }
//    }
}