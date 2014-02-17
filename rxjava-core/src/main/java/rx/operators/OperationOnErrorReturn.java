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

import java.util.Arrays;
import java.util.concurrent.atomic.AtomicReference;

import rx.Observable;
import rx.Observable.OnSubscribeFunc;
import rx.Observer;
import rx.Subscription;
import rx.exceptions.CompositeException;
import rx.functions.Action0;
import rx.functions.Func1;
import rx.subscriptions.Subscriptions;

/**
 * Instruct an Observable to emit a particular item to its Observer's <code>onNext</code> method
 * rather than invoking <code>onError</code> if it encounters an error.
 * <p>
 * <img width="640" src="https://github.com/Netflix/RxJava/wiki/images/rx-operators/onErrorReturn.png">
 * <p>
 * By default, when an Observable encounters an error that prevents it from emitting the expected
 * item to its Observer, the Observable invokes its Observer's <code>onError</code> method, and then
 * quits without invoking any more of its Observer's methods. The onErrorReturn operation changes
 * this behavior. If you pass a function (resumeFunction) to onErrorReturn, if the original
 * Observable encounters an error, instead of invoking its Observer's <code>onError</code> method,
 * it will instead pass the return value of resumeFunction to the Observer's <code>onNext</code>
 * method.
 * <p>
 * You can use this to prevent errors from propagating or to supply fallback data should errors be
 * encountered.
 */
public final class OperationOnErrorReturn<T> {

    public static <T> OnSubscribeFunc<T> onErrorReturn(Observable<? extends T> originalSequence, Func1<Throwable, ? extends T> resumeFunction) {
        return new OnErrorReturn<T>(originalSequence, resumeFunction);
    }

    private static class OnErrorReturn<T> implements OnSubscribeFunc<T> {
        private final Func1<Throwable, ? extends T> resumeFunction;
        private final Observable<? extends T> originalSequence;

        public OnErrorReturn(Observable<? extends T> originalSequence, Func1<Throwable, ? extends T> resumeFunction) {
            this.resumeFunction = resumeFunction;
            this.originalSequence = originalSequence;
        }

        public Subscription onSubscribe(final Observer<? super T> observer) {
            final SafeObservableSubscription subscription = new SafeObservableSubscription();

            // AtomicReference since we'll be accessing/modifying this across threads so we can switch it if needed
            final AtomicReference<SafeObservableSubscription> subscriptionRef = new AtomicReference<SafeObservableSubscription>(subscription);

            // subscribe to the original Observable and remember the subscription
            subscription.wrap(originalSequence.subscribe(new Observer<T>() {
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
                            /* error occurred, so execute the function, give it the exception and call onNext with the response */
                            onNext(resumeFunction.call(ex));
                            /*
                             * we are not handling an exception thrown from this function ... should we do something?
                             * error handling within an error handler is a weird one to determine what we should do
                             * right now I'm going to just let it throw whatever exceptions occur (such as NPE)
                             * but I'm considering calling the original Observer.onError to act as if this OnErrorReturn operator didn't happen
                             */

                            /* we are now completed */
                            onCompleted();

                            /* unsubscribe since it blew up */
                            currentSubscription.unsubscribe();
                        } catch (Throwable e) {
                            // the return function failed so we need to call onError
                            // I am using CompositeException so that both exceptions can be seen
                            observer.onError(new CompositeException("OnErrorReturn function failed", Arrays.asList(ex, e)));
                        }
                    }
                }

                public void onCompleted() {
                    // forward the successful calls
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
