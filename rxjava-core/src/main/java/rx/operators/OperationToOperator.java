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

import rx.Observable;
import rx.Observable.Operator;
import rx.Subscriber;
import rx.functions.Func1;
import rx.observers.Subscribers;
import rx.subjects.PublishSubject;

/**
 * Converts a function converting one Observable into another into an
 * {@link Operator}.
 * 
 * @param <R>
 *            to type
 * @param <T>
 *            from type
 */
public class OperationToOperator<R, T> implements Operator<R, T> {

    /**
     * Converts a function of an {@link Observable} into another
     * {@link Observable} into an Operator.
     * 
     * @param function
     *            converts an observable into another observable
     * @return the {@link Operator} equivalent of the function
     */
    public static <R, T> Operator<R, T> toOperator(Func1<? super Observable<T>, ? extends Observable<R>> function) {
        return new OperationToOperator<R, T>(function);
    }

    /**
     * The function to present as an {@link Operator}.
     */
    private final Func1<? super Observable<T>, ? extends Observable<R>> function;

    /**
     * Constructor.
     * 
     * @param function
     *            converts an Observable into another Observable.
     */
    public OperationToOperator(Func1<? super Observable<T>, ? extends Observable<R>> function) {
        this.function = function;
    }

    @Override
    public Subscriber<? super T> call(Subscriber<? super R> subscriber) {
        final PublishSubject<T> subject = PublishSubject.create();
        Subscriber<T> result = Subscribers.from(subject);
        subscriber.add(result);
        function.call(subject).subscribe(subscriber);
        return result;
    }

}
