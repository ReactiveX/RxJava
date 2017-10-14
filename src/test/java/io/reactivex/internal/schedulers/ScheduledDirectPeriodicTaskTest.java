/**
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

package io.reactivex.internal.schedulers;

import java.util.List;

import org.junit.Test;

import io.reactivex.TestHelper;
import io.reactivex.exceptions.TestException;
import io.reactivex.plugins.RxJavaPlugins;

public class ScheduledDirectPeriodicTaskTest {

    @Test
    public void runnableThrows() {
        List<Throwable> errors = TestHelper.trackPluginErrors();
        try {
            ScheduledDirectPeriodicTask task = new ScheduledDirectPeriodicTask(new Runnable() {
                @Override
                public void run() {
                    throw new TestException();
                }
            });

            task.run();

            TestHelper.assertUndeliverable(errors, 0, TestException.class);
        } finally {
            RxJavaPlugins.reset();
        }
    }
}
