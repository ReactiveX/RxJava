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

import static rx.Observable.*;
import rx.Notification;
import rx.Observable;
import rx.functions.Func1;
import rx.functions.Func2;
import rx.functions.Functions;

/**
 * Returns an Observable that emits a Boolean value that indicate whether two
 * sequences are equal by comparing the elements pairwise.
 */
public class OperationSequenceEqual {

    public static <T> Observable<Boolean> sequenceEqual(
            Observable<? extends T> first, Observable<? extends T> second,
            final Func2<? super T, ? super T, Boolean> equality) {
        Observable<Notification<T>> firstObservable = concat(
                first.map(new Func1<T, Notification<T>>() {

                    @Override
                    public Notification<T> call(T t1) {
                        return new Notification<T>(t1);
                    }

                }), from(new Notification<T>()));

        Observable<Notification<T>> secondObservable = concat(
                second.map(new Func1<T, Notification<T>>() {

                    @Override
                    public Notification<T> call(T t1) {
                        return new Notification<T>(t1);
                    }

                }), from(new Notification<T>()));

        return zip(firstObservable, secondObservable,
                new Func2<Notification<T>, Notification<T>, Boolean>() {

                    @Override
                    public Boolean call(Notification<T> t1, Notification<T> t2) {
                        if (t1.isOnCompleted() && t2.isOnCompleted()) {
                            return true;
                        }
                        if (t1.isOnCompleted() || t2.isOnCompleted()) {
                            return false;
                        }
                        // Now t1 and t2 must be 'onNext'.
                        return equality.call(t1.getValue(), t2.getValue());
                    }

                }).all(Functions.<Boolean> identity());
    }
}
