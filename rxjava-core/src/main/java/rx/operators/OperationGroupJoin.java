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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import rx.Observable;
import rx.Observable.OnSubscribeFunc;
import rx.Observer;
import rx.Subscription;
import rx.functions.Func1;
import rx.functions.Func2;
import rx.subjects.PublishSubject;
import rx.subjects.Subject;
import rx.subscriptions.CompositeSubscription;
import rx.subscriptions.RefCountSubscription;
import rx.subscriptions.SerialSubscription;

/**
 * Corrrelates two sequences when they overlap and groups the results.
 * 
 * @see <a href="http://msdn.microsoft.com/en-us/library/hh244235.aspx">MSDN: Observable.GroupJoin</a>
 */
public class OperationGroupJoin<T1, T2, D1, D2, R> implements OnSubscribeFunc<R> {
    protected final Observable<T1> left;
    protected final Observable<T2> right;
    protected final Func1<? super T1, ? extends Observable<D1>> leftDuration;
    protected final Func1<? super T2, ? extends Observable<D2>> rightDuration;
    protected final Func2<? super T1, ? super Observable<T2>, ? extends R> resultSelector;

    public OperationGroupJoin(
            Observable<T1> left,
            Observable<T2> right,
            Func1<? super T1, ? extends Observable<D1>> leftDuration,
            Func1<? super T2, ? extends Observable<D2>> rightDuration,
            Func2<? super T1, ? super Observable<T2>, ? extends R> resultSelector) {
        this.left = left;
        this.right = right;
        this.leftDuration = leftDuration;
        this.rightDuration = rightDuration;
        this.resultSelector = resultSelector;
    }

    @Override
    public Subscription onSubscribe(Observer<? super R> t1) {
        ResultManager ro = new ResultManager(t1);
        ro.init();
        return ro;
    }

    /** Manages sub-observers and subscriptions. */
    class ResultManager implements Subscription {
        final RefCountSubscription cancel;
        final Observer<? super R> observer;
        final CompositeSubscription group;
        final Object guard = new Object();
        int leftIds;
        int rightIds;
        final Map<Integer, Observer<T2>> leftMap = new HashMap<Integer, Observer<T2>>();
        final Map<Integer, T2> rightMap = new HashMap<Integer, T2>();
        boolean leftDone;
        boolean rightDone;

        public ResultManager(Observer<? super R> observer) {
            this.observer = observer;
            this.group = new CompositeSubscription();
            this.cancel = new RefCountSubscription(group);
        }

        public void init() {
            SerialSubscription s1 = new SerialSubscription();
            SerialSubscription s2 = new SerialSubscription();

            group.add(s1);
            group.add(s2);

            s1.setSubscription(left.subscribe(new LeftObserver(s1)));
            s2.setSubscription(right.subscribe(new RightObserver(s2)));

        }

        @Override
        public void unsubscribe() {
            cancel.unsubscribe();
        }
        
        @Override
        public boolean isUnsubscribed() {
            return cancel.isUnsubscribed();
        }

        void groupsOnCompleted() {
            List<Observer<T2>> list = new ArrayList<Observer<T2>>(leftMap.values());
            leftMap.clear();
            rightMap.clear();
            for (Observer<T2> o : list) {
                o.onCompleted();
            }
        }

        /** Observe the left source. */
        class LeftObserver implements Observer<T1> {
            final Subscription tosource;

            public LeftObserver(Subscription tosource) {
                this.tosource = tosource;
            }

            @Override
            public void onNext(T1 args) {
                try {
                    int id;
                    Subject<T2, T2> subj = PublishSubject.create();
                    synchronized (guard) {
                        id = leftIds++;
                        leftMap.put(id, subj);
                    }

                    Observable<T2> window = Observable.create(new WindowObservableFunc<T2>(subj, cancel));

                    Observable<D1> duration = leftDuration.call(args);

                    SerialSubscription sduration = new SerialSubscription();
                    group.add(sduration);
                    sduration.setSubscription(duration.subscribe(new LeftDurationObserver(id, sduration, subj)));

                    R result = resultSelector.call(args, window);

                    synchronized (guard) {
                        observer.onNext(result);
                        for (T2 t2 : rightMap.values()) {
                            subj.onNext(t2);

                        }
                    }
                } catch (Throwable t) {
                    onError(t);
                }
            }

            @Override
            public void onCompleted() {
                synchronized (guard) {
                    leftDone = true;
                    if (rightDone) {
                        groupsOnCompleted();
                        observer.onCompleted();
                        cancel.unsubscribe();
                    }
                }
            }

