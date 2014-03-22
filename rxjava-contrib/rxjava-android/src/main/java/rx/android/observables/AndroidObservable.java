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
import rx.functions.Func1;
import rx.operators.OperatorObserveFromAndroidComponent;
import rx.operators.OperatorWeakBinding;
import rx.operators.OperatorWeakBinding.BoundPayload;

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

    private static final Func1<Activity, Boolean> ACTIVITY_VALIDATOR = new Func1<Activity, Boolean>() {
        @Override
        public Boolean call(Activity activity) {
            return !activity.isFinishing();
        }
    };

    private static final Func1<Fragment, Boolean> FRAGMENT_VALIDATOR = new Func1<Fragment, Boolean>() {
        @Override
        public Boolean call(Fragment fragment) {
            return fragment.isAdded();
        }
    };

    private static final Func1<android.support.v4.app.Fragment, Boolean> FRAGMENTV4_VALIDATOR =
            new Func1<android.support.v4.app.Fragment, Boolean>() {
                @Override
                public Boolean call(android.support.v4.app.Fragment fragment) {
                    return fragment.isAdded();
                }
            };

    private AndroidObservable() {
    }

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
     * @deprecated Use {@link #bindActivity(android.app.Activity, rx.Observable)} instead
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
     * @deprecated Use {@link #bindFragment(Fragment, rx.Observable)} or
     *             {@link #bindFragment(android.support.v4.app.Fragment, rx.Observable)} instead
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

    /**
     * Binds the given source sequence to the life-cycle of an activity.
     * <p/>
     * This helper will schedule the given sequence to be observed on the main UI thread and ensure
     * that no notifications will be forwarded to the activity in case it gets destroyed by the Android runtime
     * or garbage collected by the VM.
     *
     * @param activity         the activity to bind the source sequence to
     * @param sourceObservable the source sequence
     */
    public static <A extends  Activity, T> Observable<BoundPayload<A, T>> bindActivity(A activity, Observable<T> sourceObservable) {
        Assertions.assertUiThread();
        return sourceObservable.observeOn(mainThread()).lift(new OperatorWeakBinding<T, A>(activity, ACTIVITY_VALIDATOR));
    }

    /**
     * Binds the given source sequence to the life-cycle of a fragment (native).
     * <p/>
     * This helper will schedule the given sequence to be observed on the main UI thread and ensure
     * that no notifications will be forwarded to the fragment in case it gets detached from its
     * activity or garbage collected by the VM.
     *
     * @param fragment         the fragment to bind the source sequence to
     * @param sourceObservable the source sequence
     */
    public static <F extends Fragment, T> Observable<BoundPayload<F, T>> bindFragment(F fragment, Observable<T> sourceObservable) {
        Assertions.assertUiThread();
        return sourceObservable.lift(new OperatorWeakBinding<T, F>(fragment, FRAGMENT_VALIDATOR));
    }

    /**
     * Binds the given source sequence to the life-cycle of a fragment (support-v4).
     * <p/>
     * This helper will schedule the given sequence to be observed on the main UI thread and ensure
     * that no notifications will be forwarded to the fragment in case it gets detached from its
     * activity or garbage collected by the VM.
     *
     * @param fragment         the fragment to bind the source sequence to
     * @param sourceObservable the source sequence
     */
    public static <F extends android.support.v4.app.Fragment, T> Observable<BoundPayload<F, T>> bindFragment(F fragment, Observable<T> sourceObservable) {
        Assertions.assertUiThread();
        return sourceObservable.lift(new OperatorWeakBinding<T, F>(fragment, FRAGMENTV4_VALIDATOR));
    }

}
