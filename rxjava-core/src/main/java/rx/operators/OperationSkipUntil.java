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
package rx.operators;

import java.util.concurrent.atomic.AtomicBoolean;

import rx.Observable;
import rx.Observable.OnSubscribeFunc;
import rx.Observer;
import rx.Subscription;
import rx.subscriptions.CompositeSubscription;
import rx.subscriptions.SerialSubscription;

/**
 * Skip elements from the source Observable until the secondary
 * observable fires an element.
 * 
 * @see <a href='http://msdn.microsoft.com/en-us/library/hh229358.aspx'>MSDN: Observable.SkipUntil</a>
 */
public class OperationSkipUntil<T, U> implements OnSubscribeFunc<T> {
    protected final Observable<T> source;
    protected final Observable<U> other;

    public OperationSkipUntil(Observable<T> source, Observable<U> other) {
        this.source = source;
        this.other = other;
    }

    @Override
    public Subscription onSubscribe(Observer<? super T> t1) {
        return new ResultManager(t1).init();
    }

    /** Manage the source and other observers. */
    private class ResultManager implements Subscription, Observer<T> {
        final Observer<? super T> observer;
        final CompositeSubscription cancel;
        final Object guard = new Object();
        final AtomicBoolean running = new AtomicBoolean();

        public ResultManager(Observer<? super T> observer) {
            this.observer = observer;
            this.cancel = new CompositeSubscription();
        }

        public ResultManager init() {

            SerialSubscription toSource = new SerialSubscription();
            SerialSubscription toOther = new SerialSubscription();

            cancel.add(toSource);
            cancel.add(toOther);

            toSource.setSubscription(source.subscribe(this));
            toOther.setSubscription(other.subscribe(new OtherObserver(toOther)));

            return this;
        }

        @Override
        public void unsubscribe() {
            cancel.unsubscribe();
        }
        
        @Override
        public boolean isUnsubscribed() {
            return cancel.isUnsubscribed();
        }

        @Override
        public void onNext(T args) {
            if (running.get()) {
                observer.onNext(args);
            }
        }

        @Override
        public void onError(Throwable e) {
            synchronized (guard) {
                observer.onError(e);
                unsubscribe();
            }
        }

        @Override
        public void onCompleted() {
            synchronized (guard) {
                observer.onCompleted();
                unsubscribe();
            }
        }

        /** Observe the other stream. */
        private class OtherObserver implements Observer<U> {
            final Subscription self;

            public OtherObserver(Subscription self) {
                this.self = self;
            }

            @Override
            public void onNext(U args) {
                running.set(true);
                self.unsubscribe();
            }

            @Override
            public void onError(Throwable e) {
                ResultManager.this.onError(e);
            }

            @Override
            public void onCompleted() {
                self.unsubscribe();
            }

        }

    }
}
