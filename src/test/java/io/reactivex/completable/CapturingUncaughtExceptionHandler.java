/**
 * Copyright (c) 2016-present, RxJava Contributors.
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

package io.reactivex.completable;

import java.util.concurrent.CountDownLatch;

public final class CapturingUncaughtExceptionHandler implements Thread.UncaughtExceptionHandler {
    public int count;
    public Throwable caught;
    public CountDownLatch completed = new CountDownLatch(1);

    @Override
    public void uncaughtException(Thread t, Throwable e) {
        count++;
        caught = e;
        completed.countDown();
    }
}
