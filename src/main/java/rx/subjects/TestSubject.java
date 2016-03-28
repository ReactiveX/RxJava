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

import rx.Observer;
import rx.Scheduler;
import rx.functions.Action0;
import rx.functions.Action1;
import rx.internal.operators.NotificationLite;
import rx.schedulers.TestScheduler;
import rx.subjects.SubjectSubscriptionManager.SubjectObserver;

import java.util.concurrent.TimeUnit;

/**
 * A variety of Subject that is useful for testing purposes. It operates on a {@link TestScheduler} and allows
 * you to precisely time emissions and notifications to the Subject's subscribers using relative virtual time
 * controlled by the {@link TestScheduler}.
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
                o.emitFirst(state.getLatest(), state.nl);
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

    /**
     * Schedule a call to {@code onCompleted} on TestScheduler.
     */
    @Override
    public void onCompleted() {
        onCompleted(0);
    }

    void _onCompleted() {
        if (state.active) {
            for (SubjectObserver<T> bo : state.terminate(NotificationLite.instance().completed())) {
                bo.onCompleted();
            }
        }
    }

    /**
     * Schedule a call to {@code onCompleted} relative to "now()" +n milliseconds in the future.
     *
     * @param delayTime
     *         the number of milliseconds in the future relative to "now()" at which to call {@code onCompleted}
     */
    public void onCompleted(long delayTime) {
        innerScheduler.schedule(new Action0() {

            @Override
            public void call() {
                _onCompleted();
            }

        }, delayTime, TimeUnit.MILLISECONDS);
    }

    /**
     * Schedule a call to {@code onError} on TestScheduler.
     */
    @Override
    public void onError(final Throwable e) {
        onError(e, 0);
    }

    void _onError(final Throwable e) {
        if (state.active) {
            for (SubjectObserver<T> bo : state.terminate(NotificationLite.instance().error(e))) {
                bo.onError(e);
            }
        }
    }

    /**
     * Schedule a call to {@code onError} relative to "now()" +n milliseconds in the future.
     *
     * @param e
     *         the {@code Throwable} to pass to the {@code onError} method
     * @param delayTime
     *         the number of milliseconds in the future relative to "now()" at which to call {@code onError}
     */
    public void onError(final Throwable e, long delayTime) {
        innerScheduler.schedule(new Action0() {

            @Override
            public void call() {
                _onError(e);
            }

        }, delayTime, TimeUnit.MILLISECONDS);
    }

    /**
     * Schedule a call to {@code onNext} on TestScheduler.
     */
    @Override
    public void onNext(T v) {
        onNext(v, 0);
    }

    void _onNext(T v) {
        for (Observer<? super T> o : state.observers()) {
            o.onNext(v);
        }
    }

    /**
     * Schedule a call to {@code onNext} relative to "now()" +n milliseconds in the future.
     *
     * @param v
     *         the item to emit
     * @param delayTime
     *         the number of milliseconds in the future relative to "now()" at which to call {@code onNext}
     */
    public void onNext(final T v, long delayTime) {
        innerScheduler.schedule(new Action0() {

            @Override
            public void call() {
                _onNext(v);
            }

        }, delayTime, TimeUnit.MILLISECONDS);
    }

    @Override
    public boolean hasObservers() {
        return state.observers().length > 0;
    }
}
