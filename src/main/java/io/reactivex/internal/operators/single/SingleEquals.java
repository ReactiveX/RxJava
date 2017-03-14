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

import java.util.concurrent.atomic.AtomicInteger;

import io.reactivex.*;
import io.reactivex.disposables.*;
import io.reactivex.internal.functions.ObjectHelper;
import io.reactivex.plugins.RxJavaPlugins;

public final class SingleEquals<T> extends Single<Boolean> {

    final SingleSource<? extends T> first;
    final SingleSource<? extends T> second;

    public SingleEquals(SingleSource<? extends T> first, SingleSource<? extends T> second) {
        this.first = first;
        this.second = second;
    }

    @Override
    protected void subscribeActual(final SingleObserver<? super Boolean> s) {

        final AtomicInteger count = new AtomicInteger();
        final Object[] values = { null, null };

        final CompositeDisposable set = new CompositeDisposable();
        s.onSubscribe(set);

        first.subscribe(new InnerObserver<T>(0, set, values, s, count));
        second.subscribe(new InnerObserver<T>(1, set, values, s, count));
    }

    static class InnerObserver<T> implements SingleObserver<T> {
        final int index;
        final CompositeDisposable set;
        final Object[] values;
        final SingleObserver<? super Boolean> s;
        final AtomicInteger count;

        InnerObserver(int index, CompositeDisposable set, Object[] values, SingleObserver<? super Boolean> s, AtomicInteger count) {
            this.index = index;
            this.set = set;
            this.values = values;
            this.s = s;
            this.count = count;
        }
        @Override
        public void onSubscribe(Disposable d) {
            set.add(d);
        }

        @Override
        public void onSuccess(T value) {
            values[index] = value;

            if (count.incrementAndGet() == 2) {
                s.onSuccess(ObjectHelper.equals(values[0], values[1]));
            }
        }

        @Override
        public void onError(Throwable e) {
            for (;;) {
                int state = count.get();
                if (state >= 2) {
                    RxJavaPlugins.onError(e);
                    return;
                }
                if (count.compareAndSet(state, 2)) {
                    set.dispose();
                    s.onError(e);
                    return;
                }
            }
        }

    }

}
