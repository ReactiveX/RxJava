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
package io.reactivex.internal.operators.observable;

import io.reactivex.Observable;
import io.reactivex.Observer;
import io.reactivex.disposables.Disposable;
import java.util.concurrent.atomic.AtomicBoolean;
import org.reactivestreams.Publisher;
import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;

public final class ObservableFromPublisher<T> extends Observable<T> {
    private final Publisher<? extends T> publisher;

    public ObservableFromPublisher(Publisher<? extends T> publisher) {
        this.publisher = publisher;
    }

    @Override
    protected void subscribeActual(final Observer<? super T> o) {
        publisher.subscribe(new PublisherSubscriber<T>(o));
    }

    static final class PublisherSubscriber<T>
    extends AtomicBoolean
    implements Subscriber<T>, Disposable {

        /** */
        private static final long serialVersionUID = -7306579371159152354L;
        
        private final Observer<? super T> o;
        private Subscription inner;

        PublisherSubscriber(Observer<? super T> o) {
            this.o = o;
        }

        @Override
        public void onComplete() {
            o.onComplete();
        }

        @Override
        public void onError(Throwable t) {
            o.onError(t);
        }

        @Override
        public void onNext(T t) {
            o.onNext(t);
        }

        @Override
        public void onSubscribe(Subscription inner) {
            this.inner = inner;
            o.onSubscribe(this);
            inner.request(Long.MAX_VALUE);
        }

        @Override public void dispose() {
            if (compareAndSet(false, true)) {
                inner.cancel();
                inner = null;
            }
        }

        @Override public boolean isDisposed() {
            return get();
        }
    }
}
