/**
 * Copyright 2014 Netflix, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package rx.internal.operators;

import java.util.Arrays;

import rx.Observable.Operator;
import rx.Producer;
import rx.Subscriber;
import rx.exceptions.CompositeException;
import rx.exceptions.Exceptions;
import rx.functions.Func1;
import rx.plugins.RxJavaPlugins;

/**
 * Instruct an Observable to emit a particular item to its Observer's <code>onNext</code> method
 * rather than invoking <code>onError</code> if it encounters an error.
 * <p>
 * <img width="640" src="https://github.com/ReactiveX/RxJava/wiki/images/rx-operators/onErrorReturn.png" alt="">
 * <p>
 * By default, when an Observable encounters an error that prevents it from emitting the expected
 * item to its Observer, the Observable invokes its Observer's <code>onError</code> method, and then
 * quits without invoking any more of its Observer's methods. The onErrorReturn operation changes
 * this behavior. If you pass a function (resumeFunction) to onErrorReturn, if the original
 * Observable encounters an error, instead of invoking its Observer's <code>onError</code> method,
 * it will instead pass the return value of resumeFunction to the Observer's <code>onNext</code>
 * method.
 * <p>
 * You can use this to prevent errors from propagating or to supply fallback data should errors be
 * encountered.
 * 
 * @param <T> the value type
 */
public final class OperatorOnErrorReturn<T> implements Operator<T, T> {
    final Func1<Throwable, ? extends T> resultFunction;

    public OperatorOnErrorReturn(Func1<Throwable, ? extends T> resultFunction) {
        this.resultFunction = resultFunction;
    }

    @Override
    public Subscriber<? super T> call(final Subscriber<? super T> child) {
        Subscriber<T> parent = new Subscriber<T>() {

            private boolean done = false;

            @Override
            public void onNext(T t) {
                if (done) {
                    return;
                }
                child.onNext(t);
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
                    T result = resultFunction.call(e);
                    child.onNext(result);
                } catch (Throwable x) {
                    child.onError(new CompositeException(Arrays.asList(e, x)));
                    return;
                }
                child.onCompleted();
            }

            @Override
            public void onCompleted() {
                if (done) {
                    return;
                }
                done = true;
                child.onCompleted();
            }
            
            @Override
            public void setProducer(final Producer producer) {
                child.setProducer(new Producer() {
                    @Override
                    public void request(long n) {
                        producer.request(n);
                    }
                });
            }
            
        };
        child.add(parent);
        return parent;
    }
}
