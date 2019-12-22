/**
 * Copyright (c) 2016-present, RxJava Contributors.
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

package io.reactivex.rxjava3.core;

import java.lang.management.ManagementFactory;
import java.util.concurrent.Callable;

import org.reactivestreams.Subscription;

import io.reactivex.rxjava3.disposables.Disposable;
import io.reactivex.rxjava3.functions.*;

/**
 * Measure various prepared flows about their memory usage and print the result
 * in a JMH compatible format; run {@link #main(String[])}.
 */
public final class MemoryPerf {

    private MemoryPerf() { }

    static long memoryUse() {
        return ManagementFactory.getMemoryMXBean().getHeapMemoryUsage().getUsed();
    }

    static final class MyRx2Subscriber implements FlowableSubscriber<Object> {

        org.reactivestreams.Subscription upstream;

        @Override
        public void onSubscribe(Subscription s) {
            this.upstream = s;
        }

        @Override
        public void onComplete() {

        }

        @Override
        public void onError(Throwable e) {

        }

        @Override
        public void onNext(Object t) {

        }
    }

    static final class MyRx2Observer implements io.reactivex.rxjava3.core.Observer<Object>, io.reactivex.rxjava3.core.SingleObserver<Object>,
    io.reactivex.rxjava3.core.MaybeObserver<Object>, io.reactivex.rxjava3.core.CompletableObserver {

        Disposable upstream;

        @Override
        public void onSubscribe(Disposable d) {
            this.upstream = d;
        }

        @Override
        public void onComplete() {

        }

        @Override
        public void onError(Throwable e) {

        }

        @Override
        public void onNext(Object t) {

        }

        @Override
        public void onSuccess(Object value) {

        }
    }
    static <U> void checkMemory(Callable<U> item, String name, String typeLib) throws Exception {
        checkMemory(item, name, typeLib, 1000000);
    }

    static <U> void checkMemory(Callable<U> item, String name, String typeLib, int n) throws Exception {
        // make sure classes are initialized
        item.call();

        Object[] array = new Object[n];

        Thread.sleep(100);
        System.gc();
        Thread.sleep(100);

        long before = memoryUse();

        for (int i = 0; i < n; i++) {
            array[i] = item.call();
        }

        Thread.sleep(100);
        System.gc();
        Thread.sleep(100);

        long after = memoryUse();

        double use = Math.max(0.0, (after - before) / 1024.0 / 1024.0);

        System.out.print(name);
        System.out.print(" ");
        System.out.print(typeLib);
        System.out.print("     thrpt ");
        System.out.print(n);
        System.out.printf("           %.3f  0.000 MB%n", use);

        if (array.hashCode() == 1) {
            System.out.print("");
        }

        array = null;
        item = null;

        Thread.sleep(100);
        System.gc();
        Thread.sleep(100);
    }

