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

package io.reactivex.internal.operators.completable;

import static org.junit.Assert.*;
import org.junit.Test;

import io.reactivex.processors.PublishProcessor;
import io.reactivex.subjects.PublishSubject;

public class CompletableSubscribeTest {
    @Test
    public void subscribeAlreadyCancelled() {

        PublishProcessor<Integer> pp = PublishProcessor.create();

        pp.ignoreElements().test(true);

        assertFalse(pp.hasSubscribers());
    }


    @Test
    public void methodTestNoCancel() {
        PublishSubject<Integer> ps = PublishSubject.create();

        ps.ignoreElements().test(false);

        assertTrue(ps.hasObservers());
    }
}
