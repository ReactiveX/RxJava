/**
 * Copyright 2016 Netflix, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */

package rx.internal.operators;

import java.util.Arrays;

import rx.*;
import rx.exceptions.*;
import rx.functions.*;
import rx.plugins.RxJavaPlugins;

/**
 * Generates a resource, derives a Single from it and disposes that resource once the
 * Single terminates.
 * @param <T> the value type of the Single
 * @param <Resource> the resource type
 */
public final class SingleOnSubscribeUsing<T, Resource> implements Single.OnSubscribe<T> {
    final Func0<Resource> resourceFactory;
    final Func1<? super Resource, ? extends Single<? extends T>> singleFactory;
    final Action1<? super Resource> disposeAction; 
    final boolean disposeEagerly;

    public SingleOnSubscribeUsing(Func0<Resource> resourceFactory,
            Func1<? super Resource, ? extends Single<? extends T>> observableFactory,
            Action1<? super Resource> disposeAction, boolean disposeEagerly) {
        this.resourceFactory = resourceFactory;
        this.singleFactory = observableFactory;
        this.disposeAction = disposeAction;
        this.disposeEagerly = disposeEagerly;
    }
    
    @Override
    public void call(final SingleSubscriber<? super T> child) {
        final Resource resource;
        
        try {
            resource = resourceFactory.call();
        } catch (Throwable ex) {
            Exceptions.throwIfFatal(ex);
            child.onError(ex);
            return;
        }
        
        Single<? extends T> single;
        
        try {
            single = singleFactory.call(resource);
        } catch (Throwable ex) {
            handleSubscriptionTimeError(child, resource, ex);
            return;
        }
        
        if (single == null) {
            handleSubscriptionTimeError(child, resource, new NullPointerException("The single"));
            return;
        }
        
        SingleSubscriber<T> parent = new SingleSubscriber<T>() {
            @Override
            public void onSuccess(T value) {
                if (disposeEagerly) {
                    try {
                        disposeAction.call(resource);
                    } catch (Throwable ex) {
                        Exceptions.throwIfFatal(ex);
                        
                        child.onError(ex);
                        return;
                    }
                }
                
                child.onSuccess(value);
                
                if (!disposeEagerly) {
                    try {
                        disposeAction.call(resource);
                    } catch (Throwable ex2) {
                        Exceptions.throwIfFatal(ex2);
                        RxJavaPlugins.getInstance().getErrorHandler().handleError(ex2);
                    }
                }
            }
            
            @Override
            public void onError(Throwable error) {
                handleSubscriptionTimeError(child, resource, error);
            }
        };
        child.add(parent);
        
        single.subscribe(parent);
    }

    void handleSubscriptionTimeError(SingleSubscriber<? super T> t, Resource resource, Throwable ex) {
        Exceptions.throwIfFatal(ex);

        if (disposeEagerly) {
            try {
                disposeAction.call(resource);
            } catch (Throwable ex2) {
                Exceptions.throwIfFatal(ex2);
                ex = new CompositeException(Arrays.asList(ex, ex2));
            }
        }
        
        t.onError(ex);
        
        if (!disposeEagerly) {
            try {
                disposeAction.call(resource);
            } catch (Throwable ex2) {
                Exceptions.throwIfFatal(ex2);
                RxJavaPlugins.getInstance().getErrorHandler().handleError(ex2);
            }
        }
    }
}
