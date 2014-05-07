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

import rx.Observer;
import rx.Subscriber;

/**
 * Enforce single-threaded, serialized, ordered execution of onNext, onCompleted, onError.
 * <p>
 * When multiple threads are notifying they will be serialized by:
 * <p>
 * <li>Allowing only one thread at a time to emit</li>
 * <li>Adding notifications to a queue if another thread is already emitting</li>
 * <li>Not holding any locks or blocking any threads while emitting</li>
 * <p>
 * 
 * @param <T>
 */
public class SerializedSubscriber<T> extends Subscriber<T> {

    private final Observer<T> s;

    public SerializedSubscriber(Subscriber<? super T> s) {
        super(s);
        this.s = new SerializedObserver<T>(s);
    }

    @Override
    public void onCompleted() {
        s.onCompleted();
    }

    @Override
    public void onError(Throwable e) {
        s.onError(e);
    }

    @Override
    public void onNext(T t) {
        s.onNext(t);
    }
}
