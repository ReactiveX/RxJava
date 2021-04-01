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

package io.reactivex.rxjava3.flowables;

import java.util.Objects;
import java.util.concurrent.TimeUnit;

import org.reactivestreams.*;

import io.reactivex.rxjava3.annotations.*;
import io.reactivex.rxjava3.core.*;
import io.reactivex.rxjava3.disposables.Disposable;
import io.reactivex.rxjava3.functions.Consumer;
import io.reactivex.rxjava3.internal.functions.*;
import io.reactivex.rxjava3.internal.operators.flowable.*;
import io.reactivex.rxjava3.internal.util.ConnectConsumer;
import io.reactivex.rxjava3.plugins.RxJavaPlugins;
import io.reactivex.rxjava3.schedulers.Schedulers;

/**
 * A {@code ConnectableFlowable} resembles an ordinary {@link Flowable}, except that it does not begin
 * emitting items when it is subscribed to, but only when its {@link #connect} method is called. In this way you
 * can wait for all intended {@link Subscriber}s to {@link Flowable#subscribe} to the {@code Flowable}
 * before the {@code Flowable} begins emitting items.
 * <p>
 * <img width="640" height="510" src="https://github.com/ReactiveX/RxJava/wiki/images/rx-operators/publishConnect.v3.png" alt="">
 * <p>
 * When the upstream terminates, the {@code ConnectableFlowable} remains in this terminated state and,
 * depending on the actual underlying implementation, relays cached events to late {@code Subscriber}s.
 * In order to reuse and restart this {@code ConnectableFlowable}, the {@link #reset()} method has to be called.
 * When called, this {@code ConnectableFlowable} will appear as fresh, unconnected source to new {@code Subscriber}s.
 * Disposing the connection will reset the {@code ConnectableFlowable} to its fresh state and there is no need to call
 * {@code reset()} in this case.
 * <p>
 * Note that although {@link #connect()} and {@link #reset()} are safe to call from multiple threads, it is recommended
 * a dedicated thread or business logic manages the connection or resetting of a {@code ConnectableFlowable} so that
 * there is no unwanted signal loss due to early {@code connect()} or {@code reset()} calls while {@code Subscriber}s are
 * still being subscribed to to this {@code ConnectableFlowable} to receive signals from the get go.
 * <p>
 * @see <a href="https://github.com/ReactiveX/RxJava/wiki/Connectable-Observable-Operators">RxJava Wiki: Connectable Observable Operators</a>
 * @param <T>
 *          the type of items emitted by the {@code ConnectableFlowable}
 * @since 2.0.0
 */
public abstract class ConnectableFlowable<T> extends Flowable<T> {

    /**
     * Instructs the {@code ConnectableFlowable} to begin emitting the items from its underlying
     * {@link Flowable} to its {@link Subscriber}s.
     * <dl>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>The behavior is determined by the implementor of this abstract class.</dd>
     * </dl>
     *
     * @param connection
     *          the action that receives the connection subscription before the subscription to source happens
     *          allowing the caller to synchronously disconnect a synchronous source
     * @throws NullPointerException if {@code connection} is {@code null}
     * @see <a href="http://reactivex.io/documentation/operators/connect.html">ReactiveX documentation: Connect</a>
     */
    @SchedulerSupport(SchedulerSupport.NONE)
    public abstract void connect(@NonNull Consumer<? super Disposable> connection);

    /**
     * Resets this {@code ConnectableFlowable} into its fresh state if it has terminated.
     * <p>
     * Calling this method on a fresh or active {@code ConnectableFlowable} has no effect.
     * <dl>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>The behavior is determined by the implementor of this abstract class.</dd>
     * </dl>
     * @since 3.0.0
     */
    @SchedulerSupport(SchedulerSupport.NONE)
    public abstract void reset();

    /**
     * Instructs the {@code ConnectableFlowable} to begin emitting the items from its underlying
     * {@link Flowable} to its {@link Subscriber}s.
     * <p>
     * To disconnect from a synchronous source, use the {@link #connect(io.reactivex.rxjava3.functions.Consumer)} method.
     * <dl>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>The behavior is determined by the implementor of this abstract class.</dd>
     * </dl>
     *
     * @return the subscription representing the connection
     * @see <a href="http://reactivex.io/documentation/operators/connect.html">ReactiveX documentation: Connect</a>
     */
    @NonNull
    @SchedulerSupport(SchedulerSupport.NONE)
    public final Disposable connect() {
        ConnectConsumer cc = new ConnectConsumer();
        connect(cc);
        return cc.disposable;
    }

