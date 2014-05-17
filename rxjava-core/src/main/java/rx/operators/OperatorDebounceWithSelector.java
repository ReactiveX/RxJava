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

import rx.Observable;
import rx.Observable.Operator;
import rx.Subscriber;
import rx.functions.Func1;
import rx.observers.SerializedSubscriber;
import rx.operators.OperatorDebounceWithTime.DebounceState;
import rx.subscriptions.SerialSubscription;

/**
 * Delay the emission via another observable if no new source appears in the meantime.
 * 
 * @param <T> the value type of the main sequence
 * @param <U> the value type of the boundary sequence
 */
public final class OperatorDebounceWithSelector<T, U> implements Operator<T, T> {
    final Func1<? super T, ? extends Observable<U>> selector;
    
    public OperatorDebounceWithSelector(Func1<? super T, ? extends Observable<U>> selector) {
        this.selector = selector;
    }
    
    @Override
    public Subscriber<? super T> call(final Subscriber<? super T> child) {
        final SerializedSubscriber<T> s = new SerializedSubscriber<T>(child);
        final SerialSubscription ssub = new SerialSubscription();
        child.add(ssub);
        
        return new Subscriber<T>(child) {
            final DebounceState<T> state = new DebounceState<T>();
            final Subscriber<?> self = this;
            @Override
            public void onNext(T t) {
                Observable<U> debouncer;
                
                try {
                    debouncer = selector.call(t);
                } catch (Throwable e) {
                    onError(e);
                    return;
                }
                
                
                final int index = state.next(t);
                
                Subscriber<U> debounceSubscriber = new Subscriber<U>() {

                    @Override
                    public void onNext(U t) {
                        onCompleted();
                    }

                    @Override
                    public void onError(Throwable e) {
                        self.onError(e);
                    }

                    @Override
                    public void onCompleted() {
                        state.emit(index, s, self);
                        unsubscribe();
                    }
                };
                ssub.set(debounceSubscriber);
                
                debouncer.unsafeSubscribe(debounceSubscriber);
                
            }

            @Override
            public void onError(Throwable e) {
                s.onError(e);
                unsubscribe();
                state.clear();
            }

            @Override
            public void onCompleted() {
                state.emitAndComplete(s, this);
            }
        };
    }
    
}