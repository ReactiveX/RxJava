/**
 * Copyright (c) 2016-present, RxJava Contributors.
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
package io.reactivex.rxjava3.testsupport;

import org.junit.rules.TestRule;
import org.junit.runner.Description;
import org.junit.runners.model.Statement;

import io.reactivex.rxjava3.exceptions.UndeliverableException;
import io.reactivex.rxjava3.plugins.RxJavaPlugins;

/**
 * A rule for suppressing UndeliverableException handling.
 *
 * <p>Test classes that use this rule can suppress UndeliverableException
 * handling by annotating the test method with SuppressUndeliverable.
 */
public class SuppressUndeliverableRule implements TestRule {

    static final class SuppressUndeliverableRuleStatement extends Statement {
        private Statement base;

        SuppressUndeliverableRuleStatement(Statement base) {
            this.base = base;
        }

        @Override
        public void evaluate() throws Throwable {
            try {
                RxJavaPlugins.setErrorHandler(throwable -> {
                    if (!(throwable instanceof UndeliverableException)) {
                        throwable.printStackTrace();
                        Thread currentThread = Thread.currentThread();
                        currentThread.getUncaughtExceptionHandler().uncaughtException(currentThread, throwable);
                    }
                });
                base.evaluate();
            } finally {
                RxJavaPlugins.setErrorHandler(null);
            }
        }
    }

    @Override
    public Statement apply(Statement base, Description description) {
        if (description != null && description.getAnnotation(SuppressUndeliverable.class) != null) {
            return new SuppressUndeliverableRuleStatement(base);
        } else {
            return base;
        }
    }
}
