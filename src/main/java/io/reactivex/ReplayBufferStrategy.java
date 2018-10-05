/**
 * Copyright (c) 2016-present, RxJava Contributors.
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

public enum ReplayBufferStrategy {
    /**
     * The replay subject would buffer <em>all</em> onNext values.
     * */
    ALWAYS,
    /**
     * The replay subject would buffer only while no downstream is available to consume the onNext values. Buffered
     * values would be emitted to any downstream until the onNext is get called, which means <em>only</em> in
     * onNext calls the buffered values would be cleaned.
     * */
    NO_OBSERVER,
    /**
     * The replay subject would buffer only while no downstream is available to consume the onNext values. After the
     * first downstream is subscribed all buffered values would be emitted then buffer would be cleaned.
     * */
    NO_OBSERVER_EMIT_ONCE
}
