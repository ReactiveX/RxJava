/**
 * Copyright 2016 Netflix, Inc.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in
 * compliance with the License. You may obtain a copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software distributed under the License is
 * distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See
 * the License for the specific language governing permissions and limitations under the License.
 */

package io.reactivex.internal.operators.flowable;

import java.util.*;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.*;

import org.reactivestreams.*;

import io.reactivex.disposables.Disposable;
import io.reactivex.internal.queue.SpscLinkedArrayQueue;
import io.reactivex.internal.subscriptions.SubscriptionHelper;
import io.reactivex.internal.util.ExceptionHelper;

public final class BlockingFlowableIterator<T> 
extends AtomicReference<Subscription>
implements Subscriber<T>, Iterator<T>, Runnable, Disposable {

    /** */
    private static final long serialVersionUID = 6695226475494099826L;

    final SpscLinkedArrayQueue<T> queue;
    
    final long batchSize;
    
    final long limit;
    
    final Lock lock;
    
    final Condition condition;
    
    long produced;
    
    volatile boolean done;
    Throwable error;

    volatile boolean cancelled;
    
    public BlockingFlowableIterator(int batchSize) {
        this.queue = new SpscLinkedArrayQueue<T>(batchSize);
        this.batchSize = batchSize;
        this.limit = batchSize - (batchSize >> 2);
        this.lock = new ReentrantLock();
        this.condition = lock.newCondition();
    }

    @Override
    public boolean hasNext() {
        for (;;) {
            if (cancelled) {
                return false;
            }
            boolean d = done;
            boolean empty = queue.isEmpty();
            if (d) {
                Throwable e = error;
                if (e != null) {
                    throw ExceptionHelper.wrapOrThrow(e);
                } else
                if (empty) {
                    return false;
                }
            }
            if (empty) {
                lock.lock();
                try {
                    while (!cancelled && !done && queue.isEmpty()) {
                        condition.await();
                    }
                } catch (InterruptedException ex) {
                    run();
                    throw ExceptionHelper.wrapOrThrow(ex);
                } finally {
                    lock.unlock();
                }
            } else {
                return true;
            }
        }
    }

    @Override
    public T next() {
        if (hasNext()) {
            T v = queue.poll();
            
            if (v == null) {
                run();
                
                throw new IllegalStateException("Queue empty?!");
            }
            
            long p = produced + 1;
            if (p == limit) {
                produced = 0;
                get().request(p);
            } else {
                produced = p;
            }
            
            return v;
        }
        throw new NoSuchElementException();
    }

    @Override
    public void onSubscribe(Subscription s) {
        if (SubscriptionHelper.setOnce(this, s)) {
            s.request(batchSize);
        }
    }

    @Override
    public void onNext(T t) {
        if (!queue.offer(t)) {
            SubscriptionHelper.cancel(this);
            
            onError(new IllegalStateException("Queue full?!"));
        } else {
            signalConsumer();
        }
    }

    @Override
    public void onError(Throwable t) {
        error = t;
        done = true;
        signalConsumer();
    }

    @Override
    public void onComplete() {
        done = true;
        signalConsumer();
    }
    
    void signalConsumer() {
        lock.lock();
        try {
            condition.signalAll();
        } finally {
            lock.unlock();
        }
    }

    @Override
    public void run() {
        SubscriptionHelper.cancel(this);
        signalConsumer();
    }
    
    @Override // otherwise default method which isn't available in Java 7
    public void remove() {
        throw new UnsupportedOperationException("remove");
    }
    
    @Override
    public void dispose() {
        SubscriptionHelper.cancel(this);
    }
    
    @Override
    public boolean isDisposed() {
        return SubscriptionHelper.isCancelled(get());
    }
}