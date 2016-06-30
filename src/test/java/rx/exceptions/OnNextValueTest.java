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
package rx.exceptions;

import org.junit.Test;

import rx.Observable;
import rx.Observer;
import rx.exceptions.OnErrorThrowable.OnNextValue;
import rx.functions.Func1;

import java.io.PrintWriter;
import java.io.StringWriter;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * ```java
 * public OnNextValue(Object value) {
 *    super("OnError while emitting onNext value: " + value);
 *    this.value = value;
 * }
 * ```
 * I know this is probably a helpful error message in some cases but this can be a really costly operation when an objects toString is an expensive call or contains a lot of output. I don't think we should be printing this in any case but if so it should be on demand (overload of getMessage()) rather than eagerly.
 * <p/>
 * In my case it is causing a toString of a large context object that is normally only used for debugging purposes which makes the exception logs hard to use and they are rolling over the log files very quickly.
 * <p/>
 * There is an added danger that if there is a bug in the toString method it will cause inconsistent exception creation. If the object throws an exception while rendering a string it will actually end up not seeing the real exception.
 */
public final class OnNextValueTest {
    private static final class BadToString {

        private final boolean throwDuringToString;

        private BadToString(boolean throwDuringToString) {
            this.throwDuringToString = throwDuringToString;
        }

        @Override
        public String toString() {
            if (throwDuringToString) {
                throw new IllegalArgumentException("Error Making toString");
            } else {
                return "BadToString";
            }
        }
    }

    private static class BadToStringObserver implements Observer<BadToString> {
        @Override
        public void onCompleted() {
            System.out.println("On Complete");
            fail("OnComplete shouldn't be reached");
        }

        @Override
        public void onError(Throwable e) {
            String trace = stackTraceAsString(e);
            System.out.println("On Error: " + trace);

            assertTrue(trace, trace.contains("OnNextValue"));

            assertTrue("No Cause on throwable" + e, e.getCause() != null);
            assertTrue(e.getCause().getClass().getSimpleName() + " no OnNextValue",
                    e.getCause() instanceof OnErrorThrowable.OnNextValue);
        }

        @Override
        public void onNext(BadToString badToString) {
            System.out.println("On Next");
            fail("OnNext shouldn't be reached");

        }
    }

    public static String stackTraceAsString(Throwable e) {
        StringWriter sw = new StringWriter();
        e.printStackTrace(new PrintWriter(sw));
        return sw.toString();
    }

    @Test
    public void addOnNextValueExceptionAdded() throws Exception {
        Observer<BadToString> observer = new BadToStringObserver();

        Observable.just(new BadToString(false))
                .map(new Func1<BadToString, BadToString>() {
                    @Override
                    public BadToString call(BadToString badToString) {
                        throw new IllegalArgumentException("Failure while handling");
                    }
                }).subscribe(observer);

    }

    @Test
    public void addOnNextValueExceptionNotAddedWithBadString() throws Exception {
        Observer<BadToString> observer = new BadToStringObserver();

        Observable.just(new BadToString(true))
                .map(new Func1<BadToString, BadToString>() {
                    @Override
                    public BadToString call(BadToString badToString) {
                        throw new IllegalArgumentException("Failure while handling");
                    }
                }).subscribe(observer);

    }
    
    @Test
    public void testRenderInteger() {
        assertEquals("123", OnNextValue.renderValue(123));
    }
    
    @Test
    public void testRenderByte() {
        assertEquals("10", OnNextValue.renderValue((byte) 10));
    }
    
    @Test
    public void testRenderBoolean() {
        assertEquals("true", OnNextValue.renderValue(true));
    }
    
    @Test
    public void testRenderShort() {
        assertEquals("10", OnNextValue.renderValue((short) 10));
    }
    
    @Test
    public void testRenderLong() {
        assertEquals("10", OnNextValue.renderValue(10L));
    }
    
    @Test
    public void testRenderCharacter() {
        assertEquals("10", OnNextValue.renderValue(10L));
    }
    
    @Test
    public void testRenderFloat() {
        assertEquals("10.0", OnNextValue.renderValue(10.0f));
    }
    
    @Test
    public void testRenderDouble() {
        assertEquals("10.0", OnNextValue.renderValue(10.0));
    }
    
    @Test
    public void testRenderVoid() {
        assertEquals("null", OnNextValue.renderValue((Void) null));
    }
}
