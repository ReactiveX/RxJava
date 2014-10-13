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

public class ViewActionSetSelected implements Action1<Boolean> {

    private final View view;

    public ViewActionSetSelected(View view) {
        this.view = view;
    }

    @Override
    public void call(Boolean aBoolean) {
        view.setSelected(aBoolean);
    }
}

