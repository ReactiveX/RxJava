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
package rx.internal.operators;

import rx.Observable;
import rx.Observable.OnSubscribe;
import rx.Subscriber;
import rx.functions.Action0;
import rx.functions.Action1;
import rx.functions.Func0;
import rx.functions.Func1;
import rx.subscriptions.Subscriptions;

/**
 * Constructs an observable sequence that depends on a resource object.
 */
public final class OnSubscribeUsing<T, Resource> implements OnSubscribe<T> {

    private final Func0<Resource> resourceFactory;
    private final Func1<? super Resource, ? extends Observable<? extends T>> observableFactory;
    private final Action1<? super Resource> dispose;

    public OnSubscribeUsing(Func0<Resource> resourceFactory,
            Func1<? super Resource, ? extends Observable<? extends T>> observableFactory,
            Action1<? super Resource> dispose) {
        this.resourceFactory = resourceFactory;
        this.observableFactory = observableFactory;
        this.dispose = dispose;
    }

    @Override
    public void call(Subscriber<? super T> subscriber) {
        try {
            final Resource resource = resourceFactory.call();
            subscriber.add(Subscriptions.create(new Action0() {

                @Override
                public void call() {
                    dispose.call(resource);
                }

            }));
            Observable<? extends T> observable = observableFactory.call(resource);
            observable.subscribe(subscriber);
        } catch (Throwable e) {
            // eagerly call unsubscribe since this operator is specifically about resource management
            subscriber.unsubscribe();
            // then propagate error
            subscriber.onError(e);
        }
    }

}
