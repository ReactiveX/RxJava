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

import java.util.NoSuchElementException;

import rx.Observable.Operator;
import rx.Subscriber;
import rx.internal.producers.SingleProducer;
import rx.internal.util.RxJavaPluginUtils;

/**
 * If the Observable completes after emitting a single item that matches a
 * predicate, return an Observable containing that item. If it emits more than
 * one such item or no item, throw an IllegalArgumentException.
 * @param <T> the value type
 */
public final class OperatorSingle<T> implements Operator<T, T> {

    private final boolean hasDefaultValue;
    private final T defaultValue;

    private static class Holder {
        final static OperatorSingle<?> INSTANCE = new OperatorSingle<Object>();
    }
    
    /**
     * Returns a singleton instance of OperatorSingle (if the stream is empty or has 
     * more than one element an error will be emitted) that is cast to the generic type.
     *  
     * @param <T> the value type
     * @return a singleton instance of an Operator that will emit a single value only unless the stream has zero or more than one element in which case it will emit an error.
     */
    @SuppressWarnings("unchecked")
    public static <T> OperatorSingle<T> instance() {
        return (OperatorSingle<T>) Holder.INSTANCE;
    }

    OperatorSingle() {
        this(false, null);
    }

    public OperatorSingle(T defaultValue) {
        this(true, defaultValue);
    }

    private OperatorSingle(boolean hasDefaultValue, final T defaultValue) {
        this.hasDefaultValue = hasDefaultValue;
        this.defaultValue = defaultValue;
    }

    @Override
    public Subscriber<? super T> call(final Subscriber<? super T> child) {

        final ParentSubscriber<T> parent = new ParentSubscriber<T>(child, hasDefaultValue,
                defaultValue);
        child.add(parent);
        return parent;
    }

    private static final class ParentSubscriber<T> extends Subscriber<T> {
        private final Subscriber<? super T> child;
        private final boolean hasDefaultValue;
        private final T defaultValue;
        
        private T value;
        private boolean isNonEmpty;
        private boolean hasTooManyElements;

        
        ParentSubscriber(Subscriber<? super T> child, boolean hasDefaultValue,
                T defaultValue) {
            this.child = child;
            this.hasDefaultValue = hasDefaultValue;
            this.defaultValue = defaultValue;
            request(2); // could go unbounded, but test expect this
        }

        @Override
        public void onNext(T value) {
            if (hasTooManyElements) {
                return;
            } else
            if (isNonEmpty) {
                hasTooManyElements = true;
                child.onError(new IllegalArgumentException("Sequence contains too many elements"));
                unsubscribe();
            } else {
                this.value = value;
                isNonEmpty = true;
            }
        }

        @Override
        public void onCompleted() {
            if (hasTooManyElements) {
                // We have already sent an onError message
            } else {
                if (isNonEmpty) {
                    child.setProducer(new SingleProducer<T>(child, value));
                } else {
                    if (hasDefaultValue) {
                        child.setProducer(new SingleProducer<T>(child, defaultValue));
                    } else {
                        child.onError(new NoSuchElementException("Sequence contains no elements"));
                    }
                }
            }
        }

        @Override
        public void onError(Throwable e) {
            if (hasTooManyElements) {
                RxJavaPluginUtils.handleException(e);
                return;
            }
            
            child.onError(e);
        }

    }

}
