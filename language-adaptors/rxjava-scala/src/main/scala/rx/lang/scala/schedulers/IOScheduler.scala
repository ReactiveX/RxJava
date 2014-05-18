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

import rx.lang.scala.Scheduler

object IOScheduler {
  /**
   * [[rx.lang.scala.Scheduler]] intended for IO-bound work.
   * <p>
   * The implementation is backed by an `Executor` thread-pool that will grow as needed.
   * <p>
   * This can be used for asynchronously performing blocking IO.
   * <p>
   * Do not perform computational work on this scheduler. Use [[rx.lang.scala.schedulers.ComputationScheduler]] instead.
   *
   * @return [[rx.lang.scala.Scheduler]] for IO-bound work
   */
  def apply(): IOScheduler = {
    new IOScheduler(rx.schedulers.Schedulers.io)
  }
}

class IOScheduler private[scala] (val asJavaScheduler: rx.Scheduler)
  extends Scheduler {}

