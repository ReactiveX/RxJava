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

import java.util.Arrays;
import java.util.concurrent.atomic.AtomicBoolean;

import rx.Observer;
import rx.Subscription;
import rx.exceptions.CompositeException;
import rx.exceptions.OnErrorNotImplementedException;
import rx.observers.SerializedObserver;
import rx.plugins.RxJavaPlugins;
import rx.subscriptions.Subscriptions;

/**
 * Wrapper around Observer to ensure compliance with Rx contract.
 * <p>
 * The following is taken from <a href="http://go.microsoft.com/fwlink/?LinkID=205219">the Rx Design Guidelines
 * document</a>:
 * <pre>
 * Messages sent to instances of the IObserver interface follow the following grammar:
 * 
 * OnNext* (OnCompleted | OnError)?
 * 
 * This grammar allows observable sequences to send any amount (0 or more) of OnNext messages to the subscribed
 * observer instance, optionally followed by a single success (OnCompleted) or failure (OnError) message.
 * 
 * The single message indicating that an observable sequence has finished ensures that consumers of the
 * observable sequence can deterministically establish that it is safe to perform cleanup operations.
 * 
 * A single failure further ensures that abort semantics can be maintained for operators that work on
 * multiple observable sequences (see paragraph 6.6).
 * </pre>
 * 
 * <p>
 * This wrapper will do the following:
 * <ul>
 * <li>Allow only single execution of either onError or onCompleted.</li>
 * <li>Once an onComplete or onError are performed, no further calls can be executed</li>
 * <li>If unsubscribe is called, this means we call completed() and don't allow any further onNext calls.</li>
 * <li>When onError or onComplete occur it will unsubscribe from the Observable (if executing asynchronously).</li>
 * </ul>
 * <p>
 * It will not synchronize onNext execution. Use the {@link SerializedObserver} to do that.
 * 
 * @param <T>
 * @deprecated replaced by SafeSubscriber
 */
@Deprecated
public class SafeObserver<T> implements Observer<T> {

    private final Observer<? super T> actual;
    private final AtomicBoolean isFinished = new AtomicBoolean(false);
    private final Subscription subscription;

    public SafeObserver(Observer<? super T> actual) {
        this.subscription = Subscriptions.empty();
        this.actual = actual;
    }

    public SafeObserver(SafeObservableSubscription subscription, Observer<? super T> actual) {
        this.subscription = subscription;
        this.actual = actual;
    }

    @Override
    public void onCompleted() {
        if (isFinished.compareAndSet(false, true)) {
            try {
                actual.onCompleted();
            } catch (Throwable e) {
                // handle errors if the onCompleted implementation fails, not just if the Observable fails
                _onError(e);
            } finally {
                // auto-unsubscribe
                subscription.unsubscribe();
            }
        }
    }

    @Override
    public void onError(Throwable e) {
        if (isFinished.compareAndSet(false, true)) {
            _onError(e);
        }
    }

    @Override
    public void onNext(T args) {
        try {
            if (!isFinished.get()) {
                actual.onNext(args);
            }
        } catch (Throwable e) {
            // handle errors if the onNext implementation fails, not just if the Observable fails
            onError(e);
        }
    }

    /*
     * The logic for `onError` without the `isFinished` check so it can be called from within `onCompleted`.
     * 
     * See https://github.com/Netflix/RxJava/issues/630 for the report of this bug.
     */
    protected void _onError(Throwable e) {
        try {
            RxJavaPlugins.getInstance().getErrorHandler().handleError(e);
            actual.onError(e);
        } catch (Throwable e2) {
            if (e2 instanceof OnErrorNotImplementedException) {
                /*
                 * onError isn't implemented so throw
                 * 
                 * https://github.com/Netflix/RxJava/issues/198
                 * 
                 * Rx Design Guidelines 5.2
                 * 
                 * "when calling the Subscribe method that only has an onNext argument, the OnError behavior will be
                 * to rethrow the exception on the thread that the message comes out from the observable sequence.
                 * The OnCompleted behavior in this case is to do nothing."
                 */
                try {
                    subscription.unsubscribe();
                } catch (Throwable unsubscribeException) {
                    RxJavaPlugins.getInstance().getErrorHandler().handleError(unsubscribeException);
                    throw new RuntimeException("Observer.onError not implemented and error while unsubscribing.", new CompositeException(Arrays.asList(e, unsubscribeException)));
                }
                throw (OnErrorNotImplementedException) e2;
            } else {
                /*
                 * throw since the Rx contract is broken if onError failed
                 * 
                 * https://github.com/Netflix/RxJava/issues/198
                 */
                RxJavaPlugins.getInstance().getErrorHandler().handleError(e2);
                try {
                    subscription.unsubscribe();
                } catch (Throwable unsubscribeException) {
                    RxJavaPlugins.getInstance().getErrorHandler().handleError(unsubscribeException);
                    throw new RuntimeException("Error occurred when trying to propagate error to Observer.onError and during unsubscription.", new CompositeException(Arrays.asList(e, e2, unsubscribeException)));
                }

                throw new RuntimeException("Error occurred when trying to propagate error to Observer.onError", new CompositeException(Arrays.asList(e, e2)));
            }
        }
        // if we did not throw about we will unsubscribe here, if onError failed then unsubscribe happens in the catch
        try {
            subscription.unsubscribe();
        } catch (RuntimeException unsubscribeException) {
            RxJavaPlugins.getInstance().getErrorHandler().handleError(unsubscribeException);
            throw unsubscribeException;
        }
    }

}
