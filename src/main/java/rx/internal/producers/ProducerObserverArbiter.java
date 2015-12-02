/**
 * Copyright 2015 Netflix, Inc.
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
package rx.internal.producers;

import java.util.*;

import rx.*;
import rx.Observer;
import rx.exceptions.*;
import rx.internal.operators.BackpressureUtils;

/**
 * Producer that serializes any event emission with requesting and producer changes.
 * <p>
 * The implementation shortcuts on error and overwrites producers that got delayed, similar
 * to ProducerArbiter.
 *
 * @param <T> the value type
 */
public final class ProducerObserverArbiter<T> implements Producer, Observer<T> {
    final Subscriber<? super T> child;
     
    boolean emitting;
     
    List<T> queue;
    
    Producer currentProducer;
    long requested;
    
    long missedRequested;
    Producer missedProducer;
    Object missedTerminal;
    
    volatile boolean hasError;
    
    static final Producer NULL_PRODUCER = new Producer() {
        @Override
        public void request(long n) {
            
        }
    };
     
    public ProducerObserverArbiter(Subscriber<? super T> child) {
        this.child = child;
    }
     
    @Override
    public void onNext(T t) {
        synchronized (this) {
            if (emitting) {
                List<T> q = queue;
                if (q == null) {
                    q = new ArrayList<T>(4);
                    queue = q;
                }
                q.add(t);
                return;
            }
        }
        boolean skipFinal = false;
        try {
            child.onNext(t);
            
            long r = requested;
            if (r != Long.MAX_VALUE) {
                requested = r - 1;
            }
            
            emitLoop();
            skipFinal = true;
        } finally {
            if (!skipFinal) {
                synchronized (this) {
                    emitting = false;
                }
            }
        }
    }
     
    @Override
    public void onError(Throwable e) {
        boolean emit;
        synchronized (this) {
            if (emitting) {
                missedTerminal = e;
                emit = false;
            } else {
                emitting = true;
                emit = true;
            }
        }
        if (emit) {
            child.onError(e);
        } else {
            hasError = true;
        }
    }
    
    @Override
    public void onCompleted() {
        synchronized (this) {
            if (emitting) {
                missedTerminal = true;
                return;
            }
            emitting = true;
        }
        child.onCompleted();
    }
    
    @Override
    public void request(long n) {
        if (n < 0) {
            throw new IllegalArgumentException("n >= 0 required");
        }
        if (n == 0) {
            return;
        }
        synchronized (this) {
            if (emitting) {
                missedRequested += n;
                return;
            }
            emitting = true;
        }
        Producer p = currentProducer;
        boolean skipFinal = false;
        try {
            long r = requested;
            long u = r + n;
            if (u < 0) {
                u = Long.MAX_VALUE;
            }
            requested = u;

            emitLoop();
            skipFinal = true;
        } finally {
            if (!skipFinal) {
                synchronized (this) {
                    emitting = false;
                }
            }
        }
        if (p != null) {
            p.request(n);
        }
    }
    
    public void setProducer(Producer p) {
        synchronized (this) {
            if (emitting) {
                missedProducer = p != null ? p : NULL_PRODUCER;
                return;
            }
            emitting = true;
        }
        boolean skipFinal = false;
        currentProducer = p;
        long r = requested;
        try {
            emitLoop();
            skipFinal = true;
        } finally {
            if (!skipFinal) {
                synchronized (this) {
                    emitting = false;
                }
            }
        }
        if (p != null && r != 0) {
            p.request(r);
        }
    }
    
    void emitLoop() {
        final Subscriber<? super T> c = child;

        long toRequest = 0L;
        Producer requestFrom = null;
        
        outer:
        for (;;) {
            long localRequested;
            Producer localProducer;
            Object localTerminal;
            List<T> q;
            boolean quit = false;
            synchronized (this) {
                localRequested = missedRequested;
                localProducer = missedProducer;
                localTerminal = missedTerminal;
                q = queue;
                if (localRequested == 0L && localProducer == null && q == null
                        && localTerminal == null) {
                    emitting = false;
                    quit = true;
                } else {
                    missedRequested = 0L;
                    missedProducer = null;
                    queue = null;
                    missedTerminal = null;
                }
            }
            if (quit) {
                if (toRequest != 0L && requestFrom != null) {
                    requestFrom.request(toRequest);
                }
                return;
            }
            
            boolean empty = q == null || q.isEmpty();
            if (localTerminal != null) {
                if (localTerminal != Boolean.TRUE) {
                    c.onError((Throwable)localTerminal);
                    return;
                } else
                if (empty) {
                    c.onCompleted();
                    return;
                }
            }
            long e = 0;
            if (q != null) {
                for (T v : q) {
                    if (c.isUnsubscribed()) {
                        return;
                    } else
                    if (hasError) {
                        continue outer; // if an error has been set, shortcut the loop and act on it
                    }
                    try {
                        c.onNext(v);
                    } catch (Throwable ex) {
                        Exceptions.throwOrReport(ex, c, v);
                        return;
                    }
                }
                e += q.size();
            }
            long r = requested;
            // if requested is max, we don't do any accounting
            if (r != Long.MAX_VALUE) {
                // if there were missing requested, add it up
                if (localRequested != 0L) {
                    long u = r + localRequested;
                    if (u < 0) {
                        u = Long.MAX_VALUE;
                    }
                    r = u;
                }
                // if there were emissions and we don't run on max since the last check, subtract
                if (e != 0L && r != Long.MAX_VALUE) {
                    long u = r - e;
                    if (u < 0) {
                        throw new IllegalStateException("More produced than requested");
                    }
                    r = u;
                }
                requested = r;
            }
            if (localProducer != null) {
                if (localProducer == NULL_PRODUCER) {
                    currentProducer = null;
                } else {
                    currentProducer = localProducer;
                    if (r != 0L) {
                        toRequest = BackpressureUtils.addCap(toRequest, r);
                        requestFrom = localProducer;
                    }
                }
            } else {
                Producer p = currentProducer;
                if (p != null && localRequested != 0L) {
                    toRequest = BackpressureUtils.addCap(toRequest, localRequested);
                    requestFrom = p;
                }
            }
        }
    }
}