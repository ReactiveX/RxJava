/**
 * Copyright 2016 Netflix, Inc.
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

package io.reactivex.internal.operators.maybe;

import static org.junit.Assert.*;

import java.util.concurrent.*;

import org.junit.Test;

import io.reactivex.functions.Action;
import io.reactivex.processors.PublishProcessor;
import io.reactivex.schedulers.Schedulers;

public class MaybeUnsubscribeOnTest {

    @Test
    public void normal() throws Exception {
        PublishProcessor<Integer> pp = PublishProcessor.create();

        final String[] name = { null };

        final CountDownLatch cdl = new CountDownLatch(1);

        pp.doOnCancel(new Action() {
            @Override
            public void run() throws Exception {
                name[0] = Thread.currentThread().getName();
                cdl.countDown();
            }
        })
        .toMaybe()
        .unsubscribeOn(Schedulers.single())
        .test(true)
        ;

        assertTrue(cdl.await(5, TimeUnit.SECONDS));

        int times = 10;

        while (times-- > 0 && pp.hasSubscribers()) {
            Thread.sleep(100);
        }

        assertFalse(pp.hasSubscribers());

        assertNotEquals(Thread.currentThread().getName(), name[0]);
    }
}
