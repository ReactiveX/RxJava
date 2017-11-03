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

package io.reactivex.internal.operators.single;

import io.reactivex.Single;
import io.reactivex.TestHelper;
import io.reactivex.functions.Function;
import io.reactivex.subjects.PublishSubject;
import org.junit.Test;
import org.reactivestreams.Publisher;

public class SingleToFlowableTest {

    @Test
    public void dispose() {
        TestHelper.checkDisposed(PublishSubject.create().singleOrError().toFlowable());
    }

    @Test
    public void doubleOnSubscribe() {
        TestHelper.checkDoubleOnSubscribeSingleToFlowable(new Function<Single<Object>, Publisher<Object>>() {
            @Override
            public Publisher<Object> apply(Single<Object> s) throws Exception {
                return s.toFlowable();
            }
        });
    }
}
