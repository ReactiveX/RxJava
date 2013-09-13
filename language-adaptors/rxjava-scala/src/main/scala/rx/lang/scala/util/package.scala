/**
 * Copyright 2013 Netflix, Inc.
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
package rx.lang.scala

package object util {
  type Closing = rx.util.Closing

  object Closings {
    def create(): Closing = rx.util.Closings.create()
  }

  type CompositeException = rx.util.CompositeException

  // TODO not sure if we need this in Scala
  object Exceptions {
    def propageate(ex: Throwable) = rx.util.Exceptions.propagate(ex)
  }

  // rx.util.OnErrorNotImplementedException TODO what's this?

  type Opening = rx.util.Opening

  object Openings {
    def create(): Opening = rx.util.Openings.create()
  }

  // rx.util.Range not needed because there's a standard Scala Range

  type Timestamped[+T] = rx.util.Timestamped[_ <: T]
  object Timestamped {
    def apply[T](timestampMillis: Long, value: T): Timestamped[T] = {
      new rx.util.Timestamped(timestampMillis, value)
    }
  }
}