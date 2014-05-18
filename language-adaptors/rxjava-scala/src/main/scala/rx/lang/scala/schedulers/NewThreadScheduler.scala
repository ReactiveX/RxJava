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

object NewThreadScheduler {

  /**
   * Returns a [[rx.lang.scala.Scheduler]] that creates a new `java.lang.Thread` for each unit of work.
   */
  def apply(): NewThreadScheduler =  {
    new NewThreadScheduler(rx.schedulers.Schedulers.newThread())
  }
}

class NewThreadScheduler private[scala] (val asJavaScheduler: rx.Scheduler) extends Scheduler {}
