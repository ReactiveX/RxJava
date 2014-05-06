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
package rx.lang.clojure.interop;

// Dummy class with some overloads to make sure that type hinting works
// correctly with the fn and action macros. Used only for testing.
public class DummyObservable {

    public String call(Object f) {
        return "Object";
    }

    public String call(rx.functions.Func1 f) {
        return "rx.functions.Func1";
    }

    public String call(rx.functions.Func2 f) {
        return "rx.functions.Func2";
    }

    public String call(rx.functions.Action1 f) {
        return "rx.functions.Action1";
    }

    public String call(rx.functions.Action2 f) {
        return "rx.functions.Action2";
    }

}
