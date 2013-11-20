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

/**
 * Provides [[rx.lang.scala.util.Opening]]s, [[rx.lang.scala.util.Closing]]s, and [[rx.util.Timestamped]].
 */
package object util {

  /**
   * Tagging interface for objects which can open buffers.
   * @see [[rx.lang.scala.Observable `Observable.buffer(Observable[Opening], Opening => Observable[Closing])`]]
   */
  type Opening = rx.util.Opening

  /**
   * Creates an object which can open buffers.
   * @see [[rx.lang.scala.Observable `Observable.buffer(Observable[Opening], Opening => Observable[Closing])`]]
   */
  def Opening() = rx.util.Openings.create()
  
  /**
   * Tagging interface for objects which can close buffers.
   * @see [[rx.lang.scala.Observable `Observable.buffer(Observable[Opening], Opening => Observable[Closing])`]]
   */
  type Closing = rx.util.Closing

  /**
   * Creates an object which can close buffers.
   * @see [[rx.lang.scala.Observable `Observable.buffer(Observable[Opening], Opening => Observable[Closing])`]]
   */
  def Closing() = rx.util.Closings.create()
  
  // rx.util.Range not needed because there's a standard Scala Range
  
}
