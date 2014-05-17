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
import rx.Observable.OnSubscribe;
import rx.Subscriber;
import rx.Subscription;
import rx.functions.Action1;
import rx.functions.Func0;
import rx.functions.Func1;
import rx.observables.ConnectableObservable;
import rx.observers.SafeSubscriber;
import rx.subjects.Subject;

/**
 * Returns an observable sequence that contains the elements of a sequence
 * produced by multicasting the source sequence within a selector function.
 *
 * @see <a href='http://msdn.microsoft.com/en-us/library/hh229708(v=vs.103).aspx'>MSDN: Observable.Multicast</a>
 *
 * @param <TInput> the input value type
 * @param <TIntermediate> the intermediate type
 * @param <TResult> the result type
 */
public final class OperatorMulticastSelector<TInput, TIntermediate, TResult> implements OnSubscribe<TResult> {
    final Observable<? extends TInput> source;
    final Func0<? extends Subject<? super TInput, ? extends TIntermediate>> subjectFactory;
    final Func1<? super Observable<TIntermediate>, ? extends Observable<TResult>> resultSelector;
    
    public OperatorMulticastSelector(Observable<? extends TInput> source,
            Func0<? extends Subject<? super TInput, ? extends TIntermediate>> subjectFactory,
            Func1<? super Observable<TIntermediate>, ? extends Observable<TResult>> resultSelector) {
        this.source = source;
        this.subjectFactory = subjectFactory;
        this.resultSelector = resultSelector;
    }
    
    @Override
    public void call(Subscriber<? super TResult> child) {
        Observable<TResult> observable;
        ConnectableObservable<TIntermediate> connectable;
        try {
            Subject<? super TInput, ? extends TIntermediate> subject = subjectFactory.call();
            
            connectable = new OperatorMulticast<TInput, TIntermediate>(source, subject);
            
            observable = resultSelector.call(connectable);
        } catch (Throwable t) {
            child.onError(t);
            return;
        }
        
        final SafeSubscriber<TResult> s = new SafeSubscriber<TResult>(child);
        
        observable.unsafeSubscribe(s);
        
        connectable.connect(new Action1<Subscription>() {
            @Override
            public void call(Subscription t1) {
                s.add(t1);
            }
        });
    }
    
}
