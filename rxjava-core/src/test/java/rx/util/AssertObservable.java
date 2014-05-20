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
package rx.util;

import rx.Notification;
import rx.Observable;
import rx.functions.Func1;
import rx.functions.Func2;

public class AssertObservable {
    /**
     * Asserts that two Observables are equal. If they are not, an {@link AssertionError} is thrown
     * with the given message. If <code>expecteds</code> and <code>actuals</code> are
     * <code>null</code>, they are considered equal.
     * 
     * @param expected
     *            Observable with expected values.
     * @param actual
     *            Observable with actual values
     */
    public static <T> void assertObservableEqualsBlocking(Observable<T> expected, Observable<T> actual) {
        assertObservableEqualsBlocking(null, expected, actual);
    }

    /**
     * Asserts that two Observables are equal. If they are not, an {@link AssertionError} is thrown
     * with the given message. If <code>expected</code> and <code>actual</code> are
     * <code>null</code>, they are considered equal.
     * 
     * @param message
     *            the identifying message for the {@link AssertionError} (<code>null</code> okay)
     * @param expected
     *            Observable with expected values.
     * @param actual
     *            Observable with actual values
     */
    public static <T> void assertObservableEqualsBlocking(String message, Observable<T> expected, Observable<T> actual) {
        assertObservableEquals(expected, actual).toBlocking().lastOrDefault(null);
    }

    /**
     * Asserts that two {@link Observable}s are equal and returns an empty {@link Observable}. If
     * they are not, an {@link Observable} is returned that calls onError with an {@link AssertionError} when subscribed to. If <code>expected</code> and <code>actual</code>
     * are <code>null</code>, they are considered equal.
     * 
     * @param message
     *            the identifying message for the {@link AssertionError} (<code>null</code> okay)
     * @param expected
     *            Observable with expected values.
     * @param actual
     *            Observable with actual values
     */
    public static <T> Observable<Void> assertObservableEquals(Observable<T> expected, Observable<T> actual) {
        return assertObservableEquals(null, expected, actual);
    }

    /**
     * Asserts that two {@link Observable}s are equal and returns an empty {@link Observable}. If
     * they are not, an {@link Observable} is returned that calls onError with an {@link AssertionError} when subscribed to with the given message. If <code>expected</code>
     * and <code>actual</code> are <code>null</code>, they are considered equal.
     * 
     * @param message
     *            the identifying message for the {@link AssertionError} (<code>null</code> okay)
     * @param expected
     *            Observable with expected values.
     * @param actual
     *            Observable with actual values
     */
    public static <T> Observable<Void> assertObservableEquals(final String message, Observable<T> expected, Observable<T> actual) {
        if (actual == null && expected != null) {
            return Observable.error(new AssertionError((message != null ? message + ": " : "") + "Actual was null and expected was not"));
        }
        if (actual != null && expected == null) {
            return Observable.error(new AssertionError((message != null ? message + ": " : "") + "Expected was null and actual was not"));
        }
        if (actual == null && expected == null) {
            return Observable.empty();
        }

        Func2<? super Notification<T>, ? super Notification<T>, Notification<String>> zipFunction = new Func2<Notification<T>, Notification<T>, Notification<String>>() {
            @Override
            public Notification<String> call(Notification<T> expectedNotfication, Notification<T> actualNotification) {
                if (expectedNotfication.equals(actualNotification)) {
                    StringBuilder message = new StringBuilder();
                    message.append(expectedNotfication.getKind());
                    if (expectedNotfication.hasValue())
                        message.append(" ").append(expectedNotfication.getValue());
                    if (expectedNotfication.hasThrowable())
                        message.append(" ").append(expectedNotfication.getThrowable());
                    return Notification.createOnNext("equals " + message.toString());
                }
                else {
                    StringBuilder error = new StringBuilder();
                    error.append("expected:<").append(expectedNotfication.getKind());
                    if (expectedNotfication.hasValue())
                        error.append(" ").append(expectedNotfication.getValue());
                    if (expectedNotfication.hasThrowable())
                        error.append(" ").append(expectedNotfication.getThrowable());
                    error.append("> but was:<").append(actualNotification.getKind());
                    if (actualNotification.hasValue())
                        error.append(" ").append(actualNotification.getValue());
                    if (actualNotification.hasThrowable())
                        error.append(" ").append(actualNotification.getThrowable());
                    error.append(">");

                    return Notification.createOnError(new AssertionError(error.toString()));
                }
            }
        };

        Func2<Notification<String>, Notification<String>, Notification<String>> accumulator = new Func2<Notification<String>, Notification<String>, Notification<String>>() {
            @Override
            public Notification<String> call(Notification<String> a, Notification<String> b) {
                String message = a.isOnError() ? a.getThrowable().getMessage() : a.getValue();
                boolean fail = a.isOnError();

                message += "\n\t" + (b.isOnError() ? b.getThrowable().getMessage() : b.getValue());
                fail |= b.isOnError();

                if (fail)
                    return Notification.createOnError(new AssertionError(message));
                else
                    return Notification.createOnNext(message);
            }
        };

        Observable<Void> outcomeObservable = Observable.zip(expected.materialize(), actual.materialize(), zipFunction).reduce(accumulator).map(new Func1<Notification<String>, Notification<Void>>() {
            @Override
            public Notification<Void> call(Notification<String> outcome) {
                if (outcome.isOnError()) {
                    String fullMessage = (message != null ? message + ": " : "") + "Observables are different\n\t" + outcome.getThrowable().getMessage();
                    return Notification.createOnError(new AssertionError(fullMessage));
                }
                return Notification.createOnCompleted();
            }
        }).dematerialize();
        return outcomeObservable;
    }
}
