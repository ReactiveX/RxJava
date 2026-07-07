/*
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

package io.reactivex.rxjava4.internal.operators.streamable;

import static org.junit.jupiter.api.Assertions.*;

import java.util.NoSuchElementException;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.Test;

import io.reactivex.rxjava4.core.*;
import io.reactivex.rxjava4.disposables.*;
import io.reactivex.rxjava4.exceptions.TestException;
import io.reactivex.rxjava4.subjects.CompletableSubject;

public class StreamableFromCompletableTest extends StreamableBaseTest {

    @Test
    public void normal() throws Throwable {
        Streamable.fromCompletable(Completable.complete())
        .test()
        .awaitDone(5, TimeUnit.SECONDS)
        .assertResult();
    }

    @Test
    public void normalViaCompletable() throws Throwable {
        Completable.complete()
        .toStreamable()
        .test()
        .awaitDone(5, TimeUnit.SECONDS)
        .assertResult();
    }

    @Test
    public void crash() throws Throwable {
        Completable.error(new TestException())
        .toStreamable()
        .test()
        .awaitDone(5, TimeUnit.SECONDS)
        .assertFailure(TestException.class);
    }

    @Test
    public void dispose() {
        var cs = CompletableSubject.create();

        var cd = new CompositeDisposable();
        var ts = cs.toStreamable().stream(cd);

        assertFalse((ts instanceof Disposable d) && d.isDisposed(), "Disposed?");
        assertTrue(cs.hasObservers(), "has no observers?");

        assertThrows(NoSuchElementException.class, () -> {
            var _ = ts.current();
        });

        cd.dispose();

        assertFalse(cs.hasObservers(), "has observers?");
        assertTrue((ts instanceof Disposable d) && d.isDisposed(), "Not Disposed?");
    }
}