    /**
     * Returns a {@link Flowable} that stays connected to this {@code ConnectableFlowable} as long as there
     * is at least one subscription to this {@code ConnectableFlowable}.
     * <dl>
     *  <dt><b>Backpressure:</b></dt>
     *  <dd>The operator itself doesn't interfere with backpressure which is determined by the upstream
     *  {@code ConnectableFlowable}'s backpressure behavior.</dd>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>This {@code refCount} overload does not operate on any particular {@link Scheduler}.</dd>
     * </dl>
     * @return the new {@code Flowable} instance
     * @see <a href="http://reactivex.io/documentation/operators/refcount.html">ReactiveX documentation: RefCount</a>
     * @see #refCount(int)
     * @see #refCount(long, TimeUnit)
     * @see #refCount(int, long, TimeUnit)
     */
    @NonNull
    @CheckReturnValue
    @SchedulerSupport(SchedulerSupport.NONE)
    @BackpressureSupport(BackpressureKind.PASS_THROUGH)
    public Flowable<T> refCount() {
        return RxJavaPlugins.onAssembly(new FlowableRefCount<>(this));
    }

    /**
     * Connects to the upstream {@code ConnectableFlowable} if the number of subscribed
     * subscriber reaches the specified count and disconnect if all subscribers have unsubscribed.
     * <dl>
     *  <dt><b>Backpressure:</b></dt>
     *  <dd>The operator itself doesn't interfere with backpressure which is determined by the upstream
     *  {@code ConnectableFlowable}'s backpressure behavior.</dd>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>This {@code refCount} overload does not operate on any particular {@link Scheduler}.</dd>
     * </dl>
     * <p>History: 2.1.14 - experimental
     * @param subscriberCount the number of subscribers required to connect to the upstream
     * @return the new {@link Flowable} instance
     * @throws IllegalArgumentException if {@code subscriberCount} is non-positive
     * @since 2.2
     */
    @CheckReturnValue
    @SchedulerSupport(SchedulerSupport.NONE)
    @BackpressureSupport(BackpressureKind.PASS_THROUGH)
    @NonNull
    public final Flowable<T> refCount(int subscriberCount) {
        return refCount(subscriberCount, 0, TimeUnit.NANOSECONDS, Schedulers.trampoline());
    }

    /**
     * Connects to the upstream {@code ConnectableFlowable} if the number of subscribed
     * subscriber reaches 1 and disconnect after the specified
     * timeout if all subscribers have unsubscribed.
     * <dl>
     *  <dt><b>Backpressure:</b></dt>
     *  <dd>The operator itself doesn't interfere with backpressure which is determined by the upstream
     *  {@code ConnectableFlowable}'s backpressure behavior.</dd>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>This {@code refCount} overload operates on the {@code computation} {@link Scheduler}.</dd>
     * </dl>
     * <p>History: 2.1.14 - experimental
     * @param timeout the time to wait before disconnecting after all subscribers unsubscribed
     * @param unit the time unit of the timeout
     * @return the new {@link Flowable} instance
     * @throws NullPointerException if {@code unit} is {@code null}
     * @see #refCount(long, TimeUnit, Scheduler)
     * @since 2.2
     */
    @CheckReturnValue
    @SchedulerSupport(SchedulerSupport.COMPUTATION)
    @BackpressureSupport(BackpressureKind.PASS_THROUGH)
    @NonNull
    public final Flowable<T> refCount(long timeout, @NonNull TimeUnit unit) {
        return refCount(1, timeout, unit, Schedulers.computation());
    }

    /**
     * Connects to the upstream {@code ConnectableFlowable} if the number of subscribed
     * subscriber reaches 1 and disconnect after the specified
     * timeout if all subscribers have unsubscribed.
     * <dl>
     *  <dt><b>Backpressure:</b></dt>
     *  <dd>The operator itself doesn't interfere with backpressure which is determined by the upstream
     *  {@code ConnectableFlowable}'s backpressure behavior.</dd>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>This {@code refCount} overload operates on the specified {@link Scheduler}.</dd>
     * </dl>
     * <p>History: 2.1.14 - experimental
     * @param timeout the time to wait before disconnecting after all subscribers unsubscribed
     * @param unit the time unit of the timeout
     * @param scheduler the target scheduler to wait on before disconnecting
     * @return the new {@link Flowable} instance
     * @throws NullPointerException if {@code unit} or {@code scheduler} is {@code null}
     * @since 2.2
     */
    @CheckReturnValue
    @SchedulerSupport(SchedulerSupport.CUSTOM)
    @BackpressureSupport(BackpressureKind.PASS_THROUGH)
    @NonNull
    public final Flowable<T> refCount(long timeout, @NonNull TimeUnit unit, @NonNull Scheduler scheduler) {
        return refCount(1, timeout, unit, scheduler);
    }

