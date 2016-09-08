/**
 * Copyright 2016 Netflix, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package rx.internal.util;

import static org.junit.Assert.*;

import java.util.concurrent.atomic.AtomicReference;

import org.junit.*;

import rx.exceptions.TestException;

public class ExceptionUtilsTest {
    AtomicReference<Throwable> error = new AtomicReference<Throwable>();

    @Test
    public void addToTerminatedFalse() {
        ExceptionsUtils.terminate(error);
        Assert.assertFalse(ExceptionsUtils.addThrowable(error, new TestException()));
    }

    @Test
    public void doubleTerminate() {
        Assert.assertNull(ExceptionsUtils.terminate(error));
        Assert.assertNotNull(ExceptionsUtils.terminate(error));
    }

    @Test
    public void isTerminated() {
        Assert.assertFalse(ExceptionsUtils.isTerminated(error));
        Assert.assertFalse(ExceptionsUtils.isTerminated(error.get()));

        ExceptionsUtils.addThrowable(error, new TestException());

        Assert.assertFalse(ExceptionsUtils.isTerminated(error));
        Assert.assertFalse(ExceptionsUtils.isTerminated(error.get()));

        ExceptionsUtils.terminate(error);

        Assert.assertTrue(ExceptionsUtils.isTerminated(error));
        Assert.assertTrue(ExceptionsUtils.isTerminated(error.get()));
    }

    @Test
    public void utilityEnum() {
        assertEquals(0, ExceptionsUtils.values().length);
        try {
            ExceptionsUtils.valueOf("INSTANCE");
            fail("Failed to throw IllegalArgumentException");
        } catch (IllegalArgumentException ex) {
            // expected
        }
    }
}
