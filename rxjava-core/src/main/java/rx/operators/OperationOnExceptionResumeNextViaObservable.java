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

import java.util.concurrent.atomic.AtomicReference;

import rx.Observable;
import rx.Observable.OnSubscribeFunc;
import rx.Observer;
import rx.Subscription;
import rx.functions.Action0;
import rx.subscriptions.Subscriptions;

/**
 * Instruct an Observable to pass control to another Observable rather than invoking
 * <code>onError</code> if it encounters an error of type {@link java.lang.Exception}.
 * <p>
 * This differs from {@link Observable#onErrorResumeNext} in that this one does not handle {@link java.lang.Throwable} or {@link java.lang.Error} but lets those continue through.
 * <p>
 * <img width="640" src="https://github.com/Netflix/RxJava/wiki/images/rx-operators/onErrorResumeNext.png">
 * <p>
 * By default, when an Observable encounters an error that prevents it from emitting the expected
 * item to its Observer, the Observable invokes its Observer's <code>onError</code> method, and
 * then quits without invoking any more of its Observer's methods. The onErrorResumeNext operation
 * changes this behavior. If you pass an Observable (resumeSequence) to onErrorResumeNext, if the
 * source Observable encounters an error, instead of invoking its Observer's <code>onError</code>
 * method, it will instead relinquish control to this new Observable, which will invoke the
 * Observer's <code>onNext</code> method if it is able to do so. In such a case, because no
 * Observable necessarily invokes <code>onError</code>, the Observer may never know that an error
 * happened.
 * <p>
 * You can use this to prevent errors from propagating or to supply fallback data should errors be
 * encountered.
 */
public final class OperationOnExceptionResumeNextViaObservable<T> {

    public static <T> OnSubscribeFunc<T> onExceptionResumeNextViaObservable(Observable<? extends T> originalSequence, Observable<? extends T> resumeSequence) {
        return new OnExceptionResumeNextViaObservable<T>(originalSequence, resumeSequence);
    }

    private static class OnExceptionResumeNextViaObservable<T> implements OnSubscribeFunc<T> {

        private final Observable<? extends T> resumeSequence;
        private final Observable<? extends T> originalSequence;

        public OnExceptionResumeNextViaObservable(Observable<? extends T> originalSequence, Observable<? extends T> resumeSequence) {
            this.resumeSequence = resumeSequence;
            this.originalSequence = originalSequence;
        }

        public Subscription onSubscribe(final Observer<? super T> observer) {
            final SafeObservableSubscription subscription = new SafeObservableSubscription();

            // AtomicReference since we'll be accessing/modifying this across threads so we can switch it if needed
            final AtomicReference<SafeObservableSubscription> subscriptionRef = new AtomicReference<SafeObservableSubscription>(subscription);

            // subscribe to the original Observable and remember the subscription
            subscription.wrap(originalSequence.subscribe(new Observer<T>() {
                public void onNext(T value) {
                    // forward the successful calls unless resumed
                    if (subscriptionRef.get() == subscription)
                        observer.onNext(value);
                }

                /**
                 * When we receive java.lang.Exception, instead of passing the onError forward, we intercept and "resume" with the resumeSequence.
                 * For Throwable and Error we pass thru.
                 */
                public void onError(Throwable ex) {
                    if (ex instanceof Exception) {
                        /* remember what the current subscription is so we can determine if someone unsubscribes concurrently */
                        SafeObservableSubscription currentSubscription = subscriptionRef.get();
                        // check that we have not been unsubscribed and not already resumed before we can process the error
                        if (currentSubscription == subscription) {
                            /* error occurred, so switch subscription to the 'resumeSequence' */
                            SafeObservableSubscription innerSubscription = new SafeObservableSubscription(resumeSequence.subscribe(observer));
                            /* we changed the sequence, so also change the subscription to the one of the 'resumeSequence' instead */
                            if (!subscriptionRef.compareAndSet(currentSubscription, innerSubscription)) {
                                // we failed to set which means 'subscriptionRef' was set to NULL via the unsubscribe below
                                // so we want to immediately unsubscribe from the resumeSequence we just subscribed to
                                innerSubscription.unsubscribe();
                            }
                        }
                    } else {
                        observer.onError(ex);
                    }
                }

                public void onCompleted() {
                    // forward the successful calls unless resumed
                    if (subscriptionRef.get() == subscription)
                        observer.onCompleted();
                }
            }));

            return Subscriptions.create(new Action0() {
                public void call() {
                    // this will get either the original, or the resumeSequence one and unsubscribe on it
                    Subscription s = subscriptionRef.getAndSet(null);
                    if (s != null) {
                        s.unsubscribe();
                    }
                }
            });
        }
    }
}
