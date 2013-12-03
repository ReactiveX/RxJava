package rx.observables;

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
import rx.Observer;
import rx.Subscription;
import rx.Observable.OnSubscribeFunc;
import rx.util.functions.Func1;
import rx.util.functions.Func2;

public class StringObservable {
    /**
     * Decodes a stream the multibyte chunks into a stream of strings that works on infinite streams and where handles when a multibyte character spans two chunks.
     * 
     * @param src
     * @param charsetName
     * @return
     */
    public static Observable<String> decode(Observable<byte[]> src, String charsetName) {
        return decode(src, Charset.forName(charsetName));
    }

    /**
     * Decodes a stream the multibyte chunks into a stream of strings that works on infinite streams and where handles when a multibyte character spans two chunks.
     * 
     * @param src
     * @param charset
     * @return
     */
    public static Observable<String> decode(Observable<byte[]> src, Charset charset) {
        return decode(src, charset.newDecoder().onMalformedInput(CodingErrorAction.REPLACE).onUnmappableCharacter(CodingErrorAction.REPLACE));
    }

    /**
     * Decodes a stream the multibyte chunks into a stream of strings that works on infinite streams and where handles when a multibyte character spans two chunks.
     * This method allows for more control over how malformed and unmappable characters are handled.
     * 
     * @param src
     * @param charsetDecoder
     * @return
     */
    public static Observable<String> decode(final Observable<byte[]> src, final CharsetDecoder charsetDecoder) {
        return Observable.create(new OnSubscribeFunc<String>() {
            @Override
            public Subscription onSubscribe(final Observer<? super String> observer) {
                return src.subscribe(new Observer<byte[]>() {
                    private ByteBuffer leftOver = null;

                    @Override
                    public void onCompleted() {
                        if (process(null, leftOver, true))
                            observer.onCompleted();
                    }

                    @Override
                    public void onError(Throwable e) {
                        if (process(null, leftOver, true))
                            observer.onError(e);
                    }

                    @Override
                    public void onNext(byte[] bytes) {
                        process(bytes, leftOver, false);
                    }

                    public boolean process(byte[] next, ByteBuffer last, boolean endOfInput) {
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
                                observer.onError(e);
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
                            observer.onNext(string);

                        return true;
                    }
                });
            }
        });
    }

    /**
     * Encodes a possible infinite stream of strings into a Observable of byte arrays.
     * 
     * @param src
     * @param charsetName
     * @return
     */
    public static Observable<byte[]> encode(Observable<String> src, String charsetName) {
        return encode(src, Charset.forName(charsetName));
    }

    /**
     * Encodes a possible infinite stream of strings into a Observable of byte arrays.
     * 
     * @param src
     * @param charset
     * @return
     */
    public static Observable<byte[]> encode(Observable<String> src, Charset charset) {
        return encode(src, charset.newEncoder().onMalformedInput(CodingErrorAction.REPLACE).onUnmappableCharacter(CodingErrorAction.REPLACE));
    }

    /**
     * Encodes a possible infinite stream of strings into a Observable of byte arrays.
     * This method allows for more control over how malformed and unmappable characters are handled.
     * 
     * @param src
     * @param charsetEncoder
     * @return
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
     * Gather up all of the strings in to one string to be able to use it as one message. Don't use this on infinite streams.
     * 
     * @param src
     * @return
     */
    public static Observable<String> stringConcat(Observable<String> src) {
        return src.aggregate(new Func2<String, String, String>() {
            public String call(String a, String b) {
                return a + b;
            }
        });
    }

    /**
     * Rechunks the strings based on a regex pattern and works on infinite stream.
     * 
     * resplit(["boo:an", "d:foo"], ":") --> ["boo", "and", "foo"]
     * resplit(["boo:an", "d:foo"], "o") --> ["b", "", ":and:f", "", ""]
     * 
     * See {@link Pattern}
     * 
     * @param src
     * @param regex
     * @return
     */
    public static Observable<String> split(final Observable<String> src, String regex) {
        final Pattern pattern = Pattern.compile(regex);
        return Observable.create(new OnSubscribeFunc<String>() {
            @Override
            public Subscription onSubscribe(final Observer<? super String> observer) {
                return src.subscribe(new Observer<String>() {
                    private String leftOver = null;

                    @Override
                    public void onCompleted() {
                        output(leftOver);
                        observer.onCompleted();
                    }

                    @Override
                    public void onError(Throwable e) {
                        output(leftOver);
                        observer.onError(e);
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
                     * @param part
                     */
                    private void output(String part) {
                        if (part.isEmpty()) {
                            emptyPartCount++;
                        }
                        else {
                            for(; emptyPartCount>0; emptyPartCount--)
                                observer.onNext("");
                            observer.onNext(part);
                        }
                    }
                });
            }
        });
    }
}
