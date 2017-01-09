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
package io.reactivex.exceptions;

import org.junit.*;

/**
 * Checks the Exception classes to verify they don't crash with null argument.
 */
public class ExceptionsNullTest {

    @Ignore("OnCompleteFailedException will likely not be ported")
    @Test
    public void testOnCompleteFailedExceptionNull() {
//        Throwable t = new OnCompleteFailedException(null);
//
//        Assert.assertTrue(t.getCause() instanceof NullPointerException);
    }

    @Ignore("OnCompleteFailedException will likely not be ported")
    @Test
    public void testOnCompleteFailedExceptionMessageAndNull() {
//        Throwable t = new OnCompleteFailedException("Message", null);
//
//        Assert.assertTrue(t.getCause() instanceof NullPointerException);
    }

    @Ignore("OnErrorFailedException will likely not be ported")
    @Test
    public void testOnErrorFailedExceptionNull() {
//        Throwable t = new OnErrorFailedException(null);
//
//        Assert.assertTrue(t.getCause() instanceof NullPointerException);
    }

    @Ignore("OnErrorFailedException will likely not be ported")
    @Test
    public void testOnErrorFailedExceptionMessageAndNull() {
//        Throwable t = new OnErrorFailedException("Message", null);
//
//        Assert.assertTrue(t.getCause() instanceof NullPointerException);
    }

    @Ignore("UnsubscribeFailedException will likely not be ported")
    @Test
    public void testUnsubscribeFailedExceptionNull() {
//        Throwable t = new UnsubscribeFailedException(null);
//
//        Assert.assertTrue(t.getCause() instanceof NullPointerException);
    }

    @Ignore("UnsubscribeFailedException will likely not be ported")
    @Test
    public void testUnsubscribeFailedExceptionMessageAndNull() {
//        Throwable t = new UnsubscribeFailedException("Message", null);
//
//        Assert.assertTrue(t.getCause() instanceof NullPointerException);
    }

    @Ignore("OnErrorNotImplementedException will likely not be ported")
    @Test
    public void testOnErrorNotImplementedExceptionNull() {
//        Throwable t = new OnErrorNotImplementedException(null);
//
//        Assert.assertTrue(t.getCause() instanceof NullPointerException);
    }

    @Ignore("OnErrorNotImplementedException will likely not be ported")
    @Test
    public void testOnErrorNotImplementedExceptionMessageAndNull() {
//        Throwable t = new OnErrorNotImplementedException("Message", null);
//
//        Assert.assertTrue(t.getCause() instanceof NullPointerException);
    }

    @Ignore("OnErrorThrowable may be ported later")
    @Test
    public void testOnErrorThrowableFrom() {
//        Throwable t = OnErrorThrowable.from(null);
//        Assert.assertTrue(t.getCause() instanceof NullPointerException);
    }

    @Ignore("OnErrorThrowable may be ported later")
    @Test
    public void testOnErrorThrowableAddValueAsLastCause() {
//        Throwable t = OnErrorThrowable.addValueAsLastCause(null, "value");
//        Assert.assertTrue(t instanceof NullPointerException);
    }

}
