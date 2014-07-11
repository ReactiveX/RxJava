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
package rx.joins;

import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

import rx.Observer;
import rx.functions.Action0;
import rx.functions.Action1;
import rx.functions.Action5;
import rx.functions.Actions;
import rx.functions.Func5;

/**
 * Represents an execution plan for join patterns.
 */
public final class Plan5<T1, T2, T3, T4, T5, R> extends Plan0<R> {
    protected final Pattern5<T1, T2, T3, T4, T5> expression;
    protected final Func5<T1, T2, T3, T4, T5, R> selector;

    public Plan5(Pattern5<T1, T2, T3, T4, T5> expression, Func5<T1, T2, T3, T4, T5, R> selector) {
        this.expression = expression;
        this.selector = selector;
    }

    @Override
    public ActivePlan0 activate(Map<Object, JoinObserver> externalSubscriptions,
            final Observer<R> observer, final Action1<ActivePlan0> deactivate) {
        Action1<Throwable> onError = Actions.onErrorFrom(observer);

        final JoinObserver1<T1> jo1 = createObserver(externalSubscriptions, expression.o1(), onError);
        final JoinObserver1<T2> jo2 = createObserver(externalSubscriptions, expression.o2(), onError);
        final JoinObserver1<T3> jo3 = createObserver(externalSubscriptions, expression.o3(), onError);
        final JoinObserver1<T4> jo4 = createObserver(externalSubscriptions, expression.o4(), onError);
        final JoinObserver1<T5> jo5 = createObserver(externalSubscriptions, expression.o5(), onError);

        final AtomicReference<ActivePlan0> self = new AtomicReference<ActivePlan0>();

        ActivePlan0 activePlan = new ActivePlan5<T1, T2, T3, T4, T5>(
        		jo1, jo2, jo3, jo4, jo5,
                new Action5<T1, T2, T3, T4, T5>() {
                    @Override
                    public void call(T1 t1, T2 t2, T3 t3, T4 t4, T5 t5) {
                        R result;
                        try {
                            result = selector.call(t1, t2, t3, t4, t5);
                        } catch (Throwable t) {
                            observer.onError(t);
                            return;
                        }
                        observer.onNext(result);
                    }
                },
                new Action0() {
                    @Override
                    public void call() {
                    	ActivePlan0 ap = self.get();
                        jo1.removeActivePlan(ap);
                        jo2.removeActivePlan(ap);
                        jo3.removeActivePlan(ap);
                        jo4.removeActivePlan(ap);
                        jo5.removeActivePlan(ap);
                        deactivate.call(ap);
                    }
                });

        self.set(activePlan);

        jo1.addActivePlan(activePlan);
        jo2.addActivePlan(activePlan);
        jo3.addActivePlan(activePlan);
        jo4.addActivePlan(activePlan);
        jo5.addActivePlan(activePlan);

        return activePlan;
    }

}
