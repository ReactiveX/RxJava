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
package rx.subjects;

import java.util.concurrent.TimeUnit;

import rx.Observer;
import rx.Scheduler;
import rx.functions.Action0;
import rx.functions.Action1;
import rx.internal.operators.NotificationLite;
import rx.schedulers.TestScheduler;
import rx.subjects.SubjectSubscriptionManager.SubjectObserver;

/**
 * A variety of Subject that is useful for testing purposes. It operates on a {@link TestScheduler} and allows
 * you to precisely time emissions and notifications to the Subject's subscribers.
 *
 * @param <T>
 *          the type of item observed by and emitted by the subject
 */
public final class TestSubject<T> extends Subject<T, T> {

    /**
     * Creates and returns a new {@code TestSubject}.
     *
     * @param <T> the value type
     * @param scheduler a {@link TestScheduler} on which to operate this Subject
     * @return the new {@code TestSubject}
     */
    public static <T> TestSubject<T> create(TestScheduler scheduler) {
        final SubjectSubscriptionManager<T> state = new SubjectSubscriptionManager<T>();

        state.onAdded = new Action1<SubjectObserver<T>>() {

            @Override
            public void call(SubjectObserver<T> o) {
                o.emitFirst(state.get(), state.nl);
            }
            
        };
        state.onTerminated = state.onAdded;

        return new TestSubject<T>(state, state, scheduler);
    }

    private final SubjectSubscriptionManager<T> state;
    private final Scheduler.Worker innerScheduler;

    protected TestSubject(OnSubscribe<T> onSubscribe, SubjectSubscriptionManager<T> state, TestScheduler scheduler) {
        super(onSubscribe);
        this.state = state;
        this.innerScheduler = scheduler.createWorker();
    }

    @Override
    public void onCompleted() {
        onCompleted(innerScheduler.now());
    }

    private void _onCompleted() {
        if (state.active) {
            for (SubjectObserver<T> bo : state.terminate(NotificationLite.instance().completed())) {
                bo.onCompleted();
            }
        }
    }

    /**
     * Schedule a call to the {@code onCompleted} methods of all of the subscribers to this Subject to begin at
     * a particular time.
     * 
     * @param timeInMilliseconds
     *         the time at which to begin calling the {@code onCompleted} methods of the subscribers
     */
    public void onCompleted(long timeInMilliseconds) {
        innerScheduler.schedule(new Action0() {

            @Override
            public void call() {
                _onCompleted();
            }

        }, timeInMilliseconds, TimeUnit.MILLISECONDS);
    }

    @Override
    public void onError(final Throwable e) {
        onError(e, innerScheduler.now());
    }

    private void _onError(final Throwable e) {
        if (state.active) {
            for (SubjectObserver<T> bo : state.terminate(NotificationLite.instance().error(e))) {
                bo.onError(e);
            }
        }
    }

    /**
     * Schedule a call to the {@code onError} methods of all of the subscribers to this Subject to begin at
     * a particular time.
     * 
     * @param e
     *         the {@code Throwable} to pass to the {@code onError} methods of the subscribers
     * @param timeInMilliseconds
     *         the time at which to begin calling the {@code onError} methods of the subscribers
     */
    public void onError(final Throwable e, long timeInMilliseconds) {
        innerScheduler.schedule(new Action0() {

            @Override
            public void call() {
                _onError(e);
            }

        }, timeInMilliseconds, TimeUnit.MILLISECONDS);
    }

    @Override
    public void onNext(T v) {
        onNext(v, innerScheduler.now());
    }

    private void _onNext(T v) {
        for (Observer<? super T> o : state.observers()) {
            o.onNext(v);
        }
    }

    /**
     * Emit an item to all of the subscribers to this Subject at a particular time.
     * 
     * @param v
     *         the item to emit
     * @param timeInMilliseconds
     *         the time at which to begin calling the {@code onNext} methods of the subscribers in order to emit
     *         the item
     */
    public void onNext(final T v, long timeInMilliseconds) {
        innerScheduler.schedule(new Action0() {

            @Override
            public void call() {
                _onNext(v);
            }

        }, timeInMilliseconds, TimeUnit.MILLISECONDS);
    }
}
