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
import rx.Producer;

import static org.mockito.Mockito.*;

public class NonOverlappingBufferProducerTest {

    @Test
    public void testRequest() {
        Producer parentProducer = mock(Producer.class);
        Producer producer = new NonOverlappingBufferProducer(parentProducer, 5);
        producer.request(3);
        verify(parentProducer).request(15);
        producer.request(4);
        verify(parentProducer).request(20);
        producer.request(Long.MAX_VALUE / 5);
        verify(parentProducer).request(Long.MAX_VALUE);
    }

    @Test
    public void testRequest2() {
        Producer parentProducer = mock(Producer.class);
        Producer producer = new NonOverlappingBufferProducer(parentProducer, 5);
        producer.request(Long.MAX_VALUE / 5 + 1);
        verify(parentProducer).request(Long.MAX_VALUE);

        producer.request(4);
        // Ignore further requests, so only call once
        verify(parentProducer, times(1)).request(Long.MAX_VALUE);
    }
}
