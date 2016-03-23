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

package io.reactivex.observables;

import java.io.Closeable;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicReference;

import io.reactivex.Observable;
import io.reactivex.Observer;
import io.reactivex.Optional;
import io.reactivex.disposables.*;
import io.reactivex.functions.Consumer;
import io.reactivex.internal.functions.Functions;
import io.reactivex.internal.operators.observable.*;
import io.reactivex.internal.subscribers.flowable.BlockingSubscriber;
import io.reactivex.internal.subscribers.observable.*;
import io.reactivex.internal.util.*;
import io.reactivex.observers.DefaultObserver;
import io.reactivex.plugins.RxJavaPlugins;

public final class BlockingObservable<T> implements Iterable<T> {
    final Observable<? extends T> o;
    protected BlockingObservable(Observable<? extends T> source) {
        this.o = source;
    }
    
    public static <T> BlockingObservable<T> from(Observable<? extends T> source) {
        return new BlockingObservable<T>(source);
    }
    
    @Override
    public Iterator<T> iterator() {
        return iterate(o);
    }
    
    public void forEach(Consumer<? super T> action) {
        BlockingIterator<T> it = iterate(o);
        while (it.hasNext()) {
            try {
                action.accept(it.next());
            } catch (Throwable e) {
                it.dispose();
                Exceptions.propagate(e);
            }
        }
    }
    
    static final <T> BlockingIterator<T> iterate(Observable<? extends T> p) {
        final BlockingQueue<Object> queue = new LinkedBlockingQueue<Object>();

        NbpLambdaSubscriber<T> ls = new NbpLambdaSubscriber<T>(
            new Consumer<T>() {
                @Override
                public void accept(T v) {
                    queue.offer(NotificationLite.next(v));
                }
            },
            new Consumer<Throwable>() {
                @Override
                public void accept(Throwable e) {
                    queue.offer(NotificationLite.error(e));
                }
            },
            new Runnable() {
                @Override
                public void run() {
                    queue.offer(NotificationLite.complete());
                }
            },
            Functions.emptyConsumer()
        );
        
        p.subscribe(ls);
        
        return new BlockingIterator<T>(queue, ls);
    }
    
    static final class BlockingIterator<T> implements Iterator<T>, Closeable, Disposable {
        final BlockingQueue<Object> queue;
        final Disposable resource;
        
        Object last;
        
        public BlockingIterator(BlockingQueue<Object> queue, Disposable resource) {
            this.queue = queue;
            this.resource = resource;
        }
        @Override
        public boolean hasNext() {
            if (last == null) { 
                Object o = queue.poll();
                if (o == null) {
                    try {
                        o = queue.take();
                    } catch (InterruptedException ex) {
                        resource.dispose();
                        Thread.currentThread().interrupt();
                        Exceptions.propagate(ex);
                    }
                }
                last = o;
                if (NotificationLite.isError(o)) {
                    resource.dispose();
                    Throwable e = NotificationLite.getError(o);
                    Exceptions.propagate(e);
                }
                if (NotificationLite.isComplete(o)) {
                    resource.dispose();
                    return false;
                }
                return true;
            }
            Object o = last;
            if (NotificationLite.isError(o)) {
                Throwable e = NotificationLite.getError(o);
                Exceptions.propagate(e);
            }
            return !NotificationLite.isComplete(o);
        }
        
        @Override
        public T next() {
            if (hasNext()) {
                Object o = last;
                last = null;
                return NotificationLite.getValue(o);
            }
            throw new NoSuchElementException();
        }
        
        @Override
        public void remove() {
            throw new UnsupportedOperationException();
        }
        
        @Override
        public void close() {
            resource.dispose();
        }
        
        @Override
        public void dispose() {
            resource.dispose();
        }
    }

    public Optional<T> firstOption() {
        return firstOption(o);
    }
    
    static <T> Optional<T> firstOption(Observable<? extends T> o) {
        final AtomicReference<T> value = new AtomicReference<T>();
        final AtomicReference<Throwable> error = new AtomicReference<Throwable>();
        final CountDownLatch cdl = new CountDownLatch(1);
        final MultipleAssignmentDisposable mad = new MultipleAssignmentDisposable();
        
        o.subscribe(new Observer<T>() {
            Disposable s;
            @Override
            public void onSubscribe(Disposable s) {
                this.s = s;
                mad.set(s);
            }
            
            @Override
            public void onNext(T t) {
                s.dispose();
                value.lazySet(t);
                cdl.countDown();
            }
            
            @Override
            public void onError(Throwable t) {
                error.lazySet(t);
                cdl.countDown();
            }
            
            @Override
            public void onComplete() {
                cdl.countDown();
            }
        });
        
        try {
            cdl.await();
        } catch (InterruptedException ex) {
            mad.dispose();
            Exceptions.propagate(ex);
        }
        
        Throwable e = error.get();
        if (e != null) {
            Exceptions.propagate(e);
        }
        T v = value.get();
        return v != null ? Optional.of(v) : Optional.<T>empty();
    }
    
