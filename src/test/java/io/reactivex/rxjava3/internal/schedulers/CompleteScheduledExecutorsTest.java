/**
 * Copyright (c) 2016-present, RxJava Contributors.
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.reactivex.rxjava3.internal.schedulers;

import com.google.common.util.concurrent.ThreadFactoryBuilder;
import io.reactivex.rxjava3.core.RxJavaTest;
import io.reactivex.rxjava3.testsupport.TestHelper;
import org.junit.Test;

import java.util.concurrent.RejectedExecutionHandler;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

public class CompleteScheduledExecutorsTest extends RxJavaTest {

    @Test
    public void constructorShouldBePrivate() {
        TestHelper.checkUtilityClass(CompleteScheduledExecutors.class);
    }

    @Test
    public void newExecutorTest() {
        ThreadFactory threadFactory = (r) -> new Thread(r);
        RejectedExecutionHandler rejectedHandler = (r, e) -> {
        };

        assertExecutorAndParam(
                CompleteScheduledExecutors.newSingleThreadExecutor(),
                1, null, null);

        assertExecutorAndParam(
                CompleteScheduledExecutors.newSingleThreadExecutor(threadFactory),
                1, threadFactory, null);

        assertExecutorAndParam(
                CompleteScheduledExecutors.newSingleThreadExecutor(rejectedHandler),
                1, null, rejectedHandler);

        assertExecutorAndParam(
                CompleteScheduledExecutors.newSingleThreadExecutor(threadFactory, rejectedHandler),
                1, threadFactory, rejectedHandler);

        assertExecutorAndParam(
                CompleteScheduledExecutors.newThreadPoolExecutor(6),
                6, null, null);

        assertExecutorAndParam(
                CompleteScheduledExecutors.newThreadPoolExecutor(6, threadFactory),
                6, threadFactory, null);

        assertExecutorAndParam(
                CompleteScheduledExecutors.newThreadPoolExecutor(6, rejectedHandler),
                6, null, rejectedHandler);

        assertExecutorAndParam(
                CompleteScheduledExecutors.newThreadPoolExecutor(6, threadFactory, rejectedHandler),
                6, threadFactory, rejectedHandler);
    }

    private void assertExecutorAndParam(CompleteScheduledExecutorService executor,
                                        int corePoolSize,
                                        ThreadFactory threadFactory,
                                        RejectedExecutionHandler rejectHandler) {
        assertNotNull(executor);

        if (executor instanceof ThreadPoolExecutor) {
            ThreadPoolExecutor ex = (ThreadPoolExecutor) executor;

            assertEquals(corePoolSize, ex.getCorePoolSize());

            if (null != threadFactory) {
                assertEquals(threadFactory, ex.getThreadFactory());
            }

            if (null != rejectHandler) {
                assertEquals(rejectHandler, ex.getRejectedExecutionHandler());
            }
        }
    }
}
