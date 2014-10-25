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
package rx.exceptions;

/**
 * {@code MissingBackpressureException} indicates that a Subscriber or operator attempted to apply reactive pull
 * backpressure to an Observable that does not implement it.
 * <p>
 * If an Observable has not been written to support reactive pull backpressure (such support is not a
 * requirement for Observables), you can apply one of the following operators to it, each of which forces a
 * simple form of backpressure behavior:
 * <dl>
 *  <dt><code>onBackpressureBuffer</code></dt>
 *   <dd>maintains a buffer of all emissions from the source Observable and emits them to downstream Subscribers
 *       according to the requests they generate</dd>
 *  <dt><code>onBackpressureDrop</code></dt>
 *   <dd>drops emissions from the source Observable unless there is a pending request from a downstream
 *       Subscriber, in which case it will emit enough items to fulfill the request</dd>
 * </dl>
 * If you do not apply either of these operators to an Observable that does not support backpressure, and if
 * either you as the Subscriber or some operator between you and the Observable attempts to apply reactive pull
 * backpressure, you will encounter a {@code MissingBackpressureException} which you will be notified of via
 * your {@code onError} callback.
 *
 * @see <a href="https://github.com/ReactiveX/RxJava/wiki/Backpressure">RxJava wiki: Backpressure</a>
 */
public class MissingBackpressureException extends Exception {

    private static final long serialVersionUID = 7250870679677032194L;

    public MissingBackpressureException() {
    }

    public MissingBackpressureException(String message) {
        super(message);
    }

}
