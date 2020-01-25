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

package io.reactivex.rxjava3.internal.operators.flowable;

import org.reactivestreams.Subscriber;

import io.reactivex.rxjava3.core.*;
import io.reactivex.rxjava3.disposables.Disposable;
import io.reactivex.rxjava3.internal.disposables.DisposableHelper;
import io.reactivex.rxjava3.internal.fuseable.*;

/**
 * Wrap a Completable into a Flowable.
 *
 * @param <T> the value type
 * @since 3.0.0
 */
public final class FlowableFromCompletable<T> extends Flowable<T> implements HasUpstreamCompletableSource {

    final CompletableSource source;

    public FlowableFromCompletable(CompletableSource source) {
        this.source = source;
    }

    @Override
    public CompletableSource source() {
        return source;
    }

    @Override
    protected void subscribeActual(Subscriber<? super T> observer) {
        source.subscribe(new FromCompletableObserver<T>(observer));
    }

    public static final class FromCompletableObserver<T>
    extends AbstractEmptyQueueFuseable<T>
    implements CompletableObserver {

        final Subscriber<? super T> downstream;

        Disposable upstream;

        public FromCompletableObserver(Subscriber<? super T> downstream) {
            this.downstream = downstream;
        }

        @Override
        public void cancel() {
            upstream.dispose();
            upstream = DisposableHelper.DISPOSED;
        }

        @Override
        public void onSubscribe(Disposable d) {
            if (DisposableHelper.validate(this.upstream, d)) {
                this.upstream = d;

                downstream.onSubscribe(this);
            }
        }

        @Override
        public void onComplete() {
            upstream = DisposableHelper.DISPOSED;
            downstream.onComplete();
        }

        @Override
        public void onError(Throwable e) {
            upstream = DisposableHelper.DISPOSED;
            downstream.onError(e);
        }
    }
}
