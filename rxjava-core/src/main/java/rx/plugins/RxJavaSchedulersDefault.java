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
package rx.plugins;

import rx.Scheduler;
import rx.functions.Func0;

/**
 * Default implementation of {@link RxJavaErrorHandler} that does nothing.
 * 
 * @ExcludeFromJavadoc
 */
public class RxJavaSchedulersDefault extends RxJavaSchedulers {

    private static RxJavaSchedulersDefault INSTANCE = new RxJavaSchedulersDefault();

    public Func0<Scheduler> getComputationScheduler() {
        return null;
    }

    public Func0<Scheduler> getIOScheduler() {
        return null;
    }

    public Func0<Scheduler> getNewThreadScheduler() {
        return null;
    }

    public static RxJavaSchedulers getInstance() {
        return INSTANCE;
    }

}
