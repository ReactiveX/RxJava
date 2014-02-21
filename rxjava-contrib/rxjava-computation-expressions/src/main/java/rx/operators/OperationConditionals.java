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

import java.util.Map;

import rx.Observable;
import rx.Observable.OnSubscribeFunc;
import rx.Observer;
import rx.Subscription;
import rx.functions.Func0;
import rx.subscriptions.MultipleAssignmentSubscription;
import rx.subscriptions.Subscriptions;

/**
 * Implementation of conditional-based operations such as Case, If, DoWhile and While.
 */
public final class OperationConditionals {
    /** Utility class. */
    private OperationConditionals() {
        throw new IllegalStateException("No instances!");
    }

    /**
     * Return a subscription function that subscribes to an observable sequence
     * chosen from a map of observables via a selector function or to the
     * default observable.
     * 
     * @param <K>
     *            the case key type
     * @param <R>
     *            the result value type
     * @param caseSelector
     *            the function that produces a case key when an Observer subscribes
     * @param mapOfCases
     *            a map that maps a case key to an observable sequence
     * @param defaultCase
     *            the default observable if the {@code mapOfCases} doesn't contain a value for
     *            the key returned by the {@case caseSelector}
     * @return a subscription function
     */
    public static <K, R> OnSubscribeFunc<R> switchCase(
            Func0<? extends K> caseSelector,
            Map<? super K, ? extends Observable<? extends R>> mapOfCases,
            Observable<? extends R> defaultCase) {
        return new SwitchCase<K, R>(caseSelector, mapOfCases, defaultCase);
    }

    /**
     * Return a subscription function that subscribes to either the
     * then or orElse Observables depending on a condition function.
     * 
     * @param <R>
     *            the result value type
     * @param condition
     *            the condition to decide which Observables to subscribe to
     * @param then
     *            the Observable sequence to subscribe to if {@code condition} is {@code true}
     * @param orElse
     *            the Observable sequence to subscribe to if {@code condition} is {@code false}
     * @return a subscription function
     */
    public static <R> OnSubscribeFunc<R> ifThen(
            Func0<Boolean> condition,
            Observable<? extends R> then,
            Observable<? extends R> orElse) {
        return new IfThen<R>(condition, then, orElse);
    }

    /**
     * Return a subscription function that subscribes to the source Observable,
     * then resubscribes only if the postCondition evaluates to true.
     * 
     * @param <T>
     *            the result value type
     * @param source
     *            the source Observable
     * @param postCondition
     *            the post condition after the source completes
     * @return a subscription function.
     */
    public static <T> OnSubscribeFunc<T> doWhile(Observable<? extends T> source, Func0<Boolean> postCondition) {
        return new WhileDoWhile<T>(source, TRUE, postCondition);
    }

    /**
     * Return a subscription function that subscribes and resubscribes to the source
     * Observable if the preCondition evaluates to true.
     * 
     * @param <T>
     *            the result value type
     * @param source
     *            the source Observable
     * @param preCondition
     *            the condition to evaluate before subscribing to source,
     *            and subscribe to source if it returns {@code true}
     * @return a subscription function.
     */
    public static <T> OnSubscribeFunc<T> whileDo(Observable<? extends T> source, Func0<Boolean> preCondition) {
        return new WhileDoWhile<T>(source, preCondition, preCondition);
    }

    /**
     * Select an observable from a map based on a case key returned by a selector
     * function when an observer subscribes.
     * 
     * @param <K>
     *            the case key type
     * @param <R>
     *            the result value type
     */
    private static final class SwitchCase<K, R> implements OnSubscribeFunc<R> {
        final Func0<? extends K> caseSelector;
        final Map<? super K, ? extends Observable<? extends R>> mapOfCases;
        final Observable<? extends R> defaultCase;

        public SwitchCase(Func0<? extends K> caseSelector,
                Map<? super K, ? extends Observable<? extends R>> mapOfCases,
                Observable<? extends R> defaultCase) {
            this.caseSelector = caseSelector;
            this.mapOfCases = mapOfCases;
            this.defaultCase = defaultCase;
        }

        @Override
        public Subscription onSubscribe(Observer<? super R> t1) {
            Observable<? extends R> target;
            try {
                K caseKey = caseSelector.call();
                if (mapOfCases.containsKey(caseKey)) {
                    target = mapOfCases.get(caseKey);
                } else {
                    target = defaultCase;
                }
            } catch (Throwable t) {
                t1.onError(t);
                return Subscriptions.empty();
            }
            return target.subscribe(t1);
        }
    }

    /** Returns always true. */
    private static final class Func0True implements Func0<Boolean> {
        @Override
        public Boolean call() {
            return true;
        }
    }

    /** Returns always true function. */
    private static final Func0True TRUE = new Func0True();

    /**
     * Given a condition, subscribe to one of the observables when an Observer
     * subscribes.
     * 
     * @param <R>
     *            the result value type
     */
    private static final class IfThen<R> implements OnSubscribeFunc<R> {
        final Func0<Boolean> condition;
        final Observable<? extends R> then;
        final Observable<? extends R> orElse;

        public IfThen(Func0<Boolean> condition, Observable<? extends R> then, Observable<? extends R> orElse) {
            this.condition = condition;
            this.then = then;
            this.orElse = orElse;
        }

        @Override
        public Subscription onSubscribe(Observer<? super R> t1) {
            Observable<? extends R> target;
            try {
                if (condition.call()) {
                    target = then;
                } else {
                    target = orElse;
                }
            } catch (Throwable t) {
                t1.onError(t);
                return Subscriptions.empty();
            }
            return target.subscribe(t1);
        }
    }

    /**
     * Repeatedly subscribes to the source observable if the pre- or
     * postcondition is true.
     * <p>
     * This combines the While and DoWhile into a single operation through
     * the conditions.
     * 
     * @param <T>
     *            the result value type
     */
    private static final class WhileDoWhile<T> implements OnSubscribeFunc<T> {
        final Func0<Boolean> preCondition;
        final Func0<Boolean> postCondition;
        final Observable<? extends T> source;

        public WhileDoWhile(Observable<? extends T> source,
                Func0<Boolean> preCondition, Func0<Boolean> postCondition) {
            this.source = source;
            this.preCondition = preCondition;
            this.postCondition = postCondition;
        }

        @Override
        public Subscription onSubscribe(Observer<? super T> t1) {
            boolean first;
            try {
                first = preCondition.call();
            } catch (Throwable t) {
                t1.onError(t);
                return Subscriptions.empty();
            }
            if (first) {
                MultipleAssignmentSubscription ssub = new MultipleAssignmentSubscription();

                ssub.set(source.subscribe(new SourceObserver(t1, ssub)));

                return ssub;
            } else {
                t1.onCompleted();
            }
            return Subscriptions.empty();
        }

        /** Observe the source. */
        final class SourceObserver implements Observer<T> {
            final MultipleAssignmentSubscription cancel;
            final Observer<? super T> observer;

            public SourceObserver(Observer<? super T> observer, MultipleAssignmentSubscription cancel) {
                this.observer = observer;
                this.cancel = cancel;
            }

            @Override
            public void onNext(T args) {
                observer.onNext(args);
            }

            @Override
            public void onError(Throwable e) {
                observer.onError(e);
                cancel.unsubscribe();
            }

            @Override
            public void onCompleted() {
                boolean next;
                try {
                    next = postCondition.call();
                } catch (Throwable t) {
                    observer.onError(t);
                    return;
                }
                if (next) {
                    cancel.set(source.subscribe(this));
                } else {
                    observer.onCompleted();
                    cancel.unsubscribe();
                }
            }

        }
    }
}
