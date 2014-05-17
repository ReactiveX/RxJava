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

import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.Charset;
import java.nio.charset.CharsetDecoder;
import java.nio.charset.CharsetEncoder;
import java.nio.charset.CoderResult;
import java.nio.charset.CodingErrorAction;
import java.util.Arrays;
import java.util.regex.Pattern;

import rx.Observable;
import rx.Observable.OnSubscribe;
import rx.Observable.Operator;
import rx.Subscriber;
import rx.functions.Func1;
import rx.functions.Func2;

public class StringObservable {
    /**
     * Reads from the bytes from a source {@link InputStream} and outputs {@link Observable} of
     * {@code byte[]}s
     * <p>
     * <img width="640" src="https://raw.github.com/wiki/Netflix/RxJava/images/rx-operators/St.from.png">
     * 
     * @param i
     *            Source {@link InputStream}
     * @return the Observable containing read byte arrays from the input
     */
    public static Observable<byte[]> from(final InputStream i) {
        return from(i, 8 * 1024);
    }

    /**
     * Reads from the bytes from a source {@link InputStream} and outputs {@link Observable} of
     * {@code byte[]}s
     * <p>
     * <img width="640" src="https://raw.github.com/wiki/Netflix/RxJava/images/rx-operators/St.from.png">
     * 
     * @param i
     *            Source {@link InputStream}
     * @param size
     *            internal buffer size
     * @return the Observable containing read byte arrays from the input
     */
    public static Observable<byte[]> from(final InputStream i, final int size) {
        return Observable.create(new OnSubscribe<byte[]>() {
            @Override
            public void call(Subscriber<? super byte[]> o) {
                byte[] buffer = new byte[size];
                try {
                    if (o.isUnsubscribed())
                        return;
                    int n = 0;
                    n = i.read(buffer);
                    while (n != -1 && !o.isUnsubscribed()) {
                        o.onNext(Arrays.copyOf(buffer, n));
                        n = i.read(buffer);
                    }
                } catch (IOException e) {
                    o.onError(e);
                }
                if (o.isUnsubscribed())
                    return;
                o.onCompleted();
            }
        });
    }

    /**
     * Reads from the characters from a source {@link Reader} and outputs {@link Observable} of
     * {@link String}s
     * <p>
     * <img width="640" src="https://raw.github.com/wiki/Netflix/RxJava/images/rx-operators/St.from.png">
     * 
     * @param i
     *            Source {@link Reader}
     * @return the Observable of Strings read from the source
     */
    public static Observable<String> from(final Reader i) {
        return from(i, 8 * 1024);
    }

    /**
     * Reads from the characters from a source {@link Reader} and outputs {@link Observable} of
     * {@link String}s
     * <p>
     * <img width="640" src="https://raw.github.com/wiki/Netflix/RxJava/images/rx-operators/St.from.png">
     * 
     * @param i
     *            Source {@link Reader}
     * @param size
     *            internal buffer size
     * @return the Observable of Strings read from the source
     */
    public static Observable<String> from(final Reader i, final int size) {
        return Observable.create(new OnSubscribe<String>() {
            @Override
            public void call(Subscriber<? super String> o) {
                char[] buffer = new char[size];
                try {
                    if (o.isUnsubscribed())
                        return;
                    int n = 0;
                    n = i.read(buffer);
                    while (n != -1 && !o.isUnsubscribed()) {
                        o.onNext(new String(buffer, 0, n));
                        n = i.read(buffer);
                    }
                } catch (IOException e) {
                    o.onError(e);
                }
                if (o.isUnsubscribed())
                    return;
                o.onCompleted();
            }
        });
    }

    /**
     * Decodes a stream the multibyte chunks into a stream of strings that works on infinite streams
     * and where handles when a multibyte character spans two chunks.
     * <p>
     * <img width="640" src="https://raw.github.com/wiki/Netflix/RxJava/images/rx-operators/St.decode.png">
     * 
     * @param src
     * @param charsetName
     * @return the Observable returning a stream of decoded strings
     */
    public static Observable<String> decode(Observable<byte[]> src, String charsetName) {
        return decode(src, Charset.forName(charsetName));
    }

    /**
     * Decodes a stream the multibyte chunks into a stream of strings that works on infinite streams
     * and where handles when a multibyte character spans two chunks.
     * <p>
     * <img width="640" src="https://raw.github.com/wiki/Netflix/RxJava/images/rx-operators/St.decode.png">
     * 
     * @param src
     * @param charset
     * @return the Observable returning a stream of decoded strings
     */
    public static Observable<String> decode(Observable<byte[]> src, Charset charset) {
        return decode(src, charset.newDecoder().onMalformedInput(CodingErrorAction.REPLACE).onUnmappableCharacter(CodingErrorAction.REPLACE));
    }

