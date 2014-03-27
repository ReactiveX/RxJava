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
import rx.Observable.OnSubscribeFunc;
import rx.Observer;
import rx.Subscriber;
import rx.Subscription;
import rx.functions.Func1;
import rx.observables.GroupedObservable;
import rx.subjects.PublishSubject;
import rx.subjects.Subject;
import rx.subscriptions.CompositeSubscription;
import rx.subscriptions.SerialSubscription;

/**
 * Groups the elements of an observable sequence according to a specified key selector, value selector and duration selector function.
 * 
 * @see <a href='http://msdn.microsoft.com/en-us/library/hh211932.aspx'>MSDN: Observable.GroupByUntil</a>
 * @see <a href='http://msdn.microsoft.com/en-us/library/hh229433.aspx'>MSDN: Observable.GroupByUntil</a>
 */
public class OperationGroupByUntil<TSource, TKey, TResult, TDuration> implements OnSubscribeFunc<GroupedObservable<TKey, TResult>> {
    final Observable<TSource> source;
    final Func1<? super TSource, ? extends TKey> keySelector;
    final Func1<? super TSource, ? extends TResult> valueSelector;
    final Func1<? super GroupedObservable<TKey, TResult>, ? extends Observable<? extends TDuration>> durationSelector;

    public OperationGroupByUntil(Observable<TSource> source,
            Func1<? super TSource, ? extends TKey> keySelector,
            Func1<? super TSource, ? extends TResult> valueSelector,
            Func1<? super GroupedObservable<TKey, TResult>, ? extends Observable<? extends TDuration>> durationSelector) {
        this.source = source;
        this.keySelector = keySelector;
        this.valueSelector = valueSelector;
        this.durationSelector = durationSelector;
    }

    @Override
    public Subscription onSubscribe(Observer<? super GroupedObservable<TKey, TResult>> t1) {
        SerialSubscription cancel = new SerialSubscription();
        ResultSink sink = new ResultSink(t1, cancel);
        cancel.set(sink.run());
        return cancel;
    }

    /** The source value sink and group manager. */
    class ResultSink implements Observer<TSource> {
        /** Guarded by gate. */
        protected final Observer<? super GroupedObservable<TKey, TResult>> observer;
        protected final Subscription cancel;
        protected final CompositeSubscription group = new CompositeSubscription();
        protected final Object gate = new Object();
        /** Guarded by gate. */
        protected final Map<TKey, GroupSubject<TKey, TResult>> map = new HashMap<TKey, GroupSubject<TKey, TResult>>();

        public ResultSink(Observer<? super GroupedObservable<TKey, TResult>> observer, Subscription cancel) {
            this.observer = observer;
            this.cancel = cancel;
        }

        /** Prepare the subscription tree. */
        public Subscription run() {
            SerialSubscription toSource = new SerialSubscription();
            group.add(toSource);

            toSource.set(source.subscribe(this));

            return group;
        }

        @Override
        public void onNext(TSource args) {
            TKey key;
            TResult value;
            try {
                key = keySelector.call(args);
                value = valueSelector.call(args);
            } catch (Throwable t) {
                onError(t);
                return;
            }

            GroupSubject<TKey, TResult> g;
            boolean newGroup = false;
            synchronized (gate) {
                g = map.get(key);
                if (g == null) {
                    g = create(key);
                    map.put(key, g);
                    newGroup = true;
                }
            }

            if (newGroup) {
                Observable<? extends TDuration> duration;
                try {
                    duration = durationSelector.call(g.toObservable());
                } catch (Throwable t) {
                    onError(t);
                    return;
                }

                synchronized (gate) {
                    observer.onNext(g.toObservable());
                }

                SerialSubscription durationHandle = new SerialSubscription();
                group.add(durationHandle);

                DurationObserver durationObserver = new DurationObserver(key, durationHandle);
                durationHandle.set(duration.subscribe(durationObserver));

            }

            synchronized (gate) {
                g.onNext(value);
            }
        }

        @Override
        public void onError(Throwable e) {
            synchronized (gate) {
                List<GroupSubject<TKey, TResult>> gs = new ArrayList<GroupSubject<TKey, TResult>>(map.values());
                map.clear();
                for (GroupSubject<TKey, TResult> g : gs) {
                    g.onError(e);
                }
                observer.onError(e);
            }
            cancel.unsubscribe();
        }

        @Override
        public void onCompleted() {
            synchronized (gate) {
                List<GroupSubject<TKey, TResult>> gs = new ArrayList<GroupSubject<TKey, TResult>>(map.values());
                map.clear();
                for (GroupSubject<TKey, TResult> g : gs) {
                    g.onCompleted();
                }
                observer.onCompleted();
            }
            cancel.unsubscribe();
        }

        /** Create a new group. */
        public GroupSubject<TKey, TResult> create(TKey key) {
            PublishSubject<TResult> publish = PublishSubject.create();
            return new GroupSubject<TKey, TResult>(key, publish);
        }

        /** Terminate a group. */
        public void expire(TKey key, Subscription handle) {
            synchronized (gate) {
                GroupSubject<TKey, TResult> g = map.remove(key);
                if (g != null) {
                    g.onCompleted();
                }
            }
            handle.unsubscribe();
        }

        /** Observe the completion of a group. */
        class DurationObserver implements Observer<TDuration> {
            final TKey key;
            final Subscription handle;

            public DurationObserver(TKey key, Subscription handle) {
                this.key = key;
                this.handle = handle;
            }

            @Override
            public void onNext(TDuration args) {
                expire(key, handle);
            }

            @Override
            public void onError(Throwable e) {
                ResultSink.this.onError(e);
            }

            @Override
            public void onCompleted() {
                expire(key, handle);
            }

        }
    }

    /** A grouped observable with subject-like behavior. */
    public static class GroupSubject<K, V> implements Observer<V> {
        protected final Subject<V, V> publish;
        private final K key;

        public GroupSubject(K key, final Subject<V, V> publish) {
            this.key = key;
            this.publish = publish;
        }

        public GroupedObservable<K, V> toObservable() {
            return new GroupedObservable<K, V>(key, new OnSubscribe<V>() {
                @Override
                public void call(Subscriber<? super V> o) {
                    publish.subscribe(o);
                }
            });
        }

        @Override
        public void onNext(V args) {
            publish.onNext(args);
        }

        @Override
        public void onError(Throwable e) {
            publish.onError(e);
        }

        @Override
        public void onCompleted() {
            publish.onCompleted();
        }

    }
}
