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

import static org.mockito.Mockito.verify;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;
import rx.Observable;
import rx.Observer;
import rx.operators.OperationObserveFromAndroidComponent;

import android.app.Activity;
import android.app.Fragment;
import android.os.Build;
import android.support.v4.app.FragmentActivity;


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
    public static <T> Observable<T> fromActivity(Activity activity, Observable<T> sourceObservable) {
        return OperationObserveFromAndroidComponent.observeFromAndroidComponent(sourceObservable, activity);
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
    public static <T> Observable<T> fromFragment(Object fragment, Observable<T> sourceObservable) {
        if (USES_SUPPORT_FRAGMENTS && fragment instanceof android.support.v4.app.Fragment) {
            return OperationObserveFromAndroidComponent.observeFromAndroidComponent(sourceObservable, (android.support.v4.app.Fragment) fragment);
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB && fragment instanceof Fragment) {
            return OperationObserveFromAndroidComponent.observeFromAndroidComponent(sourceObservable, (Fragment) fragment);
        } else {
            throw new IllegalArgumentException("Target fragment is neither a native nor support library Fragment");
        }
    }

    @RunWith(RobolectricTestRunner.class)
    @Config(manifest = Config.NONE)
    public static final class AndroidObservableTest {

        // support library fragments
        private FragmentActivity fragmentActivity;
        private android.support.v4.app.Fragment supportFragment;

        // native fragments
        private Activity activity;
        private Fragment fragment;

        @Mock
        private Observer<String> observer;

        @Before
        public void setup() {
            MockitoAnnotations.initMocks(this);
            supportFragment = new android.support.v4.app.Fragment();
            fragmentActivity = Robolectric.buildActivity(FragmentActivity.class).create().get();
            fragmentActivity.getSupportFragmentManager().beginTransaction().add(supportFragment, null).commit();

            fragment = new Fragment();
            activity = Robolectric.buildActivity(Activity.class).create().get();
            activity.getFragmentManager().beginTransaction().add(fragment, null).commit();
        }

        @Test
        public void itSupportsFragmentsFromTheSupportV4Library() {
            fromFragment(supportFragment, Observable.just("success")).subscribe(observer);
            verify(observer).onNext("success");
            verify(observer).onCompleted();
        }

        @Test
        public void itSupportsNativeFragments() {
            fromFragment(fragment, Observable.just("success")).subscribe(observer);
            verify(observer).onNext("success");
            verify(observer).onCompleted();
        }

        @Test(expected = IllegalArgumentException.class)
        public void itThrowsIfObjectPassedIsNotAFragment() {
            fromFragment("not a fragment", Observable.never());
        }
    }

}
