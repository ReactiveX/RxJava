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

package io.reactivex.rxjava3.internal.schedulers;

import static org.junit.Assert.*;

import org.junit.Test;

import io.reactivex.rxjava3.core.RxJavaTest;
import io.reactivex.rxjava3.functions.Function;
import io.reactivex.rxjava3.internal.functions.Functions;
import io.reactivex.rxjava3.testsupport.TestHelper;

public class SchedulerPoolFactoryTest extends RxJavaTest {

    @Test
    public void utilityClass() {
        TestHelper.checkUtilityClass(SchedulerPoolFactory.class);
    }

    @Test
    public void boolPropertiesDisabledReturnsDefaultDisabled() throws Throwable {
        assertTrue(SchedulerPoolFactory.getBooleanProperty(false, "key", false, true, failingPropertiesAccessor));
        assertFalse(SchedulerPoolFactory.getBooleanProperty(false, "key", true, false, failingPropertiesAccessor));
    }

    @Test
    public void boolPropertiesEnabledMissingReturnsDefaultMissing() throws Throwable {
        assertTrue(SchedulerPoolFactory.getBooleanProperty(true, "key", true, false, missingPropertiesAccessor));
        assertFalse(SchedulerPoolFactory.getBooleanProperty(true, "key", false, true, missingPropertiesAccessor));
    }

    @Test
    public void boolPropertiesFailureReturnsDefaultMissing() throws Throwable {
        assertTrue(SchedulerPoolFactory.getBooleanProperty(true, "key", true, false, failingPropertiesAccessor));
        assertFalse(SchedulerPoolFactory.getBooleanProperty(true, "key", false, true, failingPropertiesAccessor));
    }

    @Test
    public void boolPropertiesReturnsValue() throws Throwable {
        assertTrue(SchedulerPoolFactory.getBooleanProperty(true, "true", true, false, Functions.<String>identity()));
        assertFalse(SchedulerPoolFactory.getBooleanProperty(true, "false", false, true, Functions.<String>identity()));
    }

    static final Function<String, String> failingPropertiesAccessor = new Function<String, String>() {
        @Override
        public String apply(String v) throws Throwable {
            throw new SecurityException();
        }
    };

    static final Function<String, String> missingPropertiesAccessor = new Function<String, String>() {
        @Override
        public String apply(String v) throws Throwable {
            return null;
        }
    };
}
