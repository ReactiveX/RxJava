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

import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import rx.Observable;
import rx.Observable.Operator;
import rx.Observer;
import rx.Subscriber;
import rx.functions.Func1;
import rx.observers.SerializedObserver;
import rx.observers.SerializedSubscriber;
import rx.subscriptions.CompositeSubscription;

/**
 * Creates potentially overlapping windows of the source items where each window is
 * started by a value emitted by an observable and closed when an associated Observable emits 
 * a value or completes.
 * 
 * @param <T> the value type
 * @param <U> the type of the window opening event
 * @param <V> the type of the window closing event
 */
public final class OperatorWindowWithStartEndObservable<T, U, V> implements Operator<Observable<T>, T> {
    final Observable<? extends U> windowOpenings;
    final Func1<? super U, ? extends Observable<? extends V>> windowClosingSelector;

    public OperatorWindowWithStartEndObservable(Observable<? extends U> windowOpenings, 
            Func1<? super U, ? extends Observable<? extends V>> windowClosingSelector) {
        this.windowOpenings = windowOpenings;
        this.windowClosingSelector = windowClosingSelector;
    }
    
    @Override
    public Subscriber<? super T> call(Subscriber<? super Observable<T>> child) {
        final SourceSubscriber sub = new SourceSubscriber(child);
        
        Subscriber<U> open = new Subscriber<U>(child) {

            @Override
            public void onNext(U t) {
                sub.beginWindow(t);
            }

            @Override
            public void onError(Throwable e) {
                sub.onError(e);
            }

            @Override
            public void onCompleted() {
                sub.onCompleted();
            }
        };
           
        windowOpenings.unsafeSubscribe(open);
        
        return sub;
    }
    /** Serialized access to the subject. */
    static final class SerializedSubject<T> {
        final Observer<T> consumer;
        final Observable<T> producer;

        public SerializedSubject(Observer<T> consumer, Observable<T> producer) {
            this.consumer = new SerializedObserver<T>(consumer);
            this.producer = producer;
        }
        
    }
    final class SourceSubscriber extends Subscriber<T> {
        final Subscriber<? super Observable<T>> child;
        final CompositeSubscription csub;
        final Object guard;
        /** Guarded by guard. */
        final List<SerializedSubject<T>> chunks;
        /** Guarded by guard. */
        boolean done;
        public SourceSubscriber(Subscriber<? super Observable<T>> child) {
            super(child);
            this.child = new SerializedSubscriber<Observable<T>>(child);
            this.guard = new Object();
            this.chunks = new LinkedList<SerializedSubject<T>>();
            this.csub = new CompositeSubscription();
            child.add(csub);
        }
        @Override
        public void onNext(T t) {
            List<SerializedSubject<T>> list;
            synchronized (guard) {
                if (done) {
                    return;
                }
                list = new ArrayList<SerializedSubject<T>>(chunks);
            }
            for (SerializedSubject<T> cs : list) {
                cs.consumer.onNext(t);
            }
        }

        @Override
        public void onError(Throwable e) {
            List<SerializedSubject<T>> list;
            synchronized (guard) {
                if (done) {
                    return;
                }
                done = true;
                list = new ArrayList<SerializedSubject<T>>(chunks);
                chunks.clear();
            }
            for (SerializedSubject<T> cs : list) {
                cs.consumer.onError(e);
            }
            child.onError(e);
        }

        @Override
        public void onCompleted() {
            List<SerializedSubject<T>> list;
            synchronized (guard) {
                if (done) {
                    return;
                }
                done = true;
                list = new ArrayList<SerializedSubject<T>>(chunks);
                chunks.clear();
            }
            for (SerializedSubject<T> cs : list) {
                cs.consumer.onCompleted();
            }
            child.onCompleted();
        }
        
        void beginWindow(U token) {
            final SerializedSubject<T> s = createSerializedSubject();
            synchronized (guard) {
                if (done) {
                    return;
                }
                chunks.add(s);
            }
            child.onNext(s.producer);
            
            Observable<? extends V> end;
            try {
                end = windowClosingSelector.call(token);
            } catch (Throwable e) {
                onError(e);
                return;
            }
            
            Subscriber<V> v = new Subscriber<V>() {
                boolean once = true;
                @Override
                public void onNext(V t) {
                    onCompleted();
                }

                @Override
                public void onError(Throwable e) {
                    
                }

                @Override
                public void onCompleted() {
                    if (once) {
                        once = false;
                        endWindow(s);
                        csub.remove(this);
                    }
                }
                
            };
            csub.add(v);
            
            end.unsafeSubscribe(v);
        }
        void endWindow(SerializedSubject<T> window) {
            boolean terminate = false;
            synchronized (guard) {
                if (done) {
                    return;
                }
                Iterator<SerializedSubject<T>> it = chunks.iterator();
                while (it.hasNext()) {
                    SerializedSubject<T> s = it.next();
                    if (s == window) {
                        terminate = true;
                        it.remove();
                        break;
                    }
                }
            }
            if (terminate) {
                window.consumer.onCompleted();
            }
        }
        SerializedSubject<T> createSerializedSubject() {
            BufferUntilSubscriber<T> bus = BufferUntilSubscriber.create();
            return new SerializedSubject<T>(bus, bus);
        }
    }
}
