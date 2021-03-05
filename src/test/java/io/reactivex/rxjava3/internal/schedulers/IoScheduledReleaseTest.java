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

import io.reactivex.rxjava3.core.Completable;
import io.reactivex.rxjava3.core.Flowable;
import io.reactivex.rxjava3.core.RxJavaTest;
import io.reactivex.rxjava3.schedulers.Schedulers;
import org.junit.Test;

import java.util.concurrent.TimeUnit;

public class IoScheduledReleaseTest extends RxJavaTest {

    /* This test will be stuck in a deadlock if IoScheduler.USE_SCHEDULED_RELEASE is not set */
    @Test
    public void scheduledRelease() {
        boolean savedScheduledRelease = IoScheduler.USE_SCHEDULED_RELEASE;
        IoScheduler.USE_SCHEDULED_RELEASE = true;
        try {
            Flowable.just("item")
                    .observeOn(Schedulers.io())
                    .firstOrError()
                    .map(item -> {
                        for (int i = 0; i < 50; i++) {
                            Completable.complete()
                                    .observeOn(Schedulers.io())
                                    .blockingAwait();
                        }
                        return "Done";
                    })
                    .ignoreElement()
                    .test()
                    .awaitDone(5, TimeUnit.SECONDS)
                    .assertComplete();
        } finally {
            IoScheduler.USE_SCHEDULED_RELEASE = savedScheduledRelease;
        }
    }
}
