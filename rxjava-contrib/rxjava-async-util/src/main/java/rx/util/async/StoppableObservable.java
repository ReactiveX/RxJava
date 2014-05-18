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
package rx.util.async;

import rx.Observable;
import rx.Subscription;

/**
 * An Observable that provides a Subscription interface to signal a stop condition to an asynchronous task.
 *
 * @see <a href="https://github.com/Netflix/RxJava/wiki/Async-Operators#wiki-runasync">RxJava Wiki: runAsync()</a>
 */
public class StoppableObservable<T> extends Observable<T> implements Subscription {
    private final Subscription token;
    public StoppableObservable(Observable.OnSubscribe<T> onSubscribe, Subscription token) {
        super(onSubscribe);
        this.token = token;
    }
    
    @Override
    public boolean isUnsubscribed() {
        return token.isUnsubscribed();
    }
    
    @Override
    public void unsubscribe() {
        token.unsubscribe();
    }
}
