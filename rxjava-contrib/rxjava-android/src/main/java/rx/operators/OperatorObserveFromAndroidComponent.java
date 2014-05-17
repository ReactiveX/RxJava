/**
 * Copyright 2014 Netflix, Inc.
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
import rx.Subscriber;
import rx.android.schedulers.AndroidSchedulers;
import rx.functions.Action0;
import rx.subscriptions.Subscriptions;

import android.app.Activity;
import android.util.Log;

@Deprecated
public class OperatorObserveFromAndroidComponent {

    public static <T> Observable<T> observeFromAndroidComponent(Observable<T> source, android.app.Fragment fragment) {
        return Observable.create(new OnSubscribeFragment<T>(source, fragment));
    }

    public static <T> Observable<T> observeFromAndroidComponent(Observable<T> source, android.support.v4.app.Fragment fragment) {
        return Observable.create(new OnSubscribeSupportFragment<T>(source, fragment));
    }

    public static <T> Observable<T> observeFromAndroidComponent(Observable<T> source, Activity activity) {
        return Observable.create(new OnSubscribeBase<T, Activity>(source, activity));
    }

    private static class OnSubscribeBase<T, AndroidComponent> implements Observable.OnSubscribe<T> {

        private static final String LOG_TAG = "AndroidObserver";

        private final Observable<T> source;
        private volatile AndroidComponent componentRef;
        private volatile Observer<? super T> observerRef;

        private OnSubscribeBase(Observable<T> source, AndroidComponent component) {
            this.source = source;
            this.componentRef = component;
        }

        private void log(String message) {
            if (Log.isLoggable(LOG_TAG, Log.DEBUG)) {
                String thread = Thread.currentThread().getName();
                Log.d(LOG_TAG, "[" + thread + "] componentRef = " + componentRef + "; observerRef = " + observerRef);
                Log.d(LOG_TAG, "[" + thread + "]" + message);
            }
        }

        protected boolean isComponentValid(AndroidComponent component) {
            return true;
        }

        @Override
        public void call(final Subscriber<? super T> subscriber) {
            observerRef = subscriber;
            source.observeOn(AndroidSchedulers.mainThread()).subscribe(new Subscriber<T>(subscriber) {
                @Override
                public void onCompleted() {
                    if (componentRef != null && isComponentValid(componentRef)) {
                        observerRef.onCompleted();
                    } else {
                        unsubscribe();
                        log("onComplete: target component released or detached; dropping message");
                    }
                }

                @Override
                public void onError(Throwable e) {
                    if (componentRef != null && isComponentValid(componentRef)) {
                        observerRef.onError(e);
                    } else {
                        unsubscribe();
                        log("onError: target component released or detached; dropping message");
                    }
                }

                @Override
                public void onNext(T args) {
                    if (componentRef != null && isComponentValid(componentRef)) {
                        observerRef.onNext(args);
                    } else {
                        unsubscribe();
                        log("onNext: target component released or detached; dropping message");
                    }
                }
            });
            subscriber.add(Subscriptions.create(new Action0() {
                @Override
                public void call() {
                    log("unsubscribing from source sequence");
                    releaseReferences();
                }
            }));
        }

        private void releaseReferences() {
            observerRef = null;
            componentRef = null;
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
