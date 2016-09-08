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
package rx.internal.operators;

import org.junit.*;

import rx.Observable;
import rx.exceptions.TestException;
import rx.functions.*;
import rx.observers.TestSubscriber;
import rx.subjects.PublishSubject;

public class OperatorMapPairTest {
    @Test
    public void castCrashUnsubscribes() {

        PublishSubject<Integer> ps = PublishSubject.create();

        TestSubscriber<Integer> ts = TestSubscriber.create();

        ps.flatMap(new Func1<Integer, Observable<Integer>>() {
            @Override
            public Observable<Integer> call(Integer t) {
                throw new TestException();
            }
        }, new Func2<Integer, Integer, Integer>() {
            @Override
            public Integer call(Integer t1, Integer t2) {
                return t1;
            }
        }).unsafeSubscribe(ts);

        Assert.assertTrue("Not subscribed?", ps.hasObservers());

        ps.onNext(1);

        Assert.assertFalse("Subscribed?", ps.hasObservers());

        ts.assertError(TestException.class);
    }
}
