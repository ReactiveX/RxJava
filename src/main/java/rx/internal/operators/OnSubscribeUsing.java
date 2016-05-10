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

import java.util.concurrent.atomic.AtomicBoolean;

import rx.*;
import rx.Observable.OnSubscribe;
import rx.exceptions.*;
import rx.functions.*;
import rx.observers.Subscribers;

/**
 * Constructs an observable sequence that depends on a resource object.
 * 
 * @param <T> the output value type
 * @param <Resource> the resource type
 */
public final class OnSubscribeUsing<T, Resource> implements OnSubscribe<T> {

    private final Func0<Resource> resourceFactory;
    private final Func1<? super Resource, ? extends Observable<? extends T>> observableFactory;
    private final Action1<? super Resource> dispose;
    private final boolean disposeEagerly;

    public OnSubscribeUsing(Func0<Resource> resourceFactory,
            Func1<? super Resource, ? extends Observable<? extends T>> observableFactory,
            Action1<? super Resource> dispose, boolean disposeEagerly) {
        this.resourceFactory = resourceFactory;
        this.observableFactory = observableFactory;
        this.dispose = dispose;
        this.disposeEagerly = disposeEagerly;
    }

    @Override
    public void call(final Subscriber<? super T> subscriber) {

        try {

            // create the resource
            final Resource resource = resourceFactory.call();
            // create an action/subscription that disposes only once
            final DisposeAction<Resource> disposeOnceOnly = new DisposeAction<Resource>(dispose,
                    resource);
            // dispose on unsubscription
            subscriber.add(disposeOnceOnly);
            // create the observable
            final Observable<? extends T> source;

            try {
                source = observableFactory
                // create the observable
                        .call(resource);
            } catch (Throwable e) {
                Throwable disposeError = dispose(disposeOnceOnly);
                Exceptions.throwIfFatal(e);
                Exceptions.throwIfFatal(disposeError);
                if (disposeError != null) {
                    subscriber.onError(new CompositeException(e, disposeError));
                } else {
                    // propagate error
                    subscriber.onError(e);
                }
                return;
            }

            final Observable<? extends T> observable;
            // supplement with on termination disposal if requested
            if (disposeEagerly) {
                observable = source
                // dispose on completion or error
                        .doOnTerminate(disposeOnceOnly);
            } else {
                observable = source
                // dispose after the terminal signals were sent out
                        .doAfterTerminate(disposeOnceOnly);
            }
            
            try {
                // start
                observable.unsafeSubscribe(Subscribers.wrap(subscriber));
            } catch (Throwable e) {
                Throwable disposeError = dispose(disposeOnceOnly);
                Exceptions.throwIfFatal(e);
                Exceptions.throwIfFatal(disposeError);
                if (disposeError != null)
                    subscriber.onError(new CompositeException(e, disposeError));
                else
                    // propagate error
                    subscriber.onError(e);
            }
        } catch (Throwable e) {
            // then propagate error
            Exceptions.throwOrReport(e, subscriber);
        }
    }

    private Throwable dispose(final Action0 disposeOnceOnly) {
        try {
            disposeOnceOnly.call();
            return null;
        } catch (Throwable e) {
            return e;
        }
    }

    private static final class DisposeAction<Resource> extends AtomicBoolean implements Action0,
            Subscription {
        private static final long serialVersionUID = 4262875056400218316L;

        private Action1<? super Resource> dispose;
        private Resource resource;

        DisposeAction(Action1<? super Resource> dispose, Resource resource) {
            this.dispose = dispose;
            this.resource = resource;
            lazySet(false); // StoreStore barrier
        }

        @Override
        public void call() {
            if (compareAndSet(false, true)) {
                try {
                    dispose.call(resource);
                } finally {
                    resource = null;
                    dispose = null;
                }
            }
        }

        @Override
        public boolean isUnsubscribed() {
            return get();
        }

        @Override
        public void unsubscribe() {
            call();
        }
    }
}