    public T first() {
        Optional<T> o = firstOption();
        if (o.isPresent()) {
            return o.get();
        }
        throw new NoSuchElementException();
    }
    
    public T first(T defaultValue) {
        Optional<T> o = firstOption();
        if (o.isPresent()) {
            return o.get();
        }
        return defaultValue;
    }
    
    public Optional<T> lastOption() {
        return lastOption(o);
    }
    
    static <T> Optional<T> lastOption(Observable<? extends T> o) {
        final AtomicReference<T> value = new AtomicReference<T>();
        final AtomicReference<Throwable> error = new AtomicReference<Throwable>();
        final CountDownLatch cdl = new CountDownLatch(1);
        final MultipleAssignmentDisposable mad = new MultipleAssignmentDisposable();
        
        o.subscribe(new Observer<T>() {
            @Override
            public void onSubscribe(Disposable s) {
                mad.set(s);
            }
            
            @Override
            public void onNext(T t) {
                value.lazySet(t);
            }
            
            @Override
            public void onError(Throwable t) {
                error.lazySet(t);
                cdl.countDown();
            }
            
            @Override
            public void onComplete() {
                cdl.countDown();
            }
        });
        
        try {
            cdl.await();
        } catch (InterruptedException ex) {
            mad.dispose();
            Exceptions.propagate(ex);
        }
        
        Throwable e = error.get();
        if (e != null) {
            Exceptions.propagate(e);
        }
        T v = value.get();
        return v != null ? Optional.of(v) : Optional.<T>empty();
    }
    
    public T last() {
        Optional<T> o = lastOption();
        if (o.isPresent()) {
            return o.get();
        }
        throw new NoSuchElementException();
    }
    
    public T last(T defaultValue) {
        Optional<T> o = lastOption();
        if (o.isPresent()) {
            return o.get();
        }
        return defaultValue;
    }
    
    public T single() {
        Optional<T> o = firstOption(this.o.single());
        if (o.isPresent()) {
            return o.get();
        }
        throw new NoSuchElementException();
    }
    
    public T single(T defaultValue) {
        @SuppressWarnings("unchecked")
        Optional<T> o = firstOption(((Observable<T>)this.o).single(defaultValue));
        if (o.isPresent()) {
            return o.get();
        }
        return defaultValue;
    }
    
    public Iterable<T> mostRecent(T initialValue) {
        return NbpBlockingOperatorMostRecent.mostRecent(o, initialValue);
    }
    
    public Iterable<T> next() {
        return NbpBlockingOperatorNext.next(o);
    }
    
    public Iterable<T> latest() {
        return NbpBlockingOperatorLatest.latest(o);
    }
    
    public Future<T> toFuture() {
        final CountDownLatch cdl = new CountDownLatch(1);
        final AtomicReference<T> value = new AtomicReference<T>();
        final AtomicReference<Throwable> error = new AtomicReference<Throwable>();
        final MultipleAssignmentDisposable mad = new MultipleAssignmentDisposable();
        
        o.subscribe(new Observer<T>() {

            @Override
            public void onSubscribe(Disposable d) {
                mad.set(d);
            }

            @Override
            public void onNext(T v) {
                value.lazySet(v);
            }

            @Override
            public void onError(Throwable e) {
                error.lazySet(e);
                cdl.countDown();
            }

            @Override
            public void onComplete() {
                cdl.countDown();
            }
            
        });
        
        return new Future<T>() {
            
            @Override
            public boolean cancel(boolean mayInterruptIfRunning) {
                if (cdl.getCount() != 0) {
                    mad.dispose();
                    return true;
                }
                return false;
            }

            @Override
            public boolean isCancelled() {
                return mad.isDisposed();
            }

            @Override
            public boolean isDone() {
                return cdl.getCount() == 0 && !mad.isDisposed();
            }

            @Override
            public T get() throws InterruptedException, ExecutionException {
                if (cdl.getCount() != 0) {
                    cdl.await();
                }
                Throwable e = error.get();
                if (e != null) {
                    throw new ExecutionException(e);
                }
                return value.get();
            }

            @Override
            public T get(long timeout, TimeUnit unit)
                    throws InterruptedException, ExecutionException, TimeoutException {
                if (cdl.getCount() != 0) {
                    if (!cdl.await(timeout, unit)) {
                        throw new TimeoutException();
                    }
                }
                Throwable e = error.get();
                if (e != null) {
                    throw new ExecutionException(e);
                }
                return value.get();
            }
            
        };
    }
    
