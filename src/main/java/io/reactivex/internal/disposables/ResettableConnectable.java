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

package io.reactivex.internal.disposables;

import io.reactivex.annotations.Experimental;
import io.reactivex.flowables.ConnectableFlowable;
import io.reactivex.observables.ConnectableObservable;

/**
 * Interface allowing conditional resetting of connections in {@link ConnectableObservable}s
 * and {@link ConnectableFlowable}s.
 * @since 2.2.2 - experimental
 */
@Experimental
public interface ResettableConnectable {

    /**
     * Reset the connectable if the current internal connection object is the
     * same as the provided object.
     * @param connectionObject the connection object identifying the last known
     * active connection
     */
    void resetIf(Object connectionObject);
}
