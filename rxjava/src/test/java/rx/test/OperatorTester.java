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
package rx.test;

import rx.Scheduler;

/**
 * Common utility functions for testing operator implementations.
 */
public class OperatorTester {
    /*
     * This is purposefully package-only so it does not leak into the public API outside of this package.
     * 
     * This package is implementation details and not part of the Javadocs and thus can change without breaking backwards compatibility.
     * 
     * benjchristensen => I'm procrastinating the decision of where and how these types of classes (see rx.subjects.UnsubscribeTester) should exist.
     * If they are only for internal implementations then I don't want them as part of the API.
     * If they are truly useful for everyone to use then an "rx.testing" package may make sense.
     */

    private OperatorTester() {
    }

    /**
     * Used for mocking of Schedulers since many Scheduler implementations are static/final.
     * 
     * @param underlying
     * @return
     */
    public static Scheduler forwardingScheduler(Scheduler underlying) {
        return new ForwardingScheduler(underlying);
    }

    public static class ForwardingScheduler extends Scheduler {
        private final Scheduler underlying;

        public ForwardingScheduler(Scheduler underlying) {
            this.underlying = underlying;
        }

        @Override
        public Worker createWorker() {
            return underlying.createWorker();
        }

    }
}