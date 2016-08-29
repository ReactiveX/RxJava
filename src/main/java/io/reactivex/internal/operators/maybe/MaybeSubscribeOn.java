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

package io.reactivex.internal.operators.maybe;

import io.reactivex.*;

public final class MaybeSubscribeOn<T> extends Maybe<T> {
    final MaybeSource<? extends T> source;

    final Scheduler scheduler;
    
    public MaybeSubscribeOn(MaybeSource<? extends T> source, Scheduler scheduler) {
        this.source = source;
        this.scheduler = scheduler;
    }

    @Override
    protected void subscribeActual(final MaybeObserver<? super T> s) {

        // FIXME cancel schedule
        scheduler.scheduleDirect(new Runnable() {
            @Override
            public void run() {
                source.subscribe(s);
            }
        });
    
    }

}
