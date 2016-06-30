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
package rx;

/**
 * Interface that establishes a request-channel between an Observable and a Subscriber and allows
 * the Subscriber to request a certain amount of items from the Observable (otherwise known as
 * backpressure).
 * 
 * <p>The request amount only affects calls to {@link Subscriber#onNext(Object)}; onError and onCompleted may appear without
 * requests.
 * 
 * <p>However, backpressure is somewhat optional in RxJava 1.x and Subscribers may not
 * receive a Producer via their {@link Subscriber#setProducer(Producer)} method and will run
 * in unbounded mode. Depending on the chain of operators, this can lead to {@link rx.exceptions.MissingBackpressureException}.
 */
public interface Producer {

    /**
     * Request a certain maximum number of items from this Producer. This is a way of requesting backpressure.
     * To disable backpressure, pass {@code Long.MAX_VALUE} to this method.
     * <p>
     * Requests are additive but if a sequence of requests totals more than {@code Long.MAX_VALUE} then 
     * {@code Long.MAX_VALUE} requests will be actioned and the extras <i>may</i> be ignored. Arriving at 
     * {@code Long.MAX_VALUE} by addition of requests cannot be assumed to disable backpressure. For example, 
     * the code below may result in {@code Long.MAX_VALUE} requests being actioned only.
     * 
     * <pre>
     * request(100);
     * request(Long.MAX_VALUE-1);
     * </pre> 
     *
     * @param n the maximum number of items you want this Producer to produce, or {@code Long.MAX_VALUE} if you
     *          want the Producer to produce items at its own pace
     * @throws IllegalArgumentException if the request amount is negative
     */
    void request(long n);

}
