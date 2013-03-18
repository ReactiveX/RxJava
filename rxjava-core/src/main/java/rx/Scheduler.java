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
package rx;

import rx.util.functions.Action0;
import rx.util.functions.Func0;

import java.util.concurrent.TimeUnit;

public interface Scheduler {

    Subscription schedule(Action0 action);

    Subscription schedule(Func0<Subscription> action);

    Subscription schedule(Action0 action, long timespan, TimeUnit unit);

    Subscription schedule(Func0<Subscription> action, long timespan, TimeUnit unit);

    long now();

}
