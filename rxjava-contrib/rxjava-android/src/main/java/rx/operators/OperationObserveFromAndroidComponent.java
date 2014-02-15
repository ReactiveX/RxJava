/**
 * Copyright 2013 Netflix, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package rx.operators;

import rx.Observable;
import rx.Observer;
import rx.Subscription;
import rx.Scheduler.Inner;
import rx.android.schedulers.AndroidSchedulers;
import rx.subscriptions.Subscriptions;
import rx.util.functions.Action0;
import rx.util.functions.Action1;
import android.app.Activity;
import android.os.Looper;
import android.util.Log;

public class OperationObserveFromAndroidComponent {

    public static <T> Observable<T> observeFromAndroidComponent(Observable<T> source, android.app.Fragment fragment) {
        return Observable.create(new OnSubscribeFragment<T>(source, fragment));
    }

    public static <T> Observable<T> observeFromAndroidComponent(Observable<T> source, android.support.v4.app.Fragment fragment) {
        return Observable.create(new OnSubscribeSupportFragment<T>(source, fragment));
    }

    public static <T> Observable<T> observeFromAndroidComponent(Observable<T> source, Activity activity) {
        return Observable.create(new OnSubscribeBase<T, Activity>(source, activity));
    }

    private static class OnSubscribeBase<T, AndroidComponent> implements Observable.OnSubscribeFunc<T> {

        private static final String LOG_TAG = "AndroidObserver";

        private final Observable<T> source;
        private AndroidComponent componentRef;
        private Observer<? super T> observerRef;

        private OnSubscribeBase(Observable<T> source, AndroidComponent component) {
            this.source = source;
            this.componentRef = component;
        }

        private void log(String message) {
            if (Log.isLoggable(LOG_TAG, Log.DEBUG)) {
                Log.d(LOG_TAG, "componentRef = " + componentRef);
                Log.d(LOG_TAG, "observerRef = " + observerRef);
                Log.d(LOG_TAG, message);
            }
        }

        protected boolean isComponentValid(AndroidComponent component) {
            return true;
        }

        @Override
        public Subscription onSubscribe(Observer<? super T> observer) {
            assertUiThread();
            observerRef = observer;
            final Subscription sourceSub = source.observeOn(AndroidSchedulers.mainThread()).subscribe(new Observer<T>() {
                @Override
                public void onCompleted() {
                    if (componentRef != null && isComponentValid(componentRef)) {
                        observerRef.onCompleted();
                    } else {
                        log("onComplete: target component released or detached; dropping message");
                    }
                }

                @Override
                public void onError(Throwable e) {
                    if (componentRef != null && isComponentValid(componentRef)) {
                        observerRef.onError(e);
                    } else {
                        log("onError: target component released or detached; dropping message");
                    }
                }

                @Override
                public void onNext(T args) {
                    if (componentRef != null && isComponentValid(componentRef)) {
                        observerRef.onNext(args);
                    } else {
                        log("onNext: target component released or detached; dropping message");
                    }
                }
            });
            return Subscriptions.create(new Action0() {
                @Override
                public void call() {
                    log("unsubscribing from source sequence");
                    AndroidSchedulers.mainThread().schedule(new Action1<Inner>() {

                        @Override
                        public void call(Inner t1) {
                            releaseReferences();
                            sourceSub.unsubscribe();
                        }

                    });
                }
            });
        }

        private void releaseReferences() {
            observerRef = null;
            componentRef = null;
        }

        private void assertUiThread() {
            if (Looper.getMainLooper() != Looper.myLooper()) {
                throw new IllegalStateException("Observers must subscribe from the main UI thread, but was " + Thread.currentThread());
            }
        }
    }

    private static final class OnSubscribeFragment<T> extends OnSubscribeBase<T, android.app.Fragment> {

        private OnSubscribeFragment(Observable<T> source, android.app.Fragment fragment) {
            super(source, fragment);
        }

        @Override
        protected boolean isComponentValid(android.app.Fragment fragment) {
            return fragment.isAdded();
        }
    }

    private static final class OnSubscribeSupportFragment<T> extends OnSubscribeBase<T, android.support.v4.app.Fragment> {

        private OnSubscribeSupportFragment(Observable<T> source, android.support.v4.app.Fragment fragment) {
            super(source, fragment);
        }

        @Override
        protected boolean isComponentValid(android.support.v4.app.Fragment fragment) {
            return fragment.isAdded();
        }
    }
}
