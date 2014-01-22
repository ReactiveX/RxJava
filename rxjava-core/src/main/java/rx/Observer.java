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
package rx;

import rx.subscriptions.CompositeSubscription;
import rx.util.OnErrorNotImplementedException;
import rx.util.functions.Action0;
import rx.util.functions.Action1;

/**
 * Provides a mechanism for receiving push-based notifications.
 * <p>
 * After an Observer calls an {@link Observable}'s <code>Observable.subscribe</code> method, the {@link Observable} calls the Observer's <code>onNext</code> method to provide notifications. A
 * well-behaved {@link Observable} will
 * call an Observer's <code>onCompleted</code> closure exactly once or the Observer's <code>onError</code> closure exactly once.
 * <p>
 * For more information see the <a href="https://github.com/Netflix/RxJava/wiki/Observable">RxJava Wiki</a>
 * 
 * @param <T>
 */
public abstract class Observer<T> implements Subscription {

    private final CompositeSubscription cs;

    protected Observer(CompositeSubscription cs) {
        this.cs = cs;
    }

    protected Observer() {
        this(new CompositeSubscription());
    }

    protected Observer(Observer<?> op) {
        this(op.cs);
    }

    /**
     * Notifies the Observer that the {@link Observable} has finished sending push-based notifications.
     * <p>
     * The {@link Observable} will not call this closure if it calls <code>onError</code>.
     */
    public abstract void onCompleted();

    /**
     * Notifies the Observer that the {@link Observable} has experienced an error condition.
     * <p>
     * If the {@link Observable} calls this closure, it will not thereafter call <code>onNext</code> or <code>onCompleted</code>.
     * 
     * @param e
     */
    public abstract void onError(Throwable e);

    /**
     * Provides the Observer with new data.
     * <p>
     * The {@link Observable} calls this closure 1 or more times, unless it calls <code>onError</code> in which case this closure may never be called.
     * <p>
     * The {@link Observable} will not call this closure again after it calls either <code>onCompleted</code> or <code>onError</code>.
     * 
     * @param args
     */
    public abstract void onNext(T args);

    /**
     * Create an empty Observer that ignores all events.
     */
    public static final <T> Observer<T> create() {
        return new Observer<T>() {

            @Override
            public final void onCompleted() {
                // do nothing
            }

            @Override
            public final void onError(Throwable e) {
                throw new OnErrorNotImplementedException(e);
            }

            @Override
            public final void onNext(T args) {
                // do nothing
            }

        };
    }

    /**
     * Create an Observer that receives `onNext` and ignores `onError` and `onCompleted`.
     */
    public static final <T> Observer<T> create(final Action1<? super T> onNext) {
        if (onNext == null) {
            throw new IllegalArgumentException("onNext can not be null");
        }

        return new Observer<T>() {

            @Override
            public final void onCompleted() {
                // do nothing
            }

            @Override
            public final void onError(Throwable e) {
                throw new OnErrorNotImplementedException(e);
            }

            @Override
            public final void onNext(T args) {
                onNext.call(args);
            }

        };
    }

    /**
     * Create an Observer that receives `onNext` and `onError` and ignores `onCompleted`.
     * 
     */
    public static final <T> Observer<T> create(final Action1<? super T> onNext, final Action1<Throwable> onError) {
        if (onNext == null) {
            throw new IllegalArgumentException("onNext can not be null");
        }
        if (onError == null) {
            throw new IllegalArgumentException("onError can not be null");
        }

        return new Observer<T>() {

            @Override
            public final void onCompleted() {
                // do nothing
            }

            @Override
            public final void onError(Throwable e) {
                onError.call(e);
            }

            @Override
            public final void onNext(T args) {
                onNext.call(args);
            }

        };
    }

    /**
     * Create an Observer that receives `onNext`, `onError` and `onCompleted`.
     * 
     */
    public static final <T> Observer<T> create(final Action1<? super T> onNext, final Action1<Throwable> onError, final Action0 onComplete) {
        if (onNext == null) {
            throw new IllegalArgumentException("onNext can not be null");
        }
        if (onError == null) {
            throw new IllegalArgumentException("onError can not be null");
        }
        if (onComplete == null) {
            throw new IllegalArgumentException("onComplete can not be null");
        }

        return new Observer<T>() {

            @Override
            public final void onCompleted() {
                onComplete.call();
            }

            @Override
            public final void onError(Throwable e) {
                onError.call(e);
            }

            @Override
            public final void onNext(T args) {
                onNext.call(args);
            }

        };
    }

    /**
     * Used to register an unsubscribe callback.
     */
    public final void add(Subscription s) {
        cs.add(s);
    }

    @Override
    public final void unsubscribe() {
        cs.unsubscribe();
    }

    public final boolean isUnsubscribed() {
        return cs.isUnsubscribed();
    }
}
