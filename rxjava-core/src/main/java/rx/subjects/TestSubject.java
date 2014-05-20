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
import rx.operators.NotificationLite;
import rx.schedulers.TestScheduler;
import rx.subjects.SubjectSubscriptionManager.SubjectObserver;

/**
 * Subject that, once and {@link Observer} has subscribed, publishes all subsequent events to the subscriber.
 * <p>
 * <img width="640" src="https://raw.github.com/wiki/Netflix/RxJava/images/rx-operators/S.PublishSubject.png">
 * <p>
 * Example usage:
 * <p>
 * <pre> {@code

 * PublishSubject<Object> subject = PublishSubject.create();
  // observer1 will receive all onNext and onCompleted events
  subject.subscribe(observer1);
  subject.onNext("one");
  subject.onNext("two");
  // observer2 will only receive "three" and onCompleted
  subject.subscribe(observer2);
  subject.onNext("three");
  subject.onCompleted();

  } </pre>
 * 
 * @param <T>
 */
public final class TestSubject<T> extends Subject<T, T> {

    public static <T> TestSubject<T> create(TestScheduler scheduler) {
        final SubjectSubscriptionManager<T> state = new SubjectSubscriptionManager<T>();

        state.onAdded = new Action1<SubjectObserver<T>>() {

            @Override
            public void call(SubjectObserver<T> o) {
                o.emitFirst(state.get());
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

    public void onNext(final T v, long timeInMilliseconds) {
        innerScheduler.schedule(new Action0() {

            @Override
            public void call() {
                _onNext(v);
            }

        }, timeInMilliseconds, TimeUnit.MILLISECONDS);
    }
}
