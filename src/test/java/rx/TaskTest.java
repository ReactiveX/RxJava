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
package rx;

import static org.junit.Assert.assertEquals;

import java.util.Arrays;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.Test;

import rx.Task.OnExecute;
import rx.Task.TaskObserver;
import rx.functions.Func1;
import rx.functions.Func2;
import rx.observers.TestSubscriber;
import rx.schedulers.Schedulers;

public class TaskTest {

    @Test
    public void testHelloWorld() {
        TestSubscriber<String> ts = new TestSubscriber<String>();
        Task.just("Hello World!").subscribe(ts);
        ts.assertReceivedOnNext(Arrays.asList("Hello World!"));
    }

    @Test
    public void testHelloWorld2() {
        final AtomicReference<String> v = new AtomicReference<String>();
        Task.just("Hello World!").execute(new TaskObserver<String>() {

            @Override
            public void onSuccess(String value) {
                v.set(value);
            }

            @Override
            public void onError(Throwable error) {

            }

        });
        assertEquals("Hello World!", v.get());
    }

    @Test
    public void testMap() {
        TestSubscriber<String> ts = new TestSubscriber<String>();
        Task.just("A")
                .map(new Func1<String, String>() {

                    @Override
                    public String call(String s) {
                        return s + "B";
                    }

                })
                .subscribe(ts);
        ts.assertReceivedOnNext(Arrays.asList("AB"));
    }

    @Test
    public void testZip() {
        TestSubscriber<String> ts = new TestSubscriber<String>();
        Task<String> a = Task.just("A");
        Task<String> b = Task.just("B");

        Task.zip(a, b, new Func2<String, String, String>() {

            @Override
            public String call(String a, String b) {
                return a + b;
            }

        })
                .subscribe(ts);
        ts.assertReceivedOnNext(Arrays.asList("AB"));
    }

    @Test
    public void testZipWith() {
        TestSubscriber<String> ts = new TestSubscriber<String>();

        Task.just("A").zipWith(Task.just("B"), new Func2<String, String, String>() {

            @Override
            public String call(String a, String b) {
                return a + b;
            }

        })
                .subscribe(ts);
        ts.assertReceivedOnNext(Arrays.asList("AB"));
    }

    @Test
    public void testMerge() {
        TestSubscriber<String> ts = new TestSubscriber<String>();
        Task<String> a = Task.just("A");
        Task<String> b = Task.just("B");

        Task.merge(a, b).subscribe(ts);
        ts.assertReceivedOnNext(Arrays.asList("A", "B"));
    }

    @Test
    public void testMergeWith() {
        TestSubscriber<String> ts = new TestSubscriber<String>();

        Task.just("A").mergeWith(Task.just("B")).subscribe(ts);
        ts.assertReceivedOnNext(Arrays.asList("A", "B"));
    }

    @Test
    public void testCreateSuccess() {
        TestSubscriber<String> ts = new TestSubscriber<String>();
        Task.create(new OnExecute<String>() {

            @Override
            public void call(TaskObserver<? super String> s) {
                s.onSuccess("Hello");
            }

        }).subscribe(ts);
        ts.assertReceivedOnNext(Arrays.asList("Hello"));
    }

    @Test
    public void testCreateError() {
        TestSubscriber<String> ts = new TestSubscriber<String>();
        Task.create(new OnExecute<String>() {

            @Override
            public void call(TaskObserver<? super String> s) {
                s.onError(new RuntimeException("fail"));
            }

        }).subscribe(ts);
        assertEquals(1, ts.getOnErrorEvents().size());
    }

    @Test
    public void testAsync() {
        TestSubscriber<String> ts = new TestSubscriber<String>();
        Task.just("Hello")
                .subscribeOn(Schedulers.io())
                .map(new Func1<String, String>() {

                    @Override
                    public String call(String v) {
                        System.out.println("SubscribeOn Thread: " + Thread.currentThread());
                        return v;
                    }

                })
                .observeOn(Schedulers.computation())
                .map(new Func1<String, String>() {

                    @Override
                    public String call(String v) {
                        System.out.println("ObserveOn Thread: " + Thread.currentThread());
                        return v;
                    }

                })
                .subscribe(ts);
        ts.awaitTerminalEvent();
        ts.assertReceivedOnNext(Arrays.asList("Hello"));
    }

    @Test
    public void testFlatMap() {
        TestSubscriber<String> ts = new TestSubscriber<String>();
        Task.just("Hello").flatMap(new Func1<String, Task<String>>() {

            @Override
            public Task<String> call(String s) {
                return Task.just(s + " World!").subscribeOn(Schedulers.computation());
            }

        }).subscribe(ts);
        ts.awaitTerminalEvent();
        ts.assertReceivedOnNext(Arrays.asList("Hello World!"));
    }
}
