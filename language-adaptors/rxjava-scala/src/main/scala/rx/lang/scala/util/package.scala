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

  /**
   * Tagging interface for objects which can open buffers.
   * @see [[Observable.buffer]]
   */
  type Opening = rx.util.Opening

  /**
   * Creates an object which can open buffers.
   * @see [[Observable.buffer]]
   */
  def Opening() = rx.util.Openings.create()
  
  /**
   * Tagging interface for objects which can close buffers.
   * @see [[Observable.buffer]]
   */
  type Closing = rx.util.Closing

  /**
   * Creates an object which can close buffers.
   * @see [[Observable.buffer]]
   */
  def Closing() = rx.util.Closings.create()
  
  // rx.util.Range not needed because there's a standard Scala Range

  class Timestamped[+T](val asJava: rx.util.Timestamped[_ <: T]) {}
  
  object Timestamped {
    def apply[T](timestampMillis: Long, value: T): Timestamped[T] = {
      new Timestamped(new rx.util.Timestamped(timestampMillis, value))
    }
    
    def apply[T](asJava: rx.util.Timestamped[_ <: T]): Timestamped[T] = {
      new Timestamped(asJava)
    }
    
    def unapply[T](v: Timestamped[T]): Option[(Long, T)] = unapply(v.asJava)
    
    def unapply[T](v: rx.util.Timestamped[_ <: T]): Option[(Long, T)] = {
      Some((v.getTimestampMillis, v.getValue))
    }
  }
  
}