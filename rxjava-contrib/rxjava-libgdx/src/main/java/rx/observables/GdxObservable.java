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
package rx.observables;

import java.awt.Dimension;

import com.badlogic.gdx.Application;

import rx.Observable;

/**
 * Allows creating observables from various sources specific to libgdx. 
 */
public enum GdxObservable { ; // no instances

    /**
     * Creates an observable corresponding to component resize events.
     * 
     * @param component
     *            The component to register the observable for.
     * @return Observable emitting the current size of the given component after each resize event.
     */
    public static Observable<Dimension> fromResizing(Application app) {
// FIXME       return ApplicationEventSource.fromResizing(app);
      return null;
    }
}
