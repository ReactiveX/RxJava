/**
 * Copyright 2016 Netflix, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in
 * compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is
 * distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See
 * the License for the specific language governing permissions and limitations under the License.
 */

package io.reactivex;

import io.reactivex.functions.Function;

/**
 * Convenience interface and callback used by the lift operator that given a child CompletableSubscriber,
 * return a parent CompletableSubscriber that does any kind of lifecycle-related transformations.
 */
public interface CompletableOperator extends Function<CompletableObserver, CompletableObserver> {

}
