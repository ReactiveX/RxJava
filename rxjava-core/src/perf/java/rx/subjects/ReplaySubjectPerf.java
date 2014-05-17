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

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicLong;

import org.openjdk.jmh.annotations.GenerateMicroBenchmark;
import org.openjdk.jmh.annotations.Param;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.logic.BlackHole;

import rx.Observer;

/**
 * Benchmarks the {@link ReplaySubject}.
 */
public class ReplaySubjectPerf {

    @State(Scope.Thread)
    public static class Input {
        @Param({ "1", "512", "1024", "1048576" })
        public int nextRuns;
    }

    @GenerateMicroBenchmark
    public void subscribeBeforeEvents(final Input input, final BlackHole bh) throws Exception {
        ReplaySubject<Object> subject = ReplaySubject.create();
        final CountDownLatch latch = new CountDownLatch(1);
        final AtomicLong sum = new AtomicLong();

        subject.subscribe(new Observer<Object>() {
            @Override
            public void onCompleted() {
                latch.countDown();
            }

            @Override
            public void onError(Throwable e) {
            }

            @Override
            public void onNext(Object o) {
                sum.incrementAndGet();
            }
        });
        for (int i = 0; i < input.nextRuns; i++) {
            subject.onNext("Response");
        }
        subject.onCompleted();
        latch.await();
        bh.consume(sum);
    }

    @GenerateMicroBenchmark
    public void subscribeAfterEvents(final Input input, final BlackHole bh) throws Exception {
        ReplaySubject<Object> subject = ReplaySubject.create();
        final CountDownLatch latch = new CountDownLatch(1);
        final AtomicLong sum = new AtomicLong();

        for (int i = 0; i < input.nextRuns; i++) {
            subject.onNext("Response");
        }
        subject.onCompleted();

        subject.subscribe(new Observer<Object>() {
            @Override
            public void onCompleted() {
                latch.countDown();
            }

            @Override
            public void onError(Throwable e) {
            }

            @Override
            public void onNext(Object o) {
                sum.incrementAndGet();
            }
        });
        latch.await();
        bh.consume(sum);
    }

}
