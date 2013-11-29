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
 * Contains special Observables.
 * 
 * In Scala, this package only contains [[rx.lang.scala.observables.BlockingObservable]].
 * In the corresponding Java package `rx.observables`, there is also a
 * `GroupedObservable` and a `ConnectableObservable`, but these are not needed
 * in Scala, because we use a pair `(key, observable)` instead of `GroupedObservable`
 * and a pair `(startFunction, observable)` instead of `ConnectableObservable`. 
 */
package object observables {}
