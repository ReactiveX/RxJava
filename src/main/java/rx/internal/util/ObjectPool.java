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

import java.lang.ref.Reference;
import java.lang.ref.ReferenceQueue;
import java.lang.ref.WeakReference;
import java.util.Queue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.TimeUnit;

import rx.Scheduler;
import rx.functions.Action0;
import rx.internal.util.unsafe.MpmcArrayQueue;
import rx.internal.util.unsafe.UnsafeAccess;
import rx.schedulers.Schedulers;

public abstract class ObjectPool<T> {
    private final Queue<T> pool;
    private final int maxSize;

    private final Scheduler.Worker schedulerWorker;

    /*
     * Approach to using WeakReference inspired by https://gist.github.com/UnquietCode/5717608
     */
    private final ConcurrentHashMap<WeakReference<Ref<T>>, T> references = new ConcurrentHashMap<WeakReference<Ref<T>>, T>();
    private final ReferenceQueue<Ref<T>> referenceQueue = new ReferenceQueue<Ref<T>>();

    public ObjectPool() {
        this(0, 0, 7);
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
        this.maxSize = max;
        //         initialize pool
        if (UnsafeAccess.isUnsafeAvailable()) {
            pool = new MpmcArrayQueue<T>(Math.max(maxSize, 1024));
        } else {
            pool = new ConcurrentLinkedQueue<T>();
        }

        for (int i = 0; i < min; i++) {
            pool.add(createObject());
        }

        schedulerWorker = Schedulers.computation().createWorker();
        schedulerWorker.schedulePeriodically(new Action0() {

            @Override
            public void call() {
                int size = pool.size();
                Reference<? extends Ref<T>> r = null;
                while ((r = referenceQueue.poll()) != null) {
                    // we got one that was ready for GC so use it
                    T availableObject = references.remove(r);
                    if (availableObject != null) {
                        if (size < min) {
                            // reset and add
                            resetObject(availableObject);
                            pool.offer(availableObject);
                            size++;
                        }
                    }
                }

                if (size < min) {
                    int sizeToBeAdded = max - size;
                    for (int i = 0; i < sizeToBeAdded; i++) {
                        pool.add(createObject());
                    }
                } else if (size > max) {
                    int sizeToBeRemoved = size - max;
                    for (int i = 0; i < sizeToBeRemoved; i++) {
                        //                        pool.pollLast();
                        pool.poll();
                    }
                }
            }

        }, validationInterval, validationInterval, TimeUnit.SECONDS);

    }

    /**
     * Gets the next free object from the pool. If the pool doesn't contain any objects,
     * a new object will be created and given to the caller of this method back.
     *
     * @return T borrowed object
     */
    public Ref<T> borrowObject() {
        T object = null;
        if ((object = pool.poll()) == null) {
            object = createObject();
        }

        Ref<T> ref = new Ref<T>(object);

        // only try and reclaim if we're under our size limit
        if (references.size() < maxSize) {
            WeakReference<Ref<T>> weakRef = new WeakReference<Ref<T>>(ref, referenceQueue);
            references.put(weakRef, object);
        }

        return ref;
    }

    /**
     * Shutdown this pool.
     */
    public void shutdown() {
        schedulerWorker.unsubscribe();
    }

    /**
     * Creates a new object.
     *
     * @return T new object
     */
    protected abstract T createObject();

    protected abstract void resetObject(T t);

    public static class Ref<T> {
        private final T t;

        private Ref(T t) {
            this.t = t;
        }

        public T get() {
            return t;
        }
    }
}