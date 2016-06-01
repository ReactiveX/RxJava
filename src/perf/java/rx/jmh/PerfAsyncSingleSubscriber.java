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
package rx.jmh;

import java.util.concurrent.CountDownLatch;

import org.openjdk.jmh.infra.Blackhole;

import rx.SingleSubscriber;

/**
 * A SingleSubscriber implementation for asynchronous benchmarks that sends
 * the onSuccess and onError signals to a JMH Blackhole.
 * <p>
 * Use {@code sleepAwait} or {@code spinAwait}.
 */
public final class PerfAsyncSingleSubscriber extends SingleSubscriber<Object> {
    final Blackhole bh;
    
    final CountDownLatch cdl;
    
    public PerfAsyncSingleSubscriber(Blackhole bh) {
        this.bh = bh;
        this.cdl = new CountDownLatch(1);
    }
    
    @Override
    public void onSuccess(Object value) {
        bh.consume(value);
        cdl.countDown();
    }
    
    @Override
    public void onError(Throwable error) {
        bh.consume(error);
        cdl.countDown();
    }
    
    /**
     * Sleeps until the subscriber receives an event.
     */
    public void sleepAwait() {
        try {
            cdl.await();
        } catch (InterruptedException ex) {
            throw new RuntimeException(ex);
        }
    }
    
    /**
     * Spins until the subscriber receives an events.
     */
    public void spinAwait() {
        while (cdl.getCount() != 0) ;
    }
}
