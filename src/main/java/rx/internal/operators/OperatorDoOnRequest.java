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

import rx.Observable.Operator;
import rx.Producer;
import rx.Subscriber;
import rx.functions.Action1;

/**
 * This operator modifies an {@link rx.Observable} so a given action is invoked when the {@link rx.Observable.Producer} receives a request.
 * 
 * @param <T>
 *            The type of the elements in the {@link rx.Observable} that this operator modifies
 */
public class OperatorDoOnRequest<T> implements Operator<T, T> {

    final Action1<Long> request;

    public OperatorDoOnRequest(Action1<Long> request) {
        this.request = request;
    }

    @Override
    public Subscriber<? super T> call(final Subscriber<? super T> child) {

        final ParentSubscriber<T> parent = new ParentSubscriber<T>(child);

        child.setProducer(new Producer() {

            @Override
            public void request(long n) {
                request.call(n);
                parent.requestMore(n);
            }

        });
        child.add(parent);
        return parent;
    }

    private static final class ParentSubscriber<T> extends Subscriber<T> {
        private final Subscriber<? super T> child;

        ParentSubscriber(Subscriber<? super T> child) {
            this.child = child;
            this.request(0);
        }

        private void requestMore(long n) {
            request(n);
        }

        @Override
        public void onCompleted() {
            child.onCompleted();
        }

        @Override
        public void onError(Throwable e) {
            child.onError(e);
        }

        @Override
        public void onNext(T t) {
            child.onNext(t);
        }
    }
}
