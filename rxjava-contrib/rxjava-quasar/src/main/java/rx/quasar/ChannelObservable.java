/**
 * Copyright 2014 Netflix, Inc.
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */package rx.quasar;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import rx.Observable;
import rx.Observer;
import rx.Scheduler;
import co.paralleluniverse.fibers.FiberAsync;
import co.paralleluniverse.fibers.SuspendExecution;
import co.paralleluniverse.fibers.Suspendable;
import co.paralleluniverse.strands.Strand;
import co.paralleluniverse.strands.channels.Channel;
import co.paralleluniverse.strands.channels.Channels;
import co.paralleluniverse.strands.channels.ReceivePort;
import co.paralleluniverse.strands.channels.SendPort;

/**
 * This class contains static methods that connect {@link Observable}s and {@link Channel}s.
 */
public final class ChannelObservable {
    private ChannelObservable() {
    }

    /**
     * Converts an {@link Iterable} sequence into an Observable that emits each message received on the channel.
     * <p>
     * <img width="640" src="https://raw.github.com/wiki/Netflix/RxJava/images/rx-operators/from.png">
     * <p>
     * @param channel
     *                the source {@link ReceivePort}
     * @param <T>
     *                the type of messages on the channel and the type of items to be
     *                emitted by the resulting Observable
     * @return an Observable that emits each message received on the source {@link ReceivePort}
     * @see <a href="https://github.com/Netflix/RxJava/wiki/Creating-Observables#wiki-from">RxJava Wiki: from()</a>
     */
    public static <T> Observable<T> from(ReceivePort<T> channel) {
        return Observable.create(new OnSubscribeFromChannel<T>(channel));
    }

    /**
     * Converts an {@link Iterable} sequence into an Observable that operates on the specified
     * scheduler, emitting each message received on the channel.
     * <p>
     * <img width="640" src="https://raw.github.com/wiki/Netflix/RxJava/images/rx-operators/from.s.png">
     * <p>
     * @param channel
     *                  the source {@link ReceivePort}
     * @param scheduler
     *                  the scheduler on which the Observable is to emit the messages received on the channel
     * @param <T>
     *                  the type of messages on the channel and the type of items to be
     *                  emitted by the resulting Observable
     * @return an Observable that emits each message received on the source {@link ReceivePort}, on the
     *         specified scheduler
     * @see <a href="https://github.com/Netflix/RxJava/wiki/Creating-Observables#wiki-from">RxJava Wiki: from()</a>
     * @see <a href="http://msdn.microsoft.com/en-us/library/hh212140.aspx">MSDN: Observable.ToObservable</a>
     */
    public static <T> Observable<T> from(ReceivePort<T> channel, Scheduler scheduler) {
        return Observable.create(new OnSubscribeFromChannel<T>(channel)).subscribeOn(scheduler);
    }

    /**
     * Converts a {@link SendPort} channel into an {@link Observer}.
     * <p>
     * @param <T>     the type of messages that can be sent to the channel and the type of items to be
     *                received by the Observer
     * @param channel the target {@link SendPort}
     * @return the observer
     */
    public static <T> Observer<T> to(final SendPort<T> channel) {
        return new Observer<T>() {

            @Override
            @Suspendable
            public void onNext(T t) {
                try {
                    channel.send(t);
                } catch (InterruptedException ex) {
                    Strand.interrupted();
                } catch (SuspendExecution ex) {
                    throw new AssertionError(ex);
                }
            }

            @Override
            public void onCompleted() {
                channel.close();
            }

            @Override
            public void onError(Throwable e) {
                channel.close(e);
            }
        };
    }

    /**
     * Creates a {@link ReceivePort} subscribed to an {@link Observable}.
     * <p>
     * @param <T>        the type of messages emitted by the observable and received on the channel.
     * @param bufferSize the channel's buffer size
     * @param policy     the channel's {@link Channels.OverflowPolicy}
     * @param o          the observable
     * @return A new channel with the given buffer size and overflow policy that will receive all events emitted by the observable.
     */
    public static <T> ReceivePort<T> subscribe(int bufferSize, Channels.OverflowPolicy policy, Observable<? extends T> o) {
        final Channel<T> channel = Channels.newChannel(bufferSize, policy);

        o.subscribe(new Observer<T>() {
            @Override
            @Suspendable
            public void onNext(T t) {
                try {
                    channel.send(t);
                } catch (InterruptedException ex) {
                    Strand.interrupted();
                } catch (SuspendExecution ex) {
                    throw new AssertionError(ex);
                }
            }

            @Override
            public void onCompleted() {
                channel.close();
            }

            @Override
            public void onError(Throwable e) {
                channel.close(e);
            }
        });
        return channel;
    }

    /**
     * Takes an observable that generates <i>at most one value</i>, blocks until it completes and returns the result.
     * If the observable completes before a value has been emitted, this method returns {@code null}.
     * It the observable fails, this function throws an {@link ExecutionException} that wraps the observable's exception.
     *
     * @param o the observable
     * @return the observable's result, or {@code null} if the observable completes before a value is emitted.
     * @throws ExecutionException if the observable fails
     */
    public static <T> T get(final Observable<T> o) throws ExecutionException, SuspendExecution, InterruptedException {
        return new AsyncObservable<T>(o).run();
    }

    /**
     * Takes an observable that generates <i>at most one value</i>, blocks until it completes or the timeout expires, and returns the result.
     * If the observable completes before a value has been emitted, this method returns {@code null}.
     * It the observable fails, this function throws an {@link ExecutionException} that wraps the observable's exception.
     *
     * @param o       the observable
     * @param timeout the maximum time this method will blcok
     * @param unit    the timeout's time unit
     * @return the observable's result, or {@code null} if the observable completes before a value is emitted.
     * @throws ExecutionException if the observable fails
     * @throws TimeoutException   if the timeout expires before the observable completes
     */
    public static <T> T get(final Observable<T> o, long timeout, TimeUnit unit) throws ExecutionException, SuspendExecution, InterruptedException, TimeoutException {
        return new AsyncObservable<T>(o).run(timeout, unit);
    }

    private static class AsyncObservable<T> extends FiberAsync<T, Void, ExecutionException> implements Observer<T> {
        private final Observable<T> o;

        public AsyncObservable(Observable<T> o) {
            this.o = o;
        }

        @Override
        protected Void requestAsync() {
            o.subscribe(this);
            return null;
        }

        @Override
        public void onNext(T t) {
            if (isCompleted())
                throw new IllegalStateException("Operation already completed");
            asyncCompleted(t);
        }

        @Override
        public void onError(Throwable e) {
            if (isCompleted())
                throw new IllegalStateException("Operation already completed");
            asyncFailed(e);
        }

        @Override
        public void onCompleted() {
            if (!isCompleted())
                asyncCompleted(null);
        }

        @Override
        protected ExecutionException wrapException(Throwable t) {
            return new ExecutionException(t);
        }
    }
}
