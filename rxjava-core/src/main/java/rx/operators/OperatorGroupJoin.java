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
import rx.Observable.OnSubscribe;
import rx.Observer;
import rx.Subscriber;
import rx.Subscription;
import rx.functions.Func1;
import rx.functions.Func2;
import rx.observers.SerializedObserver;
import rx.observers.SerializedSubscriber;
import rx.subjects.PublishSubject;
import rx.subjects.Subject;
import rx.subscriptions.CompositeSubscription;
import rx.subscriptions.RefCountSubscription;

/**
 * Corrrelates two sequences when they overlap and groups the results.
 * 
 * @see <a href="http://msdn.microsoft.com/en-us/library/hh244235.aspx">MSDN: Observable.GroupJoin</a>
 * @param <T1> the left value type
 * @param <T2> the right value type
 * @param <D1> the value type of the left duration
 * @param <D2> the value type of the right duration
 * @param <R> the result value type
 */
public final class OperatorGroupJoin<T1, T2, D1, D2, R> implements OnSubscribe<R> {
    protected final Observable<T1> left;
    protected final Observable<T2> right;
    protected final Func1<? super T1, ? extends Observable<D1>> leftDuration;
    protected final Func1<? super T2, ? extends Observable<D2>> rightDuration;
    protected final Func2<? super T1, ? super Observable<T2>, ? extends R> resultSelector;

