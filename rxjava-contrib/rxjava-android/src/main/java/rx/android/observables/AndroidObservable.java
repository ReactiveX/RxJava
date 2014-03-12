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
package rx.android.observables;

import static rx.android.schedulers.AndroidSchedulers.mainThread;

import rx.Observable;
import rx.operators.OperatorObserveFromAndroidComponent;
import rx.operators.OperatorWeakBinding;

import android.app.Activity;
import android.app.Fragment;
import android.os.Build;


public final class AndroidObservable {

    private static final boolean USES_SUPPORT_FRAGMENTS;

    static {
        boolean supportFragmentsAvailable = false;
        try {
            Class.forName("android.support.v4.app.Fragment");
            supportFragmentsAvailable = true;
        } catch (ClassNotFoundException e) {
        }
        USES_SUPPORT_FRAGMENTS = supportFragmentsAvailable;
    }

    private AndroidObservable() {}

    /**
     * Transforms a source observable to be attached to the given Activity, in such a way that notifications will always
     * arrive on the main UI thread. Currently, this is equivalent to calling <code>observeOn(AndroidSchedulers.mainThread())</code>,
     * but this behavior may change in the future, so it is encouraged to use this wrapper instead.
     * <p/>
     * You must unsubscribe from the returned observable in <code>onDestroy</code> to not leak the given Activity.
     * <p/>
     * Ex.:
     * <pre>
     *     // in any Activity
     *     mSubscription = fromActivity(this, Observable.just("value")).subscribe(...);
     *     // in onDestroy
     *     mSubscription.unsubscribe();
     * </pre>
     *
     * @param activity         the activity in which the source observable will be observed
     * @param sourceObservable the observable sequence to observe from the given Activity
     * @param <T>
     * @return a new observable sequence that will emit notifications on the main UI thread
     */
    @Deprecated
    public static <T> Observable<T> fromActivity(Activity activity, Observable<T> sourceObservable) {
        Assertions.assertUiThread();
        return OperatorObserveFromAndroidComponent.observeFromAndroidComponent(sourceObservable, activity);
    }

    /**
     * Transforms a source observable to be attached to the given fragment, in such a way that notifications will always
     * arrive on the main UI thread. Moreover, it will be guaranteed that no notifications will be delivered to the
     * fragment while it's in detached state (i.e. its host Activity was destroyed.) In other words, during calls
     * to onNext, you may assume that fragment.getActivity() will never return null.
     * <p/>
     * This method accepts both native fragments and support library fragments in its first parameter. It will throw
     * for unsupported types.
     * <p/>
     * You must unsubscribe from the returned observable in <code>onDestroy</code> to not leak the given fragment.
     * <p/>
     * Ex.:
     * <pre>
     *     // in any Fragment
     *     mSubscription = fromFragment(this, Observable.just("value")).subscribe(...);
     *     // in onDestroy
     *     mSubscription.unsubscribe();
     * </pre>
     *
     * @param fragment         the fragment in which the source observable will be observed
     * @param sourceObservable the observable sequence to observe from the given fragment
     * @param <T>
     * @return a new observable sequence that will emit notifications on the main UI thread
     */
    @Deprecated
    public static <T> Observable<T> fromFragment(Object fragment, Observable<T> sourceObservable) {
        Assertions.assertUiThread();
        if (USES_SUPPORT_FRAGMENTS && fragment instanceof android.support.v4.app.Fragment) {
            return OperatorObserveFromAndroidComponent.observeFromAndroidComponent(sourceObservable, (android.support.v4.app.Fragment) fragment);
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB && fragment instanceof Fragment) {
            return OperatorObserveFromAndroidComponent.observeFromAndroidComponent(sourceObservable, (Fragment) fragment);
        } else {
            throw new IllegalArgumentException("Target fragment is neither a native nor support library Fragment");
        }
    }

    public static <T> Observable<T> bindActivity(Activity activity, Observable<T> cachedSequence) {
        return cachedSequence.observeOn(mainThread()).lift(new OperatorWeakBinding<T, Activity>(activity));
    }

    public static <T> Observable<T> bindFragment(Object fragment, Observable<T> cachedSequence) {
        Observable<T> source = cachedSequence.observeOn(mainThread());
        if (USES_SUPPORT_FRAGMENTS && fragment instanceof android.support.v4.app.Fragment) {
            android.support.v4.app.Fragment f = (android.support.v4.app.Fragment) fragment;
            return source.lift(new OperatorWeakBinding<T, android.support.v4.app.Fragment>(f));
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB && fragment instanceof Fragment) {
            Fragment f = (Fragment) fragment;
            return source.lift(new OperatorWeakBinding<T, Fragment>(f));
        } else {
            throw new IllegalArgumentException("Target fragment is neither a native nor support library Fragment");
        }
    }
}
