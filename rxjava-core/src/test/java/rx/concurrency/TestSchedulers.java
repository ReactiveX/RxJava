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
package rx.concurrency;

import static org.junit.Assert.*;

import org.junit.Test;

import rx.Observable;
import rx.util.functions.Action1;
import rx.util.functions.Func1;

public class TestSchedulers {

    @Test
    public void testComputationThreadPool1() {

        Observable<Integer> o1 = Observable.<Integer> from(1, 2, 3, 4, 5);
        Observable<Integer> o2 = Observable.<Integer> from(6, 7, 8, 9, 10);
        @SuppressWarnings("unchecked")
        Observable<String> o = Observable.<Integer> merge(o1, o2).map(new Func1<Integer, String>() {

            @Override
            public String call(Integer t) {
                assertTrue(Thread.currentThread().getName().startsWith("RxComputationThreadPool"));
                return "Value_" + t + "_Thread_" + Thread.currentThread().getName();
            }
        });

        o.subscribeOn(Schedulers.threadPoolForComputation()).forEach(new Action1<String>() {

            @Override
            public void call(String t) {
                System.out.println("t: " + t);
            }
        });

    }

    @Test
    public void testIOThreadPool1() {

        Observable<Integer> o1 = Observable.<Integer> from(1, 2, 3, 4, 5);
        Observable<Integer> o2 = Observable.<Integer> from(6, 7, 8, 9, 10);
        @SuppressWarnings("unchecked")
        Observable<String> o = Observable.<Integer> merge(o1, o2).map(new Func1<Integer, String>() {

            @Override
            public String call(Integer t) {
                assertTrue(Thread.currentThread().getName().startsWith("RxIOThreadPool"));
                return "Value_" + t + "_Thread_" + Thread.currentThread().getName();
            }
        });

        o.subscribeOn(Schedulers.threadPoolForIO()).forEach(new Action1<String>() {

            @Override
            public void call(String t) {
                System.out.println("t: " + t);
            }
        });

    }
}