    public static void main(String[] args) throws Exception {

        System.out.println("Benchmark  (lib-type)   Mode  Cnt       Score       Error  Units");

        // ---------------------------------------------------------------------------------------------------------------------

        checkMemory(new Callable<Object>() {
            @Override
            public Object call() throws Exception {
                return io.reactivex.rxjava3.core.Observable.just(1);
            }
        }, "just", "Rx2Observable");

        checkMemory(new Callable<Object>() {
            @Override
            public Object call() throws Exception {
                return io.reactivex.rxjava3.core.Observable.range(1, 10);
            }
        }, "range", "Rx2Observable");

        checkMemory(new Callable<Object>() {
            @Override
            public Object call() throws Exception {
                return io.reactivex.rxjava3.core.Observable.empty();
            }
        }, "empty", "Rx2Observable");

        checkMemory(new Callable<Object>() {
            @Override
            public Object call() throws Exception {
                return io.reactivex.rxjava3.core.Observable.fromCallable(new Callable<Object>() {
                    @Override
                    public Object call() throws Exception {
                        return 1;
                    }
                });
            }
        }, "fromCallable", "Rx2Observable");

        checkMemory(new Callable<Object>() {
            @Override
            public Object call() throws Exception {
                return new MyRx2Observer();
            }
        }, "consumer", "Rx2Observable");

        checkMemory(new Callable<Object>() {
            @Override
            public Object call() throws Exception {
                return new io.reactivex.rxjava3.observers.TestObserver<>();
            }
        }, "test-consumer", "Rx2Observable");

        checkMemory(new Callable<Object>() {
            @Override
            public Object call() throws Exception {
                return io.reactivex.rxjava3.core.Observable.just(1).subscribeWith(new MyRx2Observer());
            }
        }, "just+consumer", "Rx2Observable");

        checkMemory(new Callable<Object>() {
            @Override
            public Object call() throws Exception {
                return io.reactivex.rxjava3.core.Observable.range(1, 10).subscribeWith(new MyRx2Observer());
            }
        }, "range+consumer", "Rx2Observable");

        checkMemory(new Callable<Object>() {
            @Override
            public Object call() throws Exception {
                return io.reactivex.rxjava3.core.Observable.range(1, 10).map(new Function<Integer, Object>() {
                    @Override
                    public Object apply(Integer v) throws Exception {
                        return v;
                    }
                }).subscribeWith(new MyRx2Observer());
            }
        }, "range+map+consumer", "Rx2Observable");

        checkMemory(new Callable<Object>() {
            @Override
            public Object call() throws Exception {
                return io.reactivex.rxjava3.core.Observable.range(1, 10).map(new Function<Integer, Object>() {
                    @Override
                    public Object apply(Integer v) throws Exception {
                        return v;
                    }
                }).filter(new Predicate<Object>() {
                    @Override
                    public boolean test(Object v) throws Exception {
                        return true;
                    }
                }).subscribeWith(new MyRx2Observer());
            }
        }, "range+map+filter+consumer", "Rx2Observable");

        checkMemory(new Callable<Object>() {
            @Override
            public Object call() throws Exception {
                return io.reactivex.rxjava3.core.Observable.range(1, 10).subscribeOn(io.reactivex.rxjava3.schedulers.Schedulers.computation()).subscribeWith(new MyRx2Observer());
            }
        }, "range+subscribeOn+consumer", "Rx2Observable");

        checkMemory(new Callable<Object>() {
            @Override
            public Object call() throws Exception {
                return io.reactivex.rxjava3.core.Observable.range(1, 10).observeOn(io.reactivex.rxjava3.schedulers.Schedulers.computation()).subscribeWith(new MyRx2Observer());
            }
        }, "range+observeOn+consumer", "Rx2Observable");

        checkMemory(new Callable<Object>() {
            @Override
            public Object call() throws Exception {
                return io.reactivex.rxjava3.core.Observable.range(1, 10).subscribeOn(io.reactivex.rxjava3.schedulers.Schedulers.computation()).observeOn(io.reactivex.rxjava3.schedulers.Schedulers.computation()).subscribeWith(new MyRx2Observer());
            }
        }, "range+subscribeOn+observeOn+consumer", "Rx2Observable");

        checkMemory(new Callable<Object>() {
            @Override
            public Object call() throws Exception {
                return io.reactivex.rxjava3.subjects.AsyncSubject.create();
            }
        }, "Async", "Rx2Observable");

        checkMemory(new Callable<Object>() {
            @Override
            public Object call() throws Exception {
                return io.reactivex.rxjava3.subjects.PublishSubject.create();
            }
        }, "Publish", "Rx2Observable");

        checkMemory(new Callable<Object>() {
            @Override
            public Object call() throws Exception {
                return io.reactivex.rxjava3.subjects.ReplaySubject.create();
            }
        }, "Replay", "Rx2Observable");

        checkMemory(new Callable<Object>() {
            @Override
            public Object call() throws Exception {
                return io.reactivex.rxjava3.subjects.BehaviorSubject.create();
            }
        }, "Behavior", "Rx2Observable");

        checkMemory(new Callable<Object>() {
            @Override
            public Object call() throws Exception {
                return io.reactivex.rxjava3.subjects.UnicastSubject.create();
            }
        }, "Unicast", "Rx2Observable");

        checkMemory(new Callable<Object>() {
            @Override
            public Object call() throws Exception {
                return io.reactivex.rxjava3.subjects.AsyncSubject.create().subscribeWith(new MyRx2Observer());
            }
        }, "Async+consumer", "Rx2Observable");

        checkMemory(new Callable<Object>() {
            @Override
            public Object call() throws Exception {
                return io.reactivex.rxjava3.subjects.PublishSubject.create().subscribeWith(new MyRx2Observer());
            }
        }, "Publish+consumer", "Rx2Observable");

        checkMemory(new Callable<Object>() {
            @Override
            public Object call() throws Exception {
                return io.reactivex.rxjava3.subjects.ReplaySubject.create().subscribeWith(new MyRx2Observer());
            }
        }, "Replay+consumer", "Rx2Observable");

        checkMemory(new Callable<Object>() {
            @Override
            public Object call() throws Exception {
                return io.reactivex.rxjava3.subjects.BehaviorSubject.create().subscribeWith(new MyRx2Observer());
            }
        }, "Behavior+consumer", "Rx2Observable");

        checkMemory(new Callable<Object>() {
            @Override
            public Object call() throws Exception {
                return io.reactivex.rxjava3.subjects.UnicastSubject.create().subscribeWith(new MyRx2Observer());
            }
        }, "Unicast+consumer", "Rx2Observable");

        // ---------------------------------------------------------------------------------------------------------------------

        checkMemory(new Callable<Object>() {
            @Override
            public Object call() throws Exception {
                return io.reactivex.rxjava3.core.Flowable.just(1);
            }
        }, "just", "Rx2Flowable");

        checkMemory(new Callable<Object>() {
            @Override
            public Object call() throws Exception {
                return io.reactivex.rxjava3.core.Flowable.range(1, 10);
            }
        }, "range", "Rx2Flowable");

        checkMemory(new Callable<Object>() {
            @Override
            public Object call() throws Exception {
                return io.reactivex.rxjava3.core.Flowable.empty();
            }
        }, "empty", "Rx2Flowable");

        checkMemory(new Callable<Object>() {
            @Override
            public Object call() throws Exception {
                return io.reactivex.rxjava3.core.Flowable.empty();
            }
        }, "empty", "Rx2Flowable", 10000000);

        checkMemory(new Callable<Object>() {
            @Override
            public Object call() throws Exception {
                return io.reactivex.rxjava3.core.Flowable.fromCallable(new Callable<Object>() {
                    @Override
                    public Object call() throws Exception {
                        return 1;
                    }
                });
            }
        }, "fromCallable", "Rx2Flowable");

        checkMemory(new Callable<Object>() {
            @Override
            public Object call() throws Exception {
                return new MyRx2Subscriber();
            }
        }, "consumer", "Rx2Flowable");

        checkMemory(new Callable<Object>() {
            @Override
            public Object call() throws Exception {
                return new io.reactivex.rxjava3.observers.TestObserver<>();
            }
        }, "test-consumer", "Rx2Flowable");

        checkMemory(new Callable<Object>() {
            @Override
            public Object call() throws Exception {
                return io.reactivex.rxjava3.core.Flowable.just(1).subscribeWith(new MyRx2Subscriber());
            }
        }, "just+consumer", "Rx2Flowable");

        checkMemory(new Callable<Object>() {
            @Override
            public Object call() throws Exception {
                return io.reactivex.rxjava3.core.Flowable.range(1, 10).subscribeWith(new MyRx2Subscriber());
            }
        }, "range+consumer", "Rx2Flowable");

        checkMemory(new Callable<Object>() {
            @Override
            public Object call() throws Exception {
                return io.reactivex.rxjava3.core.Flowable.range(1, 10).map(new Function<Integer, Object>() {
                    @Override
                    public Object apply(Integer v) throws Exception {
                        return v;
                    }
                }).subscribeWith(new MyRx2Subscriber());
            }
        }, "range+map+consumer", "Rx2Flowable");

        checkMemory(new Callable<Object>() {
            @Override
            public Object call() throws Exception {
                return io.reactivex.rxjava3.core.Flowable.range(1, 10).map(new Function<Integer, Object>() {
                    @Override
                    public Object apply(Integer v) throws Exception {
                        return v;
                    }
                }).filter(new Predicate<Object>() {
                    @Override
                    public boolean test(Object v) throws Exception {
                        return true;
                    }
                }).subscribeWith(new MyRx2Subscriber());
            }
        }, "range+map+filter+consumer", "Rx2Flowable");

        checkMemory(new Callable<Object>() {
            @Override
            public Object call() throws Exception {
                return io.reactivex.rxjava3.core.Flowable.range(1, 10).subscribeOn(io.reactivex.rxjava3.schedulers.Schedulers.computation()).subscribeWith(new MyRx2Subscriber());
            }
        }, "range+subscribeOn+consumer", "Rx2Flowable");

        checkMemory(new Callable<Object>() {
            @Override
            public Object call() throws Exception {
                return io.reactivex.rxjava3.core.Flowable.range(1, 10).observeOn(io.reactivex.rxjava3.schedulers.Schedulers.computation()).subscribeWith(new MyRx2Subscriber());
            }
        }, "range+observeOn+consumer", "Rx2Flowable");

        checkMemory(new Callable<Object>() {
            @Override
            public Object call() throws Exception {
                return io.reactivex.rxjava3.core.Flowable.range(1, 10).subscribeOn(io.reactivex.rxjava3.schedulers.Schedulers.computation()).observeOn(io.reactivex.rxjava3.schedulers.Schedulers.computation()).subscribeWith(new MyRx2Subscriber());
            }
        }, "range+subscribeOn+observeOn+consumer", "Rx2Flowable");

        checkMemory(new Callable<Object>() {
            @Override
            public Object call() throws Exception {
                return io.reactivex.rxjava3.processors.AsyncProcessor.create();
            }
        }, "Async", "Rx2Flowable");

        checkMemory(new Callable<Object>() {
            @Override
            public Object call() throws Exception {
                return io.reactivex.rxjava3.processors.PublishProcessor.create();
            }
        }, "Publish", "Rx2Flowable");

        checkMemory(new Callable<Object>() {
            @Override
            public Object call() throws Exception {
                return io.reactivex.rxjava3.processors.ReplayProcessor.create();
            }
        }, "Replay", "Rx2Flowable");

        checkMemory(new Callable<Object>() {
            @Override
            public Object call() throws Exception {
                return io.reactivex.rxjava3.processors.BehaviorProcessor.create();
            }
        }, "Behavior", "Rx2Flowable");

        checkMemory(new Callable<Object>() {
            @Override
            public Object call() throws Exception {
                return io.reactivex.rxjava3.processors.UnicastProcessor.create();
            }
        }, "Unicast", "Rx2Flowable");

        checkMemory(new Callable<Object>() {
            @Override
            public Object call() throws Exception {
                return io.reactivex.rxjava3.processors.AsyncProcessor.create().subscribeWith(new MyRx2Subscriber());
            }
        }, "Async+consumer", "Rx2Flowable");

        checkMemory(new Callable<Object>() {
            @Override
            public Object call() throws Exception {
                return io.reactivex.rxjava3.processors.PublishProcessor.create().subscribeWith(new MyRx2Subscriber());
            }
        }, "Publish+consumer", "Rx2Flowable");

        checkMemory(new Callable<Object>() {
            @Override
            public Object call() throws Exception {
                return io.reactivex.rxjava3.processors.ReplayProcessor.create().subscribeWith(new MyRx2Subscriber());
            }
        }, "Replay+consumer", "Rx2Flowable");

        checkMemory(new Callable<Object>() {
            @Override
            public Object call() throws Exception {
                return io.reactivex.rxjava3.processors.BehaviorProcessor.create().subscribeWith(new MyRx2Subscriber());
            }
        }, "Behavior+consumer", "Rx2Flowable");

        checkMemory(new Callable<Object>() {
            @Override
            public Object call() throws Exception {
                return io.reactivex.rxjava3.processors.UnicastProcessor.create().subscribeWith(new MyRx2Subscriber());
            }
        }, "Unicast+consumer", "Rx2Flowable");

        // ---------------------------------------------------------------------------------------------------------------------
    }
}
