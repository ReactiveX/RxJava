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

import static org.junit.Assert.*;
import org.junit.Test;

import rx.Completable;
import rx.functions.Func1;
import rx.observers.AssertableSubscriber;
import rx.subjects.PublishSubject;

public class CompletableOnErrorXTest {

    @Test
    public void nextUnsubscribe() {
        PublishSubject<Integer> ps = PublishSubject.create();
        
        AssertableSubscriber<Void> as = ps.toCompletable()
        .onErrorResumeNext(new Func1<Throwable, Completable>() {
            @Override
            public Completable call(Throwable e) {
                return Completable.complete();
            }
        })
        .test();

        assertTrue(ps.hasObservers());

        as.unsubscribe();

        assertFalse("Still subscribed!", ps.hasObservers());
    }

    @Test
    public void completeUnsubscribe() {
        PublishSubject<Integer> ps = PublishSubject.create();
        
        AssertableSubscriber<Void> as = ps.toCompletable()
        .onErrorComplete()
        .test();

        assertTrue(ps.hasObservers());

        as.unsubscribe();

        assertFalse("Still subscribed!", ps.hasObservers());
    }
}
