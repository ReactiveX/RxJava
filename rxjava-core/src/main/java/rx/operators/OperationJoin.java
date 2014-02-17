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

import java.util.HashMap;
import java.util.Map;

import rx.Observable;
import rx.Observable.OnSubscribeFunc;
import rx.Observer;
import rx.Subscription;
import rx.functions.Func1;
import rx.functions.Func2;
import rx.subscriptions.CompositeSubscription;
import rx.subscriptions.SerialSubscription;

/**
 * Correlates the elements of two sequences based on overlapping durations.
 */
public class OperationJoin<TLeft, TRight, TLeftDuration, TRightDuration, R> implements OnSubscribeFunc<R> {
    final Observable<TLeft> left;
    final Observable<TRight> right;
    final Func1<TLeft, Observable<TLeftDuration>> leftDurationSelector;
    final Func1<TRight, Observable<TRightDuration>> rightDurationSelector;
    final Func2<TLeft, TRight, R> resultSelector;

    public OperationJoin(
            Observable<TLeft> left,
            Observable<TRight> right,
            Func1<TLeft, Observable<TLeftDuration>> leftDurationSelector,
            Func1<TRight, Observable<TRightDuration>> rightDurationSelector,
            Func2<TLeft, TRight, R> resultSelector) {
        this.left = left;
        this.right = right;
        this.leftDurationSelector = leftDurationSelector;
        this.rightDurationSelector = rightDurationSelector;
        this.resultSelector = resultSelector;
    }

    @Override
    public Subscription onSubscribe(Observer<? super R> t1) {
        SerialSubscription cancel = new SerialSubscription();
        ResultSink result = new ResultSink(t1, cancel);
        cancel.setSubscription(result.run());
        return cancel;
    }

    /** Manage the left and right sources. */
    class ResultSink {
        final Object gate = new Object();
        final CompositeSubscription group = new CompositeSubscription();
        boolean leftDone;
        int leftId;
        final Map<Integer, TLeft> leftMap = new HashMap<Integer, TLeft>();
        boolean rightDone;
        int rightId;
        final Map<Integer, TRight> rightMap = new HashMap<Integer, TRight>();
        final Observer<? super R> observer;
        final Subscription cancel;

        public ResultSink(Observer<? super R> observer, Subscription cancel) {
            this.observer = observer;
            this.cancel = cancel;
        }

        public Subscription run() {
            SerialSubscription leftCancel = new SerialSubscription();
            SerialSubscription rightCancel = new SerialSubscription();

            group.add(leftCancel);
            group.add(rightCancel);

            leftCancel.setSubscription(left.subscribe(new LeftObserver(leftCancel)));
            rightCancel.setSubscription(right.subscribe(new RightObserver(rightCancel)));

            return group;
        }

        /** Observes the left values. */
        class LeftObserver implements Observer<TLeft> {
            final Subscription self;

            public LeftObserver(Subscription self) {
                this.self = self;
            }

            protected void expire(int id, Subscription resource) {
                synchronized (gate) {
                    if (leftMap.remove(id) != null && leftMap.isEmpty() && leftDone) {
                        observer.onCompleted();
                        cancel.unsubscribe();
                    }
                }
                group.remove(resource);
            }

            @Override
            public void onNext(TLeft args) {
                int id, highRightId;

                synchronized (gate) {
                    id = leftId++;
                    leftMap.put(id, args);
                    highRightId = rightId;
                }
                SerialSubscription md = new SerialSubscription();
                group.add(md);

                Observable<TLeftDuration> duration;
                try {
                    duration = leftDurationSelector.call(args);
                } catch (Throwable t) {
                    observer.onError(t);
                    cancel.unsubscribe();
                    return;
                }

                md.setSubscription(duration.subscribe(new LeftDurationObserver(id, md)));

                synchronized (gate) {
                    for (Map.Entry<Integer, TRight> entry : rightMap.entrySet()) {
                        if (entry.getKey() < highRightId) {
                            TRight r = entry.getValue();
                            R result;
                            try {
                                result = resultSelector.call(args, r);
                            } catch (Throwable t) {
                                observer.onError(t);
                                cancel.unsubscribe();
                                return;
                            }
                            observer.onNext(result);
                        }
                    }
                }
            }

            @Override
            public void onError(Throwable e) {
                synchronized (gate) {
                    observer.onError(e);
                    cancel.unsubscribe();
                }
            }

            @Override
            public void onCompleted() {
                synchronized (gate) {
                    leftDone = true;
                    if (rightDone || leftMap.isEmpty()) {
                        observer.onCompleted();
                        cancel.unsubscribe();
                    } else {
                        self.unsubscribe();
                    }
                }
            }

            /** Observes the left duration. */
            class LeftDurationObserver implements Observer<TLeftDuration> {
                final int id;
                final Subscription handle;

                public LeftDurationObserver(int id, Subscription handle) {
                    this.id = id;
                    this.handle = handle;
                }

                @Override
                public void onNext(TLeftDuration args) {
                    expire(id, handle);
                }

                @Override
                public void onError(Throwable e) {
                    LeftObserver.this.onError(e);
                }

                @Override
                public void onCompleted() {
                    expire(id, handle);
                }

            }
        }

        /** Observes the right values. */
        class RightObserver implements Observer<TRight> {
            final Subscription self;

            public RightObserver(Subscription self) {
                this.self = self;
            }

            void expire(int id, Subscription resource) {
                synchronized (gate) {
                    if (rightMap.remove(id) != null && rightMap.isEmpty() && rightDone) {
                        observer.onCompleted();
                        cancel.unsubscribe();
                    }
                }
                group.remove(resource);
            }

            @Override
            public void onNext(TRight args) {
                int id = 0, highLeftId;
                synchronized (gate) {
                    id = rightId++;
                    rightMap.put(id, args);
                    highLeftId = leftId;
                }
                SerialSubscription md = new SerialSubscription();
                group.add(md);

                Observable<TRightDuration> duration;
                try {
                    duration = rightDurationSelector.call(args);
                } catch (Throwable t) {
                    observer.onError(t);
                    cancel.unsubscribe();
                    return;
                }

                md.setSubscription(duration.subscribe(new RightDurationObserver(id, md)));

                synchronized (gate) {
                    for (Map.Entry<Integer, TLeft> entry : leftMap.entrySet()) {
                        if (entry.getKey() < highLeftId) {
                            TLeft lv = entry.getValue();
                            R result;
                            try {
                                result = resultSelector.call(lv, args);
                            } catch (Throwable t) {
                                observer.onError(t);
                                cancel.unsubscribe();
                                return;
                            }
                            observer.onNext(result);
                        }
                    }
                }
            }

            @Override
            public void onError(Throwable e) {
                synchronized (gate) {
                    observer.onError(e);
                    cancel.unsubscribe();
                }
            }

            @Override
            public void onCompleted() {
                synchronized (gate) {
                    rightDone = true;
                    if (leftDone || rightMap.isEmpty()) {
                        observer.onCompleted();
                        cancel.unsubscribe();
                    } else {
                        self.unsubscribe();
                    }
                }
            }

            /** Observe the right duration. */
            class RightDurationObserver implements Observer<TRightDuration> {
                final int id;
                final Subscription handle;

                public RightDurationObserver(int id, Subscription handle) {
                    this.id = id;
                    this.handle = handle;
                }

                @Override
                public void onNext(TRightDuration args) {
                    expire(id, handle);
                }

                @Override
                public void onError(Throwable e) {
                    RightObserver.this.onError(e);
                }

                @Override
                public void onCompleted() {
                    expire(id, handle);
                }

            }
        }
    }
}
