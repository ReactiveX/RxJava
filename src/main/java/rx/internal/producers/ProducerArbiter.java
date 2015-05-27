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

import rx.*;

/**
 * Producer that allows changing an underlying producer atomically and correctly resume with the accumulated
 * requests.
 */
public final class ProducerArbiter implements Producer {
    long requested;
    Producer currentProducer;
 
    boolean emitting;
    long missedRequested;
    long missedProduced;
    Producer missedProducer;
     
    static final Producer NULL_PRODUCER = new Producer() {
        @Override
        public void request(long n) {
            
        }
    };
     
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
        boolean skipFinal = false;
        try {
            long r = requested;
            long u = r + n;
            if (u < 0) {
                u = Long.MAX_VALUE;
            }
            requested = u;
             
            Producer p = currentProducer;
            if (p != null) {
                p.request(n);
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
     
    public void produced(long n) {
        if (n <= 0) {
            throw new IllegalArgumentException("n > 0 required");
        }
        synchronized (this) {
            if (emitting) {
                missedProduced += n;
                return;
            }
            emitting = true;
        }
         
        boolean skipFinal = false;
        try {
            long r = requested;
            if (r != Long.MAX_VALUE) {
                long u = r - n;
                if (u < 0) {
                    throw new IllegalStateException("more items arrived than were requested");
                }
                requested = u;
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
     
    public void setProducer(Producer newProducer) {
        synchronized (this) {
            if (emitting) {
                missedProducer = newProducer == null ? NULL_PRODUCER : newProducer;
                return;
            }
            emitting = true;
        }
        boolean skipFinal = false;
        try {
            currentProducer = newProducer;
            if (newProducer != null) {
                newProducer.request(requested);
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
 
    public void emitLoop() {
        for (;;) {
            long localRequested;
            long localProduced;
            Producer localProducer;
            synchronized (this) {
                localRequested = missedRequested;
                localProduced = missedProduced;
                localProducer = missedProducer;
                if (localRequested == 0L 
                        && localProduced == 0L
                        && localProducer == null) {
                    emitting = false;
                    return;
                }
                missedRequested = 0L;
                missedProduced = 0L;
                missedProducer = null;
            }
             
            long r = requested;
             
            if (r != Long.MAX_VALUE) {
                long u = r + localRequested;
                if (u < 0 || u == Long.MAX_VALUE) {
                    r = Long.MAX_VALUE;
                    requested = r;
                } else {
                    long v = u - localProduced;
                    if (v < 0) {
                        throw new IllegalStateException("more produced than requested");
                    }
                    r = v;
                    requested = v;
                }
            }
            if (localProducer != null) {
                if (localProducer == NULL_PRODUCER) {
                    currentProducer = null;
                } else {
                    currentProducer = localProducer;
                    localProducer.request(r);
                }
            } else {
                Producer p = currentProducer;
                if (p != null && localRequested != 0L) {
                    p.request(localRequested);
                }
            }
        }
    }
}