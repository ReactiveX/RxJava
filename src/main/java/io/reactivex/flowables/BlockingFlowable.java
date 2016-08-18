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

package io.reactivex.flowables;

import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicReference;

import org.reactivestreams.*;

import io.reactivex.Flowable;
import io.reactivex.Optional;
import io.reactivex.disposables.*;
import io.reactivex.exceptions.Exceptions;
import io.reactivex.functions.*;
import io.reactivex.internal.disposables.SequentialDisposable;
import io.reactivex.internal.functions.Functions;
import io.reactivex.internal.operators.flowable.*;
import io.reactivex.internal.subscribers.flowable.*;
import io.reactivex.internal.util.NotificationLite;
import io.reactivex.plugins.RxJavaPlugins;
import io.reactivex.subscribers.DefaultSubscriber;

public final class BlockingFlowable<T> implements Publisher<T>, Iterable<T> {
    final Publisher<? extends T> o;
    protected BlockingFlowable(Publisher<? extends T> source) {
        this.o = source;
    }
    
    @SuppressWarnings("unchecked")
    public static <T> BlockingFlowable<T> from(Publisher<? extends T> source) {
        if (source instanceof BlockingFlowable) {
            return (BlockingFlowable<T>)source;
        }
        return new BlockingFlowable<T>(source);
    }
    
    @Override
    public Iterator<T> iterator() {
        return iterate(o);
    }
    
    public void forEach(Consumer<? super T> action) {
        BlockingFlowableIterator<T> it = iterate(o);
        while (it.hasNext()) {
            try {
                action.accept(it.next());
            } catch (Throwable e) {
                Exceptions.throwIfFatal(e);
                it.dispose();
                throw Exceptions.propagate(e);
            }
        }
    }
    
    static <T> BlockingFlowableIterator<T> iterate(Publisher<? extends T> p) {
        BlockingFlowableIterator<T> it = new BlockingFlowableIterator<T>(Flowable.bufferSize());
        p.subscribe(it);
        return it;
    }
    
    public Optional<T> firstOption() {
        T v = first(o);
        return v != null ? Optional.of(v) : Optional.<T>empty();
    }
    
    static <T> T first(Publisher<? extends T> o) {
        BlockingFirstSubscriber<T> s = new BlockingFirstSubscriber<T>();
        o.subscribe(s);
        return s.blockingGet();
    }
    
    public T first() {
        T v = first(o);
        if (v != null) {
            return v;
        }
        throw new NoSuchElementException();
    }
    
    public T first(T defaultValue) {
        T v = first(o);
        if (v != null) {
            return v;
        }
        return defaultValue;
    }
    
    public Optional<T> lastOption() {
        T v = last(o);
        return v != null ? Optional.of(v) : Optional.<T>empty();
    }
    
    static <T> T last(Publisher<? extends T> o) {
        BlockingLastSubscriber<T> s = new BlockingLastSubscriber<T>();
        o.subscribe(s);
        return s.blockingGet();
    }
    
    public T last() {
        T v = last(o);
        if (v != null) {
            return v;
        }
        throw new NoSuchElementException();
    }
    
    public T last(T defaultValue) {
        T v = last(o);
        if (v != null) {
            return v;
        }
        return defaultValue;
    }
    
    @SuppressWarnings("unchecked")
    public T single() {
        return first(new FlowableSingle<T>((Publisher<T>)this.o, null));
    }
    
    @SuppressWarnings("unchecked")
    public T single(T defaultValue) {
        return first(new FlowableSingle<T>((Publisher<T>)this.o, defaultValue));
    }
    
    public Iterable<T> mostRecent(T initialValue) {
        return BlockingFlowableMostRecent.mostRecent(o, initialValue);
    }
    
    public Iterable<T> next() {
        return BlockingFlowableNext.next(o);
    }
    
    public Iterable<T> latest() {
        return BlockingFlowableLatest.latest(o);
    }
    
