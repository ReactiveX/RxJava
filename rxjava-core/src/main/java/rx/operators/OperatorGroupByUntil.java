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
import rx.Observable.Operator;
import rx.Subscriber;
import rx.Subscription;
import rx.functions.Func1;
import rx.observables.GroupedObservable;
import rx.observers.SerializedObserver;
import rx.observers.SerializedSubscriber;
import rx.subjects.PublishSubject;
import rx.subjects.Subject;
import rx.subscriptions.CompositeSubscription;

/**
 * Groups the elements of an observable sequence according to a specified key selector, value selector and duration selector function.
 * 
 * @see <a href='http://msdn.microsoft.com/en-us/library/hh211932.aspx'>MSDN: Observable.GroupByUntil</a>
 * @see <a href='http://msdn.microsoft.com/en-us/library/hh229433.aspx'>MSDN: Observable.GroupByUntil</a>
 * 
 * @param <T> the source value type
 * @param <K> the group key type
 * @param <R> the value type of the groups
 * @param <D> the type of the duration
 */
public class OperatorGroupByUntil<T, K, R, D> implements Operator<GroupedObservable<K, R>, T> {
    final Func1<? super T, ? extends K> keySelector;
    final Func1<? super T, ? extends R> valueSelector;
    final Func1<? super GroupedObservable<K, R>, ? extends Observable<? extends D>> durationSelector;

    public OperatorGroupByUntil(
            Func1<? super T, ? extends K> keySelector,
            Func1<? super T, ? extends R> valueSelector,
            Func1<? super GroupedObservable<K, R>, ? extends Observable<? extends D>> durationSelector) {
        this.keySelector = keySelector;
        this.valueSelector = valueSelector;
        this.durationSelector = durationSelector;
    }

    @Override
    public Subscriber<? super T> call(Subscriber<? super GroupedObservable<K, R>> child) {
        final SerializedSubscriber<GroupedObservable<K, R>> s = new SerializedSubscriber<GroupedObservable<K, R>>(child);
        final CompositeSubscription csub = new CompositeSubscription();
        child.add(csub);
        
        return new Subscriber<T>(child) {
            final Object guard = new Object();
            /** Guarded by guard. */
            Map<K, GroupSubject<K, R>> groups = new HashMap<K, GroupSubject<K, R>>();
            
            final Subscriber<T> self = this;
            @Override
            public void onNext(T t) {
                K key;
                R value;
                try {
                    key = keySelector.call(t);
                    value = valueSelector.call(t);
                } catch (Throwable e) {
                    onError(e);
                    return;
                }
                
                GroupSubject<K, R> gs;
                boolean newGroup = false;
                synchronized (guard) {
                    if (groups == null) {
                        return;
                    }
                    gs = groups.get(key);
                    if (gs == null) {
                        gs = GroupSubject.create(key);
                        groups.put(key, gs);
                        newGroup = true;
                    }
                }
                
                if (newGroup) {
                    final GroupedObservable<K, R> groupObs = gs.toObservable();
                    
                    Observable<? extends D> durationObs;
                    try {
                        durationObs = durationSelector.call(groupObs);
                    } catch (Throwable e) {
                        onError(e);
                        return;
                    }
                    
                    s.onNext(groupObs);
                    
                    final K fKey = key;
                    Subscriber<D> durationSub = new Subscriber<D>() {
                        boolean once = true;
                        @Override
                        public void onNext(D t) {
                            onCompleted();
                        }

                        @Override
                        public void onError(Throwable e) {
                            self.onError(e);
                        }

                        @Override
                        public void onCompleted() {
                            if (once) {
                                once = false;
                                expire(fKey, this);
                            }
                        }
                    };
                    csub.add(durationSub);
                    
                    durationObs.unsafeSubscribe(durationSub);
                }
                
                gs.onNext(value);
            }

            void expire(K key, Subscription subscription) {
                GroupSubject<K, R> g;
                synchronized (guard) {
                    if (groups == null) {
                        return;
                    }
                    g = groups.remove(key);
                }
                if (g != null) {
                    g.onCompleted();
                }
                csub.remove(subscription);
            } 
            
            @Override
            public void onError(Throwable e) {
                List<GroupSubject<K, R>> localGroups;
                synchronized (guard) {
                    if (groups == null) {
                        return;
                    }
                    localGroups = new ArrayList<GroupSubject<K, R>>(groups.values());
                    groups = null;
                }
                for (GroupSubject<K, R> g : localGroups) {
                    g.onError(e);
                }
                s.onError(e);
                unsubscribe();
            }

            @Override
            public void onCompleted() {
                List<GroupSubject<K, R>> localGroups;
                synchronized (guard) {
                    if (groups == null) {
                        return;
                    }
                    localGroups = new ArrayList<GroupSubject<K, R>>(groups.values());
                    groups = null;
                }
                for (GroupSubject<K, R> g : localGroups) {
                    g.onCompleted();
                }
                s.onCompleted();
                unsubscribe();
            }
            
        };
    }

    /** 
     * A grouped observable with subject-like behavior. 
     * @param <K> the key type
     * @param <R> the value type
     */
    public static final class GroupSubject<K, R> extends Subscriber<R> {
        
        static <K, R> GroupSubject<K, R> create(K key) {
            Subject<R, R> publish = BufferUntilSubscriber.create();
            return new GroupSubject<K, R>(key, publish);
        }
        
        final Observable<R> publishObservable;
        final SerializedObserver<R> publishSerial;
        final K key;

        public GroupSubject(K key, final Subject<R, R> publish) {
            this.key = key;
            this.publishObservable = publish;
            this.publishSerial = new SerializedObserver<R>(publish);
        }

        public GroupedObservable<K, R> toObservable() {
            return new GroupedObservable<K, R>(key, new OnSubscribe<R>() {
                @Override
                public void call(Subscriber<? super R> o) {
                    publishObservable.unsafeSubscribe(o);
                }
            });
        }

        @Override
        public void onNext(R args) {
            publishSerial.onNext(args);
        }

        @Override
        public void onError(Throwable e) {
            publishSerial.onError(e);
        }

        @Override
        public void onCompleted() {
            publishSerial.onCompleted();
        }

    }
}
