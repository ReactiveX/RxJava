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
import java.util.Collections;
import java.util.List;
import rx.Observable;
import rx.Observable.Operator;
import rx.Observer;
import rx.Subscriber;
import rx.functions.Func0;
import rx.observers.SerializedSubscriber;
import rx.observers.Subscribers;

/**
 * Creates non-overlapping windows of items where each window is terminated by
 * an event from a secondary observable and a new window is started immediately.
 * 
 * @param <T> the value type
 * @param <U> the boundary value type
 */
public final class OperatorWindowWithObservable<T, U> implements Operator<Observable<T>, T> {
    final Func0<? extends Observable<? extends U>> otherFactory;

    public OperatorWindowWithObservable(Func0<? extends Observable<? extends U>> otherFactory) {
        this.otherFactory = otherFactory;
    }
    public OperatorWindowWithObservable(final Observable<U> other) {
        this.otherFactory = new Func0<Observable<U>>() {

            @Override
            public Observable<U> call() {
                return other;
            }
            
        };
    }
    
    @Override
    public Subscriber<? super T> call(Subscriber<? super Observable<T>> child) {
        
        Observable<? extends U> other;
        try {
            other = otherFactory.call();
        } catch (Throwable e) {
            child.onError(e);
            return Subscribers.empty();
        }
        
        SourceSubscriber<T> sub = new SourceSubscriber<T>(child);
        BoundarySubscriber<T, U> bs = new BoundarySubscriber<T, U>(child, sub);
        
        sub.replaceWindow();
        
        other.unsafeSubscribe(bs);
        
        return sub;
    }
    /** Indicate the current subject should complete and a new subject be emitted. */
    static final Object NEXT_SUBJECT = new Object();
    /** For error and completion indication. */
    static final NotificationLite<Object> nl = NotificationLite.instance();
    /** Observes the source. */
    static final class SourceSubscriber<T> extends Subscriber<T> {
        final Subscriber<? super Observable<T>> child;
        final Object guard;
        /** Accessed from the serialized part. */
        Observer<T> consumer;
        /** Accessed from the serialized part. */
        Observable<T> producer;
        /** Guarded by guard. */
        boolean emitting;
        /** Guarded by guard. */
        List<Object> queue;
        
        public SourceSubscriber(Subscriber<? super Observable<T>> child) {
            super(child);
            this.child = new SerializedSubscriber<Observable<T>>(child);
            this.guard = new Object();
        }
        @Override
        public void onNext(T t) {
            List<Object> localQueue;
            synchronized (guard) {
                if (emitting) {
                    if (queue == null) {
                        queue = new ArrayList<Object>();
                    }
                    queue.add(t);
                    return;
                }
                localQueue = queue;
                queue = null;
                emitting = true;
            }
            boolean once = true;
            boolean skipFinal = false;
            try {
                do {
                    drain(localQueue);
                    if (once) {
                        once = true;
                        emitValue(t);
                    }
                    
                    synchronized (guard) {
                        localQueue = queue;
                        queue = null;
                        if (localQueue == null) {
                            emitting = false;
                            skipFinal = true;
                            return;
                        }
                    }
                } while (!child.isUnsubscribed());
            } finally {
                if (!skipFinal) {
                    synchronized (guard) {
                        emitting = false;
                    }
                }
            }
        }

        void drain(List<Object> queue) {
            if (queue == null) {
                return;
            }
            for (Object o : queue) {
                if (o == NEXT_SUBJECT) {
                    replaceSubject();
                } else
                if (nl.isError(o)) {
                    error(nl.getError(o));
                    break;
                } else
                if (nl.isCompleted(o)) {
                    complete();
                    break;
                } else {
                    @SuppressWarnings("unchecked")
                    T t = (T)o;
                    emitValue(t);
                }
            }
        }
        void replaceSubject() {
            Observer<T> s = consumer;
            if (s != null) {
                s.onCompleted();
            }
            createNewWindow();
            child.onNext(producer);
        }
        void createNewWindow() {
            BufferUntilSubscriber<T> bus = BufferUntilSubscriber.create();
            consumer = bus;
            producer = bus;
        }
        void emitValue(T t) {
            Observer<T> s = consumer;
            if (s != null) {
                s.onNext(t);
            }
        }
        
        @Override
        public void onError(Throwable e) {
            synchronized (guard) {
                if (emitting) {
                    queue = Collections.singletonList(nl.error(e));
                    return;
                }
                queue = null;
                emitting = true;
            }
            error(e);
        }

        @Override
        public void onCompleted() {
            List<Object> localQueue;
            synchronized (guard) {
                if (emitting) {
                    if (queue == null) {
                        queue = new ArrayList<Object>();
                    }
                    queue.add(nl.completed());
                    return;
                }
                localQueue = queue;
                queue = null;
                emitting = true;
            }
            try {
                drain(localQueue);
            } catch (Throwable e) {
                error(e);
                return;
            }
            complete();
        }
        void replaceWindow() {
            List<Object> localQueue;
            synchronized (guard) {
                if (emitting) {
                    if (queue == null) {
                        queue = new ArrayList<Object>();
                    }
                    queue.add(NEXT_SUBJECT);
                    return;
                }
                localQueue = queue;
                queue = null;
                emitting = true;
            }
            boolean once = true;
            boolean skipFinal = false;
            try {
                do {
                    drain(localQueue);
                    if (once) {
                        once = false;
                        replaceSubject();
                    }
                    synchronized (guard) {
                        localQueue = queue;
                        queue = null;
                        if (localQueue == null) {
                            emitting = false;
                            skipFinal = true;
                            return;
                        }
                    }
                } while (!child.isUnsubscribed());
            } finally {
                if (!skipFinal) {
                    synchronized (guard) {
                        emitting = false;
                    }
                }
            }
        }
        void complete() {
            Observer<T> s = consumer;
            consumer = null;
            producer = null;
            
            if (s != null) {
                s.onCompleted();
            }
            child.onCompleted();
            unsubscribe();
        }
        void error(Throwable e) {
            Observer<T> s = consumer;
            consumer = null;
            producer = null;
            
            if (s != null) {
                s.onError(e);
            }
            child.onError(e);
            unsubscribe();
        }
    }
    /** Observes the boundary. */
    static final class BoundarySubscriber<T, U> extends Subscriber<U> {
        final SourceSubscriber<T> sub;
        public BoundarySubscriber(Subscriber<?> child, SourceSubscriber<T> sub) {
            super(child);
            this.sub = sub;
        }
        @Override
        public void onNext(U t) {
            sub.replaceWindow();
        }

        @Override
        public void onError(Throwable e) {
            sub.onError(e);
        }

        @Override
        public void onCompleted() {
            sub.onCompleted();
        }
    }
}
