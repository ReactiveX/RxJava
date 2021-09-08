/*
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

module io.reactivex.rxjava3 {
    exports io.reactivex.rxjava3.annotations;
    exports io.reactivex.rxjava3.core;
    exports io.reactivex.rxjava3.disposables;
    exports io.reactivex.rxjava3.exceptions;
    exports io.reactivex.rxjava3.flowables;
    exports io.reactivex.rxjava3.functions;
    exports io.reactivex.rxjava3.observables;
    exports io.reactivex.rxjava3.observers;
    exports io.reactivex.rxjava3.operators;
    exports io.reactivex.rxjava3.parallel;
    exports io.reactivex.rxjava3.plugins;
    exports io.reactivex.rxjava3.processors;
    exports io.reactivex.rxjava3.schedulers;
    exports io.reactivex.rxjava3.subjects;
    exports io.reactivex.rxjava3.subscribers;

    requires transitive org.reactivestreams;
}