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
package rx.subjects;

import rx.Subscriber;
import rx.observers.SerializedObserver;

public class SerializedSubject<T, R> extends Subject<T, R> {
    private final SerializedObserver<T> observer;

    public SerializedSubject(final Subject<T, R> actual) {
        super(new OnSubscribe<R>() {

            @Override
            public void call(Subscriber<? super R> child) {
                actual.unsafeSubscribe(child);
            }

        });
        this.observer = new SerializedObserver<T>(actual);
    }

    @Override
    public void onCompleted() {
        observer.onCompleted();
    }

    @Override
    public void onError(Throwable e) {
        observer.onError(e);
    }

    @Override
    public void onNext(T t) {
        observer.onNext(t);
    }

}
