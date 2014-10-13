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

public class ViewActionSetVisibility implements Action1<Boolean> {

    private final View view;
    private final int visibilityOnFalse;

    public ViewActionSetVisibility(View view) {
        this(view, View.GONE);
    }

    public ViewActionSetVisibility(View view, int visibilityOnFalse) {
        if (visibilityOnFalse != View.GONE &&
                visibilityOnFalse != View.INVISIBLE &&
                visibilityOnFalse != View.VISIBLE) {
            throw new IllegalArgumentException(visibilityOnFalse +
                    " is not a valid visibility value. See android.view.View VISIBLE, GONE, and INVISIBLE");
        }
        this.view = view;
        this.visibilityOnFalse = visibilityOnFalse;
    }

    @Override
    public void call(Boolean aBoolean) {
        int visibility = aBoolean ? View.VISIBLE : visibilityOnFalse;
        view.setVisibility(visibility);
    }
}

