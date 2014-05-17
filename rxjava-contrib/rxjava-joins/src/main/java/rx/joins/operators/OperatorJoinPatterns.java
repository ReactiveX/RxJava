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
package rx.joins.operators;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import rx.Observable;
import rx.Observable.OnSubscribe;
import rx.Observer;
import rx.Subscriber;
import rx.functions.Action1;
import rx.functions.Func1;
import rx.joins.ActivePlan0;
import rx.joins.JoinObserver;
import rx.joins.Pattern1;
import rx.joins.Pattern2;
import rx.joins.Plan0;
import rx.subscriptions.CompositeSubscription;

/**
 * Join patterns: And, Then, When.
 */
public class OperatorJoinPatterns {
    /**
     * Creates a pattern that matches when both observable sequences have an available element.
     */
    public static <T1, T2> Pattern2<T1, T2> and(/* this */Observable<T1> left, Observable<T2> right) {
        if (left == null) {
            throw new NullPointerException("left");
        }
        if (right == null) {
            throw new NullPointerException("right");
        }
        return new Pattern2<T1, T2>(left, right);
    }

    /**
     * Matches when the observable sequence has an available element and projects the element by invoking the selector function.
     */
    public static <T1, R> Plan0<R> then(/* this */Observable<T1> source, Func1<T1, R> selector) {
        if (source == null) {
            throw new NullPointerException("source");
        }
        if (selector == null) {
            throw new NullPointerException("selector");
        }
        return new Pattern1<T1>(source).then(selector);
    }

    /**
     * Joins together the results from several patterns.
     */
    public static <R> OnSubscribe<R> when(Plan0<R>... plans) {
        if (plans == null) {
            throw new NullPointerException("plans");
        }
        return when(Arrays.asList(plans));
    }

    /**
     * Joins together the results from several patterns.
     */
    public static <R> OnSubscribe<R> when(final Iterable<? extends Plan0<R>> plans) {
        if (plans == null) {
            throw new NullPointerException("plans");
        }
        return new OnSubscribe<R>() {
            @Override
            public void call(final Subscriber<? super R> t1) {
                final Map<Object, JoinObserver> externalSubscriptions = new HashMap<Object, JoinObserver>();
                final Object gate = new Object();
                final List<ActivePlan0> activePlans = new ArrayList<ActivePlan0>();

                final Observer<R> out = new Observer<R>() {
                    @Override
                    public void onNext(R args) {
                        t1.onNext(args);
                    }

                    @Override
                    public void onError(Throwable e) {
                        for (JoinObserver po : externalSubscriptions.values()) {
                            po.unsubscribe();
                        }
                        t1.onError(e);
                    }

                    @Override
                    public void onCompleted() {
                        t1.onCompleted();
                    }
                };

                try {
                    for (Plan0<R> plan : plans) {
                        activePlans.add(plan.activate(externalSubscriptions, out, new Action1<ActivePlan0>() {
                            @Override
                            public void call(ActivePlan0 activePlan) {
                                activePlans.remove(activePlan);
                                if (activePlans.isEmpty()) {
                                    out.onCompleted();
                                }
                            }
                        }));
                    }
                } catch (Throwable t) {
                    Observable.<R> error(t).unsafeSubscribe(t1);
                    return;
                }
                CompositeSubscription group = new CompositeSubscription();
                t1.add(group);
                for (JoinObserver jo : externalSubscriptions.values()) {
                    jo.subscribe(gate);
                    group.add(jo);
                }
            }
        };
    }
}
