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
package rx.jmh;

import org.openjdk.jmh.annotations.Param;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.logic.BlackHole;

import rx.Observable;
import rx.Observable.OnSubscribe;
import rx.Observer;
import rx.Subscriber;
import rx.observers.TestSubscriber;

/**
 * Exposes an Observable and Observer that increments n Integers and consumes them in a Blackhole.
 */
public abstract class InputWithIncrementingIntegerBase {
    
    protected abstract int getSize();

    public Observable<Integer> observable;
    private BlackHole bh;

    @Setup
    public void setup(final BlackHole bh) {
        this.bh = bh;
        observable = Observable.create(new OnSubscribe<Integer>() {
            @Override
            public void call(Subscriber<? super Integer> o) {
                for (int value = 0; value < getSize(); value++) {
                    if (o.isUnsubscribed())
                        return;
                    o.onNext(value);
                }
                o.onCompleted();
            }
        });
    }

    public TestSubscriber<Integer> newSubscriber() {
        return new TestSubscriber<Integer>(new Observer<Integer>() {
            @Override
            public void onCompleted() {
            }

            @Override
            public void onError(Throwable e) {
                throw new RuntimeException(e);
            }

            @Override
            public void onNext(Integer value) {
                bh.consume(value);
            }
        });
    }
}
