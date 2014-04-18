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

import java.util.Arrays;

import org.junit.Test;

import rx.Observable;
import rx.exceptions.OnErrorThrowable;
import rx.functions.Func1;
import rx.observers.TestSubscriber;

public class OperatorOnErrorFlatMapTest {

    @Test
    public void ignoreErrorsAndContinueEmitting() {
        TestSubscriber<String> ts = new TestSubscriber<String>();
        Observable.from(1, 2, 3, 4, 5, 6).map(new Func1<Integer, String>() {

            @Override
            public String call(Integer v) {
                if (v < 2 || v > 5) {
                    return "Value=" + v;
                }
                throw new RuntimeException("error in map function: " + v);
            }

        }).onErrorFlatMap(new Func1<OnErrorThrowable, Observable<String>>() {

            @Override
            public Observable<String> call(OnErrorThrowable t) {
                return Observable.empty();
            }

        }).subscribe(ts);

        ts.assertTerminalEvent();
        System.out.println(ts.getOnErrorEvents());
        assertEquals(0, ts.getOnErrorEvents().size());
        System.out.println(ts.getOnNextEvents());
        ts.assertReceivedOnNext(Arrays.asList("Value=1", "Value=6"));
    }

    @Test
    public void spliceAndContinueEmitting() {
        TestSubscriber<String> ts = new TestSubscriber<String>();
        Observable.from(1, 2, 3, 4, 5, 6).map(new Func1<Integer, String>() {

            @Override
            public String call(Integer v) {
                if (v < 2 || v > 5) {
                    return "Value=" + v;
                }
                throw new RuntimeException("error in map function: " + v);
            }

        }).onErrorFlatMap(new Func1<OnErrorThrowable, Observable<String>>() {

            @Override
            public Observable<String> call(OnErrorThrowable t) {
                return Observable.from("Error=" + t.getValue());
            }

        }).subscribe(ts);

        ts.assertTerminalEvent();
        System.out.println(ts.getOnErrorEvents());
        assertEquals(0, ts.getOnErrorEvents().size());
        System.out.println(ts.getOnNextEvents());
        ts.assertReceivedOnNext(Arrays.asList("Value=1", "Error=2", "Error=3", "Error=4", "Error=5", "Value=6"));
    }

}
