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

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import rx.Observable;
import rx.Observer;
import rx.Subscription;
import rx.util.AtomicObservableSubscription;
import rx.util.AtomicObserver;
import rx.util.functions.Action0;
import rx.util.functions.Func1;

public final class OperationFinally {

    /**
     * Call a given action when a sequence completes (with or without an
     * exception).  The returned observable is exactly as threadsafe as the
     * source observable; in particular, any situation allowing the source to
     * call onComplete or onError multiple times allows the returned observable
     * to call the action multiple times.
     * <p/>
     * Note that "finally" is a Java reserved word and cannot be an identifier,
     * so we use "finally0".
     * 
     * @param sequence An observable sequence of elements
     * @param action An action to be taken when the sequence is complete or throws an exception
     * @return An observable sequence with the same elements as the input.
     *         After the last element is consumed (just before {@link Observer#onComplete} is called),
     *         or when an exception is thrown (just before {@link Observer#onError}), the action will be taken.
     * @see http://msdn.microsoft.com/en-us/library/hh212133(v=vs.103).aspx
     */
    public static <T> Func1<Observer<T>, Subscription> finally0(final Observable<T> sequence, final Action0 action) {
        return new Func1<Observer<T>, Subscription>() {
            @Override
            public Subscription call(Observer<T> observer) {
                return new Finally<T>(sequence, action).call(observer);
            }
        };
    }

    private static class Finally<T> implements Func1<Observer<T>, Subscription> {
        private final Observable<T> sequence;
        private final Action0 finalAction;
        private Subscription s;

        Finally(final Observable<T> sequence, Action0 finalAction) {
            this.sequence = sequence;
            this.finalAction = finalAction;
        }

        private final AtomicObservableSubscription Subscription = new AtomicObservableSubscription();

        private final Subscription actualSubscription = new Subscription() {
            @Override
            public void unsubscribe() {
                if (null != s)
                    s.unsubscribe();
            }
        };

        public Subscription call(Observer<T> observer) {
            s = sequence.subscribe(new FinallyObserver(observer));
            return Subscription.wrap(actualSubscription);
        }

        private class FinallyObserver implements Observer<T> {
            private final Observer<T> observer;

            FinallyObserver(Observer<T> observer) {
                this.observer = observer;
            }

            @Override
            public void onCompleted() {
                finalAction.call();
                observer.onCompleted();
            }

            @Override
            public void onError(Exception e) {
                finalAction.call();
                observer.onError(e);
            }

            @Override
            public void onNext(T args) {
                observer.onNext(args);
            }
        }
    }

    public static class UnitTest {
        private static class TestAction implements Action0 {
            public int called = 0;
            @Override public void call() {
                called++;
            }
        }
        
        @Test
        public void testFinally() {
            final String[] n = {"1", "2", "3"};
            final Observable<String> nums = Observable.toObservable(n);
            TestAction action = new TestAction();
            action.called = 0;
            @SuppressWarnings("unchecked")
            Observable<String> fin = Observable.create(finally0(nums, action));
            @SuppressWarnings("unchecked")
            Observer<String> aObserver = mock(Observer.class);
            fin.subscribe(aObserver);
            Assert.assertEquals(1, action.called);

            action.called = 0;
            Observable<String> error = Observable.<String>error(new RuntimeException("expected"));
            fin = Observable.create(finally0(error, action));
            fin.subscribe(aObserver);
            Assert.assertEquals(1, action.called);
        }
    }
}