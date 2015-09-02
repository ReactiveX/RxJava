/**
 * Copyright 2015 Netflix, Inc.
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

package io.reactivex.internal.util;

import java.util.Queue;

import org.reactivestreams.Subscriber;

import io.reactivex.disposables.Disposable;

public interface QueueDrain<T, U> {
    
    boolean cancelled();
    
    boolean done();
    
    Throwable error();
    
    boolean enter();
    
    long requested();
    
    long produced(long n);
    
    /**
     * Adds m to the wip counter.
     * @param m
     * @return
     */
    int leave(int m);
    
    /**
     * Accept the value and return true if forwarded.
     * @param a
     * @param v
     * @return
     */
    boolean accept(Subscriber<? super U> a, T v);
    
    default void drainLoop(Queue<T> q, Subscriber<? super U> a, boolean delayError) {
        
        int missed = 1;
        
        for (;;) {
            if (checkTerminated(done(), q.isEmpty(), a, delayError, q)) {
                return;
            }
            
            long r = requested();
            boolean unbounded = r == Long.MAX_VALUE;
            long e = 0L;
            
            while (e != r) {
                boolean d = done();
                T v = q.poll();
                
                boolean empty = v == null;
                
                if (checkTerminated(d, empty, a, delayError, q)) {
                    return;
                }
                
                if (empty) {
                    break;
                }
                
                if (accept(a, v)) {
                    e++;
                }
            }
            
            if (e != 0L && !unbounded) {
                produced(e);
            }
            
            missed = leave(-missed);
            if (missed == 0) {
                break;
            }
        }
    }

    /**
     * Drain the queue but give up with an error if there aren't enough requests.
     * @param q
     * @param a
     * @param delayError
     */
    default void drainMaxLoop(Queue<T> q, Subscriber<? super U> a, boolean delayError, 
            Disposable dispose) {
        int missed = 1;
        
        for (;;) {
            for (;;) {
                boolean d = done();
                T v = q.poll();
                
                boolean empty = v == null;
                
                if (checkTerminated(d, empty, a, delayError, q)) {
                    if (dispose != null) {
                        dispose.dispose();
                    }
                    return;
                }
                
                if (empty) {
                    break;
                }

                long r = requested();
                if (r != 0L) {
                    if (accept(a, v)) {
                        if (r != Long.MAX_VALUE) {
                            r = produced(1);
                        }
                    }
                } else {
                    q.clear();
                    if (dispose != null) {
                        dispose.dispose();
                    }
                    a.onError(new IllegalStateException("Could not emit value due to lack of requests."));
                    return;
                }
            }
            
            missed = leave(-missed);
            if (missed == 0) {
                break;
            }
        }
    }

    default boolean checkTerminated(boolean d, boolean empty, 
            Subscriber<?> s, boolean delayError, Queue<?> q) {
        if (cancelled()) {
            q.clear();
            return true;
        }
        
        if (d) {
            if (delayError) {
                if (empty) {
                    Throwable err = error();
                    if (err != null) {
                        s.onError(err);
                    } else {
                        s.onComplete();
                    }
                    return true;
                }
            } else {
                Throwable err = error();
                if (err != null) {
                    q.clear();
                    s.onError(err);
                    return true;
                } else
                if (empty) {
                    s.onComplete();
                    return true;
                }
            }
        }
        
        return false;
    }
}
