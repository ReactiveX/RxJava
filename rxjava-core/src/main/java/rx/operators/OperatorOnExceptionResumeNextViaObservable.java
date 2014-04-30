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
package rx.operators;

import rx.Observable;
import rx.Observable.Operator;
import rx.Subscriber;

/**
 * Instruct an Observable to pass control to another Observable rather than invoking
 * <code>onError</code> if it encounters an error of type {@link java.lang.Exception}.
 * <p>
 * This differs from {@link Observable#onErrorResumeNext} in that this one does not handle 
 * {@link java.lang.Throwable} or {@link java.lang.Error} but lets those continue through.
 * <p>
 * <img width="640" src="https://github.com/Netflix/RxJava/wiki/images/rx-operators/onErrorResumeNext.png">
 * <p>
 * By default, when an Observable encounters an error that prevents it from emitting the expected
 * item to its Observer, the Observable invokes its Observer's <code>onError</code> method, and
 * then quits without invoking any more of its Observer's methods. The onErrorResumeNext operation
 * changes this behavior. If you pass an Observable (resumeSequence) to onErrorResumeNext, if the
 * source Observable encounters an error, instead of invoking its Observer's <code>onError</code>
 * method, it will instead relinquish control to this new Observable, which will invoke the
 * Observer's <code>onNext</code> method if it is able to do so. In such a case, because no
 * Observable necessarily invokes <code>onError</code>, the Observer may never know that an error
 * happened.
 * <p>
 * You can use this to prevent errors from propagating or to supply fallback data should errors be
 * encountered.
 * 
 * @param <T> the value type
 */
public final class OperatorOnExceptionResumeNextViaObservable<T> implements Operator<T, T> {
    final Observable<? extends T> resumeSequence;

    public OperatorOnExceptionResumeNextViaObservable(Observable<? extends T> resumeSequence) {
        this.resumeSequence = resumeSequence;
    }

    @Override
    public Subscriber<? super T> call(final Subscriber<? super T> child) {
        // needs to independently unsubscribe so child can continue with the resume
        Subscriber<T> s = new Subscriber<T>() {

            @Override
            public void onNext(T t) {
                child.onNext(t);
            }

            @Override
            public void onError(Throwable e) {
                if (e instanceof Exception) {
                    unsubscribe();
                    resumeSequence.unsafeSubscribe(child);
                } else {
                    child.onError(e);
                }
            }

            @Override
            public void onCompleted() {
                child.onCompleted();
            }
            
        };
        child.add(s);
        
        return s;
    }
    
    
}
