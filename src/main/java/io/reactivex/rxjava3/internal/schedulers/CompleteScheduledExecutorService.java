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

package io.reactivex.rxjava3.internal.schedulers;

import java.util.concurrent.*;

/**
 *  A ScheduledExecutorService that supports setting a complete handler that
 *  will receive the Future of the submitted job once it completes.
 */
public interface CompleteScheduledExecutorService extends ScheduledExecutorService {

    interface CompleteHandler<V> {
        void onComplete(Future<V> task);
    }

    <V> Future<V> submit(Callable<V> task,
                         CompleteHandler<V> completeHandler);

    <V> Future<V> submit(Runnable task,
                         V result,
                         CompleteHandler<V> completeHandler);

    <V> ScheduledFuture<V> schedule(Callable<V> callable,
                                    long delay,
                                    TimeUnit unit,
                                    CompleteHandler<V> completeHandler);

    <V> ScheduledFuture<V> schedule(Runnable command,
                                    V result,
                                    long delay,
                                    TimeUnit unit,
                                    CompleteHandler<V> completeHandler);

    ScheduledFuture<Void> scheduleAtFixedRate(Runnable command,
                                              long initialDelay,
                                              long period,
                                              TimeUnit unit,
                                              CompleteHandler<Void> completeHandler);

    ScheduledFuture<Void> scheduleWithFixedDelay(Runnable command,
                                                 long initialDelay,
                                                 long delay,
                                                 TimeUnit unit,
                                                 CompleteHandler<Void> completeHandler);
}