    public Future<T> toFuture() {
        final CountDownLatch cdl = new CountDownLatch(1);
        final AtomicReference<T> value = new AtomicReference<T>();
        final AtomicReference<Throwable> error = new AtomicReference<Throwable>();
        final SequentialDisposable sd = new SequentialDisposable();
        
        o.subscribe(new Subscriber<T>() {

            @Override
            public void onSubscribe(Subscription d) {
                sd.replace(Disposables.from(d));
                d.request(Long.MAX_VALUE);
            }

            @Override
            public void onNext(T v) {
                if (value.get() != null) {
                    sd.dispose();
                    onError(new IndexOutOfBoundsException("More than one element received"));
                    return;
                }
                value.lazySet(v);
            }

            @Override
            public void onError(Throwable e) {
                error.lazySet(e);
                cdl.countDown();
            }

            @Override
            public void onComplete() {
                if (value.get() == null) {
                    onError(new NoSuchElementException("The source is empty"));
                    return;
                }
                cdl.countDown();
            }
            
        });
        
        return new Future<T>() {
            
            @Override
            public boolean cancel(boolean mayInterruptIfRunning) {
                if (cdl.getCount() != 0) {
                    sd.dispose();
                    error.set(new CancellationException());
                    cdl.countDown();
                    return true;
                }
                return false;
            }

            @Override
            public boolean isCancelled() {
                return sd.isDisposed();
            }

            @Override
            public boolean isDone() {
                return cdl.getCount() == 0 && !sd.isDisposed();
            }

            @Override
            public T get() throws InterruptedException, ExecutionException {
                if (cdl.getCount() != 0) {
                    cdl.await();
                }
                Throwable e = error.get();
                if (e != null) {
                    if (e instanceof CancellationException) {
                        throw (CancellationException)e;
                    }
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
                    if (e instanceof CancellationException) {
                        throw (CancellationException)e;
                    }
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
            throw new IllegalStateException("Interrupted while waiting for subscription to complete.", e);
        }
    }
    
    /**
     * Runs the source observable to a terminal event, ignoring any values and rethrowing any exception.
     */
    public void run() {
        final CountDownLatch cdl = new CountDownLatch(1);
        final Throwable[] error = { null };
        LambdaSubscriber<T> ls = new LambdaSubscriber<T>(Functions.emptyConsumer(), 
        new Consumer<Throwable>() {
            @Override
            public void accept(Throwable e) {
                error[0] = e;
                cdl.countDown();
            }
        }, new Action() {
            @Override
            public void run() {
                cdl.countDown();
            }
        }, new Consumer<Subscription>() {
            @Override
            public void accept(Subscription s) {
                s.request(Long.MAX_VALUE);
            }
        });
        
        o.subscribe(ls);
        
        awaitForComplete(cdl, ls);
        Throwable e = error[0];
        if (e != null) {
            Exceptions.propagate(e);
        }
    }
    
    /**
     * Subscribes to the source and calls the Subscriber methods on the current thread.
     * <p>
     * The unsubscription and backpressure is composed through.
     * @param subscriber the subscriber to forward events and calls to in the current thread
     */
    @Override
    public void subscribe(Subscriber<? super T> subscriber) {
        final BlockingQueue<Object> queue = new LinkedBlockingQueue<Object>();
        
        BlockingSubscriber<T> bs = new BlockingSubscriber<T>(queue);
        
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
                if (NotificationLite.acceptFull(o, subscriber)) {
                    break;
                }
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            subscriber.onError(e);
        } finally {
            bs.cancel();
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
        subscribe(onNext, RxJavaPlugins.errorConsumer(), Functions.EMPTY_RUNNABLE);
    }
    
    /**
     * Subscribes to the source and calls the given actions on the current thread.
     * @param onNext the callback action for each source value
     * @param onError the callback action for an error event
     */
    public void subscribe(final Consumer<? super T> onNext, final Consumer<? super Throwable> onError) {
        subscribe(onNext, onError, Functions.EMPTY_RUNNABLE);
    }
    
    /**
     * Subscribes to the source and calls the given actions on the current thread.
     * @param onNext the callback action for each source value
     * @param onError the callback action for an error event
     * @param onComplete the callback action for the completion event.
     */
    public void subscribe(final Consumer<? super T> onNext, final Consumer<? super Throwable> onError, final Runnable onComplete) {
        subscribe(new DefaultSubscriber<T>() {
            boolean done;
            @Override
            public void onNext(T t) {
                if (done) {
                    return;
                }
                try {
                    onNext.accept(t);
                } catch (Throwable ex) {
                    Exceptions.throwIfFatal(ex);
                    cancel();
                    onError(ex);
                }
            }
            
            @Override
            public void onError(Throwable e) {
                if (done) {
                    RxJavaPlugins.onError(e);
                    return;
                }
                done = true;
                try {
                    onError.accept(e);
                } catch (Throwable ex) {
                    Exceptions.throwIfFatal(ex);
                    RxJavaPlugins.onError(ex);
                }
            }
            
            @Override
            public void onComplete() {
                if (done) {
                    return;
                }
                done = true;
                try {
                    onComplete.run();
                } catch (Throwable ex) {
                    Exceptions.throwIfFatal(ex);
                    RxJavaPlugins.onError(ex);
                }
            }
        });
    }
}