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
package rx.subjects;

import com.google.j2objc.annotations.AutoreleasePool;


import static org.junit.Assert.assertEquals;

import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.*;

import co.touchlab.doppel.testing.DoppelHacks;
import rx.*;
import rx.Observable;
import rx.Observable.OnSubscribe;
import rx.Observer;
import rx.functions.*;
import rx.observers.TestSubscriber;
import rx.schedulers.Schedulers;

public class ReplaySubjectBoundedConcurrencyTest {


    @Test
    public void testReplaySubjectEmissionSubscriptionRace() throws Exception {
        Scheduler s = Schedulers.io();
        Scheduler.Worker worker = Schedulers.io().createWorker();
        try {
            for (@AutoreleasePool int i = 0; i < 5000; i++) {
                if (i % 100 == 0) {
                    System.out.println(i);
                }
                final ReplaySubject<Object> rs = ReplaySubject.createWithSize(2);

                final CountDownLatch finish = new CountDownLatch(1);
                final CountDownLatch start = new CountDownLatch(1);

                worker.schedule(new Action0() {
                    @Override
                    public void call() {
                        try {
                            start.await();
                        } catch (Exception e1) {
                            e1.printStackTrace();
                        }
                        rs.onNext(1);
                    }
                });

                final AtomicReference<Object> o = new AtomicReference<Object>();

                rs.subscribeOn(s).observeOn(Schedulers.io())
                .subscribe(new Observer<Object>() {

                    @Override
                    public void onCompleted() {
                        o.set(-1);
                        finish.countDown();
                    }

                    @Override
                    public void onError(Throwable e) {
                        o.set(e);
                        finish.countDown();
                    }

                    @Override
                    public void onNext(Object t) {
                        o.set(t);
                        finish.countDown();
                    }

                });
                start.countDown();

                if (!finish.await(5, TimeUnit.SECONDS)) {
                    System.out.println(o.get());
                    System.out.println(rs.hasObservers());
                    rs.onCompleted();
                    Assert.fail("Timeout @ " + i);
                    break;
                } else {
                    Assert.assertEquals(1, o.get());
                    worker.schedule(new Action0() {
                        @Override
                        public void call() {
                            rs.onCompleted();
                        }
                    });
                }
            }
        } finally {
            worker.unsubscribe();
        }
    }


}