/**
 * Copyright 2015 Netflix, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package rx.internal.producers;

import org.junit.*;

import rx.Producer;
import rx.exceptions.TestException;

public class ProducerArbiterTest {

    @Test
    public void negativeRequestThrows() {
        ProducerArbiter pa = new ProducerArbiter();
        try {
            pa.request(-99);
            Assert.fail("Failed to throw on invalid request amount");
        } catch (IllegalArgumentException ex) {
            Assert.assertEquals("n >= 0 required", ex.getMessage());
        }
    }

    @Test
    public void negativeProducedThrows() {
        ProducerArbiter pa = new ProducerArbiter();
        try {
            pa.produced(-99);
            Assert.fail("Failed to throw on invalid produced amount");
        } catch (IllegalArgumentException ex) {
            Assert.assertEquals("n > 0 required", ex.getMessage());
        }
    }

    @Test
    public void overproductionThrows() {
        ProducerArbiter pa = new ProducerArbiter();
        try {
            pa.produced(1);
            Assert.fail("Failed to throw on overproduction amount");
        } catch (IllegalStateException ex) {
            Assert.assertEquals("more items arrived than were requested", ex.getMessage());
        }
    }

    @Test
    public void nullProducerAccepted() {
        ProducerArbiter pa = new ProducerArbiter();
        pa.setProducer(null);
    }

    @Test
    public void failedRequestUnlocksEmitting() {
        ProducerArbiter pa = new ProducerArbiter();
        pa.setProducer(new Producer() {
            @Override
            public void request(long n) {
                if (n != 0) {
                    throw new TestException("Forced failure");
                }
            }
        });
        try {
            pa.request(1);
            Assert.fail("Failed to throw on overproduction amount");
        } catch (TestException ex) {
            Assert.assertEquals("Forced failure", ex.getMessage());
            Assert.assertFalse("Still emitting?!", pa.emitting);
        }
    }

    @Test
    public void reentrantSetProducerNull() {
        final ProducerArbiter pa = new ProducerArbiter();
        pa.setProducer(new Producer() {
            @Override
            public void request(long n) {
                pa.setProducer(null);
            }
        });
    }

    @Test
    public void reentrantSetProducer() {
        final ProducerArbiter pa = new ProducerArbiter();
        pa.setProducer(new Producer() {
            @Override
            public void request(long n) {
                pa.setProducer(new ProducerArbiter());
            }
        });
    }

    @Test
    public void overproductionReentrantThrows() {
        final ProducerArbiter pa = new ProducerArbiter();
        try {
            pa.setProducer(new Producer() {
                @Override
                public void request(long n) {
                    if (n != 0) {
                        pa.produced(2);
                    }
                }
            });
            pa.request(1);
            Assert.fail("Failed to throw on overproduction amount");
        } catch (IllegalStateException ex) {
            Assert.assertEquals("more produced than requested", ex.getMessage());
        }
    }

}
