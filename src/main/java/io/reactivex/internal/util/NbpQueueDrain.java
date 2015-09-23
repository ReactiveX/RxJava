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

import io.reactivex.NbpObservable.NbpSubscriber;
import io.reactivex.disposables.Disposable;

public interface NbpQueueDrain<T, U> {
    
    boolean cancelled();
    
    boolean done();
    
    Throwable error();
    
    boolean enter();
    
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
     */
    void accept(NbpSubscriber<? super U> a, T v);
    
    default void drainLoop(Queue<T> q, NbpSubscriber<? super U> a, boolean delayError, Disposable dispose) {
        
        int missed = 1;
        
        for (;;) {
            if (checkTerminated(done(), q.isEmpty(), a, delayError, q, dispose)) {
                return;
            }
            
            for (;;) {
                boolean d = done();
                T v = q.poll();
                
                boolean empty = v == null;
                
                if (checkTerminated(d, empty, a, delayError, q, dispose)) {
                    return;
                }
                
                if (empty) {
                    break;
                }

                accept(a, v);
            }
            
            missed = leave(-missed);
            if (missed == 0) {
                break;
            }
        }
    }

    default boolean checkTerminated(boolean d, boolean empty, 
            NbpSubscriber<?> s, boolean delayError, Queue<?> q, Disposable disposable) {
        if (cancelled()) {
            q.clear();
            disposable.dispose();
            return true;
        }
        
        if (d) {
            if (delayError) {
                if (empty) {
                    disposable.dispose();
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
                    disposable.dispose();
                    s.onError(err);
                    return true;
                } else
                if (empty) {
                    disposable.dispose();
                    s.onComplete();
                    return true;
                }
            }
        }
        
        return false;
    }
}
