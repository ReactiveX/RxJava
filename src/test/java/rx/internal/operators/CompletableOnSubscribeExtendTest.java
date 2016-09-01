/*
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

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.*;

import org.junit.Test;

import rx.*;
import rx.Completable.*;
import rx.functions.Func1;

import static org.junit.Assert.*;

public final class CompletableOnSubscribeExtendTest {
    @Test
    public void convertToBoolean() {
        boolean completeWorked = Completable.complete().extend(toBoolean());
        assertTrue(completeWorked);

        RuntimeException e = new RuntimeException();
        boolean errorWorked = Completable.error(e).extend(toBoolean());
        assertFalse(errorWorked);
    }

    private Func1<CompletableOnSubscribe, Boolean> toBoolean() {
        return new Func1<CompletableOnSubscribe, Boolean>() {
            @Override
            public Boolean call(CompletableOnSubscribe onSubscribe) {
                final AtomicBoolean worked = new AtomicBoolean();
                final CountDownLatch latch = new CountDownLatch(1);
                onSubscribe.call(new CompletableSubscriber() {
                    @Override
                    public void onCompleted() {
                        worked.set(true);
                        latch.countDown();
                    }

                    @Override
                    public void onError(Throwable e) {
                        worked.set(false);
                        latch.countDown();
                    }

                    @Override
                    public void onSubscribe(Subscription d) {
                    }
                });
                try {
                    latch.await();
                } catch (InterruptedException e) {
                    return false;
                }
                return worked.get();
            }
        };
    }
}
