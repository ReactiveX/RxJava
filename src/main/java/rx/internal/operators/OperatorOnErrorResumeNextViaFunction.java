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
package rx.internal.operators;

import rx.*;
import rx.Observable.Operator;
import rx.exceptions.Exceptions;
import rx.functions.Func1;
import rx.internal.producers.ProducerArbiter;
import rx.plugins.RxJavaPlugins;
import rx.subscriptions.SerialSubscription;

/**
 * Instruct an Observable to pass control to another Observable (the return value of a function)
 * rather than invoking {@code onError} if it encounters an error.
 * <p>
 * <img width="640" src="https://github.com/ReactiveX/RxJava/wiki/images/rx-operators/onErrorResumeNext.png" alt="">
 * <p>
 * By default, when an Observable encounters an error that prevents it from emitting the expected item to its
 * Observer, the Observable invokes its Observer's {@code onError} method, and then quits without invoking any
 * more of its Observer's methods. The {@code onErrorResumeNext} operation changes this behavior. If you pass a
 * function that returns an Observable ({@code resumeFunction}) to {@code onErrorResumeNext}, if the source
 * Observable encounters an error, instead of invoking its Observer's {@code onError} method, it will instead
 * relinquish control to this new Observable, which will invoke the Observer's {@code onNext} method if it is
 * able to do so. In such a case, because no Observable necessarily invokes {@code onError}, the Observer may
 * never know that an error happened.
 * <p>
 * You can use this to prevent errors from propagating or to supply fallback data should errors be
 * encountered.
 */
public final class OperatorOnErrorResumeNextViaFunction<T> implements Operator<T, T> {

    private final Func1<Throwable, ? extends Observable<? extends T>> resumeFunction;

    public OperatorOnErrorResumeNextViaFunction(Func1<Throwable, ? extends Observable<? extends T>> f) {
        this.resumeFunction = f;
    }

    @Override
    public Subscriber<? super T> call(final Subscriber<? super T> child) {
        final ProducerArbiter pa = new ProducerArbiter();
        final SerialSubscription ssub = new SerialSubscription();
        Subscriber<T> parent = new Subscriber<T>() {

            private boolean done = false;
            
            @Override
            public void onCompleted() {
                if (done) {
                    return;
                }
                done = true;
                child.onCompleted();
            }

            @Override
            public void onError(Throwable e) {
                if (done) {
                    Exceptions.throwIfFatal(e);
                    return;
                }
                done = true;
                try {
                    RxJavaPlugins.getInstance().getErrorHandler().handleError(e);
                    unsubscribe();
                    Subscriber<T> next = new Subscriber<T>() {
                        @Override
                        public void onNext(T t) {
                            child.onNext(t);
                        }
                        @Override
                        public void onError(Throwable e) {
                            child.onError(e);
                        }
                        @Override
                        public void onCompleted() {
                            child.onCompleted();
                        }
                        @Override
                        public void setProducer(Producer producer) {
                            pa.setProducer(producer);
                        }
                    };
                    ssub.set(next);
                    
                    Observable<? extends T> resume = resumeFunction.call(e);
                    resume.unsafeSubscribe(next);
                } catch (Throwable e2) {
                    child.onError(e2);
                }
            }

            @Override
            public void onNext(T t) {
                if (done) {
                    return;
                }
                child.onNext(t);
            }
            
            @Override
            public void setProducer(final Producer producer) {
                pa.setProducer(producer);
            }

        };
        child.add(ssub);
        ssub.set(parent);
        child.setProducer(pa);
        return parent;
    }

}
