/**
 * Copyright 2016 Netflix, Inc.
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
package rx.internal.util;

import rx.*;
import rx.functions.*;

/**
 * An Observer that forwards the onXXX method calls to a notification callback
 * by transforming each signal type into Notifications.
 * @param <T> the value type
 */
public final class ActionNotificationObserver<T> implements Observer<T> {
    
    final Action1<Notification<? super T>> onNotification;

    public ActionNotificationObserver(Action1<Notification<? super T>> onNotification) {
        this.onNotification = onNotification;
    }

    @Override
    public void onNext(T t) {
        onNotification.call(Notification.createOnNext(t));
    }

    @Override
    public void onError(Throwable e) {
        onNotification.call(Notification.createOnError(e));
    }

    @Override
    public void onCompleted() {
        onNotification.call(Notification.createOnCompleted());
    }
}