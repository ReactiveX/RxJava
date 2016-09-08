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


import org.junit.Test;
import rx.Scheduler;
import rx.internal.schedulers.*;
import rx.plugins.RxJavaPlugins;
import rx.plugins.RxJavaSchedulersHook;

import static org.junit.Assert.assertTrue;

public class ResetSchedulersTest {

    @SuppressWarnings("deprecation")
    @Test
    public void reset() {
        RxJavaPlugins.getInstance().reset();

        final TestScheduler testScheduler = new TestScheduler();
        RxJavaPlugins.getInstance().registerSchedulersHook(new RxJavaSchedulersHook() {
            @Override
            public Scheduler getComputationScheduler() {
                return testScheduler;
            }

            @Override
            public Scheduler getIOScheduler() {
                return testScheduler;
            }

            @Override
            public Scheduler getNewThreadScheduler() {
                return testScheduler;
            }
        });
        Schedulers.reset();

        assertTrue(Schedulers.io().equals(testScheduler));
        assertTrue(Schedulers.computation().equals(testScheduler));
        assertTrue(Schedulers.newThread().equals(testScheduler));

        RxJavaPlugins.getInstance().reset();
        RxJavaPlugins.getInstance().registerSchedulersHook(RxJavaSchedulersHook.getDefaultInstance());
        Schedulers.reset();

        assertTrue(Schedulers.io() instanceof CachedThreadScheduler);
        assertTrue(Schedulers.computation() instanceof EventLoopsScheduler);
        assertTrue(Schedulers.newThread() instanceof rx.internal.schedulers.NewThreadScheduler);
    }

}
