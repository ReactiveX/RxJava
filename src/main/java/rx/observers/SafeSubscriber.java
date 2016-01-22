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

import rx.Subscriber;
import rx.exceptions.CompositeException;
import rx.exceptions.Exceptions;
import rx.exceptions.OnCompletedFailedException;
import rx.exceptions.OnErrorFailedException;
import rx.exceptions.OnErrorNotImplementedException;
import rx.exceptions.UnsubscribeFailedException;
import rx.internal.util.RxJavaPluginUtils;

/**
 * {@code SafeSubscriber} is a wrapper around {@code Subscriber} that ensures that the {@code Subscriber}
 * complies with <a href="http://reactivex.io/documentation/contract.html">the Observable contract</a>.
 * <p>
 * The following is taken from <a href="http://go.microsoft.com/fwlink/?LinkID=205219">the Rx Design Guidelines
 * document</a>:
 * <blockquote><p>
 * Messages sent to instances of the {@code IObserver} interface follow the following grammar:
 * </p><blockquote><p> {@code OnNext* (OnCompleted | OnError)?} </p></blockquote><p>
 * This grammar allows observable sequences to send any amount (0 or more) of {@code OnNext} messages to the
 * subscriber, optionally followed by a single success ({@code OnCompleted}) or failure ({@code OnError})
 * message.
 * </p><p>
 * The single message indicating that an observable sequence has finished ensures that consumers of the
 * observable sequence can deterministically establish that it is safe to perform cleanup operations.
 * </p><p>
 * A single failure further ensures that abort semantics can be maintained for operators that work on
 * multiple observable sequences (see paragraph 6.6).
 * </p></blockquote>
 * <p>
 * This wrapper does the following:
 * <ul>
 * <li>Allows only single execution of either {@code onError} or {@code onCompleted}.</li>
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
public class SafeSubscriber<T> extends Subscriber<T> {

    private final Subscriber<? super T> actual;

    boolean done = false;

    public SafeSubscriber(Subscriber<? super T> actual) {
        super(actual);
        this.actual = actual;
    }

    /**
     * Notifies the Subscriber that the {@code Observable} has finished sending push-based notifications.
     * <p>
     * The {@code Observable} will not call this method if it calls {@link #onError}.
     */
    @Override
    public void onCompleted() {
        if (!done) {
            done = true;
            try {
                actual.onCompleted();
            } catch (Throwable e) {
                // we handle here instead of another method so we don't add stacks to the frame
                // which can prevent it from being able to handle StackOverflow
                Exceptions.throwIfFatal(e);
                RxJavaPluginUtils.handleException(e);
                throw new OnCompletedFailedException(e.getMessage(), e);
            } finally {
                try {
                    // Similarly to onError if failure occurs in unsubscribe then Rx contract is broken
                    // and we throw an UnsubscribeFailureException.
                    unsubscribe();
                } catch (Throwable e) {
                    RxJavaPluginUtils.handleException(e);
                    throw new UnsubscribeFailedException(e.getMessage(), e);
                }
            }
        }
    }

    /**
     * Notifies the Subscriber that the {@code Observable} has experienced an error condition.
     * <p>
     * If the {@code Observable} calls this method, it will not thereafter call {@link #onNext} or
     * {@link #onCompleted}.
     * 
     * @param e
     *          the exception encountered by the Observable
     */
    @Override
    public void onError(Throwable e) {
        // we handle here instead of another method so we don't add stacks to the frame
        // which can prevent it from being able to handle StackOverflow
        Exceptions.throwIfFatal(e);
        if (!done) {
            done = true;
            _onError(e);
        }
    }

    /**
     * Provides the Subscriber with a new item to observe.
     * <p>
     * The {@code Observable} may call this method 0 or more times.
     * <p>
     * The {@code Observable} will not call this method again after it calls either {@link #onCompleted} or
     * {@link #onError}.
     * 
     * @param args
     *          the item emitted by the Observable
     */
    @Override
    public void onNext(T args) {
        try {
            if (!done) {
                actual.onNext(args);
            }
        } catch (Throwable e) {
            // we handle here instead of another method so we don't add stacks to the frame
            // which can prevent it from being able to handle StackOverflow
            Exceptions.throwOrReport(e, this);
        }
    }

    /**
     * The logic for {@code onError} without the {@code isFinished} check so it can be called from within
     * {@code onCompleted}.
     * 
     * @see <a href="https://github.com/ReactiveX/RxJava/issues/630">the report of this bug</a>
     */
    protected void _onError(Throwable e) {
        RxJavaPluginUtils.handleException(e);
        try {
            actual.onError(e);
        } catch (Throwable e2) {
            if (e2 instanceof OnErrorNotImplementedException) {
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
                    RxJavaPluginUtils.handleException(unsubscribeException);
                    throw new RuntimeException("Observer.onError not implemented and error while unsubscribing.", new CompositeException(Arrays.asList(e, unsubscribeException)));
                }
                throw (OnErrorNotImplementedException) e2;
            } else {
                /*
                 * throw since the Rx contract is broken if onError failed
                 * 
                 * https://github.com/ReactiveX/RxJava/issues/198
                 */
                RxJavaPluginUtils.handleException(e2);
                try {
                    unsubscribe();
                } catch (Throwable unsubscribeException) {
                    RxJavaPluginUtils.handleException(unsubscribeException);
                    throw new OnErrorFailedException("Error occurred when trying to propagate error to Observer.onError and during unsubscription.", new CompositeException(Arrays.asList(e, e2, unsubscribeException)));
                }

                throw new OnErrorFailedException("Error occurred when trying to propagate error to Observer.onError", new CompositeException(Arrays.asList(e, e2)));
            }
        }
        // if we did not throw above we will unsubscribe here, if onError failed then unsubscribe happens in the catch
        try {
            unsubscribe();
        } catch (RuntimeException unsubscribeException) {
            RxJavaPluginUtils.handleException(unsubscribeException);
            throw new OnErrorFailedException(unsubscribeException);
        }
    }

    /**
     * Returns the {@link Subscriber} underlying this {@code SafeSubscriber}.
     *
     * @return the {@link Subscriber} that was used to create this {@code SafeSubscriber}
     */
    public Subscriber<? super T> getActual() {
        return actual;
    }
}
