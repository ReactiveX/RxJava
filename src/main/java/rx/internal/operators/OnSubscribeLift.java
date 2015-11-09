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
import rx.Observable.*;
import rx.exceptions.Exceptions;
import rx.plugins.*;

/**
 * Applies an operator to the incoming child Subscriber and subscribes
 * the resulting Subscriber to a source Observable.
 * <p>
 * By turning the original lift from an anonymous class into a named class,
 * operator optimizations can now look at the graph and discover the
 * operators and sources.
 *
 * @param <T> the source value type
 * @param <R> the result value type;
 */
public final class OnSubscribeLift<T, R> implements OnSubscribe<R> {
    /** The operator. */
    final Operator<? extends R, ? super T> operator;
    /** The upstream. */
    final OnSubscribe<? extends T> source;
    /** The callback hook to transform the operator if necessary. */
    static final RxJavaObservableExecutionHook HOOK = 
            RxJavaPlugins.getInstance().getObservableExecutionHook();
    
    /**
     * Constructs an OnSubscribeLift instance with the given source and operators.
     * <p>
     * The constructor has to take in an OnSubscribe instead of an Observable, unfortunately,
     * because the subscribe/unsafeSubscribe activities would interfere (double onStart, 
     * double wrapping by hooks, etc). 
     * @param source the source OnSubscribe
     * @param operator the operator to apply on the child subscribers to get a Subscriber for source
     */
    public OnSubscribeLift(OnSubscribe<? extends T> source, Operator<? extends R, ? super T> operator) {
        this.operator = operator;
        this.source = source;
    }

    /**
     * Returns the operator instance of this lifting OnSubscribe.
     * @return the operator instance of this lifting OnSubscribe
     */
    public Operator<? extends R, ? super T> operator() {
        return operator;
    }
    
    /**
     * Returns the source OnSubscribe of this OnSubscribe.
     * @return the source OnSubscribe of this OnSubscribe
     */
    public OnSubscribe<? extends T> source() {
        return source;
    }
    
    @Override
    public void call(Subscriber<? super R> child) {
        try {
            Operator<? extends R, ? super T> onLift = HOOK.onLift(operator);
            
            Subscriber<? super T> st = onLift.call(child);
            
            try {
                // new Subscriber created and being subscribed with so 'onStart' it
                st.onStart();
                source.call(st);
            } catch (Throwable e) {
                // localized capture of errors rather than it skipping all operators 
                // and ending up in the try/catch of the subscribe method which then
                // prevents onErrorResumeNext and other similar approaches to error handling
                Exceptions.throwIfFatal(e);
                st.onError(e);
            }
        } catch (Throwable e) {
            Exceptions.throwIfFatal(e);
            // if the lift function failed all we can do is pass the error to the final Subscriber
            // as we don't have the operator available to us
            child.onError(e);
        }
    }
}
