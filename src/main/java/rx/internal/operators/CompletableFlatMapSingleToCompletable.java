/**
 * Copyright 2016 Netflix, Inc.
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

package rx.internal.operators;

import rx.Completable;
import rx.Completable.OnSubscribe;
import rx.CompletableSubscriber;
import rx.Single;
import rx.SingleSubscriber;
import rx.Subscription;
import rx.exceptions.Exceptions;
import rx.functions.Func1;

public final class CompletableFlatMapSingleToCompletable<T> implements OnSubscribe {

    final Single<T> source;

    final Func1<? super T, ? extends Completable> mapper;

    public CompletableFlatMapSingleToCompletable(Single<T> source, Func1<? super T, ? extends Completable> mapper) {
        this.source = source;
        this.mapper = mapper;
    }

    @Override
    public void call(CompletableSubscriber t) {
        SourceSubscriber<T> parent = new SourceSubscriber<T>(t, mapper);
        t.onSubscribe(parent);
        source.subscribe(parent);
    }

    static final class SourceSubscriber<T> extends SingleSubscriber<T> implements CompletableSubscriber {
        final CompletableSubscriber actual;

        final Func1<? super T, ? extends Completable> mapper;

        public SourceSubscriber(CompletableSubscriber actual, Func1<? super T, ? extends Completable> mapper) {
            this.actual = actual;
            this.mapper = mapper;
        }

        @Override
        public void onSuccess(T value) {
            Completable c;

            try {
                c = mapper.call(value);
            } catch (Throwable ex) {
                Exceptions.throwIfFatal(ex);
                onError(ex);
                return;
            }

            if (c == null) {
                onError(new NullPointerException("The mapper returned a null Completable"));
                return;
            }

            c.subscribe(this);
        }

        @Override
        public void onError(Throwable error) {
            actual.onError(error);
        }

        @Override
        public void onCompleted() {
            actual.onCompleted();
        }

        @Override
        public void onSubscribe(Subscription d) {
            add(d);
        }
    }

}