    private void awaitForComplete(CountDownLatch latch, Disposable subscription) {
        if (latch.getCount() == 0) {
            // Synchronous observable completes before awaiting for it.
            // Skip await so InterruptedException will never be thrown.
            return;
        }
        // block until the subscription completes and then return
        try {
            latch.await();
        } catch (InterruptedException e) {
            subscription.dispose();
            // set the interrupted flag again so callers can still get it
            // for more information see https://github.com/ReactiveX/RxJava/pull/147#issuecomment-13624780
            Thread.currentThread().interrupt();
            // using Runtime so it is not checked
            throw new RuntimeException("Interrupted while waiting for subscription to complete.", e);
        }
    }
    
    /**
     * Runs the source observable to a terminal event, ignoring any values and rethrowing any exception.
     */
    public void run() {
        final CountDownLatch cdl = new CountDownLatch(1);
        final Throwable[] error = { null };
        NbpLambdaSubscriber<T> ls = new NbpLambdaSubscriber<T>(
            Functions.emptyConsumer(), 
            new Consumer<Throwable>() {
                @Override
                public void accept(Throwable e) {
                    error[0] = e;
                    cdl.countDown();
                }
            }, new Runnable() {
                @Override
                public void run() {
                    cdl.countDown();
                }
            }, Functions.emptyConsumer());
        
        o.subscribe(ls);
        
        awaitForComplete(cdl, ls);
        Throwable e = error[0];
        if (e != null) {
            if (e instanceof RuntimeException) {
                throw (RuntimeException)e;
            } else {
                throw new RuntimeException(e);
            }
        }
    }
    
    /**
     * Subscribes to the source and calls the Subscriber methods on the current thread.
     * <p>
     * The unsubscription and backpressure is composed through.
     * @param subscriber the subscriber to forward events and calls to in the current thread
     */
    public void subscribe(Observer<? super T> subscriber) {
        final BlockingQueue<Object> queue = new LinkedBlockingQueue<Object>();
        
        NbpBlockingSubscriber<T> bs = new NbpBlockingSubscriber<T>(queue);
        
        o.subscribe(bs);
        
        try {
            for (;;) {
                if (bs.isCancelled()) {
                    break;
                }
                Object o = queue.poll();
                if (o == null) {
                    if (bs.isCancelled()) {
                        break;
                    }
                    o = queue.take();
                }
                if (bs.isCancelled()) {
                    break;
                }
                if (o == BlockingSubscriber.TERMINATED) {
                    break;
                }
                if (NotificationLite.accept(o, subscriber)) {
                    break;
                }
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            subscriber.onError(e);
        } finally {
            bs.dispose();
        }
    }
    
    /**
     * Runs the source observable to a terminal event, ignoring any values and rethrowing any exception.
     */
    public void subscribe() {
        run();
    }
    
    /**
     * Subscribes to the source and calls the given action on the current thread and rethrows any exception wrapped
     * into OnErrorNotImplementedException.
     * @param onNext the callback action for each source value
     */
    public void subscribe(final Consumer<? super T> onNext) {
        subscribe(onNext, RxJavaPlugins.errorConsumer(), Functions.emptyRunnable());
    }
    
    /**
     * Subscribes to the source and calls the given actions on the current thread.
     * @param onNext the callback action for each source value
     * @param onError the callback action for an error event
     */
    public void subscribe(final Consumer<? super T> onNext, final Consumer<? super Throwable> onError) {
        subscribe(onNext, onError, Functions.emptyRunnable());
    }
    
    /**
     * Subscribes to the source and calls the given actions on the current thread.
     * @param onNext the callback action for each source value
     * @param onError the callback action for an error event
     * @param onComplete the callback action for the completion event.
     */
    public void subscribe(final Consumer<? super T> onNext, final Consumer<? super Throwable> onError, final Runnable onComplete) {
        subscribe(new DefaultObserver<T>() {
            @Override
            public void onNext(T t) {
                onNext.accept(t);
            }
            
            @Override
            public void onError(Throwable e) {
                onError.accept(e);
            }
            
            @Override
            public void onComplete() {
                onComplete.run();
            }
        });
    }
}