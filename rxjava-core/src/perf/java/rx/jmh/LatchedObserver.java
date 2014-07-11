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

import java.util.concurrent.CountDownLatch;

import org.openjdk.jmh.infra.Blackhole;

import rx.Observer;

public class LatchedObserver<T> implements Observer<T> {

    public CountDownLatch latch = new CountDownLatch(1);
    private final Blackhole bh;

    public LatchedObserver(Blackhole bh) {
        this.bh = bh;
    }

    @Override
    public void onCompleted() {
        latch.countDown();
    }

    @Override
    public void onError(Throwable e) {
        latch.countDown();
    }

    @Override
    public void onNext(T t) {
        bh.consume(t);
    }

}
