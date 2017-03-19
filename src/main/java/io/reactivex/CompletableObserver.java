/**
 * Copyright (c) 2016-present, RxJava Contributors.
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

import io.reactivex.annotations.NonNull;
import io.reactivex.disposables.Disposable;

/**
 * Represents the subscription API callbacks when subscribing to a Completable instance.
 */
public interface CompletableObserver {
    /**
     * Called once by the Completable to set a Disposable on this instance which
     * then can be used to cancel the subscription at any time.
     * @param d the Disposable instance to call dispose on for cancellation, not null
     */
    void onSubscribe(@NonNull Disposable d);

    /**
     * Called once the deferred computation completes normally.
     */
    void onComplete();

    /**
     * Called once if the deferred computation 'throws' an exception.
     * @param e the exception, not null.
     */
    void onError(@NonNull Throwable e);
}
