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

import io.reactivex.*;
import io.reactivex.disposables.Disposable;
import io.reactivex.exceptions.*;
import io.reactivex.functions.Predicate;

public final class CompletableOnErrorComplete extends Completable {

    final CompletableSource source;

    final Predicate<? super Throwable> predicate;

    public CompletableOnErrorComplete(CompletableSource source, Predicate<? super Throwable> predicate) {
        this.source = source;
        this.predicate = predicate;
    }

    @Override
    protected void subscribeActual(final CompletableObserver s) {

        source.subscribe(new OnError(s));
    }

    final class OnError implements CompletableObserver {

        private final CompletableObserver s;

        OnError(CompletableObserver s) {
            this.s = s;
        }

        @Override
        public void onComplete() {
            s.onComplete();
        }

        @Override
        public void onError(Throwable e) {
            boolean b;

            try {
                b = predicate.test(e);
            } catch (Throwable ex) {
                Exceptions.throwIfFatal(ex);
                s.onError(new CompositeException(e, ex));
                return;
            }

            if (b) {
                s.onComplete();
            } else {
                s.onError(e);
            }
        }

        @Override
        public void onSubscribe(Disposable d) {
            s.onSubscribe(d);
        }

    }
}
