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

/**
 * This behaves like {@link OperatorMerge} except that if any of the merged Observables notify of
 * an error via {@code onError}, {@code mergeDelayError} will refrain from propagating that error
 * notification until all of the merged Observables have finished emitting items.
 * <p>
 * <img width="640" src="https://github.com/ReactiveX/RxJava/wiki/images/rx-operators/mergeDelayError.png" alt="">
 * <p>
 * Even if multiple merged Observables send {@code onError} notifications, {@code mergeDelayError} will
 * only invoke the {@code onError} method of its Observers once.
 * <p>
 * This operation allows an Observer to receive all successfully emitted items from all of the
 * source Observables without being interrupted by an error notification from one of them.
 * <p>
 * <em>Note:</em> If this is used on an Observable that never completes, it will never call {@code onError} and will effectively swallow errors.
 *
 * @param <T>
 *            the source and result value type
 */
public final class OperatorMergeDelayError<T> extends OperatorMerge<T> {

    public OperatorMergeDelayError() {
        super(true);
    }
    
}
