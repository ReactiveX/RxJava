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
import rx.exceptions.*;
import rx.functions.Func1;
import rx.internal.util.RxJavaPluginUtils;

/**
 * Filters an Observable by discarding any items it emits that do not meet some test.
 * <p>
 * <img width="640" src="https://github.com/ReactiveX/RxJava/wiki/images/rx-operators/filter.png" alt="">
 * @param <T> the value type
 */
public final class OperatorFilter<T> implements Operator<T, T> {

    final Func1<? super T, Boolean> predicate;

    public OperatorFilter(Func1<? super T, Boolean> predicate) {
        this.predicate = predicate;
    }

    @Override
    public Subscriber<? super T> call(final Subscriber<? super T> child) {
        FilterSubscriber<T> parent = new FilterSubscriber<T>(child, predicate);
        child.add(parent);
        return parent;
    }

    static final class FilterSubscriber<T> extends Subscriber<T> {
        
        final Subscriber<? super T> actual;
        
        final Func1<? super T, Boolean> predicate;

        boolean done;
        
        public FilterSubscriber(Subscriber<? super T> actual, Func1<? super T, Boolean> predicate) {
            this.actual = actual;
            this.predicate = predicate;
            request(0);
        }
        
        @Override
        public void onNext(T t) {
            boolean result;
            
            try {
                result = predicate.call(t);
            } catch (Throwable ex) {
                Exceptions.throwIfFatal(ex);
                unsubscribe();
                onError(OnErrorThrowable.addValueAsLastCause(ex, t));
                return;
            }
            
            if (result) {
                actual.onNext(t);
            } else {
                request(1);
            }
        }
        
        @Override
        public void onError(Throwable e) {
            if (done) {
                RxJavaPluginUtils.handleException(e);
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
            actual.onCompleted();
        }
        @Override
        public void setProducer(Producer p) {
            super.setProducer(p);
            actual.setProducer(p);
        }
    }
}
