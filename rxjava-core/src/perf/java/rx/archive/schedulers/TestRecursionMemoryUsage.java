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
package rx.archive.schedulers;

import rx.Observable;
import rx.Observable.OnSubscribe;
import rx.Scheduler;
import rx.Scheduler.Recurse;
import rx.Subscriber;
import rx.functions.Action1;
import rx.schedulers.Schedulers;

/**
 * Used for manual testing of memory leaks with recursive schedulers.
 * 
 */
public class TestRecursionMemoryUsage {

    public static void main(String args[]) {
        usingFunc2(Schedulers.newThread());
        usingAction0(Schedulers.newThread());

        usingFunc2(Schedulers.currentThread());
        usingAction0(Schedulers.currentThread());

        usingFunc2(Schedulers.computation());
        usingAction0(Schedulers.computation());

        System.exit(0);
    }

    protected static void usingFunc2(final Scheduler scheduler) {
        System.out.println("************ usingFunc2: " + scheduler);
        Observable.create(new OnSubscribe<Long>() {

            @Override
            public void call(final Subscriber<? super Long> o) {
                o.add(scheduler.schedule(new Action1<Recurse>() {
                    long i = 0;

                    @Override
                    public void call(Recurse inner) {
                        i++;
                        if (i % 500000 == 0) {
                            System.out.println(i + "  Total Memory: "
                                    + Runtime.getRuntime().totalMemory()
                                    + "  Free: "
                                    + Runtime.getRuntime().freeMemory());
                            o.onNext(i);
                        }
                        if (i == 100000000L) {
                            o.onCompleted();
                            return;
                        }

                        inner.schedule();
                    }
                }));
            }
        }).toBlockingObservable().last();
    }

    protected static void usingAction0(final Scheduler scheduler) {
        System.out.println("************ usingAction0: " + scheduler);
        Observable.create(new OnSubscribe<Long>() {

            @Override
            public void call(final Subscriber<? super Long> o) {
                o.add(scheduler.schedule(new Action1<Recurse>() {

                    private long i = 0;

                    @Override
                    public void call(Recurse inner) {
                        i++;
                        if (i % 500000 == 0) {
                            System.out.println(i + "  Total Memory: "
                                    + Runtime.getRuntime().totalMemory()
                                    + "  Free: "
                                    + Runtime.getRuntime().freeMemory());
                            o.onNext(i);
                        }
                        if (i == 100000000L) {
                            o.onCompleted();
                            return;
                        }
                        inner.schedule();
                    }
                }));
            }
        }).toBlockingObservable().last();
    }
}
