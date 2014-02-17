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

import co.paralleluniverse.fibers.SuspendExecution;
import co.paralleluniverse.fibers.Suspendable;
import co.paralleluniverse.strands.Strand;
import co.paralleluniverse.strands.channels.Channel;
import co.paralleluniverse.strands.channels.Channels;
import co.paralleluniverse.strands.channels.ReceivePort;
import co.paralleluniverse.strands.channels.SendPort;
import rx.Observable;
import rx.Observer;
import rx.Scheduler;
import rx.util.functions.Action2;
import rx.util.functions.Actions;
import rx.util.functions.Func1;
import rx.util.functions.Functions;

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
    public final static <T> Observable<T> from(ReceivePort<T> channel) {
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
    public final static <T> Observable<T> from(ReceivePort<T> channel, Scheduler scheduler) {
        return Observable.create(new OnSubscribeFromChannel<T>(channel)).subscribeOn(scheduler);
    }

    /**
     * Converts a {@link SendPort} channel into an {@link Observer}.
     * <p>
     * @param <T>     the type of messages that can be sent to the channel and the type of items to be
     *                received by the Observer
     * @param channel the target {@link SendPort}
     * @return
     */
    public final static <T> Observer<T> to(final SendPort<T> channel) {
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
     * @param policy     the channel's {@link Channels.OverflowPolicy OverflowPolicy}
     * @param o          the observable
     * @return A new channel with the given buffer size and overflow policy that will receive all events emitted by the observable.
     */
    public final static <T> ReceivePort<T> subscribe(int bufferSize, Channels.OverflowPolicy policy, Observable<T> o) {
        final Channel<T> channel = Channels.newChannel(bufferSize, policy);

        System.out.println(Functions.fromFunc(new Func1<String, String>() {

            @Override
            public String call(String t1) {
                throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
            }
        }));
        System.out.println(Actions.toFunc(new Action2<String, String>() {

            @Override
            public void call(String t1, String t2) {
                throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
            }
        }));
        
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
}
