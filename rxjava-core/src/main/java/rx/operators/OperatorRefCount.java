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
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;
import rx.Observable.OnSubscribe;
import rx.Subscriber;
import rx.Subscription;
import rx.functions.Action0;
import rx.observables.ConnectableObservable;
import rx.subscriptions.Subscriptions;

/**
 * Returns an observable sequence that stays connected to the source as long
 * as there is at least one subscription to the observable sequence.
 * @param <T> the value type
 */
public final class OperatorRefCount<T> implements OnSubscribe<T> {
    final ConnectableObservable<? extends T> source;
    final Object guard;
    /** Guarded by guard. */
    int index;
    /** Guarded by guard. */
    boolean emitting;
    /** Guarded by guard. If true, indicates a connection request, false indicates a disconnect request. */
    List<Token> queue;
    /** Manipulated while in the serialized section. */
    int count;
    /** Manipulated while in the serialized section. */
    Subscription connection;
    /** Manipulated while in the serialized section. */
    final Map<Token, Object> connectionStatus;
    /** Occupied indicator. */
    private static final Object OCCUPIED = new Object();
    public OperatorRefCount(ConnectableObservable<? extends T> source) {
        this.source = source;
        this.guard = new Object();
        this.connectionStatus = new WeakHashMap<Token, Object>();
    }

    @Override
    public void call(Subscriber<? super T> t1) {
        int id;
        synchronized (guard) {
            id = ++index;
        }
        final Token t = new Token(id);
        t1.add(Subscriptions.create(new Action0() {
            @Override
            public void call() {
                disconnect(t);
            }
        }));
        source.unsafeSubscribe(t1);
        connect(t);
    }
    private void connect(Token id) {
        List<Token> localQueue;
        synchronized (guard) {
            if (emitting) {
                if (queue == null) {
                    queue = new ArrayList<Token>();
                }
                queue.add(id);
                return;
            }
            
            localQueue = queue;
            queue = null;
            emitting = true;
        }
        boolean once = true;
        do {
            drain(localQueue);
            if (once) {
                once = false;
                doConnect(id);
            }
            synchronized (guard) {
                localQueue = queue;
                queue = null;
                if (localQueue == null) {
                    emitting = false;
                    return;
                }
            }
        } while (true);
    }
    private void disconnect(Token id) {
        List<Token> localQueue;
        synchronized (guard) {
            if (emitting) {
                if (queue == null) {
                    queue = new ArrayList<Token>();
                }
                queue.add(id.toDisconnect()); // negative value indicates disconnect
                return;
            }
            
            localQueue = queue;
            queue = null;
            emitting = true;
        }
        boolean once = true;
        do {
            drain(localQueue);
            if (once) {
                once = false;
                doDisconnect(id);
            }
            synchronized (guard) {
                localQueue = queue;
                queue = null;
                if (localQueue == null) {
                    emitting = false;
                    return;
                }
            }
        } while (true);
    }
    private void drain(List<Token> localQueue) {
        if (localQueue == null) {
            return;
        }
        int n = localQueue.size();
        for (int i = 0; i < n; i++) {
            Token id = localQueue.get(i);
            if (id.isDisconnect()) {
                doDisconnect(id);
            } else {
                doConnect(id);
            }
        }
    }
    private void doConnect(Token id) {
        // this method is called only once per id
        // if add succeeds, id was not yet disconnected
        if (connectionStatus.put(id, OCCUPIED) == null) {
            if (count++ == 0) {
                connection = source.connect();
            }
        } else {
            // connection exists due to disconnect, just remove
            connectionStatus.remove(id);
        }
    }
    private void doDisconnect(Token id) {
        // this method is called only once per id
        // if remove succeeds, id was connected
        if (connectionStatus.remove(id) != null) {
            if (--count == 0) {
                connection.unsubscribe();
                connection = null;
            }
        } else {
            // mark id as if connected
            connectionStatus.put(id, OCCUPIED);
        }
    }
    /** Token that represens a connection request or a disconnection request. */
    private static final class Token {
        final int id;
        public Token(int id) {
            this.id = id;
        }

        @Override
        public boolean equals(Object obj) {
            if (obj == null) {
                return false;
            }
            if (obj.getClass() != getClass()) {
                return false;
            }
            int other = ((Token)obj).id;
            return id == other || -id == other;
        }

        @Override
        public int hashCode() {
            return id < 0 ? -id : id;
        }
        public boolean isDisconnect() {
            return id < 0;
        }
        public Token toDisconnect() {
            if (id < 0) {
                return this;
            }
            return new Token(-id);
        }
    }
}
