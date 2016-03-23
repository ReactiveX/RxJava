/**
 * Copyright 2016 Netflix, Inc.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in
 * compliance with the License. You may obtain a copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software distributed under the License is
 * distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See
 * the License for the specific language governing permissions and limitations under the License.
 */

package io.reactivex.internal.operators.flowable;

import org.reactivestreams.*;

import io.reactivex.Flowable.Operator;
import io.reactivex.plugins.RxJavaPlugins;

/**
 * Allows lifting operators into a chain of Publishers.
 * 
 * <p>By having a concrete Publisher as lift, operator fusing can now identify
 * both the source and the operation inside it via casting, unlike the lambda version of this.
 * 
 * @param <T> the upstream value type
 * @param <R> the downstream parameter type
 */
public final class PublisherLift<R, T> implements Publisher<R> {
    /** The actual operator. */
    final Operator<? extends R, ? super T> operator;
    /** The source publisher. */
    final Publisher<? extends T> source;
    
    public PublisherLift(Publisher<? extends T> source, Operator<? extends R, ? super T> operator) {
        this.source = source;
        this.operator = operator;
    }
    
    /**
     * Returns the operator of this lift publisher.
     * @return the operator of this lift publisher
     */
    public Operator<? extends R, ? super T> operator() {
        return operator;
    }
    
    /**
     * Returns the source of this lift publisher.
     * @return the source of this lift publisher
     */
    public Publisher<? extends T> source() {
        return source;
    }
    
    @Override
    public void subscribe(Subscriber<? super R> s) {
        try {
            if (s == null) {
                throw new NullPointerException("Operator " + operator + " received a null Subscriber");
            }
            Subscriber<? super T> st = operator.apply(s);

            if (st == null) {
                throw new NullPointerException("Operator " + operator + " returned a null Subscriber");
            }

            st = RxJavaPlugins.onSubscribe(st);

            if (st == null) {
                throw new NullPointerException("Plugin call for operator " + operator + " returned a null Subscriber");
            }

            source.subscribe(st);
        } catch (NullPointerException e) {
            throw e;
        } catch (Throwable e) {
            // TODO throw if fatal?
            // can't call onError because no way to know if a Subscription has been set or not
            // can't call onSubscribe because the call might have set a Subscription already
            RxJavaPlugins.onError(e);
            
            NullPointerException npe = new NullPointerException("Actually not, but can't throw other exceptions due to RS");
            npe.initCause(e);
            throw npe;
        }
    }
}
