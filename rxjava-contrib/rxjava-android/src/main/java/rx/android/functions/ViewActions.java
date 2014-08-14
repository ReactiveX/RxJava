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

import android.view.View;

import rx.functions.Action1;

/**
 * Utility class for the Action interfaces for use with Android {@link View}s.
 */
public class ViewActions {

    private ViewActions() {
        throw new IllegalStateException("No instances!");
    }

    /**
     * Set whether a view is enabled based on {@code boolean} values that are emitted by an
     * Observable.
     *
     * @param view
     *            to modify
     * @return an {@link Action1} that sets the enabled of a view when boolean values are emitted.
     */
    public static Action1<? super Boolean> setEnabled(View view) {
        return new ViewActionSetEnabled(view);
    }

    /**
     * Set whether a view is activated based on {@code boolean} values that are emitted by an
     * Observable.
     *
     * @param view
     *            to modify
     * @return an {@link Action1} that sets the activated of a view when boolean values are emitted.
     */
    public static Action1<? super Boolean> setActivated(View view) {
        return new ViewActionSetActivated(view);
    }

    /**
     * Set whether a view is clickable based on {@code boolean} values that are emitted by an
     * Observable.
     *
     * @param view
     *            to modify
     * @return an {@link Action1} that sets the clickable of a view when boolean values are emitted.
     */
    public static Action1<? super Boolean> setClickable(View view) {
        return new ViewActionSetClickable(view);
    }

    /**
     * Set whether a view is focusable based on {@code boolean} values that are emitted by an
     * Observable.
     *
     * @param view
     *            to modify
     * @return an {@link Action1} that sets the focusable of a view when boolean values are emitted.
     */
    public static Action1<? super Boolean> setFocusable(View view) {
        return new ViewActionSetFocusable(view);
    }

    /**
     * Set whether a view is selected based on {@code boolean} values that are emitted by an
     * Observable.
     *
     * @param view
     *            to modify
     * @return an {@link Action1} that sets the selected of a view when boolean values are emitted.
     */
    public static Action1<? super Boolean> setSelected(View view) {
        return new ViewActionSetSelected(view);
    }

    /**
     * Set the visibility of a view based on {@code boolean} values that are emitted by an
     * Observable. When {@code false} is emitted, visibility is set to {@link View#GONE}.
     *
     * @param view
     *            to modify
     * @return an {@link Action1} that sets the visibility of a view when boolean values are emitted.
     */
    public static Action1<? super Boolean> setVisibility(View view) {
        return new ViewActionSetVisibility(view);
    }

    /**
     * Set the visibility of a view based on {@code boolean} values that are emitted by an
     * Observable.
     *
     * @param view
     *            to modify
     * @param visibilityOnFalse
     *            value to use on {@link View#setVisibility(int)} when a {@code false} is emitted by
     *            the {@link rx.Observable}
     * @return an {@link Action1} that sets the visibility of a view when boolean values are emitted.
     */
    public static Action1<? super Boolean> setVisibility(View view, int visibilityOnFalse) {
        return new ViewActionSetVisibility(view, visibilityOnFalse);
    }
}
