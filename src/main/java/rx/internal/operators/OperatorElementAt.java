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

import java.util.concurrent.atomic.AtomicBoolean;

import rx.Observable.Operator;
import rx.Producer;
import rx.Subscriber;

/**
 * Returns the element at a specified index in a sequence.
 * @param <T> the value type
 */
public final class OperatorElementAt<T> implements Operator<T, T> {

    final int index;
    final boolean hasDefault;
    final T defaultValue;

    public OperatorElementAt(int index) {
        this(index, null, false);
    }

    public OperatorElementAt(int index, T defaultValue) {
        this(index, defaultValue, true);
    }

    private OperatorElementAt(int index, T defaultValue, boolean hasDefault) {
        if (index < 0) {
            throw new IndexOutOfBoundsException(index + " is out of bounds");
        }
        this.index = index;
        this.defaultValue = defaultValue;
        this.hasDefault = hasDefault;
    }

    @Override
    public Subscriber<? super T> call(final Subscriber<? super T> child) {
        Subscriber<T> parent = new Subscriber<T>() {

            private int currentIndex = 0;

            @Override
            public void onNext(T value) {
                if (currentIndex++ == index) {
                    child.onNext(value);
                    child.onCompleted();
                    unsubscribe();
                }
            }

            @Override
            public void onError(Throwable e) {
                child.onError(e);
            }

            @Override
            public void onCompleted() {
                if (currentIndex <= index) {
                    // If "subscriber.onNext(value)" is called, currentIndex must be greater than index
                    if (hasDefault) {
                        child.onNext(defaultValue);
                        child.onCompleted();
                    } else {
                        child.onError(new IndexOutOfBoundsException(index + " is out of bounds"));
                    }
                }
            }
            
            @Override
            public void setProducer(Producer p) {
                child.setProducer(new InnerProducer(p));
            }
        };
        child.add(parent);
        
        return parent;
    }
    /**
     * A producer that wraps another Producer and requests Long.MAX_VALUE
     * when the first positive request() call comes in.
     */
    static class InnerProducer extends AtomicBoolean implements Producer {
        /** */
        private static final long serialVersionUID = 1L;
        
        final Producer actual;
        
        public InnerProducer(Producer actual) {
            this.actual = actual;
        }
        @Override
        public void request(long n) {
            if (n < 0) {
                throw new IllegalArgumentException("n >= 0 required");
            }
            if (n > 0 && compareAndSet(false, true)) {
                // trigger the fast-path since the operator is going
                // to skip all but the indexth element
                actual.request(Long.MAX_VALUE);
            }
        }
    }
}
