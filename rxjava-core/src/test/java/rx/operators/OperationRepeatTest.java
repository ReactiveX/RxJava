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
package rx.operators;

import static org.junit.Assert.*;

import java.util.concurrent.atomic.AtomicInteger;

import org.junit.Test;

import rx.Observable;
import rx.Observable.OnSubscribeFunc;
import rx.Observer;
import rx.Subscription;
import rx.schedulers.Schedulers;
import rx.subscriptions.Subscriptions;

public class OperationRepeatTest {

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
        }).repeat(Schedulers.threadPoolForComputation()).take(NUM).toBlockingObservable().last();

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

}
