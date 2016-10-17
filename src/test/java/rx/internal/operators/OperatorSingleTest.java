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

import org.junit.Test;
import org.mockito.InOrder;

import co.touchlab.doppel.testing.MockGen;
import rx.Observable;
import rx.Subscriber;
import rx.doppl.mock.MSubscriber;


import static org.mockito.Matchers.isA;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;

@MockGen(classes = {"rx.doppl.mock.MObserver", "rx.doppl.mock.MSubscriber"})
public class OperatorSingleTest {

    @Test
    public void testSingleWithBackpressure() {
        Observable<Integer> observable = Observable.just(1, 2).single();

        Subscriber<Integer> subscriber = new MSubscriber<Integer>() {

            @Override
            public void onStart() {
                request(1);
            }

            @Override
            public void onCompleted() {

            }

            @Override
            public void onError(Throwable e) {

            }

            @Override
            public void onNext(Integer integer) {
                request(1);
            }
        };
        observable.subscribe(subscriber);

//        InOrder inOrder = inOrder(subscriber);
//        inOrder.verify(subscriber, times(1)).onError(isA(IllegalArgumentException.class));
//        inOrder.verifyNoMoreInteractions();
    }
}
