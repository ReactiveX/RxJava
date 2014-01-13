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

import rx.IObservable;
import rx.Observable;
import rx.Observable.OnSubscribeFunc;
import rx.Observer;
import rx.Subscription;

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
public final class OperationSynchronize<T> {

    /**
     * Accepts an observable and wraps it in another observable which ensures that the resulting observable is well-behaved.
     * 
     * A well-behaved observable ensures onNext, onCompleted, or onError calls to its subscribers are
     * not interleaved, onCompleted and onError are only called once respectively, and no
     * onNext calls follow onCompleted and onError calls.
     * 
     * @param observable
     * @param <T>
     * @return the wrapped synchronized observable sequence
     */
    public static <T> OnSubscribeFunc<T> synchronize(IObservable<? extends T> observable) {
        return new Synchronize<T>(observable, null);
    }

    /**
     * Accepts an observable and wraps it in another observable which ensures that the resulting observable is well-behaved.
     * This is accomplished by acquiring a mutual-exclusion lock for the object provided as the lock parameter.
     * 
     * A well-behaved observable ensures onNext, onCompleted, or onError calls to its subscribers are
     * not interleaved, onCompleted and onError are only called once respectively, and no
     * onNext calls follow onCompleted and onError calls.
     * 
     * @param observable
     * @param lock
     *            The lock object to synchronize each observer call on
     * @param <T>
     * @return the wrapped synchronized observable sequence
     */
    public static <T> OnSubscribeFunc<T> synchronize(IObservable<? extends T> observable, Object lock) {
        return new Synchronize<T>(observable, lock);
    }

    private static class Synchronize<T> implements OnSubscribeFunc<T> {

        public Synchronize(IObservable<? extends T> innerObservable, Object lock) {
            this.innerObservable = innerObservable;
            this.lock = lock;
        }

        private IObservable<? extends T> innerObservable;
        private SynchronizedObserver<T> atomicObserver;
        private Object lock;

        @Override
        public Subscription onSubscribe(Observer<? super T> observer) {
            SafeObservableSubscription subscription = new SafeObservableSubscription();
            if (lock == null) {
                atomicObserver = new SynchronizedObserver<T>(observer, subscription);
            }
            else {
                atomicObserver = new SynchronizedObserver<T>(observer, subscription, lock);
            }
            return subscription.wrap(innerObservable.subscribe(atomicObserver));
        }

    }
}
