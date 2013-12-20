 /**
  * Copyright 2013 Netflix, Inc.
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
import rx.Observable.OnSubscribeFunc;
import rx.Observer;
import rx.Scheduler;
import rx.Subscription;
import rx.subjects.Subject;
import rx.util.functions.Func1;

/**
 * Replay with limited buffer and/or time constraints.
 *
 *
 * @see <a href='http://msdn.microsoft.com/en-us/library/system.reactive.linq.observable.replay.aspx'>MSDN: Observable.Replay overloads</a>
 */
public final class OperationReplay {
    /** Utility class. */
    private OperationReplay() { throw new IllegalStateException("No instances!"); }
    
    
    /**
     * Creates a subject whose client observers will observe events
     * propagated through the given wrapped subject.
     */
    public static <T> Subject<T, T> createScheduledSubject(Subject<T, T> subject, Scheduler scheduler) {
        Observable<T> observedOn = subject.observeOn(scheduler);
        SubjectWrapper<T> s = new SubjectWrapper<T>(subscriberOf(observedOn), subject);
        return s;
    }
    
    /**
     * Return an OnSubscribeFunc which delegates the subscription to the given observable.
     */
    public static <T> OnSubscribeFunc<T> subscriberOf(final Observable<T> target) {
        return new OnSubscribeFunc<T>() {
            @Override
            public Subscription onSubscribe(Observer<? super T> t1) {
                return target.subscribe(t1);
            }
        };
    }
    
    /**
     * Subject that wraps another subject and uses a mapping function
     * to transform the received values.
     */
    public static final class MappingSubject<T, R> extends Subject<T, R> {
        private final Subject<R, R> subject;
        private final Func1<T, R> selector;
        public MappingSubject(OnSubscribeFunc<R> func, Subject<R, R> subject, Func1<T, R> selector) {
            super(func);
            this.subject = subject;
            this.selector = selector;
        }

        @Override
        public void onNext(T args) {
            subject.onNext(selector.call(args));
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
    
    /**
     * A subject that wraps another subject.
     */
    public static final class SubjectWrapper<T> extends Subject<T, T> {
        /** The wrapped subject. */
        final Subject<T, T> subject;
        public SubjectWrapper(OnSubscribeFunc<T> func, Subject<T, T> subject) {
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
