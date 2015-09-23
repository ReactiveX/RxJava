/**
 * Copyright 2015 Netflix, Inc.
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

import io.reactivex.NbpObservable.NbpSubscriber;
import io.reactivex.disposables.Disposable;
import io.reactivex.internal.subscriptions.SubscriptionHelper;

public abstract class NbpObserver<T> implements NbpSubscriber<T> {
    private Disposable s;
    @Override
    public final void onSubscribe(Disposable s) {
        if (SubscriptionHelper.validateDisposable(this.s, s)) {
            return;
        }
        this.s = s;
        onStart();
    }
    
    protected final void cancel() {
        s.dispose();
    }
    /**
     * Called once the subscription has been set on this observer; override this
     * to perform initialization.
     */
    protected void onStart() {
    }
    
}
