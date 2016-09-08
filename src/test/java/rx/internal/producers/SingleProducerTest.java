/**
 * Copyright 2016 Netflix, Inc.
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

package rx.internal.producers;

import org.junit.*;

import rx.Observable;
import rx.exceptions.TestException;
import rx.observers.*;

public class SingleProducerTest {

    @Test
    public void negativeRequestThrows() {
        SingleProducer<Integer> pa = new SingleProducer<Integer>(Subscribers.empty(), 1);
        try {
            pa.request(-99);
            Assert.fail("Failed to throw on invalid request amount");
        } catch (IllegalArgumentException ex) {
            Assert.assertEquals("n >= 0 required", ex.getMessage());
        }
    }

    @Test
    public void cancelBeforeOnNext() {
        TestSubscriber<Integer> ts = new TestSubscriber<Integer>();
        SingleProducer<Integer> pa = new SingleProducer<Integer>(ts, 1);

        ts.unsubscribe();

        pa.request(1);

        ts.assertNoValues();
        ts.assertNoErrors();
        ts.assertNotCompleted();
    }

    @Test
    public void cancelAfterOnNext() {
        TestSubscriber<Integer> ts = new TestSubscriber<Integer>();
        Observable.just(1).take(1).subscribe(ts);

        ts.assertValue(1);
        ts.assertNoErrors();
        ts.assertCompleted();
    }

    @Test
    public void onNextThrows() {
        TestSubscriber<Integer> ts = new TestSubscriber<Integer>(0) {
            @Override
            public void onNext(Integer t) {
                throw new TestException();
            }
        };
        SingleProducer<Integer> sp = new SingleProducer<Integer>(ts, 1);

        sp.request(1);

        ts.assertNoValues();
        ts.assertError(TestException.class);
        ts.assertNotCompleted();
    }

}
