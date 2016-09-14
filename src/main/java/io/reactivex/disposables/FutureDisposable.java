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
package io.reactivex.disposables;

import java.util.concurrent.Future;

/**
 * A Disposable container that cancels a Future instance.
 */
final class FutureDisposable extends ReferenceDisposable<Future<?>> {

    private static final long serialVersionUID = 6545242830671168775L;

    private final boolean allowInterrupt;

    FutureDisposable(Future<?> run, boolean allowInterrupt) {
        super(run);
        this.allowInterrupt = allowInterrupt;
    }

    @Override
    protected void onDisposed(Future<?> value) {
        value.cancel(allowInterrupt);
    }
}
