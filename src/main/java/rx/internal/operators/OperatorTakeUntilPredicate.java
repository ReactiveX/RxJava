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

/**
 * Returns an Observable that emits items emitted by the source Observable until
 * the provided predicate returns false
 * <p>
 * @param <T> the value type
 */
public final class OperatorTakeUntilPredicate<T> implements Operator<T, T> {
    /** Subscriber returned to the upstream. */
    private final class ParentSubscriber extends Subscriber<T> {
        private final Subscriber<? super T> child;
        private boolean done = false;

        ParentSubscriber(Subscriber<? super T> child) {
            this.child = child;
        }

        @Override
        public void onNext(T t) {
            child.onNext(t);
            
            boolean stop = false;
            try {
                stop = stopPredicate.call(t);
            } catch (Throwable e) {
                done = true;
                Exceptions.throwOrReport(e, child, t);
                unsubscribe();
                return;
            }
            if (stop) {
                done = true;
                child.onCompleted();
                unsubscribe();
            }
        }

        @Override
        public void onCompleted() {
            if (!done) {
                child.onCompleted();
            }
        }

        @Override
        public void onError(Throwable e) {
            if (!done) {
                child.onError(e);
            }
        }
        void downstreamRequest(long n) {
            request(n);
        }
    }

    final Func1<? super T, Boolean> stopPredicate;

    public OperatorTakeUntilPredicate(final Func1<? super T, Boolean> stopPredicate) {
        this.stopPredicate = stopPredicate;
    }

    @Override
    public Subscriber<? super T> call(final Subscriber<? super T> child) {
        final ParentSubscriber parent = new ParentSubscriber(child);
        child.add(parent); // don't unsubscribe downstream
        child.setProducer(new Producer() {
            @Override
            public void request(long n) {
                parent.downstreamRequest(n);
            }
        });
        
        return parent;
    }

}
