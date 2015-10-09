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

import rx.Observable;
import rx.Observable.Operator;
import rx.Producer;
import rx.Subscriber;
import rx.observers.SerializedSubscriber;

/**
 * Returns an Observable that emits an error if any item is emitted by the source and emits items from the supplied
 * alternate {@code Observable}. The errors from source are propagated as-is.
 *
 * @param <T> the source value type
 * @param <R> the result value type
 */
public final class OperatorMergeEmptyWith<T, R> implements Operator<R, T> {

    private final Observable<? extends R> alternate;

    public OperatorMergeEmptyWith(Observable<? extends R> alternate) {
        this.alternate = alternate;
    }

    @Override
    public Subscriber<? super T> call(final Subscriber<? super R> child) {
        final ChildSubscriber wrappedChild = new ChildSubscriber(child);
        final ParentSubscriber parent = new ParentSubscriber(wrappedChild);
        wrappedChild.add(parent);
        alternate.unsafeSubscribe(wrappedChild);
        return parent;
    }

    private final class ParentSubscriber extends Subscriber<T> {

        private final ChildSubscriber child;

        ParentSubscriber(ChildSubscriber child) {
            this.child = child;
        }

        @Override
        public void setProducer(final Producer producer) {
            /*
             * Always request Max from the parent as we never really expect the parent to emit an item, so the
             * actual value does not matter. However, if the parent producer is waiting for a request to emit
             * a terminal event, not requesting the same will cause the merged Observable to never complete.
             */
            producer.request(Long.MAX_VALUE);
        }

        @Override
        public void onCompleted() {
            child.parentCompleted();
        }

        @Override
        public void onError(Throwable e) {
            child.onError(e);
        }

        @Override
        public void onNext(T t) {
            onError(new IllegalStateException("Merge empty with source emitted an item: " + t));
        }
    }

    private final class ChildSubscriber extends Subscriber<R> {

        private final SerializedSubscriber<? super R> delegate;
        private boolean parentCompleted; /*Guarded by this*/
        private boolean childCompleted; /*Guarded by this*/

        ChildSubscriber(Subscriber<? super R> delegate) {
            super(delegate);
            this.delegate = new SerializedSubscriber<R>(delegate);
        }

        @Override
        public void onCompleted() {
            boolean bothCompleted = false;
            synchronized (this) {
                if (parentCompleted) {
                    bothCompleted = true;
                }
                childCompleted = true;
            }

            if (bothCompleted) {
                delegate.onCompleted();
            }
        }

        @Override
        public void onError(Throwable e) {
            delegate.onError(e);
        }

        @Override
        public void onNext(R r) {
            delegate.onNext(r);
        }

        public void parentCompleted() {
            boolean bothCompleted = false;
            synchronized (this) {
                if (childCompleted) {
                    bothCompleted = true;
                }
                parentCompleted = true;
            }

            if (bothCompleted) {
                delegate.onCompleted();
            }
        }
    }
}
