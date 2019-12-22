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

package io.reactivex.rxjava3.internal.operators.completable;

import org.junit.Test;

import io.reactivex.rxjava3.core.*;
import io.reactivex.rxjava3.exceptions.TestException;
import io.reactivex.rxjava3.functions.Function;
import io.reactivex.rxjava3.subjects.CompletableSubject;
import io.reactivex.rxjava3.testsupport.TestHelper;

public class CompletableMaterializeTest extends RxJavaTest {

    @Test
    public void error() {
        TestException ex = new TestException();
        Completable.error(ex)
        .materialize()
        .test()
        .assertResult(Notification.createOnError(ex));
    }

    @Test
    public void empty() {
        Completable.complete()
        .materialize()
        .test()
        .assertResult(Notification.createOnComplete());
    }

    @Test
    public void doubleOnSubscribe() {
        TestHelper.checkDoubleOnSubscribeCompletableToSingle(new Function<Completable, SingleSource<Notification<Object>>>() {
            @Override
            public SingleSource<Notification<Object>> apply(Completable v) throws Exception {
                return v.materialize();
            }
        });
    }

    @Test
    public void dispose() {
        TestHelper.checkDisposed(CompletableSubject.create().materialize());
    }
}
