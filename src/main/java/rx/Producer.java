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
 * @warn javadoc description missing
 */
public interface Producer {

    /**
     * Request a certain maximum number of items from this Producer. This is a way of requesting backpressure.
     * To disable backpressure, pass {@code Long.MAX_VALUE} to this method.
     *
     * @param n the maximum number of items you want this Producer to produce, or {@code Long.MAX_VALUE} if you
     *          want the Producer to produce items at its own pace
     */
    public void request(long n);

}