            @Override
            public void onError(Throwable e) {
                synchronized (guard) {
                    for (Observer<T2> o : leftMap.values()) {
                        o.onError(e);
                    }
                    observer.onError(e);
                    cancel.unsubscribe();
                }
            }

        }

        /** Observe the right source. */
        class RightObserver implements Observer<T2> {
            final Subscription tosource;

            public RightObserver(Subscription tosource) {
                this.tosource = tosource;
            }

            @Override
            public void onNext(T2 args) {
                try {
                    int id;
                    synchronized (guard) {
                        id = rightIds++;
                        rightMap.put(id, args);
                    }
                    Observable<D2> duration = rightDuration.call(args);

                    SerialSubscription sduration = new SerialSubscription();
                    group.add(sduration);
                    sduration.setSubscription(duration.subscribe(new RightDurationObserver(id, sduration)));

                    synchronized (guard) {
                        for (Observer<T2> o : leftMap.values()) {
                            o.onNext(args);
                        }
                    }
                } catch (Throwable t) {
                    onError(t);
                }
            }

            @Override
            public void onCompleted() {
                //                tosource.unsubscribe();
                synchronized (guard) {
                    rightDone = true;
                    if (leftDone) {
                        groupsOnCompleted();
                        observer.onCompleted();
                        cancel.unsubscribe();
                    }
                }
            }

            @Override
            public void onError(Throwable e) {
                synchronized (guard) {
                    for (Observer<T2> o : leftMap.values()) {
                        o.onError(e);
                    }

                    observer.onError(e);
                    cancel.unsubscribe();
                }
            }
        }

        /** Observe left duration and apply termination. */
        class LeftDurationObserver implements Observer<D1> {
            final int id;
            final Subscription sduration;
            final Observer<T2> gr;

            public LeftDurationObserver(int id, Subscription sduration, Observer<T2> gr) {
                this.id = id;
                this.sduration = sduration;
                this.gr = gr;
            }

            @Override
            public void onCompleted() {
                synchronized (guard) {
                    if (leftMap.remove(id) != null) {
                        gr.onCompleted();
                    }
                }
                group.remove(sduration);
            }

            @Override
            public void onError(Throwable e) {
                synchronized (guard) {
                    observer.onError(e);
                }
                cancel.unsubscribe();
            }

            @Override
            public void onNext(D1 args) {
                onCompleted();
            }
        }

        /** Observe right duration and apply termination. */
        class RightDurationObserver implements Observer<D2> {
            final int id;
            final Subscription sduration;

            public RightDurationObserver(int id, Subscription sduration) {
                this.id = id;
                this.sduration = sduration;
            }

            @Override
            public void onCompleted() {
                synchronized (guard) {
                    rightMap.remove(id);
                }
                group.remove(sduration);
            }

            @Override
            public void onError(Throwable e) {
                synchronized (guard) {
                    observer.onError(e);
                }
                cancel.unsubscribe();
            }

            @Override
            public void onNext(D2 args) {
                onCompleted();
            }
        }

    }

    /**
     * The reference-counted window observable.
     * Subscribes to the underlying Observable by using a reference-counted
     * subscription.
     */
    static class WindowObservableFunc<T> implements OnSubscribeFunc<T> {
        final RefCountSubscription refCount;
        final Observable<T> underlying;

        public WindowObservableFunc(Observable<T> underlying, RefCountSubscription refCount) {
            this.refCount = refCount;
            this.underlying = underlying;
        }

        @Override
        public Subscription onSubscribe(Observer<? super T> t1) {
            CompositeSubscription cs = new CompositeSubscription();
            cs.add(refCount.getSubscription());
            WindowObserver wo = new WindowObserver(t1, cs);
            cs.add(underlying.subscribe(wo));
            return cs;
        }

        /** Observe activities on the window. */
        class WindowObserver implements Observer<T> {
            final Observer<? super T> observer;
            final Subscription self;

            public WindowObserver(Observer<? super T> observer, Subscription self) {
                this.observer = observer;
                this.self = self;
            }

            @Override
            public void onNext(T args) {
                observer.onNext(args);
            }

            @Override
            public void onError(Throwable e) {
                observer.onError(e);
                self.unsubscribe();
            }

            @Override
            public void onCompleted() {
                observer.onCompleted();
                self.unsubscribe();
            }
        }
    }
}