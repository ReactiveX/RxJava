/**
 * Copyright 2013 Netflix, Inc.
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

import rx.Observable;
import rx.libgdx.events.InputEvent;
import rx.libgdx.sources.GdxEventSource;

/**
 * Allows creating observables from various sources specific to libgdx. 
 */
public enum GdxObservable { ; // no instances

    /**
     * Creates an observable corresponding to the game's input events.
     * Publish this and convert to the more specific input events you require.
     * 
     * @return Observable emitting all input events.
     */
    public static Observable<InputEvent> fromInput() {
        return GdxEventSource.fromInput();
    }
}
