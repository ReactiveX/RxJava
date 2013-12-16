/**
 * Copyright 2013 Netflix, Inc.
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
 */
package rx.operators;

import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.concurrent.Semaphore;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import rx.Notification;
import rx.Observable;
import rx.Observer;
import rx.util.Exceptions;

/**
 * Wait for and iterate over the latest values of the source observable.
 * If the source works faster than the iterator, values may be skipped, but
 * not the onError or onCompleted events.
 */
public final class OperationLatest {
    /** Utility class. */
    private OperationLatest() { throw new IllegalStateException("No instances!"); }  
    
    public static <T> Iterable<T> latest(final Observable<? extends T> source) {
        return new Iterable<T>() {
            @Override
            public Iterator<T> iterator() {
                LatestObserverIterator<T> lio = new LatestObserverIterator<T>();
                source.subscribe(lio);
                return lio;
            }
        };
    }
    
    /** Observer of source, iterator for output. */
    static final class LatestObserverIterator<T> implements Observer<T>, Iterator<T> {
        final Lock lock = new ReentrantLock();
        final Semaphore notify = new Semaphore(0);
        // observer's values
        boolean oHasValue;
        Notification.Kind oKind;
        T oValue;
        Throwable oError;
        @Override
        public void onNext(T args) {
            boolean wasntAvailable;
            lock.lock();
            try {
                wasntAvailable = !oHasValue;
                oHasValue = true;
                oValue = args;
                oKind = Notification.Kind.OnNext;
            } finally {
                lock.unlock();
            }
            if (wasntAvailable) {
                notify.release();
            }
        }

        @Override
        public void onError(Throwable e) {
            boolean wasntAvailable;
            lock.lock();
            try {
                wasntAvailable = !oHasValue;
                oHasValue = true;
                oValue = null;
                oError = e;
                oKind = Notification.Kind.OnError;
            } finally {
                lock.unlock();
            }
            if (wasntAvailable) {
                notify.release();
            }
        }

        @Override
        public void onCompleted() {
            boolean wasntAvailable;
            lock.lock();
            try {
                wasntAvailable = !oHasValue;
                oHasValue = true;
                oValue = null;
                oKind = Notification.Kind.OnCompleted;
            } finally {
                lock.unlock();
            }
            if (wasntAvailable) {
                notify.release();
            }
        }
        
        // iterator's values
        
        boolean iDone;
        boolean iHasValue;
        T iValue;
        Throwable iError;
        Notification.Kind iKind;
        
        @Override
        public boolean hasNext() {
            if (iError != null) {
                Exceptions.propagate(iError);
            }
            if (!iDone) {
                if (!iHasValue) {
                    try {
                        notify.acquire();
                    } catch (InterruptedException ex) {
                        iError = ex;
                        iHasValue = true;
                        iKind = Notification.Kind.OnError;
                        return true;
                    }
                    
                    lock.lock();
                    try {
                        iKind = oKind;
                        switch (oKind) {
                        case OnNext:
                            iValue = oValue;
                            oValue = null; // handover
                            break;
                        case OnError:
                            iError = oError;
                            oError = null; // handover
                            if (iError != null) {
                                Exceptions.propagate(iError);
                            }
                            break;
                        case OnCompleted:
                            iDone = true;
                            break;
                        }
                        oHasValue = false;
                    } finally {
                        lock.unlock();
                    }
                    iHasValue = true;
                }
            }
            return !iDone;
        }

        @Override
        public T next() {
            if (iKind == Notification.Kind.OnError) {
                Exceptions.propagate(iError);
            }
            if (hasNext()) {
                if (iKind == Notification.Kind.OnNext) {
                    T v = iValue;
                    iValue = null; // handover
                    iHasValue = false;
                    return v;
                }
            }
            throw new NoSuchElementException();
        }

        @Override
        public void remove() {
            throw new UnsupportedOperationException("Read-only iterator.");
        }
        
    }
}