    /**
     * Decodes a stream the multibyte chunks into a stream of strings that works on infinite streams
     * and where it handles when a multibyte character spans two chunks.
     * This method allows for more control over how malformed and unmappable characters are handled.
     * <p>
     * <img width="640" src="https://raw.github.com/wiki/Netflix/RxJava/images/rx-operators/St.decode.png">
     * 
     * @param src
     * @param charsetDecoder
     * @return the Observable returning a stream of decoded strings
     */
    public static Observable<String> decode(final Observable<byte[]> src, final CharsetDecoder charsetDecoder) {
        return src.lift(new Operator<String, byte[]>() {
            @Override
            public Subscriber<? super byte[]> call(final Subscriber<? super String> o) {
                return new Subscriber<byte[]>(o) {
                    private ByteBuffer leftOver = null;

                    @Override
                    public void onCompleted() {
                        if (process(null, leftOver, true))
                            o.onCompleted();
                    }

                    @Override
                    public void onError(Throwable e) {
                        if (process(null, leftOver, true))
                            o.onError(e);
                    }

                    @Override
                    public void onNext(byte[] bytes) {
                        process(bytes, leftOver, false);
                    }

                    public boolean process(byte[] next, ByteBuffer last, boolean endOfInput) {
                        if (o.isUnsubscribed())
                            return false;

                        ByteBuffer bb;
                        if (last != null) {
                            if (next != null) {
                                // merge leftover in front of the next bytes
                                bb = ByteBuffer.allocate(last.remaining() + next.length);
                                bb.put(last);
                                bb.put(next);
                                bb.flip();
                            }
                            else { // next == null
                                bb = last;
                            }
                        }
                        else { // last == null
                            if (next != null) {
                                bb = ByteBuffer.wrap(next);
                            }
                            else { // next == null
                                return true;
                            }
                        }

                        CharBuffer cb = CharBuffer.allocate((int) (bb.limit() * charsetDecoder.averageCharsPerByte()));
                        CoderResult cr = charsetDecoder.decode(bb, cb, endOfInput);
                        cb.flip();

                        if (cr.isError()) {
                            try {
                                cr.throwException();
                            }
                            catch (CharacterCodingException e) {
                                o.onError(e);
                                return false;
                            }
                        }

                        if (bb.remaining() > 0) {
                            leftOver = bb;
                        }
                        else {
                            leftOver = null;
                        }

                        String string = cb.toString();
                        if (!string.isEmpty())
                            o.onNext(string);

                        return true;
                    }
                };
            }
        });
    }

    /**
     * Encodes a possible infinite stream of strings into a Observable of byte arrays.
     * <p>
     * <img width="640" src="https://raw.github.com/wiki/Netflix/RxJava/images/rx-operators/St.encode.png">
     * 
     * @param src
     * @param charsetName
     * @return the Observable with a stream of encoded byte arrays
     */
    public static Observable<byte[]> encode(Observable<String> src, String charsetName) {
        return encode(src, Charset.forName(charsetName));
    }

    /**
     * Encodes a possible infinite stream of strings into a Observable of byte arrays.
     * <p>
     * <img width="640" src="https://raw.github.com/wiki/Netflix/RxJava/images/rx-operators/St.encode.png">
     * 
     * @param src
     * @param charset
     * @return the Observable with a stream of encoded byte arrays
     */
    public static Observable<byte[]> encode(Observable<String> src, Charset charset) {
        return encode(src, charset.newEncoder().onMalformedInput(CodingErrorAction.REPLACE).onUnmappableCharacter(CodingErrorAction.REPLACE));
    }

    /**
     * Encodes a possible infinite stream of strings into a Observable of byte arrays.
     * This method allows for more control over how malformed and unmappable characters are handled.
     * <p>
     * <img width="640" src="https://raw.github.com/wiki/Netflix/RxJava/images/rx-operators/St.encode.png">
     * 
     * @param src
     * @param charsetEncoder
     * @return the Observable with a stream of encoded byte arrays
     */
    public static Observable<byte[]> encode(Observable<String> src, final CharsetEncoder charsetEncoder) {
        return src.map(new Func1<String, byte[]>() {
            @Override
            public byte[] call(String str) {
                CharBuffer cb = CharBuffer.wrap(str);
                ByteBuffer bb;
                try {
                    bb = charsetEncoder.encode(cb);
                } catch (CharacterCodingException e) {
                    throw new RuntimeException(e);
                }
                return Arrays.copyOfRange(bb.array(), bb.position(), bb.limit());
            }
        });
    }

    /**
     * Gather up all of the strings in to one string to be able to use it as one message. Don't use
     * this on infinite streams.
     * <p>
     * <img width="640" src="https://raw.github.com/wiki/Netflix/RxJava/images/rx-operators/St.stringConcat.png">
     * 
     * @param src
     * @return the Observable returing all strings concatenated as a single string
     */
    public static Observable<String> stringConcat(Observable<String> src) {
        return src.reduce(new Func2<String, String, String>() {
            @Override
            public String call(String a, String b) {
                return a + b;
            }
        });
    }

