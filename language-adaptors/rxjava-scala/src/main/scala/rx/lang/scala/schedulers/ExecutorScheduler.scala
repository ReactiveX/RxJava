/**
 * Copyright 2013 Netflix, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package rx.lang.scala.schedulers

import java.util.concurrent.Executor
import rx.lang.scala.Scheduler

object ExecutorScheduler {

  /**
  * Returns a [[rx.lang.scala.Scheduler]] that queues work on an `java.util.concurrent.Executor`.
  *
  * Note that this does not support scheduled actions with a delay.
  */
  def apply(executor: Executor): ExecutorScheduler =  {
    new ExecutorScheduler(rx.concurrency.Schedulers.executor(executor))
  }
}


class ExecutorScheduler private[scala] (val asJavaScheduler: rx.Scheduler)
  extends Scheduler {}



