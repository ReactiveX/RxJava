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
import static org.junit.Assert.*;

import rx.*;
import rx.exceptions.TestException;
import rx.observers.*;

public class ProducerObserverArbiterTest {

    @Test
    public void negativeRequestThrows() {
        ProducerObserverArbiter<Integer> pa = new ProducerObserverArbiter<Integer>(Subscribers.empty());
        try {
            pa.request(-99);
            Assert.fail("Failed to throw on invalid request amount");
        } catch (IllegalArgumentException ex) {
            Assert.assertEquals("n >= 0 required", ex.getMessage());
        }
    }

    @Test
    public void nullProducerAccepted() {
        ProducerObserverArbiter<Integer> pa = new ProducerObserverArbiter<Integer>(Subscribers.empty());
        pa.setProducer(null);
        pa.request(5);
    }

    public void failedRequestUnlocksEmitting() {
        ProducerObserverArbiter<Integer> pa = new ProducerObserverArbiter<Integer>(Subscribers.empty());
        pa.setProducer(new Producer() {
            @Override
            public void request(long n) {
                throw new TestException("Forced failure");
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
        final ProducerObserverArbiter<Integer> pa = new ProducerObserverArbiter<Integer>(Subscribers.empty());
        pa.setProducer(new Producer() {
            @Override
            public void request(long n) {
                pa.setProducer(null);
            }
        });
    }

    @Test
    public void reentrantSetProducer() {
        final ProducerObserverArbiter<Integer> pa = new ProducerObserverArbiter<Integer>(Subscribers.empty());
        pa.setProducer(new Producer() {
            @Override
            public void request(long n) {
                pa.setProducer(new ProducerArbiter());
            }
        });
    }

    @Test
    public void reentrantOnNext() {
        @SuppressWarnings("rawtypes")
        final Observer[] o = { null };
        TestSubscriber<Integer> ts = new TestSubscriber<Integer>() {
            @SuppressWarnings("unchecked")
            @Override
            public void onNext(Integer t) {
                if (t == 1) {
                    o[0].onNext(2);
                }
                super.onNext(t);
            }
        };
        ProducerObserverArbiter<Integer> poa = new ProducerObserverArbiter<Integer>(ts);
        o[0] = poa;
        poa.request(2);
        poa.onNext(1);
        ts.assertValues(1, 2);
    }

    @Test
    public void reentrantOnError() {
        @SuppressWarnings("rawtypes")
        final Observer[] o = { null };
        TestSubscriber<Integer> ts = new TestSubscriber<Integer>() {
            @Override
            public void onNext(Integer t) {
                if (t == 1) {
                    o[0].onError(new TestException());
                }
                super.onNext(t);
            }
        };
        ProducerObserverArbiter<Integer> poa = new ProducerObserverArbiter<Integer>(ts);
        o[0] = poa;
        poa.onNext(1);
        ts.assertValue(1);
        ts.assertError(TestException.class);
    }

    @Test
    public void reentrantOnCompleted() {
        @SuppressWarnings("rawtypes")
        final Observer[] o = { null };
        TestSubscriber<Integer> ts = new TestSubscriber<Integer>() {
            @Override
            public void onNext(Integer t) {
                if (t == 1) {
                    o[0].onCompleted();
                }
                super.onNext(t);
            }
        };
        ProducerObserverArbiter<Integer> poa = new ProducerObserverArbiter<Integer>(ts);
        o[0] = poa;
        poa.onNext(1);
        ts.assertValue(1);
        ts.assertCompleted();
    }

    @Test
    public void onNextThrows() {
        @SuppressWarnings("rawtypes")
        final Observer[] o = { null };
        TestSubscriber<Integer> ts = new TestSubscriber<Integer>() {
            @Override
            public void onNext(Integer t) {
                throw new TestException();
            }
        };
        ProducerObserverArbiter<Integer> poa = new ProducerObserverArbiter<Integer>(ts);
        o[0] = poa;
        try {
            poa.onNext(1);
            Assert.fail("Arbiter didn't throw");
        } catch (TestException ex) {
            // expected
        }
    }

    @Test
    public void onNextRequests() {
        @SuppressWarnings("rawtypes")
        final ProducerObserverArbiter[] o = { null };
        TestSubscriber<Integer> ts = new TestSubscriber<Integer>() {
            @Override
            public void onNext(Integer t) {
                o[0].request(1);
            }
        };
        ProducerObserverArbiter<Integer> poa = new ProducerObserverArbiter<Integer>(ts);
        poa.request(1);
        o[0] = poa;
        try {
            poa.onNext(1);
        } catch (TestException ex) {
            // expected
        }
        assertEquals(1, poa.requested);
    }

    @Test
    public void requestIsCapped() {
        ProducerObserverArbiter<Integer> poa = new ProducerObserverArbiter<Integer>(new TestSubscriber<Integer>());

        poa.request(Long.MAX_VALUE - 1);
        poa.request(2);

        assertEquals(Long.MAX_VALUE, Long.MAX_VALUE);
    }

    @Test
    public void onNextChangesProducerNull() {
        @SuppressWarnings("rawtypes")
        final ProducerObserverArbiter[] o = { null };
        TestSubscriber<Integer> ts = new TestSubscriber<Integer>() {
            @Override
            public void onNext(Integer t) {
                o[0].setProducer(null);
            }
        };
        ProducerObserverArbiter<Integer> poa = new ProducerObserverArbiter<Integer>(ts);
        poa.request(1);
        o[0] = poa;
        try {
            poa.onNext(1);
        } catch (TestException ex) {
            // expected
        }
        assertNull(poa.currentProducer);
    }

    @Test
    public void onNextChangesProducerNotNull() {
        @SuppressWarnings("rawtypes")
        final ProducerObserverArbiter[] o = { null };
        TestSubscriber<Integer> ts = new TestSubscriber<Integer>() {
            @SuppressWarnings("unchecked")
            @Override
            public void onNext(Integer t) {
                o[0].setProducer(new SingleProducer<Integer>(o[0].child, 2));
            }
        };
        ProducerObserverArbiter<Integer> poa = new ProducerObserverArbiter<Integer>(ts);
        poa.request(1);
        o[0] = poa;
        try {
            poa.onNext(1);
        } catch (TestException ex) {
            // expected
        }
        assertNotNull(poa.currentProducer);
    }

}
