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
package rx.observables;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.fail;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.StringReader;
import java.nio.charset.Charset;
import java.nio.charset.CharsetDecoder;
import java.nio.charset.MalformedInputException;
import java.util.Arrays;
import java.util.List;

import org.junit.Test;

import rx.Observable;
import rx.Observer;
import rx.observables.StringObservable.Line;
import rx.observers.TestObserver;
import rx.util.AssertObservable;

public class StringObservableTest {

    @Test
    public void testMultibyteSpanningTwoBuffers() {
        Observable<byte[]> src = Observable.from(new byte[] { (byte) 0xc2 }, new byte[] { (byte) 0xa1 });
        String out = StringObservable.decode(src, "UTF-8").toBlockingObservable().single();

        assertEquals("\u00A1", out);
    }

    @Test
    public void testMalformedAtTheEndReplace() {
        Observable<byte[]> src = Observable.from(new byte[] { (byte) 0xc2 });
        String out = StringObservable.decode(src, "UTF-8").toBlockingObservable().single();

        // REPLACEMENT CHARACTER
        assertEquals("\uFFFD", out);
    }

    @Test
    public void testMalformedInTheMiddleReplace() {
        Observable<byte[]> src = Observable.from(new byte[] { (byte) 0xc2, 65 });
        String out = StringObservable.decode(src, "UTF-8").toBlockingObservable().single();

        // REPLACEMENT CHARACTER
        assertEquals("\uFFFDA", out);
    }

    @Test(expected = RuntimeException.class)
    public void testMalformedAtTheEndReport() {
        Observable<byte[]> src = Observable.from(new byte[] { (byte) 0xc2 });
        CharsetDecoder charsetDecoder = Charset.forName("UTF-8").newDecoder();
        StringObservable.decode(src, charsetDecoder).toBlockingObservable().single();
    }

    @Test(expected = RuntimeException.class)
    public void testMalformedInTheMiddleReport() {
        Observable<byte[]> src = Observable.from(new byte[] { (byte) 0xc2, 65 });
        CharsetDecoder charsetDecoder = Charset.forName("UTF-8").newDecoder();
        StringObservable.decode(src, charsetDecoder).toBlockingObservable().single();
    }

    @Test
    public void testPropogateError() {
        Observable<byte[]> src = Observable.from(new byte[] { 65 });
        Observable<byte[]> err = Observable.error(new IOException());
        CharsetDecoder charsetDecoder = Charset.forName("UTF-8").newDecoder();
        try {
            StringObservable.decode(Observable.concat(src, err), charsetDecoder).toList().toBlockingObservable().single();
            fail();
        } catch (RuntimeException e) {
            assertEquals(IOException.class, e.getCause().getClass());
        }
    }

    @Test
    public void testPropogateErrorInTheMiddleOfMultibyte() {
        Observable<byte[]> src = Observable.from(new byte[] { (byte) 0xc2 });
        Observable<byte[]> err = Observable.error(new IOException());
        CharsetDecoder charsetDecoder = Charset.forName("UTF-8").newDecoder();
        try {
            StringObservable.decode(Observable.concat(src, err), charsetDecoder).toList().toBlockingObservable().single();
            fail();
        } catch (RuntimeException e) {
            assertEquals(MalformedInputException.class, e.getCause().getClass());
        }
    }

    @Test
    public void testEncode() {
        assertArrayEquals(
                new byte[] { (byte) 0xc2, (byte) 0xa1 },
                StringObservable.encode(Observable.just("\u00A1"), "UTF-8").toBlockingObservable().single());
    }

    @Test
    public void testSplitOnCollon() {
        testSplit("boo:and:foo", ":", 0, "boo", "and", "foo");
    }

    @Test
    public void testSplitOnOh() {
        testSplit("boo:and:foo", "o", 0, "b", "", ":and:f");
    }

    public void testSplit(String str, String regex, int limit, String... parts) {
        testSplit(str, regex, 0, Observable.from(str), parts);
        for (int i = 0; i < str.length(); i++) {
            String a = str.substring(0, i);
            String b = str.substring(i, str.length());
            testSplit(a + "|" + b, regex, limit, Observable.from(a, b), parts);
        }
    }

