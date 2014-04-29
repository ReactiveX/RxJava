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
package rx.math.operators;

import rx.Observable.Operator;
import rx.Subscriber;
import rx.functions.Func1;

/**
 * Compute the average by extracting double values from the source via an
 * extractor function.
 * 
 * @param <T>
 *            the source value type
 */
public final class OperatorAverageDouble<T> implements Operator<Double, T> {
    final Func1<? super T, Double> valueExtractor;

    public OperatorAverageDouble(Func1<? super T, Double> valueExtractor) {
        this.valueExtractor = valueExtractor;
    }

    @Override
    public Subscriber<? super T> call(Subscriber<? super Double> child) {
    	return new AverageObserver(child);
    }

    /** Computes the average. */
    private final class AverageObserver extends Subscriber<T> {
        final Subscriber<? super Double> child;
        double sum;
        int count;

        public AverageObserver(Subscriber<? super Double> subscriber) {
        	super(subscriber);
            this.child = subscriber;
        }

        @Override
        public void onNext(T args) {
            sum += valueExtractor.call(args);
            count++;
        }

        @Override
        public void onError(Throwable e) {
            child.onError(e);
        }

        @Override
        public void onCompleted() {
            if (count > 0) {
                try {
                    child.onNext(sum / count);
                } catch (Throwable t) {
                    child.onError(t);
                    return;
                }
                child.onCompleted();
            } else {
                child.onError(new IllegalArgumentException("Sequence contains no elements"));
            }
        }

    }
}