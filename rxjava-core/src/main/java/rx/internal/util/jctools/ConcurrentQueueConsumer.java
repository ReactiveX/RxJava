/*
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
 * 
 * Original License: https://github.com/JCTools/JCTools/blob/master/LICENSE
 * Original location: https://github.com/JCTools/JCTools/blob/master/jctools-core/src/main/java/org/jctools/queues/alt/ConcurrentQueueConsumer.java
 */
package rx.internal.util.jctools;

import java.util.Queue;

/**
 * Consumers are local to the threads which use them. A thread should therefore call
 * {@link ConcurrentQueue#consumer()} to obtain an instance and should only use it's own instance to poll.
 * 
 * @author nitsanw
 * 
 */
public interface ConcurrentQueueConsumer<E> {
    /**
     * See {@link Queue#poll()} for contract. Elements are removed from the queue once read.
     * 
     * @return next element or null if queue is empty
     */
    E poll();

    /**
     * See {@link Queue#peek()} for contract.
     * 
     * @return next element or null if queue is empty
     */
    E peek();

    /**
     * Remove all elements from the queue. This will not stop the producers from adding new elements, so only
     * guarantees elements visible to the consumer on first sweep are removed.
     */
    void clear();
}