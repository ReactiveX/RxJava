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

package io.reactivex.internal.operators.flowable;

import static org.junit.Assert.assertEquals;

import java.util.*;

import org.junit.Test;
import org.reactivestreams.*;

import io.reactivex.Flowable;
import io.reactivex.exceptions.TestException;
import io.reactivex.functions.*;
import io.reactivex.internal.functions.Functions;
import io.reactivex.schedulers.Schedulers;

public class FlowableBlockingTest {

    @Test
    public void blockingFirst() {
        assertEquals(1, Flowable.range(1, 10)
                .subscribeOn(Schedulers.computation()).blockingFirst().intValue());
    }

    @Test
    public void blockingFirstDefault() {
        assertEquals(1, Flowable.<Integer>empty()
                .subscribeOn(Schedulers.computation()).blockingFirst(1).intValue());
    }

    @Test
    public void blockingSubscribeConsumer() {
        final List<Integer> list = new ArrayList<Integer>();
        
        Flowable.range(1, 5)
        .subscribeOn(Schedulers.computation())
        .blockingSubscribe(new Consumer<Integer>() {
            @Override
            public void accept(Integer v) throws Exception {
                list.add(v);
            }
        });
        
        assertEquals(Arrays.asList(1, 2, 3, 4, 5), list);
    }

    @Test
    public void blockingSubscribeConsumerConsumer() {
        final List<Object> list = new ArrayList<Object>();
        
        Flowable.range(1, 5)
        .subscribeOn(Schedulers.computation())
        .blockingSubscribe(new Consumer<Integer>() {
            @Override
            public void accept(Integer v) throws Exception {
                list.add(v);
            }
        }, Functions.emptyConsumer());
        
        assertEquals(Arrays.asList(1, 2, 3, 4, 5), list);
    }

    @Test
    public void blockingSubscribeConsumerConsumerError() {
        final List<Object> list = new ArrayList<Object>();
        
        TestException ex = new TestException();
        
        Consumer<Object> cons = new Consumer<Object>() {
            @Override
            public void accept(Object v) throws Exception {
                list.add(v);
            }
        };
        
        Flowable.range(1, 5).concatWith(Flowable.<Integer>error(ex))
        .subscribeOn(Schedulers.computation())
        .blockingSubscribe(cons, cons);
        
        assertEquals(Arrays.asList(1, 2, 3, 4, 5, ex), list);
    }

    @Test
    public void blockingSubscribeConsumerConsumerAction() {
        final List<Object> list = new ArrayList<Object>();
        
        Consumer<Object> cons = new Consumer<Object>() {
            @Override
            public void accept(Object v) throws Exception {
                list.add(v);
            }
        };
        
        Flowable.range(1, 5)
        .subscribeOn(Schedulers.computation())
        .blockingSubscribe(cons, cons, new Action() {
            @Override
            public void run() throws Exception {
                list.add(100);
            }
        });
        
        assertEquals(Arrays.asList(1, 2, 3, 4, 5, 100), list);
    }

    @Test
    public void blockingSubscribeObserver() {
        final List<Object> list = new ArrayList<Object>();
        
        Flowable.range(1, 5)
        .subscribeOn(Schedulers.computation())
        .blockingSubscribe(new Subscriber<Object>() {

            @Override
            public void onSubscribe(Subscription d) {
                d.request(Long.MAX_VALUE);
            }

            @Override
            public void onNext(Object value) {
                list.add(value);
            }

            @Override
            public void onError(Throwable e) {
                list.add(e);
            }

            @Override
            public void onComplete() {
                list.add(100);
            }
            
        });
        
        assertEquals(Arrays.asList(1, 2, 3, 4, 5, 100), list);
    }

    @Test
    public void blockingSubscribeObserverError() {
        final List<Object> list = new ArrayList<Object>();
        
        final TestException ex = new TestException();
        
        Flowable.range(1, 5).concatWith(Flowable.<Integer>error(ex))
        .subscribeOn(Schedulers.computation())
        .blockingSubscribe(new Subscriber<Object>() {

            @Override
            public void onSubscribe(Subscription d) {
                d.request(Long.MAX_VALUE);
            }

            @Override
            public void onNext(Object value) {
                list.add(value);
            }

            @Override
            public void onError(Throwable e) {
                list.add(e);
            }

            @Override
            public void onComplete() {
                list.add(100);
            }
            
        });
        
        assertEquals(Arrays.asList(1, 2, 3, 4, 5, ex), list);
    }

    @Test(expected = TestException.class)
    public void blockingForEachThrows() {
        Flowable.just(1)
        .blockingForEach(new Consumer<Integer>() {
            @Override
            public void accept(Integer e) throws Exception {
                throw new TestException();
            }
        });
    }
    
    @Test(expected = NoSuchElementException.class)
    public void blockingFirstEmpty() {
        Flowable.empty().blockingFirst();
    }

    @Test(expected = NoSuchElementException.class)
    public void blockingLastEmpty() {
        Flowable.empty().blockingLast();
    }

    @Test
    public void blockingFirstNormal() {
        assertEquals(1, Flowable.just(1, 2).blockingFirst(3).intValue());
    }

    @Test
    public void blockingLastNormal() {
        assertEquals(2, Flowable.just(1, 2).blockingLast(3).intValue());
    }

}
