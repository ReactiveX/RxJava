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
package rx.operators;

import rx.IObservable;
import rx.Observer;
import rx.Subscription;
import rx.util.OnErrorNotImplementedException;
import rx.util.functions.Action0;
import rx.util.functions.Action1;


public final class OperationSubscribe {

    /**
     * Subscribe and ignore all events.
     */
    public static <T> Subscription subscribe(final IObservable<T> observable) {
        return protectivelyWrapAndSubscribe(observable, new Observer<T>() {

            @Override
            public void onCompleted() {
                // do nothing
            }

            @Override
            public void onError(Throwable e) {
                throw new OnErrorNotImplementedException(e);
            }

            @Override
            public void onNext(T args) {
                // do nothing
            }

        });
    }

    /**
     * An {@link Observer} must call an Observable's {@code subscribe} method
     * in order to receive items and notifications from the Observable.
     */
    public static <T> Subscription subscribe(
            final IObservable<T> observable,
            final Action1<? super T> onNext) {
        if (onNext == null) {
            throw new IllegalArgumentException("onNext cannot be null");
        }

        /* Wrapping since raw functions provided by the user are being invoked.
         * 
         * See https://github.com/Netflix/RxJava/issues/216 for discussion on "Guideline 6.4: Protect calls to user code from within an operator"
         */
        return protectivelyWrapAndSubscribe(observable, new Observer<T>() {

            @Override
            public void onCompleted() {
                // do nothing
            }

            @Override
            public void onError(Throwable e) {
                throw new OnErrorNotImplementedException(e);
            }

            @Override
            public void onNext(T args) {
                onNext.call(args);
            }

        });
    }

    /**
     * An {@link Observer} must call an Observable's {@code subscribe} method in
     * order to receive items and notifications from the Observable.
     */
    public static <T> Subscription subscribe(
            final IObservable<T> observable,
            final Action1<? super T> onNext,
            final Action1<Throwable> onError) {
        if (onNext == null) {
            throw new IllegalArgumentException("onNext can not be null");
        }
        if (onError == null) {
            throw new IllegalArgumentException("onError can not be null");
        }

        /* Wrapping since raw functions provided by the user are being invoked.
         * 
         * See https://github.com/Netflix/RxJava/issues/216 for discussion on "Guideline 6.4: Protect calls to user code from within an operator"
         */
        return protectivelyWrapAndSubscribe(observable, new Observer<T>() {

            @Override
            public void onCompleted() {
                // do nothing
            }

            @Override
            public void onError(Throwable e) {
                onError.call(e);
            }

            @Override
            public void onNext(T args) {
                onNext.call(args);
            }

        });
    }

    /**
     * An {@link Observer} must call an Observable's {@code subscribe} method in
     * order to receive items and notifications from the Observable.
     */
    public static <T> Subscription subscribe(
            final IObservable<T> observable,
            final Action1<? super T> onNext,
            final Action1<Throwable> onError,
            final Action0 onComplete) {
        if (onNext == null) {
            throw new IllegalArgumentException("onNext can not be null");
        }
        if (onError == null) {
            throw new IllegalArgumentException("onError can not be null");
        }
        if (onComplete == null) {
            throw new IllegalArgumentException("onComplete can not be null");
        }

        /* Wrapping since raw functions provided by the user are being invoked.
         * 
         * See https://github.com/Netflix/RxJava/issues/216 for discussion on "Guideline 6.4: Protect calls to user code from within an operator"
         */
        return protectivelyWrapAndSubscribe(observable, new Observer<T>() {

            @Override
            public void onCompleted() {
                onComplete.call();
            }

            @Override
            public void onError(Throwable e) {
                onError.call(e);
            }

            @Override
            public void onNext(T args) {
                onNext.call(args);
            }

        });
    }

    private OperationSubscribe() {
        // Prevent external instantiation
    }

    /**
     * Protects against errors being thrown from Observer implementations and
     * ensures onNext/onError/onCompleted contract compliance.
     * <p>
     * See https://github.com/Netflix/RxJava/issues/216 for a discussion on
     * "Guideline 6.4: Protect calls to user code from within an operator"
     */
    private static <T> Subscription protectivelyWrapAndSubscribe(
            final IObservable<T> observable,
            final Observer<? super T> observer) {
        if (observable == null) {
            throw new IllegalArgumentException("observable cannot be null");
        }
        SafeObservableSubscription subscription = new SafeObservableSubscription();
        final SafeObserver<T> safeObserver = new SafeObserver<T>(subscription, observer);
        return subscription.wrap(observable.subscribe(safeObserver));
    }

}