    /**
     * Rechunks the strings based on a regex pattern and works on infinite stream.
     * 
     * <pre>
     * split(["boo:an", "d:foo"], ":") --> ["boo", "and", "foo"]
     * split(["boo:an", "d:foo"], "o") --> ["b", "", ":and:f", "", ""]
     * </pre>
     * 
     * See {@link Pattern}
     * <p>
     * <img width="640" src="https://raw.github.com/wiki/Netflix/RxJava/images/rx-operators/St.split.png">
     * 
     * @param src
     * @param regex
     * @return the Observable streaming the split values
     */
    public static Observable<String> split(final Observable<String> src, String regex) {
        final Pattern pattern = Pattern.compile(regex);

        return src.lift(new Operator<String, String>() {
            @Override
            public Subscriber<? super String> call(final Subscriber<? super String> o) {
                return new Subscriber<String>(o) {
                    private String leftOver = null;

                    @Override
                    public void onCompleted() {
                        output(leftOver);
                        if (!o.isUnsubscribed())
                            o.onCompleted();
                    }

                    @Override
                    public void onError(Throwable e) {
                        output(leftOver);
                        if (!o.isUnsubscribed())
                            o.onError(e);
                    }

                    @Override
                    public void onNext(String segment) {
                        String[] parts = pattern.split(segment, -1);

                        if (leftOver != null)
                            parts[0] = leftOver + parts[0];
                        for (int i = 0; i < parts.length - 1; i++) {
                            String part = parts[i];
                            output(part);
                        }
                        leftOver = parts[parts.length - 1];
                    }

                    private int emptyPartCount = 0;

                    /**
                     * when limit == 0 trailing empty parts are not emitted.
                     * 
                     * @param part
                     */
                    private void output(String part) {
                        if (part.isEmpty()) {
                            emptyPartCount++;
                        }
                        else {
                            for (; emptyPartCount > 0; emptyPartCount--)
                                if (!o.isUnsubscribed())
                                    o.onNext("");
                            if (!o.isUnsubscribed())
                                o.onNext(part);
                        }
                    }
                };
            }
        });
    }

    /**
     * Concatenates the sequence of values by adding a separator
     * between them and emitting the result once the source completes.
     * <p>
     * <img width="640" src="https://raw.github.com/wiki/Netflix/RxJava/images/rx-operators/St.join.png">
     * <p>
     * The conversion from the value type to String is performed via
     * {@link java.lang.String#valueOf(java.lang.Object)} calls.
     * <p>
     * For example:
     * 
     * <pre>
     * Observable&lt;Object&gt; source = Observable.from(&quot;a&quot;, 1, &quot;c&quot;);
     * Observable&lt;String&gt; result = join(source, &quot;, &quot;);
     * </pre>
     * 
     * will yield a single element equal to "a, 1, c".
     * 
     * @param source
     *            the source sequence of CharSequence values
     * @param separator
     *            the separator to a
     * @return an Observable which emits a single String value having the concatenated
     *         values of the source observable with the separator between elements
     */
    public static <T> Observable<String> join(final Observable<T> source, final CharSequence separator) {
        return source.lift(new Operator<String, T>() {
            @Override
            public Subscriber<T> call(final Subscriber<? super String> o) {
                return new Subscriber<T>(o) {
                    boolean mayAddSeparator;
                    StringBuilder b = new StringBuilder();

                    @Override
                    public void onCompleted() {
                        String str = b.toString();
                        b = null;
                        if (!o.isUnsubscribed())
                            o.onNext(str);
                        if (!o.isUnsubscribed())
                            o.onCompleted();
                    }

                    @Override
                    public void onError(Throwable e) {
                        b = null;
                        if (!o.isUnsubscribed())
                            o.onError(e);
                    }

                    @Override
                    public void onNext(Object t) {
                        if (mayAddSeparator) {
                            b.append(separator);
                        }
                        mayAddSeparator = true;
                        b.append(String.valueOf(t));
                    }
                };
            }
        });
    }

    public final static class Line {
        private final int number;
        private final String text;

        public Line(int number, String text) {
            this.number = number;
            this.text = text;
        }

        public int getNumber() {
            return number;
        }

        public String getText() {
            return text;
        }

        @Override
        public int hashCode() {
            int result = 31 + number;
            result = 31 * result + (text == null ? 0 : text.hashCode());
            return result;
        }

        @Override
        public boolean equals(Object obj) {
            if (!(obj instanceof Line))
                return false;
            Line other = (Line) obj;
            if (number != other.number)
                return false;
            if (other.text == text)
                return true;
            if (text == null)
                return false;
            return text.equals(other.text);
        }

        @Override
        public String toString() {
            return number + ":" + text;
        }
    }

    /**
     * Splits the {@link Observable} of Strings by lines and numbers them (zero based index)
     * <p>
     * <img width="640" src="https://raw.github.com/wiki/Netflix/RxJava/images/rx-operators/St.byLine.png">
     * 
     * @param source
     * @return the Observable conaining the split lines of the source
     */
    public static Observable<Line> byLine(Observable<String> source) {
        return split(source, System.getProperty("line.separator")).map(new Func1<String, Line>() {
            int lineNumber = 0;

            @Override
            public Line call(String text) {
                return new Line(lineNumber++, text);
            }
        });
    }
}
