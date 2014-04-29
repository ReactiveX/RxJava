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
package rx.operators;

import java.util.concurrent.atomic.AtomicBoolean;
import rx.Observable;
import rx.Observable.Operator;
import rx.Subscriber;
import rx.observers.SerializedSubscriber;

/**
 * Skip elements from the source Observable until the secondary
 * observable fires an element.
 * 
 * If the secondary Observable fires no elements, the primary won't fire any elements.
 * 
 * @see <a href='http://msdn.microsoft.com/en-us/library/hh229358.aspx'>MSDN: Observable.SkipUntil</a>
 * 
 * @param <T> the source and result value type
 * @param <U> element type of the signalling observable
 */
public final class OperatorSkipUntil<T, U> implements Operator<T, T> {
    final Observable<U> other;

    public OperatorSkipUntil(Observable<U> other) {
        this.other = other;
    }

    @Override
    public Subscriber<? super T> call(Subscriber<? super T> child) {
        final SerializedSubscriber<T> s = new SerializedSubscriber<T>(child);
        final AtomicBoolean gate = new AtomicBoolean();
        // u needs to unsubscribe from other independently of child
        Subscriber<U> u = new Subscriber<U>() {

            @Override
            public void onNext(U t) {
                gate.set(true);
                unsubscribe();
            }

            @Override
            public void onError(Throwable e) {
                s.onError(e);
                s.unsubscribe();
            }

            @Override
            public void onCompleted() {
                unsubscribe();
            }
        };
        child.add(u);
        other.unsafeSubscribe(u);
        
        return new Subscriber<T>(child) {
            @Override
            public void onNext(T t) {
                if (gate.get()) {
                    s.onNext(t);
                }
            }

            @Override
            public void onError(Throwable e) {
                s.onError(e);
                unsubscribe();
            }

            @Override
            public void onCompleted() {
                s.onCompleted();
                unsubscribe();
            }
        };
    }
}
