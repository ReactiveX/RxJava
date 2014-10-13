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
package rx.android.functions;

import android.app.Activity;
import android.view.View;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;

import rx.subjects.PublishSubject;

import static org.junit.Assert.assertEquals;

@RunWith(RobolectricTestRunner.class)
public class ViewActionSetVisibilityTest {

    private static View createView() {
        final Activity activity = Robolectric.buildActivity(Activity.class).create().get();
        return new View(activity);
    }

    @Test
    @SuppressWarnings("unchecked")
    public void testSetsViewVisibility() {
        final View view = createView();
        final PublishSubject<Boolean> subject = PublishSubject.create();
        subject.subscribe(ViewActions.setVisibility(view));

        assertEquals(View.VISIBLE, view.getVisibility());
        subject.onNext(false);
        assertEquals(View.GONE, view.getVisibility());
        subject.onNext(true);
        assertEquals(View.VISIBLE, view.getVisibility());
    }

    @Test
    @SuppressWarnings("unchecked")
    public void testSetsViewVisibilityWithCustomVisibility() {
        final View view = createView();
        final PublishSubject<Boolean> subject = PublishSubject.create();
        subject.subscribe(ViewActions.setVisibility(view, View.INVISIBLE));

        assertEquals(View.VISIBLE, view.getVisibility());
        subject.onNext(false);
        assertEquals(View.INVISIBLE, view.getVisibility());
        subject.onNext(true);
        assertEquals(View.VISIBLE, view.getVisibility());
    }

    @Test
    @SuppressWarnings("unchecked")
    public void testSetsViewVisibilityWithVisibleAsFalse() {
        final View view = createView();
        final PublishSubject<Boolean> subject = PublishSubject.create();
        subject.subscribe(ViewActions.setVisibility(view, View.VISIBLE));

        assertEquals(View.VISIBLE, view.getVisibility());
        subject.onNext(false);
        assertEquals(View.VISIBLE, view.getVisibility()); // shouldn't change
        subject.onNext(true);
        assertEquals(View.VISIBLE, view.getVisibility());
    }

    @Test(expected = IllegalArgumentException.class)
    @SuppressWarnings("unchecked")
    public void testSetsViewVisibilityOnlyAcceptsValidVisibilities() {
        final View view = createView();
        final PublishSubject<Boolean> subject = PublishSubject.create();
        int invalidVisibility = 123456;
        subject.subscribe(ViewActions.setVisibility(view, invalidVisibility));
    }

}
