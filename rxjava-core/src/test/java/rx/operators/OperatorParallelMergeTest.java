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
import static org.junit.Assert.assertTrue;

import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

import org.junit.Test;

import rx.Observable;
import rx.functions.Action1;
import rx.functions.Func1;
import rx.schedulers.Schedulers;
import rx.subjects.PublishSubject;

public class OperatorParallelMergeTest {

    @Test
    public void testParallelMerge() {
        PublishSubject<String> p1 = PublishSubject.<String> create();
        PublishSubject<String> p2 = PublishSubject.<String> create();
        PublishSubject<String> p3 = PublishSubject.<String> create();
        PublishSubject<String> p4 = PublishSubject.<String> create();

        Observable<Observable<String>> fourStreams = Observable.<Observable<String>> from(p1, p2, p3, p4);

        Observable<Observable<String>> twoStreams = Observable.parallelMerge(fourStreams, 2);
        Observable<Observable<String>> threeStreams = Observable.parallelMerge(fourStreams, 3);

        List<? super Observable<String>> fourList = fourStreams.toList().toBlocking().last();
        List<? super Observable<String>> threeList = threeStreams.toList().toBlocking().last();
        List<? super Observable<String>> twoList = twoStreams.toList().toBlocking().last();

        System.out.println("two list: " + twoList);
        System.out.println("three list: " + threeList);
        System.out.println("four list: " + fourList);

        assertEquals(4, fourList.size());
        assertEquals(3, threeList.size());
        assertEquals(2, twoList.size());
    }

    @Test
    public void testNumberOfThreads() {
        final ConcurrentHashMap<Long, Long> threads = new ConcurrentHashMap<Long, Long>();
        // parallelMerge into 3 streams and observeOn for each
        // we expect 3 threads in the output
        Observable.parallelMerge(getStreams(), 3)
                .flatMap(new Func1<Observable<String>, Observable<String>>() {

                    @Override
                    public Observable<String> call(Observable<String> o) {
                        // for each of the parallel 
                        return o.observeOn(Schedulers.newThread());
                    }
                })
                .toBlocking().forEach(new Action1<String>() {

                    @Override
                    public void call(String o) {
                        System.out.println("o: " + o + " Thread: " + Thread.currentThread().getId());
                        threads.put(Thread.currentThread().getId(), Thread.currentThread().getId());
                    }
                });

        assertTrue(threads.keySet().size() <= 3); // can be less than since merge doesn't block threads and may not use all of them
    }

    @Test
    public void testNumberOfThreadsOnScheduledMerge() {
        final ConcurrentHashMap<Long, Long> threads = new ConcurrentHashMap<Long, Long>();

        // now we parallelMerge into 3 streams and observeOn for each
        // we expect 3 threads in the output
        Observable.merge(Observable.parallelMerge(getStreams(), 3, Schedulers.newThread()))
                .toBlocking().forEach(new Action1<String>() {

                    @Override
                    public void call(String o) {
                        System.out.println("o: " + o + " Thread: " + Thread.currentThread().getId());
                        threads.put(Thread.currentThread().getId(), Thread.currentThread().getId());
                    }
                });

        assertTrue(threads.keySet().size() <= 3); // can be less than since merge doesn't block threads and may not use all of them
    }

    private static Observable<Observable<String>> getStreams() {
        return Observable.range(0, 10).map(new Func1<Integer, Observable<String>>() {

            @Override
            public Observable<String> call(final Integer i) {
                return Observable.interval(10, TimeUnit.MILLISECONDS).map(new Func1<Long, String>() {

                    @Override
                    public String call(Long l) {
                        return "Stream " + i + "  Value: " + l;
                    }
                }).take(5);
            }
        });
    }
}
