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

package rx.schedulers;

import java.util.concurrent.*;

import org.junit.*;

import rx.functions.Func0;
import rx.internal.schedulers.GenericScheduledExecutorService;
import rx.plugins.RxJavaHooks;

public class GenericScheduledExecutorServiceTest {

    @Test
    public void genericScheduledExecutorServiceHook() {
        // make sure the class is initialized
        Assert.assertNotNull(GenericScheduledExecutorService.class);

        final ScheduledExecutorService exec = Executors.newSingleThreadScheduledExecutor();
        try {

            RxJavaHooks.setOnGenericScheduledExecutorService(new Func0<ScheduledExecutorService>() {
                @Override
                public ScheduledExecutorService call() {
                    return exec;
                }
            });

            Schedulers.shutdown();
            Schedulers.start();

            Assert.assertSame(exec, GenericScheduledExecutorService.getInstance());

            RxJavaHooks.setOnGenericScheduledExecutorService(null);

            Schedulers.shutdown();
            // start() is package private so had to move this test here
            Schedulers.start();

            Assert.assertNotSame(exec, GenericScheduledExecutorService.getInstance());

        } finally {
            RxJavaHooks.reset();
            exec.shutdownNow();
        }

    }
}
