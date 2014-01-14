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

import rx.android.observables.AndroidObservable;


@RunWith(RobolectricTestRunner.class)
@Config(manifest = Config.NONE)
public class AndroidObservableTest {

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
        AndroidObservable.fromFragment(supportFragment, Observable.just("success")).subscribe(observer);
        verify(observer).onNext("success");
        verify(observer).onCompleted();
    }

    @Test
    public void itSupportsNativeFragments() {
        AndroidObservable.fromFragment(fragment, Observable.just("success")).subscribe(observer);
        verify(observer).onNext("success");
        verify(observer).onCompleted();
    }

    @Test(expected = IllegalArgumentException.class)
    public void itThrowsIfObjectPassedIsNotAFragment() {
        AndroidObservable.fromFragment("not a fragment", Observable.never());
    }
}
