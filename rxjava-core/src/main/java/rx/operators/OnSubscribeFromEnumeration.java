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

import rx.Observable.OnSubscribe;
import rx.Subscriber;
import rx.functions.Enumerable;

import java.util.Enumeration;

/**
 * Converts an Enumeration sequence into an Observable.
 * <p>
 * <img width="640" src="https://github.com/Netflix/RxJava/wiki/images/rx-Observers/toObservable.png">
 * <p>
 * You can convert any object that supports the Enumeration interface into an Observable that emits
 * each item in the object, with the toObservable operation.
 */
public final class OnSubscribeFromEnumeration<T> implements OnSubscribe<T> {

    final Enumerable<? extends T> enumerable;

    public OnSubscribeFromEnumeration(Enumerable<? extends T> enumerable) {
        this.enumerable = enumerable;
    }

    @Override
    public void call(Subscriber<? super T> o) {
        Enumeration<? extends T> enumeration = enumerable.elements();

        while (enumeration.hasMoreElements()) {
            T i = enumeration.nextElement();
            if (o.isUnsubscribed()) {
                return;
            }
            o.onNext(i);
        }
        if (o.isUnsubscribed()) {
            return;
        }
        o.onCompleted();
    }

}
