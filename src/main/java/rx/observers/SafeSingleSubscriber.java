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
package rx.observers;

import java.util.Arrays;

import rx.SingleSubscriber;
import rx.Subscriber;
import rx.exceptions.CompositeException;
import rx.exceptions.Exceptions;
import rx.exceptions.OnErrorFailedException;
import rx.exceptions.OnErrorNotImplementedException;
import rx.exceptions.UnsubscribeFailedException;
import rx.plugins.RxJavaHooks;

/**
 * {@code SafeSingleSubscriber} is a wrapper around {@code SingleSubscriber} that ensures that the {@code SingleSubscriber}
 * complies with <a href="http://reactivex.io/documentation/single.html">the Single contract</a>.
 * <p>
 * <p>
 * This wrapper does the following:
 * <ul>
 * <li>Allows only single execution of either {@code onSuccess} or {@code onError}.</li>
 * <li>Ensures that once an {@code onCompleted} or {@code onError} is performed, no further calls can be executed</li>
 * <li>If {@code unsubscribe} is called, the upstream {@code Observable} is notified and the event delivery will be stopped in a
 * best effort manner (i.e., further onXXX calls may still slip through).</li>
 * <li>When {@code onError} or {@code onCompleted} occur, unsubscribes from the {@code Observable} (if executing asynchronously).</li>
 * </ul>
 * {@code SafeSubscriber} will not synchronize {@code onNext} execution. Use {@link SerializedSubscriber} to do
 * that.
 *
 * @param <T>
 *            the type of item expected by the {@link Subscriber}
 */
public class SafeSingleSubscriber<T> extends SingleSubscriber<T> {

    private final SingleSubscriber<? super T> actual;

    public SafeSingleSubscriber(SingleSubscriber<? super T> actual) {
        super(actual);
        this.actual = actual;
    }

    /**
     * Notifies the SingleSubscriber that the {@code Single} has received the success notification
     * <p>
     * The {@code Single} will not call this method if it calls {@link #onError}.
     */
    @Override
    public void onSuccess(T t) {
        try {
            actual.onSuccess(t);
        } catch (Throwable e) {
            // we handle here instead of another method so we don't add stacks to the frame
            // which can prevent it from being able to handle StackOverflow
            Exceptions.throwOrReport(e, this);
        } finally {
            try {
                // Similarly to onError if failure occurs in unsubscribe then Rx contract is broken
                // and we throw an UnsubscribeFailureException.
                unsubscribe();
            } catch (Throwable e) {
                RxJavaHooks.onError(e);
                throw new UnsubscribeFailedException(e.getMessage(), e);
            }
        }
    }

    /**
     * Notifies the Subscriber that the {@code Single} has experienced an error condition.
     * <p>
     * If the {@code Single} calls this method, it will not thereafter call
     * {@link #onSuccess(Object)}.
     *
     * @param e
     *          the exception encountered by the Single
     */
    @Override
    public void onError(Throwable e) {
        // we handle here instead of another method so we don't add stacks to the frame
        // which can prevent it from being able to handle StackOverflow
        Exceptions.throwIfFatal(e);
        RxJavaHooks.onSingleError(e);
        try {
            actual.onError(e);
        } catch (OnErrorNotImplementedException e2) { // NOPMD
            /*
             * onError isn't implemented so throw
             *
             * https://github.com/ReactiveX/RxJava/issues/198
             *
             * Rx Design Guidelines 5.2
             *
             * "when calling the Subscribe method that only has an onNext argument, the OnError behavior
             * will be to rethrow the exception on the thread that the message comes out from the observable
             * sequence. The OnCompleted behavior in this case is to do nothing."
             */
            try {
                unsubscribe();
            } catch (Throwable unsubscribeException) {
                RxJavaHooks.onSingleError(unsubscribeException);
                throw new OnErrorNotImplementedException("Observer.onError not implemented and error while unsubscribing.", new CompositeException(Arrays.asList(e, unsubscribeException))); // NOPMD
            }
            throw e2;
        } catch (Throwable e2) {
            /*
             * throw since the Rx contract is broken if onError failed
             *
             * https://github.com/ReactiveX/RxJava/issues/198
             */
            RxJavaHooks.onSingleError(e2);
            try {
                unsubscribe();
            } catch (Throwable unsubscribeException) {
                RxJavaHooks.onSingleError(unsubscribeException);
                throw new OnErrorFailedException("Error occurred when trying to propagate error to Observer.onError and during unsubscription.", new CompositeException(Arrays.asList(e, e2, unsubscribeException)));
            }

            throw new OnErrorFailedException("Error occurred when trying to propagate error to Observer.onError", new CompositeException(Arrays.asList(e, e2)));
        }
        // if we did not throw above we will unsubscribe here, if onError failed then unsubscribe happens in the catch
        try {
            unsubscribe();
        } catch (Throwable unsubscribeException) {
            RxJavaHooks.onSingleError(unsubscribeException);
            throw new OnErrorFailedException(unsubscribeException);
        }
    }

    /**
     * Returns the {@link SingleSubscriber} underlying this {@code SafeSingleSubscriber}.
     *
     * @return the {@link SingleSubscriber} that was used to create this {@code SafeSingleSubscriber}
     */
    public SingleSubscriber<? super T> getActual() {
        return actual;
    }
}
