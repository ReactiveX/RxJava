/**
 * Copyright 2014 Netflix, Inc.
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
/**
 * <p>Rx Observables</p>
 * 
 * <p>A library that enables subscribing to and composing asynchronous events and
 * callbacks.</p>
 * <p>The Observable/Observer interfaces and associated operators (in
 * the .operations package) are inspired by and attempt to conform to the
 * Reactive Rx library in Microsoft .Net.</p>
 * <p>
 * More information can be found at <a
 * href="http://msdn.microsoft.com/en-us/data/gg577609">http://msdn.microsoft.com/en-us/data/gg577609</a>.
 * </p>
 * 
 * 
 * <p>Compared with the Microsoft implementation:
 * <ul>
 * <li>Observable == IObservable</li>
 * <li>Observer == IObserver</li>
 * <li>Subscription == IDisposable</li>
 * <li>ObservableExtensions == Observable</li>
 * </ul>
 * </p>
 * <p>Services which intend on exposing data asynchronously and wish
 * to allow reactive processing and composition can implement the {@link rx.Observable} interface which then allows Observers to subscribe to them
 * and receive events.</p>
 * <p>Usage examples can be found on the {@link rx.Observable} and {@link rx.Subscriber} classes.</p>
 */
package rx;

