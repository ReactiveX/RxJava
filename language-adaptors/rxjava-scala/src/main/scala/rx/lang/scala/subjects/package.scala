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
package rx.lang.scala

/**
 * Provides the type `Subject`.
 */
package object subjects {

  /**
   * A Subject is an Observable and an Observer at the same time. 
   * 
   * The Java Subject looks like this: 
   * {{{
   * public abstract class Subject<T,R> extends Observable<R> implements Observer<T>
   * }}}
   */
  type Subject[-T, +R] = rx.subjects.Subject[_ >: T, _ <: R]

  // For nicer scaladoc, we would like to present something like this:
  /*
  trait Observable[+R] {}
  trait Observer[-T] {}
  trait Subject[-T, +R] extends Observable[R] with Observer[T] { }
  */
  
  // We don't make aliases to these types, because they are considered internal/not needed by users:
  // rx.subjects.AsyncSubject
  // rx.subjects.BehaviorSubject
  // rx.subjects.PublishSubject
  // rx.subjects.ReplaySubject

}