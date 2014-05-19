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
package rx.operators;


import rx.Observable;
import rx.Observable.OnSubscribe;
import rx.Scheduler;
import rx.Subscriber;
import rx.subjects.Subject;

/**
 * Replay with limited buffer and/or time constraints.
 * 
 * 
 * @see <a href='http://msdn.microsoft.com/en-us/library/system.reactive.linq.observable.replay.aspx'>MSDN: Observable.Replay overloads</a>
 */
public final class OperatorReplay {
    /** Utility class. */
    private OperatorReplay() {
        throw new IllegalStateException("No instances!");
    }

    /**
     * Creates a subject whose client observers will observe events
     * propagated through the given wrapped subject.
     * @param <T> the element type
     * @param subject the subject to wrap
     * @param scheduler the target scheduler
     * @return the created subject
     */
    public static <T> Subject<T, T> createScheduledSubject(Subject<T, T> subject, Scheduler scheduler) {
        final Observable<T> observedOn = subject.observeOn(scheduler);
        SubjectWrapper<T> s = new SubjectWrapper<T>(new OnSubscribe<T>() {

            @Override
            public void call(Subscriber<? super T> o) {
                subscriberOf(observedOn).call(o);
            }

        }, subject);
        return s;
    }

    /**
     * Return an OnSubscribeFunc which delegates the subscription to the given observable.
     * 
     * @param <T> the value type
     * @param target the target observable
     * @return the function that delegates the subscription to the target
     */
    public static <T> OnSubscribe<T> subscriberOf(final Observable<T> target) {
        return new OnSubscribe<T>() {
            @Override
            public void call(Subscriber<? super T> t1) {
                target.unsafeSubscribe(t1);
            }
        };
    }

    /**
     * A subject that wraps another subject.
     * @param <T> the value type
     */
    public static final class SubjectWrapper<T> extends Subject<T, T> {
        /** The wrapped subject. */
        final Subject<T, T> subject;

        public SubjectWrapper(OnSubscribe<T> func, Subject<T, T> subject) {
            super(func);
            this.subject = subject;
        }

        @Override
        public void onNext(T args) {
            subject.onNext(args);
        }

        @Override
        public void onError(Throwable e) {
            subject.onError(e);
        }

        @Override
        public void onCompleted() {
            subject.onCompleted();
        }

    }
}