    public void testSplit(String message, String regex, int limit, Observable<String> src, String... parts) {
        Observable<String> act = StringObservable.split(src, regex);
        Observable<String> exp = Observable.from(parts);
        AssertObservable.assertObservableEqualsBlocking("when input is " + message + " and limit = " + limit, exp, act);
    }

    @Test
    public void testJoinMixed() {
        Observable<Object> source = Observable.<Object> from(Arrays.<Object>asList("a", 1, "c"));

        Observable<String> result = StringObservable.join(source, ", ");

        @SuppressWarnings("unchecked")
        Observer<Object> observer = mock(Observer.class);

        result.subscribe(new TestObserver<Object>(observer));

        verify(observer, times(1)).onNext("a, 1, c");
        verify(observer, times(1)).onCompleted();
        verify(observer, never()).onError(any(Throwable.class));
    }

    @Test
    public void testJoinWithEmptyString() {
        Observable<String> source = Observable.from("", "b", "c");

        Observable<String> result = StringObservable.join(source, ", ");

        @SuppressWarnings("unchecked")
        Observer<Object> observer = mock(Observer.class);

        result.subscribe(new TestObserver<Object>(observer));

        verify(observer, times(1)).onNext(", b, c");
        verify(observer, times(1)).onCompleted();
        verify(observer, never()).onError(any(Throwable.class));
    }

    @Test
    public void testJoinWithNull() {
        Observable<String> source = Observable.from("a", null, "c");

        Observable<String> result = StringObservable.join(source, ", ");

        @SuppressWarnings("unchecked")
        Observer<Object> observer = mock(Observer.class);

        result.subscribe(new TestObserver<Object>(observer));

        verify(observer, times(1)).onNext("a, null, c");
        verify(observer, times(1)).onCompleted();
        verify(observer, never()).onError(any(Throwable.class));
    }

    @Test
    public void testJoinSingle() {
        Observable<String> source = Observable.from("a");

        Observable<String> result = StringObservable.join(source, ", ");

        @SuppressWarnings("unchecked")
        Observer<Object> observer = mock(Observer.class);

        result.subscribe(new TestObserver<Object>(observer));

        verify(observer, times(1)).onNext("a");
        verify(observer, times(1)).onCompleted();
        verify(observer, never()).onError(any(Throwable.class));
    }

    @Test
    public void testJoinEmpty() {
        Observable<String> source = Observable.empty();

        Observable<String> result = StringObservable.join(source, ", ");

        @SuppressWarnings("unchecked")
        Observer<Object> observer = mock(Observer.class);

        result.subscribe(new TestObserver<Object>(observer));

        verify(observer, times(1)).onNext("");
        verify(observer, times(1)).onCompleted();
        verify(observer, never()).onError(any(Throwable.class));
    }

    @Test
    public void testJoinThrows() {
        Observable<String> source = Observable.concat(Observable.just("a"), Observable.<String> error(new RuntimeException("Forced failure")));

        Observable<String> result = StringObservable.join(source, ", ");

        @SuppressWarnings("unchecked")
        Observer<Object> observer = mock(Observer.class);

        result.subscribe(new TestObserver<Object>(observer));

        verify(observer, never()).onNext("a");
        verify(observer, never()).onCompleted();
        verify(observer, times(1)).onError(any(Throwable.class));
    }

    @Test
    public void testFromInputStream() {
        final byte[] inBytes = "test".getBytes();
        final byte[] outBytes = StringObservable.from(new ByteArrayInputStream(inBytes)).toBlockingObservable().single();
        assertNotSame(inBytes, outBytes);
        assertArrayEquals(inBytes, outBytes);
    }

    @Test
    public void testFromReader() {
        final String inStr = "test";
        final String outStr = StringObservable.from(new StringReader(inStr)).toBlockingObservable().single();
        assertNotSame(inStr, outStr);
        assertEquals(inStr, outStr);
    }

    @Test
    public void testByLine() {
        String newLine = System.getProperty("line.separator");
        
        List<Line> lines = StringObservable.byLine(Observable.from(Arrays.asList("qwer", newLine + "asdf" + newLine, "zx", "cv"))).toList().toBlockingObservable().single();

        assertEquals(Arrays.asList(new Line(0, "qwer"), new Line(1, "asdf"), new Line(2, "zxcv")), lines);
    }
}
