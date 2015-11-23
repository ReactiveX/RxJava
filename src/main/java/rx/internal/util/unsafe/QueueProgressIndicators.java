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
 * Original location: https://github.com/JCTools/JCTools/blob/master/jctools-core/src/main/java/org/jctools/queues/QueueProgressIndicators.java
 */
package rx.internal.util.unsafe;

/**
 * This interface is provided for monitoring purposes only and is only available on queues where it is easy to
 * provide it. The producer/consumer progress indicators usually correspond with the number of elements
 * offered/polled, but they are not guaranteed to maintain that semantic.
 * 
 * @author nitsanw
 *
 */
public interface QueueProgressIndicators {

    /**
     * This method has no concurrent visibility semantics. The value returned may be negative. Under normal
     * circumstances 2 consecutive calls to this method can offer an idea of progress made by producer threads
     * by subtracting the 2 results though in extreme cases (if producers have progressed by more than 2^64)
     * this may also fail.<br/>
     * This value will normally indicate number of elements passed into the queue, but may under some
     * circumstances be a derivative of that figure. This method should not be used to derive size or
     * emptiness.
     * 
     * @return the current value of the producer progress index
     */
    public long currentProducerIndex();

    /**
     * This method has no concurrent visibility semantics. The value returned may be negative. Under normal
     * circumstances 2 consecutive calls to this method can offer an idea of progress made by consumer threads
     * by subtracting the 2 results though in extreme cases (if consumers have progressed by more than 2^64)
     * this may also fail.<br/>
     * This value will normally indicate number of elements taken out of the queue, but may under some
     * circumstances be a derivative of that figure. This method should not be used to derive size or
     * emptiness.
     * 
     * @return the current value of the consumer progress index
     */
    public long currentConsumerIndex();
}