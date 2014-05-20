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

import static org.junit.Assert.assertEquals;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.Test;

import rx.Observable;
import rx.functions.Action1;
import rx.functions.Func1;

public class OperatorParallelTest {

    @Test
    public void testParallel() {
        int NUM = 1000;
        final AtomicInteger count = new AtomicInteger();
        final AtomicInteger innerCount = new AtomicInteger();
        Observable.range(1, NUM).parallel(
                new Func1<Observable<Integer>, Observable<Integer[]>>() {

                    @Override
                    public Observable<Integer[]> call(Observable<Integer> o) {
                        return o.map(new Func1<Integer, Integer[]>() {

                            @Override
                            public Integer[] call(Integer t) {
                                try {
                                    // randomize to try and force non-determinism
                                    // if we see these tests fail randomly then we have a problem with merging it all back together
                                    Thread.sleep((int) (Math.random() * 10));
                                } catch (InterruptedException e) {
                                    System.out.println("*********** error!!!!!!!");
                                    e.printStackTrace();
                                    // TODO why is this exception not being thrown?
                                    throw new RuntimeException(e);
                                }
                                //                                System.out.println("V: " + t  + " Thread: " + Thread.currentThread());
                                innerCount.incrementAndGet();
                                return new Integer[] { t, t * 99 };
                            }

                        });
                    }
                })
                .toBlocking().forEach(new Action1<Integer[]>() {

                    @Override
                    public void call(Integer[] v) {
                        count.incrementAndGet();
                        //                System.out.println("V: " + v[0] + " R: " + v[1] + " Thread: " + Thread.currentThread());
                    }

                });
        System.out.println("parallel test completed ----------");

        // just making sure we finish and get the number we expect
        assertEquals("innerCount", NUM, innerCount.get());
        assertEquals("finalCount", NUM, count.get());
    }

    @Test
    public void testParallelWithNestedAsyncWork() {
        int NUM = 20;
        final AtomicInteger count = new AtomicInteger();
        Observable.range(1, NUM).parallel(
                new Func1<Observable<Integer>, Observable<String>>() {

                    @Override
                    public Observable<String> call(Observable<Integer> o) {
                        return o.flatMap(new Func1<Integer, Observable<String>>() {

                            @Override
                            public Observable<String> call(Integer t) {
                                return Observable.from(String.valueOf(t)).delay(100, TimeUnit.MILLISECONDS);
                            }

                        });
                    }
                }).toBlocking().forEach(new Action1<String>() {

            @Override
            public void call(String v) {
                count.incrementAndGet();
            }

        });

        // just making sure we finish and get the number we expect
        assertEquals(NUM, count.get());
    }
}
