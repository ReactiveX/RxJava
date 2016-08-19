/**
 * Copyright 2016 Netflix, Inc.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in
 * compliance with the License. You may obtain a copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software distributed under the License is
 * distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See
 * the License for the specific language governing permissions and limitations under the License.
 */

package io.reactivex;

/**
 * Represents the options for applying backpressure to a source sequence.
 */
public enum BackpressureStrategy {
    /**
     * Buffer all values (unbounded) until there is a downstream demand for it.
     */
    BUFFER,
    /**
     * Drop the value if there is no current demand for it from the downstream.
     */
    DROP,
    /**
     * Have a latest value always available and overwrite it with more recent ones
     * if there is no demand for it from the downstream.
     */
    LATEST
}
