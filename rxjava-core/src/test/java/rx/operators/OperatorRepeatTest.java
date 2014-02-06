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

import static org.junit.Assert.*;

import java.util.concurrent.atomic.AtomicInteger;

import org.junit.Test;

import rx.Observable;
import rx.Observable.OnSubscribe;
import rx.Observable.OnSubscribeFunc;
import rx.Observer;
import rx.Subscriber;
import rx.Subscription;
import rx.schedulers.Schedulers;
import rx.subscriptions.Subscriptions;
import rx.util.functions.Func1;

public class OperatorRepeatTest {

    @Test
    public void testRepetition() {
        int NUM = 10;
        final AtomicInteger count = new AtomicInteger();
        int value = Observable.create(new OnSubscribeFunc<Integer>() {

            @Override
            public Subscription onSubscribe(Observer<? super Integer> o) {
                o.onNext(count.incrementAndGet());
                o.onCompleted();
                return Subscriptions.empty();
            }
        }).repeat(Schedulers.computation()).take(NUM).toBlockingObservable().last();

        assertEquals(NUM, value);
    }

    @Test
    public void testRepeatTake() {
        Observable<Integer> xs = Observable.from(1, 2);
        Object[] ys = xs.repeat(Schedulers.newThread()).take(4).toList().toBlockingObservable().last().toArray();
        assertArrayEquals(new Object[] { 1, 2, 1, 2 }, ys);
    }

    @Test
    public void testNoStackOverFlow() {
        Observable.from(1).repeat(Schedulers.newThread()).take(100000).toBlockingObservable().last();
    }

    @Test
    public void testRepeatTakeWithSubscribeOn() throws InterruptedException {

        final AtomicInteger counter = new AtomicInteger();
        Observable<Integer> oi = Observable.create(new OnSubscribe<Integer>() {

            @Override
            public void call(Subscriber<? super Integer> sub) {
                System.out.println("invoked!");
                counter.incrementAndGet();
                sub.onNext(1);
                sub.onNext(2);
                sub.onCompleted();
            }
        }).subscribeOn(Schedulers.newThread());

        Object[] ys = oi.repeat(Schedulers.newThread()).map(new Func1<Integer, Integer>() {

            @Override
            public Integer call(Integer t1) {
                System.out.println("t1: " + t1);
                try {
                    Thread.sleep(50);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                return t1;
            }

        }).take(4).toList().toBlockingObservable().last().toArray();

        assertEquals(2, counter.get());
        assertArrayEquals(new Object[] { 1, 2, 1, 2 }, ys);
    }

}
