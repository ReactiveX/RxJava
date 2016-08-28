/**
 * Copyright 2016 Netflix, Inc.
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

package io.reactivex.observable;

import static org.junit.Assert.*;

import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.Test;

import io.reactivex.Observable;
import io.reactivex.ObservableOperator;
import io.reactivex.Observer;
import io.reactivex.exceptions.TestException;
import io.reactivex.functions.Consumer;
import io.reactivex.observers.*;
import io.reactivex.subjects.PublishSubject;

public class ObservableSubscriberTest {
    @Test
    public void testOnStartCalledOnceViaSubscribe() {
        final AtomicInteger c = new AtomicInteger();
        Observable.just(1, 2, 3, 4).take(2).subscribe(new DefaultObserver<Integer>() {

            @Override
            public void onStart() {
                c.incrementAndGet();
            }

            @Override
            public void onComplete() {

            }

            @Override
            public void onError(Throwable e) {

            }

            @Override
            public void onNext(Integer t) {
            }

        });

        assertEquals(1, c.get());
    }

    @Test
    public void testOnStartCalledOnceViaUnsafeSubscribe() {
        final AtomicInteger c = new AtomicInteger();
        Observable.just(1, 2, 3, 4).take(2).subscribe(new DefaultObserver<Integer>() {

            @Override
            public void onStart() {
                c.incrementAndGet();
            }

            @Override
            public void onComplete() {

            }

            @Override
            public void onError(Throwable e) {

            }

            @Override
            public void onNext(Integer t) {
            }

        });

        assertEquals(1, c.get());
    }

    @Test
    public void testOnStartCalledOnceViaLift() {
        final AtomicInteger c = new AtomicInteger();
        Observable.just(1, 2, 3, 4).lift(new ObservableOperator<Integer, Integer>() {

            @Override
            public Observer<? super Integer> apply(final Observer<? super Integer> child) {
                return new DefaultObserver<Integer>() {

                    @Override
                    public void onStart() {
                        c.incrementAndGet();
                    }

                    @Override
                    public void onComplete() {
                        child.onComplete();
                    }

                    @Override
                    public void onError(Throwable e) {
                        child.onError(e);
                    }

                    @Override
                    public void onNext(Integer t) {
                        child.onNext(t);
                    }

                };
            }

        }).subscribe();

        assertEquals(1, c.get());
    }
    
    @Test
    public void subscribeConsumerConsumer() {
        final List<Integer> list = new ArrayList<Integer>();
        
        Observable.just(1).subscribe(new Consumer<Integer>() {
            @Override
            public void accept(Integer v) throws Exception {
                list.add(v);
            }
        }, new Consumer<Throwable>() {
            @Override
            public void accept(Throwable e) throws Exception {
                list.add(100);
            }
        });
        
        assertEquals(Arrays.asList(1), list);
    }

    @Test
    public void subscribeConsumerConsumerWithError() {
        final List<Integer> list = new ArrayList<Integer>();
        
        Observable.<Integer>error(new TestException()).subscribe(new Consumer<Integer>() {
            @Override
            public void accept(Integer v) throws Exception {
                list.add(v);
            }
        }, new Consumer<Throwable>() {
            @Override
            public void accept(Throwable e) throws Exception {
                list.add(100);
            }
        });
        
        assertEquals(Arrays.asList(100), list);
    }

    @Test
    public void methodTestCancelled() {
        PublishSubject<Integer> ps = PublishSubject.create();
        
        ps.test(true);
        
        assertFalse(ps.hasObservers());
    }
    
    @Test
    public void safeSubscriberAlreadySafe() {
        TestObserver<Integer> ts = new TestObserver<Integer>();
        Observable.just(1).safeSubscribe(new SafeObserver<Integer>(ts));
        
        ts.assertResult(1);
    }


    @Test
    public void methodTestNoCancel() {
        PublishSubject<Integer> ps = PublishSubject.create();
        
        ps.test(false);
        
        assertTrue(ps.hasObservers());
    }

}
