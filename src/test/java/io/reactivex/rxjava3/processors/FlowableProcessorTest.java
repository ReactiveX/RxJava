/**
 * Copyright (c) 2016-present, RxJava Contributors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in
 * compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is
 * distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See
 * the License for the specific language governing permissions and limitations under the License.
 */

package io.reactivex.rxjava3.processors;

import static org.junit.Assert.*;

import org.junit.Test;

import io.reactivex.rxjava3.core.RxJavaTest;
import io.reactivex.rxjava3.internal.util.ExceptionHelper;

public abstract class FlowableProcessorTest<T>  extends RxJavaTest {

    protected abstract FlowableProcessor<T> create();

    @Test
    public void onNextNull() {
        FlowableProcessor<T> p = create();

        try {
            p.onNext(null);
            fail("No NullPointerException thrown");
        } catch (NullPointerException ex) {
            assertEquals(ExceptionHelper.nullWarning("onNext called with a null value."), ex.getMessage());
        }

        p.test().assertEmpty().cancel();
    }

    @Test
    public void onErrorNull() {
        FlowableProcessor<T> p = create();

        try {
            p.onError(null);
            fail("No NullPointerException thrown");
        } catch (NullPointerException ex) {
            assertEquals(ExceptionHelper.nullWarning("onError called with a null Throwable."), ex.getMessage());
        }

        p.test().assertEmpty().cancel();
    }
}
