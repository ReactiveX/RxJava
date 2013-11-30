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
package rx.lang.scala.concurrency

import rx.lang.scala.Scheduler

object CurrentThreadScheduler {

  /**
   * Returns a [[rx.lang.scala.Scheduler]] that queues work on the current thread to be executed after the current work completes.
   */
  def apply(): CurrentThreadScheduler =  {
    new CurrentThreadScheduler(rx.concurrency.Schedulers.currentThread())
  }
}

class CurrentThreadScheduler private[scala] (val asJavaScheduler: rx.Scheduler)
  extends Scheduler {}