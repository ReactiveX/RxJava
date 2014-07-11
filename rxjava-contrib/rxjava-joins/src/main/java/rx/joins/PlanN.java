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

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

import rx.Observer;
import rx.functions.Action0;
import rx.functions.Action1;
import rx.functions.ActionN;
import rx.functions.Actions;
import rx.functions.FuncN;

/**
 * Represents an execution plan for join patterns.
 */
public final class PlanN<R> extends Plan0<R> {
    protected final PatternN expression;
    protected final FuncN<R> selector;

    public PlanN(PatternN expression, FuncN<R> selector) {
        this.expression = expression;
        this.selector = selector;
    }

    @Override
    public ActivePlan0 activate(Map<Object, JoinObserver> externalSubscriptions,
            final Observer<R> observer, final Action1<ActivePlan0> deactivate) {
        Action1<Throwable> onError = Actions.onErrorFrom(observer);

        final List<JoinObserver1<? extends Object>> observers = new ArrayList<JoinObserver1<? extends Object>>();
        for (int i = 0; i < expression.size(); i++) {
        	observers.add(createObserver(externalSubscriptions, expression.get(i), onError));
        }
        final AtomicReference<ActivePlanN> self = new AtomicReference<ActivePlanN>();

        ActivePlanN activePlan = new ActivePlanN(observers, new ActionN() {
                    @Override
                    public void call(Object... args) {
                        R result;
                        try {
                            result = selector.call(args);
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
                    	for (JoinObserver1<? extends Object> jo : observers) {
                    		jo.removeActivePlan(self.get());
                    	}
                        deactivate.call(self.get());
                    }
                });

        self.set(activePlan);

    	for (JoinObserver1<? extends Object> jo : observers) {
    		jo.addActivePlan(activePlan);
    	}

        return activePlan;
    }

}
