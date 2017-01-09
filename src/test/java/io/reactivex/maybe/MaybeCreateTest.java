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

package io.reactivex.maybe;

import static org.junit.Assert.assertTrue;

import org.junit.Test;

import io.reactivex.*;
import io.reactivex.disposables.*;
import io.reactivex.exceptions.TestException;
import io.reactivex.functions.Cancellable;

public class MaybeCreateTest {
    @Test(expected = NullPointerException.class)
    public void nullArgument() {
        Maybe.create(null);
    }

    @Test
    public void basic() {
        final Disposable d = Disposables.empty();

        Maybe.<Integer>create(new MaybeOnSubscribe<Integer>() {
            @Override
            public void subscribe(MaybeEmitter<Integer> e) throws Exception {
                e.setDisposable(d);

                e.onSuccess(1);
                e.onError(new TestException());
                e.onSuccess(2);
                e.onError(new TestException());
            }
        }).test().assertResult(1);

        assertTrue(d.isDisposed());
    }

    @Test
    public void basicWithCancellable() {
        final Disposable d1 = Disposables.empty();
        final Disposable d2 = Disposables.empty();

        Maybe.<Integer>create(new MaybeOnSubscribe<Integer>() {
            @Override
            public void subscribe(MaybeEmitter<Integer> e) throws Exception {
                e.setDisposable(d1);
                e.setCancellable(new Cancellable() {
                    @Override
                    public void cancel() throws Exception {
                        d2.dispose();
                    }
                });

                e.onSuccess(1);
                e.onError(new TestException());
                e.onSuccess(2);
                e.onError(new TestException());
            }
        }).test().assertResult(1);

        assertTrue(d1.isDisposed());
        assertTrue(d2.isDisposed());
    }

    @Test
    public void basicWithError() {
        final Disposable d = Disposables.empty();

        Maybe.<Integer>create(new MaybeOnSubscribe<Integer>() {
            @Override
            public void subscribe(MaybeEmitter<Integer> e) throws Exception {
                e.setDisposable(d);

                e.onError(new TestException());
                e.onSuccess(2);
                e.onError(new TestException());
            }
        }).test().assertFailure(TestException.class);

        assertTrue(d.isDisposed());
    }


    @Test
    public void basicWithCompletion() {
        final Disposable d = Disposables.empty();

        Maybe.<Integer>create(new MaybeOnSubscribe<Integer>() {
            @Override
            public void subscribe(MaybeEmitter<Integer> e) throws Exception {
                e.setDisposable(d);

                e.onComplete();
                e.onSuccess(2);
                e.onError(new TestException());
            }
        }).test().assertComplete();

        assertTrue(d.isDisposed());
    }

    @Test(expected = IllegalArgumentException.class)
    public void unsafeCreate() {
        Maybe.unsafeCreate(Maybe.just(1));
    }
}
