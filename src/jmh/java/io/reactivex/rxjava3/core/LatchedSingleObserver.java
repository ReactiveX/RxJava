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

package io.reactivex.rxjava3.core;

import java.util.concurrent.CountDownLatch;

import org.openjdk.jmh.infra.Blackhole;

import io.reactivex.rxjava3.disposables.Disposable;

public final class LatchedSingleObserver<T> implements SingleObserver<T> {
    final CountDownLatch cdl;
    final Blackhole bh;
    public LatchedSingleObserver(Blackhole bh) {
        this.bh = bh;
        this.cdl = new CountDownLatch(1);
    }

    @Override
    public void onSubscribe(Disposable d) {

    }

    @Override
    public void onSuccess(T value) {
        bh.consume(value);
        cdl.countDown();
    }

    @Override
    public void onError(Throwable e) {
        e.printStackTrace();
        cdl.countDown();
    }
}