    public OperatorGroupJoin(
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
    public void call(Subscriber<? super R> child) {
        ResultManager ro = new ResultManager(new SerializedSubscriber<R>(child));
        child.add(ro);
        ro.init();
    }

    /** Manages sub-observers and subscriptions. */
    final class ResultManager implements Subscription {
        final RefCountSubscription cancel;
        final Subscriber<? super R> subscriber;
        final CompositeSubscription group;
        final Object guard = new Object();
        /** Guarded by guard. */
        int leftIds;
        /** Guarded by guard. */
        int rightIds;
        /** Guarded by guard. */
        final Map<Integer, Observer<T2>> leftMap = new HashMap<Integer, Observer<T2>>();
        /** Guarded by guard. */
        final Map<Integer, T2> rightMap = new HashMap<Integer, T2>();
        /** Guarded by guard. */
        boolean leftDone;
        /** Guarded by guard. */
        boolean rightDone;

        public ResultManager(Subscriber<? super R> subscriber) {
            this.subscriber = subscriber;
            this.group = new CompositeSubscription();
            this.cancel = new RefCountSubscription(group);
        }

        public void init() {

            Subscriber<T1> s1 = new LeftObserver();
            Subscriber<T2> s2 = new RightObserver();
            
            group.add(s1);
            group.add(s2);

            left.unsafeSubscribe(s1);
            right.unsafeSubscribe(s2);
        }

        @Override
        public void unsubscribe() {
            cancel.unsubscribe();
        }
        
        @Override
        public boolean isUnsubscribed() {
            return cancel.isUnsubscribed();
        }
        /**
         * Notify everyone and cleanup.
         * @param e the exception
         */
        void errorAll(Throwable e) {
            List<Observer<T2>> list;
            synchronized (guard) {
                list = new ArrayList<Observer<T2>>(leftMap.values());
                leftMap.clear();
                rightMap.clear();
            }
            for (Observer<T2> o : list) {
                o.onError(e);
            }
            subscriber.onError(e);
            cancel.unsubscribe();
        }
        /**
         * Notify only the main subscriber and cleanup.
         * @param e  the exception
         */
        void errorMain(Throwable e) {
            synchronized (guard) {
                leftMap.clear();
                rightMap.clear();
            }            
            subscriber.onError(e);
            cancel.unsubscribe();
        }
        void complete(List<Observer<T2>> list) {
            if (list != null) {
                for (Observer<T2> o : list) {
                    o.onCompleted();
                }
                subscriber.onCompleted();
                cancel.unsubscribe();
            }
        }
        
        /** Observe the left source. */
        final class LeftObserver extends Subscriber<T1> {
            @Override
            public void onNext(T1 args) {
                try {
                    int id;
                    Subject<T2, T2> subj = PublishSubject.create();
                    Observer<T2> subjSerial = new SerializedObserver<T2>(subj);
                    
                    synchronized (guard) {
                        id = leftIds++;
                        leftMap.put(id, subjSerial);
                    }

                    Observable<T2> window = Observable.create(new WindowObservableFunc<T2>(subj, cancel));

                    Observable<D1> duration = leftDuration.call(args);

                    Subscriber<D1> d1 = new LeftDurationObserver(id);
                    group.add(d1);
                    duration.unsafeSubscribe(d1);

                    R result = resultSelector.call(args, window);

                    List<T2> rightMapValues;
                    synchronized (guard) {
                        rightMapValues = new ArrayList<T2>(rightMap.values());
                    }
                    
                    subscriber.onNext(result);
                    for (T2 t2 : rightMapValues) {
                        subjSerial.onNext(t2);
                    }
                    
                    
                } catch (Throwable t) {
                    onError(t);
                }
            }

            @Override
            public void onCompleted() {
                List<Observer<T2>> list = null;
                synchronized (guard) {
                    leftDone = true;
                    if (rightDone) {
                        list = new ArrayList<Observer<T2>>(leftMap.values());
                        leftMap.clear();
                        rightMap.clear();
                    }
                }
                complete(list);
            }

            @Override
            public void onError(Throwable e) {
                errorAll(e);
            }

        }

        /** Observe the right source. */
        final class RightObserver extends Subscriber<T2> {
            @Override
            public void onNext(T2 args) {
                try {
                    int id;
                    synchronized (guard) {
                        id = rightIds++;
                        rightMap.put(id, args);
                    }
                    Observable<D2> duration = rightDuration.call(args);

                    Subscriber<D2> d2 = new RightDurationObserver(id);
                    
                    group.add(d2);
                    duration.unsafeSubscribe(d2);

                    List<Observer<T2>> list;
                    synchronized (guard) {
                        list = new ArrayList<Observer<T2>>(leftMap.values());
                    }
                    for (Observer<T2> o : list) {
                        o.onNext(args);
                    }
                } catch (Throwable t) {
                    onError(t);
                }
            }

            @Override
            public void onCompleted() {
                List<Observer<T2>> list = null;
                synchronized (guard) {
                    rightDone = true;
                    if (leftDone) {
                        list = new ArrayList<Observer<T2>>(leftMap.values());
                        leftMap.clear();
                        rightMap.clear();
                    }
                }
                complete(list);
            }

            @Override
            public void onError(Throwable e) {
                errorAll(e);
            }
        }

        /** Observe left duration and apply termination. */
        final class LeftDurationObserver extends Subscriber<D1> {
            final int id;
            boolean once = true;

            public LeftDurationObserver(int id) {
                this.id = id;
            }

            @Override
            public void onCompleted() {
                if (once) {
                    once = false;
                    Observer<T2> gr;
                    synchronized (guard) {
                        gr = leftMap.remove(id);
                    }
                    if (gr != null) {
                        gr.onCompleted();
                    }
                    group.remove(this);
                }
            }

            @Override
            public void onError(Throwable e) {
                errorMain(e);
            }

            @Override
            public void onNext(D1 args) {
                onCompleted();
            }
        }

        /** Observe right duration and apply termination. */
        final class RightDurationObserver extends Subscriber<D2> {
            final int id;
            boolean once = true;
            public RightDurationObserver(int id) {
                this.id = id;
            }

            @Override
            public void onCompleted() {
                if (once) {
                    once = false;
                    synchronized (guard) {
                        rightMap.remove(id);
                    }
                    group.remove(this);
                }
            }

            @Override
            public void onError(Throwable e) {
                errorMain(e);
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
    final static class WindowObservableFunc<T> implements OnSubscribe<T> {
        final RefCountSubscription refCount;
        final Observable<T> underlying;

        public WindowObservableFunc(Observable<T> underlying, RefCountSubscription refCount) {
            this.refCount = refCount;
            this.underlying = underlying;
        }

        @Override
        public void call(Subscriber<? super T> t1) {
            Subscription ref = refCount.get();
            WindowSubscriber wo = new WindowSubscriber(t1, ref);
            wo.add(ref);
            
            underlying.unsafeSubscribe(wo);
        }

        /** Observe activities on the window. */
        final class WindowSubscriber extends Subscriber<T> {
            final Subscriber<? super T> subscriber;
            private final Subscription ref;

            public WindowSubscriber(Subscriber<? super T> subscriber, Subscription ref) {
                super(subscriber);
                this.subscriber = subscriber;
                this.ref = ref;
            }

            @Override
            public void onNext(T args) {
                subscriber.onNext(args);
            }

            @Override
            public void onError(Throwable e) {
                subscriber.onError(e);
                ref.unsubscribe();
            }

            @Override
            public void onCompleted() {
                subscriber.onCompleted();
                ref.unsubscribe();
            }
        }
    }
}