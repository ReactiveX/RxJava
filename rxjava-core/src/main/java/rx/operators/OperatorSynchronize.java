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
import rx.observers.SynchronizedSubscriber;

/**
 * Wraps an Observable in another Observable that ensures that the resulting Observable is
 * chronologically well-behaved.
 * <p>
 * <img width="640" src="https://github.com/Netflix/RxJava/wiki/images/rx-operators/synchronize.png">
 * <p>
 * A well-behaved Observable does not interleave its invocations of the <code>onNext</code>,
 * <code>onCompleted</code>, and <code>onError</code> methods of its Observers; it invokes
 * <code>onCompleted</code> or <code>onError</code> only once; and it never invokes
 * <code>onNext</code> after invoking either <code>onCompleted</code> or <code>onError</code>. The
 * synchronize operation enforces this, and the Observable it returns invokes <code>onNext</code>
 * and <code>onCompleted</code> or <code>onError</code> synchronously.
 * <p>
 * NOTE: {@link Observable#create} already wraps Observables so this is generally redundant.
 * 
 * @param <T>
 *            The type of the observable sequence.
 */
public final class OperatorSynchronize<T> implements Operator<T, T> {

    final Object lock;

    public OperatorSynchronize(Object lock) {
        this.lock = lock;
    }

    public OperatorSynchronize() {
        this.lock = new Object();
    }

    @Override
    public Subscriber<? super T> call(final Subscriber<? super T> s) {
        return new SynchronizedSubscriber<T>(new Subscriber<T>(s) {

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

        }, lock);
    }

}
