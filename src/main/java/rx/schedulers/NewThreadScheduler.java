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
package rx.schedulers;

import rx.Scheduler;

/**
 * @deprecated This type was never publicly instantiable. Use {@link Schedulers#newThread()}.
 */
@Deprecated
// Class was part of public API.
public final class NewThreadScheduler extends Scheduler {
    private NewThreadScheduler() {
        throw new AssertionError();
    }

    @Override
    public Worker createWorker() {
        return null;
    }
}
