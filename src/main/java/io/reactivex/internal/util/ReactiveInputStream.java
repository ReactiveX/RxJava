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

package io.reactivex.internal.util;

import io.reactivex.Flowable;
import io.reactivex.Observable;
import io.reactivex.Observer;
import io.reactivex.annotations.Nullable;
import io.reactivex.disposables.Disposable;
import io.reactivex.functions.Function;
import io.reactivex.subscribers.DisposableSubscriber;

import java.io.IOException;
import java.io.InputStream;
import java.util.concurrent.LinkedBlockingQueue;

/**
 * Write reactive flowable/observable events to an input stream, e.g. writing
 * to files or sockets.
 */
public class ReactiveInputStream<T> extends InputStream {
	/**
	 * Create an InputStream from calling toString() on each element and using
	 * the system default byte encoding.
     */
    public static <T> InputStream toStrings(Flowable<T> src) {
        return new ReactiveInputStream<T>(src, getStringBytes);
    }

    /**
     * Create an InputStream from calling toString() on each element and using
     * the system default byte encoding.
     */
    public static <T> InputStream toStrings(Observable<T> src) {
        return new ReactiveInputStream<T>(src, getStringBytes);
    }

    public static <T> InputStream toStrings(Flowable<T> src, String delimiter) {
        Flowable<String> strings = src.map(applyToString);
        Flowable<String> head = strings.take(1);
        Flowable<String> tail = strings.skip(1)
            .map(prependDelimiter(delimiter));

        return ReactiveInputStream.toStrings(
            head.concatWith(tail));
    }

    public static <T> InputStream toStrings(Observable<T> src, String delimiter) {
        Observable<String> strings = src.map(applyToString);
        Observable<String> head = strings.take(1);
        Observable<String> tail = strings.skip(1)
            .map(prependDelimiter(delimiter));

        return ReactiveInputStream.toStrings(
            head.concatWith(tail));
    }

    /**
     * Internally-used function to apply toString.getBytes to observed elements.
     */
    private static Function<Object, byte[]> getStringBytes = new Function<Object, byte[]>() {
        @Override
        public byte[] apply(Object t) {
            if (t != null) {
                return t.toString().getBytes();
            } else {
                return new byte[0];
            }
        }
    };

    /**
     * Internally-used function to apply toString to observed elements, substituting "" for nulls.
     */
    private static Function<Object, String> applyToString = new Function<Object, String>() {
        @Override
        public String apply(Object x) {
            if (x != null) {
                return x.toString();
            } else {
                return "";
            }
        }
    };

    /**
     * Internally-used function to prepend a delimiter to strings for using in
     * joining sequences.
     */
    private static Function<String, String> prependDelimiter(final String delimiter) {
        return new Function<String, String>() {
            @Override
            public String apply(String s) {
                return delimiter + s;
            }
        };
    }

    /**
     * Default buffer-size limit for queued events, mirroing the default in Flowable.
     */
    private static final int BUFFER_SIZE;

    static {
        BUFFER_SIZE = Math.max(1, Integer.getInteger("rx2.buffer-size", 128));
    }

    private final Function<? super T, byte[]> readObservableBytes;
    private boolean completed = false;
    @Nullable
    private volatile Throwable error = null;
    private final LinkedBlockingQueue<Optional<T>> queue = new LinkedBlockingQueue<Optional<T>>(BUFFER_SIZE);
    private byte[] toRead = new byte[0];
    private int readOffset = -1;
    @Nullable
    private Disposable disposeOnClose = null;

    /**
     * Create an InputStream from a Flowable.
     * Similar to creation from Observable, but wraps subscription controls with
     * flow control.
     *
     * @param src                 The source flowable.
     * @param readObservableBytes The function to to marshall events to bytes to
     *                            be used by the input stream.
     */
    public ReactiveInputStream(Flowable<T> src, Function<? super T, byte[]> readObservableBytes) {
        this.readObservableBytes = readObservableBytes;

        DisposableSubscriber<T> subscriber = new DisposableSubscriber<T>() {
            @Override
            public void onStart() {
                handleOnSubscribe(this);
                request(1);
            }

            @Override
            public void onNext(T t) {
                handleOnNext(t);
                request(1);
            }

            @Override
            public void onError(Throwable throwable) {
                handleOnError(throwable);
            }

            @Override
            public void onComplete() {
                handleOnComplete();
            }
        };

        src.subscribe(subscriber);
    }

    /**
     * Create an InputStream from an Observable.
     *
     * @param src                 The source observable.
     * @param readObservableBytes The function to to marshall events to bytes to
     *                            be used by the input stream.
     */
    public ReactiveInputStream(Observable<T> src, Function<? super T, byte[]> readObservableBytes) {
        this.readObservableBytes = readObservableBytes;
        Observer<T> observer = new Observer<T>() {
            @Override
            public void onSubscribe(Disposable d) {
                handleOnSubscribe(d);
            }

            @Override
            public void onNext(T t) {
                handleOnNext(t);
            }

            @Override
            public void onError(Throwable e) {
                handleOnError(e);
            }

            @Override
            public void onComplete() {
                handleOnComplete();
            }
        };
        src.subscribe(observer);
    }

    @Override
    public void close() throws IOException {
        completed = true;
        if (disposeOnClose != null) {
            disposeOnClose.dispose();
            disposeOnClose = null;
        }
        super.close();
    }

    private void handleOnComplete() {
        queue.offer(new Optional<T>(null));
    }

    private void handleOnError(Throwable throwable) {
        error = throwable;
        queue.offer(new Optional<T>(null));
    }

    private void handleOnNext(T t) {
        queue.offer(new Optional<T>(t));
    }

    private void handleOnSubscribe(Disposable disposable) {
        disposeOnClose = disposable;
    }

    /**
     * Complete reading by throwing any error from the underlying source if
     * necessary or return -1 to signal end of reading.
     *
     * @throws IOException The underlying error.
     */
    private int completeOrMaybeThrowError() throws IOException {
        completed = true;
        if (disposeOnClose != null) {
            disposeOnClose.dispose();
            disposeOnClose = null;
        }
        if (error != null) {
            throw new IOException(error);
        }
        return -1;
    }

    /**
     * Read a byte, possibly blocking until ready, or return -1 on completion.
     *
     * @return The byte read as an int, or -1 upon completion.
     * @throws IOException Wrap up underlying error.
     */
    @Override
    public int read() throws IOException {
        if (completed) {
            return completeOrMaybeThrowError();
        } else if (readOffset >= 0) {
            int result = toRead[readOffset];
            readOffset += 1;
            if (readOffset >= toRead.length) {
                toRead = new byte[0];
                readOffset = -1;
            }
            return result;
        } else {
            try {
                Optional<T> observed = queue.take();

                if (observed.isPresent()) {
                    try {
                        toRead = readObservableBytes.apply(observed.value);
                    } catch (Exception e) {
                        error = e;
                        return completeOrMaybeThrowError();
                    }

                    if (toRead.length > 0) {
                        readOffset = 0;
                    } else {
                        readOffset = -1;
                    }
                    return read();
                } else {
                    return completeOrMaybeThrowError();
                }
            } catch (InterruptedException e) {
                return completeOrMaybeThrowError();
            }
        }
    }

    /**
     * Private class duplicating {@code java.util.Optional} for use as a terminating sentinel.
     */
    static class Optional<T> {
        final T value;
        Optional(T value) {
            this.value = value;
        }
        boolean isPresent() {
            return value != null;
        }
    }
}
