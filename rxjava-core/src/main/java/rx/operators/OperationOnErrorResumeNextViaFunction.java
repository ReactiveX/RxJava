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
import rx.Subscription;
import rx.util.CompositeException;
import rx.util.functions.Func1;

import java.util.Arrays;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Instruct an Observable to pass control to another Observable (the return value of a function)
 * rather than invoking <code>onError</code> if it encounters an error.
 * <p>
 * <img width="640" src="https://github.com/Netflix/RxJava/wiki/images/rx-operators/onErrorResumeNext.png">
 * <p>
 * By default, when an Observable encounters an error that prevents it from emitting the expected
 * item to its Observer, the Observable invokes its Observer's <code>onError</code> method, and
 * then quits without invoking any more of its Observer's methods. The onErrorResumeNext operation
 * changes this behavior. If you pass a function that returns an Observable (resumeFunction) to
 * onErrorResumeNext, if the source Observable encounters an error, instead of invoking its
 * Observer's <code>onError</code> method, it will instead relinquish control to this new
 * Observable, which will invoke the Observer's <code>onNext</code> method if it is able to do so.
 * In such a case, because no Observable necessarily invokes <code>onError</code>, the Observer may
 * never know that an error happened.
 * <p>
 * You can use this to prevent errors from propagating or to supply fallback data should errors be
 * encountered.
 */
public final class OperationOnErrorResumeNextViaFunction<T> {

    public static <T> OnSubscribeFunc<T> onErrorResumeNextViaFunction(Observable<? extends T> originalSequence, Func1<Throwable, ? extends Observable<? extends T>> resumeFunction) {
        return new OnErrorResumeNextViaFunction<T>(originalSequence, resumeFunction);
    }

    private static class OnErrorResumeNextViaFunction<T> implements OnSubscribeFunc<T> {

        private final Func1<Throwable, ? extends Observable<? extends T>> resumeFunction;
        private final Observable<? extends T> originalSequence;

        public OnErrorResumeNextViaFunction(Observable<? extends T> originalSequence, Func1<Throwable, ? extends Observable<? extends T>> resumeFunction) {
            this.resumeFunction = resumeFunction;
            this.originalSequence = originalSequence;
        }

        public Subscription onSubscribe(final Observer<? super T> observer) {
            // AtomicReference since we'll be accessing/modifying this across threads so we can switch it if needed
            final AtomicReference<SafeObservableSubscription> subscriptionRef = new AtomicReference<SafeObservableSubscription>(new SafeObservableSubscription());

            // subscribe to the original Observable and remember the subscription
            subscriptionRef.get().wrap(new SafeObservableSubscription(originalSequence.subscribe(new Observer<T>() {
                public void onNext(T value) {
                    // forward the successful calls
                    observer.onNext(value);
                }

                /**
                 * Instead of passing the onError forward, we intercept and "resume" with the resumeSequence.
                 */
                public void onError(Throwable ex) {
                    /* remember what the current subscription is so we can determine if someone unsubscribes concurrently */
                    SafeObservableSubscription currentSubscription = subscriptionRef.get();
                    // check that we have not been unsubscribed before we can process the error
                    if (currentSubscription != null) {
                        try {
                            Observable<? extends T> resumeSequence = resumeFunction.call(ex);
                            /* error occurred, so switch subscription to the 'resumeSequence' */
                            SafeObservableSubscription innerSubscription = new SafeObservableSubscription(resumeSequence.subscribe(observer));
                            /* we changed the sequence, so also change the subscription to the one of the 'resumeSequence' instead */
                            if (!subscriptionRef.compareAndSet(currentSubscription, innerSubscription)) {
                                // we failed to set which means 'subscriptionRef' was set to NULL via the unsubscribe below
                                // so we want to immediately unsubscribe from the resumeSequence we just subscribed to
                                innerSubscription.unsubscribe();
                            }
                        } catch (Throwable e) {
                            // the resume function failed so we need to call onError
                            // I am using CompositeException so that both exceptions can be seen
                            observer.onError(new CompositeException("OnErrorResume function failed", Arrays.asList(ex, e)));
                        }
                    }
                }

                public void onCompleted() {
                    // forward the successful calls
                    observer.onCompleted();
                }
            })));

            return new Subscription() {
                public void unsubscribe() {
                    // this will get either the original, or the resumeSequence one and unsubscribe on it
                    Subscription s = subscriptionRef.getAndSet(null);
                    if (s != null) {
                        s.unsubscribe();
                    }
                }
            };
        }
    }
}
