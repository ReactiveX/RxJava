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
package rx.operators;

import rx.Observable;
import rx.Observable.OnSubscribe;
import rx.Subscriber;
import rx.Subscription;
import rx.functions.Func0;
import rx.functions.Func1;

/**
 * Constructs an observable sequence that depends on a resource object.
 */
public final class OperatorUsing<T, Resource extends Subscription> implements OnSubscribe<T> {

    private final Func0<Resource> resourceFactory;
    private final Func1<Resource, ? extends Observable<? extends T>> observableFactory;

    public OperatorUsing(Func0<Resource> resourceFactory, Func1<Resource, ? extends Observable<? extends T>> observableFactory) {
        this.resourceFactory = resourceFactory;
        this.observableFactory = observableFactory;
    }

    public void call(Subscriber<? super T> subscriber) {
        Resource resource = null;
        try {
            resource = resourceFactory.call();
            subscriber.add(resource);
            Observable<? extends T> observable = observableFactory.call(resource);
            observable.subscribe(subscriber);
        } catch (Throwable e) {
            if (resource != null) {
                resource.unsubscribe();
            }
            subscriber.onError(e);
        }
    }

}
