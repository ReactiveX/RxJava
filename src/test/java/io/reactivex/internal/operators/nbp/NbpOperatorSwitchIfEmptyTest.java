/**
 * Copyright 2015 Netflix, Inc.
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

package io.reactivex.internal.operators.nbp;

import static org.junit.Assert.*;

import java.util.Arrays;
import java.util.concurrent.atomic.AtomicBoolean;

import org.junit.Test;

import io.reactivex.*;
import io.reactivex.NbpObservable.*;
import io.reactivex.disposables.BooleanDisposable;


public class NbpOperatorSwitchIfEmptyTest {

    @Test
    public void testSwitchWhenNotEmpty() throws Exception {
        final AtomicBoolean subscribed = new AtomicBoolean(false);
        final NbpObservable<Integer> o = NbpObservable.just(4)
                .switchIfEmpty(NbpObservable.just(2)
                .doOnSubscribe(s -> subscribed.set(true)));

        assertEquals(4, o.toBlocking().single().intValue());
        assertFalse(subscribed.get());
    }

    @Test
    public void testSwitchWhenEmpty() throws Exception {
        final NbpObservable<Integer> o = NbpObservable.<Integer>empty()
                .switchIfEmpty(NbpObservable.fromIterable(Arrays.asList(42)));

        assertEquals(42, o.toBlocking().single().intValue());
    }

    @Test
    public void testSwitchTriggerUnsubscribe() throws Exception {

        BooleanDisposable bs = new BooleanDisposable();
        
        NbpObservable<Long> withProducer = NbpObservable.create(new NbpOnSubscribe<Long>() {
            @Override
            public void accept(final NbpSubscriber<? super Long> NbpSubscriber) {
                NbpSubscriber.onSubscribe(bs);
                NbpSubscriber.onNext(42L);
            }
        });

        NbpObservable.<Long>empty()
                .switchIfEmpty(withProducer)
                .lift(new NbpObservable.NbpOperator<Long, Long>() {
            @Override
            public NbpSubscriber<? super Long> apply(final NbpSubscriber<? super Long> child) {
                return new NbpObserver<Long>() {
                    @Override
                    public void onComplete() {

                    }

                    @Override
                    public void onError(Throwable e) {

                    }

                    @Override
                    public void onNext(Long aLong) {
                        cancel();
                    }
                    
                };
            }
        }).subscribe();


        assertTrue(bs.isDisposed());
        // FIXME no longer assertable
//        assertTrue(sub.isUnsubscribed());
    }

    @Test
    public void testSwitchShouldTriggerUnsubscribe() {
        BooleanDisposable bs = new BooleanDisposable();
        
        NbpObservable.create(new NbpOnSubscribe<Long>() {
            @Override
            public void accept(final NbpSubscriber<? super Long> NbpSubscriber) {
                NbpSubscriber.onSubscribe(bs);
                NbpSubscriber.onComplete();
            }
        }).switchIfEmpty(NbpObservable.<Long>never()).subscribe();
        assertTrue(bs.isDisposed());
    }
}