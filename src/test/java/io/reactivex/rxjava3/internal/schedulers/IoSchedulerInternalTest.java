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

package io.reactivex.rxjava3.internal.schedulers;

import static org.junit.Assert.*;
import java.util.concurrent.ConcurrentLinkedQueue;

import org.junit.Test;

import io.reactivex.rxjava3.core.RxJavaTest;
import io.reactivex.rxjava3.disposables.CompositeDisposable;
import io.reactivex.rxjava3.internal.schedulers.IoScheduler.*;
import io.reactivex.rxjava3.testsupport.TestHelper;

public class IoSchedulerInternalTest extends RxJavaTest {

    @Test
    public void expiredQueueEmpty() {
        ConcurrentLinkedQueue<ThreadWorker> expire = new ConcurrentLinkedQueue<>();
        CompositeDisposable cd = new CompositeDisposable();

        CachedWorkerPool.evictExpiredWorkers(expire, cd);
    }

    @Test
    public void expiredWorkerRemoved() {
        ConcurrentLinkedQueue<ThreadWorker> expire = new ConcurrentLinkedQueue<>();
        CompositeDisposable cd = new CompositeDisposable();

        ThreadWorker tw = new ThreadWorker(new RxThreadFactory("IoExpiryTest"));

        try {
            expire.add(tw);
            cd.add(tw);

            CachedWorkerPool.evictExpiredWorkers(expire, cd);

            assertTrue(tw.isDisposed());
            assertTrue(expire.isEmpty());
        } finally {
            tw.dispose();
        }
    }

    @Test
    public void noExpiredWorker() {
        ConcurrentLinkedQueue<ThreadWorker> expire = new ConcurrentLinkedQueue<>();
        CompositeDisposable cd = new CompositeDisposable();

        ThreadWorker tw = new ThreadWorker(new RxThreadFactory("IoExpiryTest"));
        tw.setExpirationTime(System.nanoTime() + 10_000_000_000L);

        try {
            expire.add(tw);
            cd.add(tw);

            CachedWorkerPool.evictExpiredWorkers(expire, cd);

            assertFalse(tw.isDisposed());
            assertFalse(expire.isEmpty());
        } finally {
            tw.dispose();
        }
    }

    @Test
    public void expireReuseRace() {
        ConcurrentLinkedQueue<ThreadWorker> expire = new ConcurrentLinkedQueue<>();
        CompositeDisposable cd = new CompositeDisposable();

        ThreadWorker tw = new ThreadWorker(new RxThreadFactory("IoExpiryTest"));
        tw.dispose();

        for (int i = 0; i < TestHelper.RACE_DEFAULT_LOOPS; i++) {
            expire.add(tw);
            cd.add(tw);

            TestHelper.race(
                    () -> CachedWorkerPool.evictExpiredWorkers(expire, cd),
                    () -> expire.remove(tw)
            );
        }
    }
}
