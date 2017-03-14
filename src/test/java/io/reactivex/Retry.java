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

import org.junit.rules.TestRule;
import org.junit.runner.Description;
import org.junit.runners.model.Statement;

/**
 * Test rule to retry flaky tests.
 * <a href="http://stackoverflow.com/a/8301639/61158">From Stackoverflow</a>.
 */
public class Retry implements TestRule {

    final class RetryStatement extends Statement {
        private final Statement base;
        private final Description description;

        RetryStatement(Statement base, Description description) {
            this.base = base;
            this.description = description;
        }

        @Override
        public void evaluate() throws Throwable {
            Throwable caughtThrowable = null;

            for (int i = 0; i < retryCount; i++) {
                try {
                    base.evaluate();
                    return;
                } catch (Throwable t) {
                    caughtThrowable = t;
                    System.err.println(description.getDisplayName() + ": run " + (i + 1) + " failed");
                    int n = sleep;
                    if (backoff && i != 0) {
                        n = n * (2 << i);
                    }
                    Thread.sleep(n);
                }
            }
            System.err.println(description.getDisplayName() + ": giving up after " + retryCount + " failures");
            throw caughtThrowable;
        }
    }

    final int retryCount;

    final int sleep;

    final boolean backoff;

    public Retry(int retryCount, int sleep, boolean backoff) {
        this.retryCount = retryCount;
        this.sleep = sleep;
        this.backoff = backoff;
    }

    @Override
    public Statement apply(Statement base, Description description) {
        return statement(base, description);
    }

    private Statement statement(final Statement base, final Description description) {
        return new RetryStatement(base, description);
    }
}