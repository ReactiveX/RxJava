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
import rx.Observable.OnSubscribe;
import rx.exceptions.Exceptions;
import rx.functions.Func1E;
import rx.plugins.RxJavaPlugins;

/**
 * Operator that maps a sequence of values into other values via a custom mapper function
 * which allows throwing an exception as well.
 * 
 * @param <T> the incoming value type
 * @param <R> the outgoing value type
 * @param <E> the exception type
 */
public final class OnSubscribeMapIO<T, R, E extends Exception> implements OnSubscribe<R> {
    final Observable<? extends T> source;
    final Func1E<? super T, ? extends R, E> mapper;
    
    public OnSubscribeMapIO(Observable<? extends T> source, Func1E<? super T, ? extends R, E> mapper) {
        this.source = source;
        this.mapper = mapper;
    }
    
    @Override
    public void call(Subscriber<? super R> t) {
        source.unsafeSubscribe(new MapIOSubscriber<T, R, E>(t, mapper));
    }
    
    /**
     * The mapping subscriber that takes in Ts, returns Rs and potentially throws Es.
     *
     * @param <T> the incoming value type
     * @param <R> the outgoing value type
     * @param <E> the exception type
     */
    static final class MapIOSubscriber<T, R, E extends Exception> extends Subscriber<T> {
        final Subscriber<? super R> actual;
        final Func1E<? super T, ? extends R, E> mapper;

        /** Strong guarantee that stops delivering events when the mapper throws. */
        boolean done;
        
        public MapIOSubscriber(Subscriber<? super R> actual, Func1E<? super T, ? extends R, E> mapper) {
            // sharing SubscriptionList, pass-through for backpressure 
            super(actual);
            this.actual = actual;
            this.mapper = mapper;
        }
        
        @Override
        public void onNext(T t) {
            if (done) {
                return;
            }
            
            R v;
            
            try {
                v = mapper.call(t);
            } catch (Throwable e) {
                Exceptions.throwIfFatal(e);
                onError(e);
                return;
            }
            
            actual.onNext(v);
        }
        
        @Override
        public void onError(Throwable e) {
            if (done) {
                RxJavaPlugins.getInstance().getErrorHandler().handleError(e);
                return;
            }
            done = true;
            actual.onError(e);
        }
        
        @Override
        public void onCompleted() {
            if (done) {
                return;
            }
            done = true;
            actual.onCompleted();
        }
    }
}
