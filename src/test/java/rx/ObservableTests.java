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
package rx;

import static org.junit.Assert.*;
import static org.mockito.Matchers.*;
import static org.mockito.Mockito.*;

import java.lang.Thread.UncaughtExceptionHandler;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;

import org.junit.*;
import org.mockito.*;

import rx.Observable.*;
import rx.exceptions.*;
import rx.functions.*;
import rx.observables.ConnectableObservable;
import rx.observers.*;
import rx.plugins.RxJavaHooks;
import rx.schedulers.*;
import rx.subjects.*;
import rx.subscriptions.BooleanSubscription;

public class ObservableTests {

    @Mock
    Observer<Integer> w;

    private static final Func1<Integer, Boolean> IS_EVEN = new Func1<Integer, Boolean>() {
        @Override
        public Boolean call(Integer value) {
            return value % 2 == 0;
        }
    };

    @Before
    public void before() {
        MockitoAnnotations.initMocks(this);
    }



    @Test
    public void testReplay() throws InterruptedException {
        final AtomicInteger counter = new AtomicInteger();
        System.out.println("a1");
        ConnectableObservable<String> o = Observable.create(new OnSubscribe<String>() {

            @Override
            public void call(final Subscriber<? super String> observer) {
                new Thread(new Runnable() {

                    @Override
                    public void run() {
                        System.out.println("b1");
                        counter.incrementAndGet();
                        System.out.println("b2");
                        observer.onNext("one");
                        System.out.println("b3");
                        observer.onCompleted();
                        System.out.println("b4");
                    }
                }).start();
            }
        }).replay();

        System.out.println("a2");

        // we connect immediately and it will emit the value
        Subscription s = o.connect();

        System.out.println("a3");


        try {

            // we then expect the following 2 subscriptions to get that same value
            final CountDownLatch latch = new CountDownLatch(1);

            System.out.println("a4");

            // subscribe once
            o.subscribe(new Action1<String>() {

                @Override
                public void call(String v) {
                    System.out.println("c1");
                    assertEquals("one", v);
                    System.out.println("c2");
                    latch.countDown();
                    System.out.println("c3");
                }
            });

            System.out.println("a5");

            /*// subscribe again
            o.subscribe(new Action1<String>() {

                @Override
                public void call(String v) {
                    System.out.println("d1");
                    assertEquals("one", v);
                    System.out.println("d2");
                    latch.countDown();
                    System.out.println("d3");
                }
            });

            System.out.println("a6");*/

            if (!latch.await(1000, TimeUnit.MILLISECONDS)) {
                fail("subscriptions did not receive values");
            }

            System.out.println("a7");
            assertEquals(1, counter.get());

            System.out.println("a8");
        } finally {
            s.unsubscribe();
        }
    }


    static final class FailingObservable extends Observable<Object> {

        protected FailingObservable() {
            super(new OnSubscribe<Object>() {
                @Override
                public void call(Subscriber<? super Object> t) {
                    throw new TestException("Forced failure");
                }
            });
        }

    }


}
