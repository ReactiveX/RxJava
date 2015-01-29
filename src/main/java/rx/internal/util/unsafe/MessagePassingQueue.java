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
 * Original location: https://github.com/JCTools/JCTools/blob/master/jctools-core/src/main/java/org/jctools/queues/MessagePassingQueue.java
 */
package rx.internal.util.unsafe;

import java.util.Queue;

/**
 * This is a tagging interface for the queues in this library which implement a subset of the {@link Queue} interface
 * sufficient for concurrent message passing.<br>
 * Message passing queues offer happens before semantics to messages passed through, namely that writes made by the
 * producer before offering the message are visible to the consuming thread after the message has been polled out of the
 * queue.
 * 
 * @author nitsanw
 * 
 * @param <M> the event/message type
 */
interface MessagePassingQueue<M> {
    
    /**
     * Called from a producer thread subject to the restrictions appropriate to the implementation and according to the
     * {@link Queue#offer(Object)} interface.
     * 
     * @param message
     * @return true if element was inserted into the queue, false iff full
     */
    boolean offer(M message);

    /**
     * Called from the consumer thread subject to the restrictions appropriate to the implementation and according to
     * the {@link Queue#poll()} interface.
     * 
     * @return a message from the queue if one is available, null iff empty
     */
    M poll();

    /**
     * Called from the consumer thread subject to the restrictions appropriate to the implementation and according to
     * the {@link Queue#peek()} interface.
     * 
     * @return a message from the queue if one is available, null iff empty
     */
    M peek();

    /**
     * This method's accuracy is subject to concurrent modifications happening as the size is estimated and as such is a
     * best effort rather than absolute value. For some implementations this method may be O(n) rather than O(1).
     * 
     * @return number of messages in the queue, between 0 and queue capacity or {@link Integer#MAX_VALUE} if not bounded
     */
    int size();
    
    /**
     * This method's accuracy is subject to concurrent modifications happening as the observation is carried out.
     * 
     * @return true if empty, false otherwise
     */
    boolean isEmpty();

}
