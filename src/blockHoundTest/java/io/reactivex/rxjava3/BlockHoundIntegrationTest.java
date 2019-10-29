/**
 * Copyright (c) 2019-present, RxJava Contributors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */

package io.reactivex.rxjava3;

import io.reactivex.rxjava3.internal.schedulers.RxThreadFactory;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import reactor.blockhound.BlockHound;

import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.FutureTask;
import java.util.concurrent.TimeUnit;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class BlockHoundIntegrationTest {

    @BeforeClass
    public static void beforeClass() {
        BlockHound.install();
    }

    @Test
    public void blockingCallInNonBlockingThread() throws Exception {
        FutureTask<Void> task = new FutureTask<Void>(new Callable<Void>() {
            @Override
            public Void call() throws Exception {
                Thread.sleep(0);
                return null;
            }
        });
        Thread thread = new RxThreadFactory("test-", 1, true).newThread(task);
        thread.start();

        try {
            task.get(5, TimeUnit.SECONDS);
            Assert.fail("Should fail");
        } catch (ExecutionException e) {
            Throwable throwable = e.getCause();
            assertNotNull("An exception was thrown", throwable);
            assertTrue("Blocking call was reported", throwable.getMessage().contains("Blocking call"));
        } finally {
            thread.interrupt();
        }
    }
}