    /**
     * Connects to the upstream {@code ConnectableFlowable} if the number of subscribed
     * subscriber reaches the specified count and disconnect after the specified
     * timeout if all subscribers have unsubscribed.
     * <dl>
     *  <dt><b>Backpressure:</b></dt>
     *  <dd>The operator itself doesn't interfere with backpressure which is determined by the upstream
     *  {@code ConnectableFlowable}'s backpressure behavior.</dd>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>This {@code refCount} overload operates on the {@code computation} {@link Scheduler}.</dd>
     * </dl>
     * <p>History: 2.1.14 - experimental
     * @param subscriberCount the number of subscribers required to connect to the upstream
     * @param timeout the time to wait before disconnecting after all subscribers unsubscribed
     * @param unit the time unit of the timeout
     * @return the new {@link Flowable} instance
     * @throws NullPointerException if {@code unit} is {@code null}
     * @throws IllegalArgumentException if {@code subscriberCount} is non-positive
     * @see #refCount(int, long, TimeUnit, Scheduler)
     * @since 2.2
     */
    @CheckReturnValue
    @SchedulerSupport(SchedulerSupport.COMPUTATION)
    @BackpressureSupport(BackpressureKind.PASS_THROUGH)
    @NonNull
    public final Flowable<T> refCount(int subscriberCount, long timeout, @NonNull TimeUnit unit) {
        return refCount(subscriberCount, timeout, unit, Schedulers.computation());
    }

    /**
     * Connects to the upstream {@code ConnectableFlowable} if the number of subscribed
     * subscriber reaches the specified count and disconnect after the specified
     * timeout if all subscribers have unsubscribed.
     * <dl>
     *  <dt><b>Backpressure:</b></dt>
     *  <dd>The operator itself doesn't interfere with backpressure which is determined by the upstream
     *  {@code ConnectableFlowable}'s backpressure behavior.</dd>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>This {@code refCount} overload operates on the specified {@link Scheduler}.</dd>
     * </dl>
     * <p>History: 2.1.14 - experimental
     * @param subscriberCount the number of subscribers required to connect to the upstream
     * @param timeout the time to wait before disconnecting after all subscribers unsubscribed
     * @param unit the time unit of the timeout
     * @param scheduler the target scheduler to wait on before disconnecting
     * @return the new {@link Flowable} instance
     * @throws NullPointerException if {@code unit} or {@code scheduler} is {@code null}
     * @throws IllegalArgumentException if {@code subscriberCount} is non-positive
     * @since 2.2
     */
    @CheckReturnValue
    @SchedulerSupport(SchedulerSupport.CUSTOM)
    @BackpressureSupport(BackpressureKind.PASS_THROUGH)
    @NonNull
    public final Flowable<T> refCount(int subscriberCount, long timeout, @NonNull TimeUnit unit, @NonNull Scheduler scheduler) {
        ObjectHelper.verifyPositive(subscriberCount, "subscriberCount");
        Objects.requireNonNull(unit, "unit is null");
        Objects.requireNonNull(scheduler, "scheduler is null");
        return RxJavaPlugins.onAssembly(new FlowableRefCount<>(this, subscriberCount, timeout, unit, scheduler));
    }

