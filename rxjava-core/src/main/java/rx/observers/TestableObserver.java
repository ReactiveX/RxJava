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
package rx.observers;

import java.util.List;
import rx.Notification;
import rx.Observer;
import rx.util.Recorded;

/**
 * Observer that records received notification events and their timestamps.
 * @param <T> the element type
 */
public interface TestableObserver<T> extends Observer<T> {
    /**
     * Return the list of received notifications.
     * @return the list of received notifications
     */
    List<Recorded<Notification<T>>> messages();
}
