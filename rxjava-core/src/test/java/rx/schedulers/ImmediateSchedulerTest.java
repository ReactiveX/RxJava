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
package rx.schedulers;

import static org.junit.Assert.assertTrue;

import org.junit.Test;

import rx.Observable;
import rx.Scheduler;
import rx.functions.Action1;
import rx.functions.Func1;

public class ImmediateSchedulerTest extends AbstractSchedulerTests {

    @Override
    protected Scheduler getScheduler() {
        return Schedulers.immediate();
    }

    @Override
    @Test
    public final void testNestedActions() {
        // ordering of nested actions will not match other schedulers
        // because there is no reordering or concurrency with ImmediateScheduler
    }

    @Override
    @Test
    public final void testSequenceOfDelayedActions() {
        // ordering of nested actions will not match other schedulers
        // because there is no reordering or concurrency with ImmediateScheduler
    }

    @Override
    @Test
    public final void testMixOfDelayedAndNonDelayedActions() {
        // ordering of nested actions will not match other schedulers
        // because there is no reordering or concurrency with ImmediateScheduler
    }

    @Test
    public final void testMergeWithoutScheduler() {

        final String currentThreadName = Thread.currentThread().getName();

        Observable<Integer> o1 = Observable.<Integer> from(1, 2, 3, 4, 5);
        Observable<Integer> o2 = Observable.<Integer> from(6, 7, 8, 9, 10);
        Observable<String> o = Observable.<Integer> merge(o1, o2).map(new Func1<Integer, String>() {

            @Override
            public String call(Integer t) {
                assertTrue(Thread.currentThread().getName().equals(currentThreadName));
                return "Value_" + t + "_Thread_" + Thread.currentThread().getName();
            }
        });

        o.toBlocking().forEach(new Action1<String>() {

            @Override
            public void call(String t) {
                System.out.println("t: " + t);
            }
        });
    }

    @Test
    public final void testMergeWithImmediateScheduler1() {

        final String currentThreadName = Thread.currentThread().getName();

        Observable<Integer> o1 = Observable.<Integer> from(1, 2, 3, 4, 5);
        Observable<Integer> o2 = Observable.<Integer> from(6, 7, 8, 9, 10);
        Observable<String> o = Observable.<Integer> merge(o1, o2).subscribeOn(Schedulers.immediate()).map(new Func1<Integer, String>() {

            @Override
            public String call(Integer t) {
                assertTrue(Thread.currentThread().getName().equals(currentThreadName));
                return "Value_" + t + "_Thread_" + Thread.currentThread().getName();
            }
        });

        o.toBlocking().forEach(new Action1<String>() {

            @Override
            public void call(String t) {
                System.out.println("t: " + t);
            }
        });
    }
}
