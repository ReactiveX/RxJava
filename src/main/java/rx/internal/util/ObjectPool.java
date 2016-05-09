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
 * 
 * Modified from http://www.javacodegeeks.com/2013/08/simple-and-lightweight-pool-implementation.html
 */
package rx.internal.util;

import java.util.Queue;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicReference;

import rx.internal.schedulers.*;
import rx.internal.util.unsafe.*;

public abstract class ObjectPool<T> implements SchedulerLifecycle {
    Queue<T> pool;
    final int minSize;
    final int maxSize;
    private final long validationInterval;

    private final AtomicReference<Future<?>> periodicTask;

    public ObjectPool() {
        this(0, 0, 67);
    }

    /**
     * Creates the pool.
     *
     * @param minIdle
     *            minimum number of objects residing in the pool
     * @param maxIdle
     *            maximum number of objects residing in the pool
     * @param validationInterval
     *            time in seconds for periodical checking of minIdle / maxIdle conditions in a separate thread.
     *            When the number of objects is less than minIdle, missing instances will be created.
     *            When the number of objects is greater than maxIdle, too many instances will be removed.
     */
    private ObjectPool(final int min, final int max, final long validationInterval) {
        this.minSize = min;
        this.maxSize = max;
        this.validationInterval = validationInterval;
        this.periodicTask = new AtomicReference<Future<?>>();
        // initialize pool
        initialize(min);

        start();
    }

    /**
     * Gets the next free object from the pool. If the pool doesn't contain any objects,
     * a new object will be created and given to the caller of this method back.
     *
     * @return T borrowed object
     */
    public T borrowObject() {
        T object;
        if ((object = pool.poll()) == null) {
            object = createObject();
        }

        return object;
    }

    /**
     * Returns object back to the pool.
     *
     * @param object
     *            object to be returned
     */
    public void returnObject(T object) {
        if (object == null) {
            return;
        }

        this.pool.offer(object);
    }

    /**
     * Shutdown this pool.
     */
    @Override
    public void shutdown() {
        Future<?> f = periodicTask.getAndSet(null);
        if (f != null) {
            f.cancel(false);
        }
    }

    @Override
    public void start() {
        for (;;) {
            if (periodicTask.get() != null) {
                return;
            }
            ScheduledExecutorService w = GenericScheduledExecutorService.getInstance();
            
            Future<?> f;
            try {
                f = w.scheduleAtFixedRate(new Runnable() {
        
                    @Override
                    public void run() {
                        int size = pool.size();
                        if (size < minSize) {
                            int sizeToBeAdded = maxSize - size;
                            for (int i = 0; i < sizeToBeAdded; i++) {
                                pool.add(createObject());
                            }
                        } else if (size > maxSize) {
                            int sizeToBeRemoved = size - maxSize;
                            for (int i = 0; i < sizeToBeRemoved; i++) {
                                //                        pool.pollLast();
                                pool.poll();
                            }
                        }
                    }
        
                }, validationInterval, validationInterval, TimeUnit.SECONDS);
            } catch (RejectedExecutionException ex) {
                RxJavaPluginUtils.handleException(ex);
                break;
            }
            if (!periodicTask.compareAndSet(null, f)) {
                f.cancel(false);
            } else {
                break;
            }
        }
    }
    
    /**
     * Creates a new object.
     *
     * @return T new object
     */
    protected abstract T createObject();

    private void initialize(final int min) {
        if (UnsafeAccess.isUnsafeAvailable()) {
            pool = new MpmcArrayQueue<T>(Math.max(maxSize, 1024));
        } else {
            pool = new ConcurrentLinkedQueue<T>();
        }

        for (int i = 0; i < min; i++) {
            pool.add(createObject());
        }
    }
}