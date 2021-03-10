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

package io.reactivex.rxjava3.internal.operators.observable;

import static org.junit.Assert.*;

import java.util.Arrays;
import java.util.Collections;
import java.util.concurrent.atomic.AtomicBoolean;

import io.reactivex.rxjava3.annotations.NonNull;
import org.junit.Test;

import io.reactivex.rxjava3.core.*;
import io.reactivex.rxjava3.disposables.*;
import io.reactivex.rxjava3.functions.Consumer;
import io.reactivex.rxjava3.observers.DefaultObserver;

public class ObservableSwitchIfEmptyTest extends RxJavaTest {

    @Test
    public void switchWhenNotEmpty() throws Exception {
        final AtomicBoolean subscribed = new AtomicBoolean(false);
        final Observable<Integer> o = Observable.just(4)
                .switchIfEmpty(Observable.just(2)
                .doOnSubscribe(d -> subscribed.set(true)));

        assertEquals(4, o.blockingSingle().intValue());
        assertFalse(subscribed.get());
    }

    @Test
    public void switchWhenEmpty() throws Exception {
        final Observable<Integer> o = Observable.<Integer>empty()
                .switchIfEmpty(Observable.fromIterable(Collections.singletonList(42)));

        assertEquals(42, o.blockingSingle().intValue());
    }

    @Test
    public void switchTriggerUnsubscribe() throws Exception {

        final Disposable d = Disposable.empty();

        Observable<Long> withProducer = Observable.unsafeCreate(observer -> {
            observer.onSubscribe(d);
            observer.onNext(42L);
        });

        Observable.<Long>empty()
                .switchIfEmpty(withProducer)
                .lift((ObservableOperator<Long, Long>) child -> new DefaultObserver<Long>() {
                    @Override
                    public void onComplete() {

                    }

                    @Override
                    public void onError(@NonNull Throwable e) {

                    }

                    @Override
                    public void onNext(@NonNull Long aLong) {
                        cancel();
                    }

                }).subscribe();

        assertTrue(d.isDisposed());
        // FIXME no longer assertable
//        assertTrue(sub.isUnsubscribed());
    }

    @Test
    public void switchShouldTriggerUnsubscribe() {
        final Disposable d = Disposable.empty();

        Observable.unsafeCreate((ObservableSource<Long>) observer -> {
            observer.onSubscribe(d);
            observer.onComplete();
        }).switchIfEmpty(Observable.never()).subscribe();
        assertTrue(d.isDisposed());
    }
}