    /**
     * Returns a {@link Flowable} that automatically connects (at most once) to this {@code ConnectableFlowable}
     * when the first {@link Subscriber} subscribes.
     * <p>
     * <img width="640" height="392" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/autoConnect.f.png" alt="">
     * <p>
     * The connection happens after the first subscription and happens at most once
     * during the lifetime of the returned {@code Flowable}. If this {@code ConnectableFlowable}
     * terminates, the connection is never renewed, no matter how {@code Subscriber}s come
     * and go. Use {@link #refCount()} to renew a connection or dispose an active
     * connection when all {@code Subscriber}s have cancelled their {@link Subscription}s.
     * <p>
     * This overload does not allow disconnecting the connection established via
     * {@link #connect(Consumer)}. Use the {@link #autoConnect(int, Consumer)} overload
     * to gain access to the {@link Disposable} representing the only connection.
     * <dl>
     *  <dt><b>Backpressure:</b></dt>
     *  <dd>The operator itself doesn't interfere with backpressure which is determined by
     *  the upstream {@code ConnectableFlowable}'s behavior.</dd>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>{@code autoConnect} does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     *
     * @return a new {@code Flowable} instance that automatically connects to this {@code ConnectableFlowable}
     *         when the first {@code Subscriber} subscribes
     * @see #refCount()
     * @see #autoConnect(int, Consumer)
     */
    @NonNull
    @CheckReturnValue
    @BackpressureSupport(BackpressureKind.PASS_THROUGH)
    @SchedulerSupport(SchedulerSupport.NONE)
    public Flowable<T> autoConnect() {
        return autoConnect(1);
    }
    /**
     * Returns a {@link Flowable} that automatically connects (at most once) to this {@code ConnectableFlowable}
     * when the specified number of {@link Subscriber}s subscribe to it.
     * <p>
     * <img width="640" height="392" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/autoConnect.f.png" alt="">
     * <p>
     * The connection happens after the given number of subscriptions and happens at most once
     * during the lifetime of the returned {@code Flowable}. If this {@code ConnectableFlowable}
     * terminates, the connection is never renewed, no matter how {@code Subscriber}s come
     * and go. Use {@link #refCount()} to renew a connection or dispose an active
     * connection when all {@code Subscriber}s have cancelled their {@link Subscription}s.
     * <p>
     * This overload does not allow disconnecting the connection established via
     * {@link #connect(Consumer)}. Use the {@link #autoConnect(int, Consumer)} overload
     * to gain access to the {@link Disposable} representing the only connection.
     * <dl>
     *  <dt><b>Backpressure:</b></dt>
     *  <dd>The operator itself doesn't interfere with backpressure which is determined by
     *  the upstream {@code ConnectableFlowable}'s behavior.</dd>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>{@code autoConnect} does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     *
     * @param numberOfSubscribers the number of subscribers to await before calling connect
     *                            on the {@code ConnectableFlowable}. A non-positive value indicates
     *                            an immediate connection.
     * @return a new {@code Flowable} instance that automatically connects to this {@code ConnectableFlowable}
     *         when the specified number of {@code Subscriber}s subscribe to it
     */
    @NonNull
    @CheckReturnValue
    @BackpressureSupport(BackpressureKind.PASS_THROUGH)
    @SchedulerSupport(SchedulerSupport.NONE)
    public Flowable<T> autoConnect(int numberOfSubscribers) {
        return autoConnect(numberOfSubscribers, Functions.emptyConsumer());
    }

    /**
     * Returns a {@link Flowable} that automatically connects (at most once) to this {@code ConnectableFlowable}
     * when the specified number of {@link Subscriber}s subscribe to it and calls the
     * specified callback with the {@link Disposable} associated with the established connection.
     * <p>
     * <img width="640" height="392" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/autoConnect.f.png" alt="">
     * <p>
     * The connection happens after the given number of subscriptions and happens at most once
     * during the lifetime of the returned {@code Flowable}. If this {@code ConnectableFlowable}
     * terminates, the connection is never renewed, no matter how {@code Subscriber}s come
     * and go. Use {@link #refCount()} to renew a connection or dispose an active
     * connection when all {@code Subscriber}s have cancelled their {@link Subscription}s.
     * <dl>
     *  <dt><b>Backpressure:</b></dt>
     *  <dd>The operator itself doesn't interfere with backpressure which is determined by
     *  the upstream {@code ConnectableFlowable}'s behavior.</dd>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>{@code autoConnect} does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     *
     * @param numberOfSubscribers the number of subscribers to await before calling connect
     *                            on the {@code ConnectableFlowable}. A non-positive value indicates
     *                            an immediate connection.
     * @param connection the callback {@link Consumer} that will receive the {@code Disposable} representing the
     *                   established connection
     * @return a new {@code Flowable} instance that automatically connects to this {@code ConnectableFlowable}
     *         when the specified number of {@code Subscriber}s subscribe to it and calls the
     *         specified callback with the {@code Disposable} associated with the established connection
     * @throws NullPointerException if {@code connection} is {@code null}
     */
    @NonNull
    @CheckReturnValue
    @BackpressureSupport(BackpressureKind.PASS_THROUGH)
    @SchedulerSupport(SchedulerSupport.NONE)
    public Flowable<T> autoConnect(int numberOfSubscribers, @NonNull Consumer<? super Disposable> connection) {
        Objects.requireNonNull(connection, "connection is null");
        if (numberOfSubscribers <= 0) {
            this.connect(connection);
            return RxJavaPlugins.onAssembly(this);
        }
        return RxJavaPlugins.onAssembly(new FlowableAutoConnect<>(this, numberOfSubscribers, connection));
    }
}
