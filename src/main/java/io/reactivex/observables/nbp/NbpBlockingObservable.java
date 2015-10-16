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

package io.reactivex.observables.nbp;

import java.util.*;
import java.util.concurrent.*;
import java.util.function.Consumer;
import java.util.stream.*;

import io.reactivex.*;
import io.reactivex.NbpObservable.NbpSubscriber;
import io.reactivex.disposables.Disposable;
import io.reactivex.internal.operators.nbp.*;
import io.reactivex.internal.subscribers.*;
import io.reactivex.internal.subscribers.nbp.*;
import io.reactivex.internal.util.*;
import io.reactivex.plugins.RxJavaPlugins;
import io.reactivex.subscribers.nbp.NbpAsyncObserver;

public final class NbpBlockingObservable<T> implements Iterable<T> {
    final NbpObservable<? extends T> o;
    protected NbpBlockingObservable(NbpObservable<? extends T> source) {
        this.o = source;
    }
    
    public static <T> NbpBlockingObservable<T> from(NbpObservable<? extends T> source) {
        return new NbpBlockingObservable<>(source);
    }
    
    @Override
    public Iterator<T> iterator() {
        return iterate(o);
    }
    
    @Override
    public void forEach(Consumer<? super T> action) {
        BlockingIterator<T> it = iterate(o);
        while (it.hasNext()) {
            try {
                action.accept(it.next());
            } catch (Throwable e) {
                it.dispose();
                throw e;
            }
        }
    }
    
    static final <T> BlockingIterator<T> iterate(NbpObservable<? extends T> p) {
        final BlockingQueue<Object> queue = new LinkedBlockingQueue<>();

        NbpLambdaSubscriber<T> ls = new NbpLambdaSubscriber<>(
            v -> queue.offer(NotificationLite.next(v)),
            e -> queue.offer(NotificationLite.error(e)),
            () -> queue.offer(NotificationLite.complete()),
            s -> { }
        );
        
        p.subscribe(ls);
        
        return new BlockingIterator<>(queue, ls);
    }
    
    static final class BlockingIterator<T> implements Iterator<T>, AutoCloseable, Disposable {
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
        public void close() throws Exception {
            resource.dispose();
        }
        
        @Override
        public void dispose() {
            resource.dispose();
        }
    }

    static <T> Stream<T> makeStream(Iterator<T> it, boolean parallel) {
        Spliterator<T> s = Spliterators.spliteratorUnknownSize(it, 0);
        Stream<T> st = StreamSupport.stream(s, parallel);
        if (it instanceof BlockingIterator) {
            
            BlockingIterator<T> bit = (BlockingIterator<T>) it;
            st = st.onClose(bit::dispose);
        }
        return st;
    }
    
    public Stream<T> stream() {
        return makeStream(iterator(), false);
    }

    public Stream<T> parallelStream() {
        return makeStream(iterator(), true);
    }

    public Optional<T> firstOption() {
        return stream().findFirst();
    }
    
    public T first() {
        return firstOption().get();
    }
    
    public T first(T defaultValue) {
        return firstOption().orElse(defaultValue);
    }
    
    public Optional<T> lastOption() {
        return stream().reduce((a, b) -> b);
    }
    
    public T last() {
        return lastOption().get();
    }
    
    public T last(T defaultValue) {
        return lastOption().orElse(defaultValue);
    }
    
    public T single() {
        Iterator<T> it = iterate(o.single());
        Optional<T> o = makeStream(it, false).findFirst();
        return o.get();
    }
    
    public T single(T defaultValue) {
        @SuppressWarnings("unchecked")
        Iterator<T> it = iterate(((NbpObservable<T>)o).single(defaultValue));
        Optional<T> o = makeStream(it, false).findFirst();
        return o.orElse(defaultValue);
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
    
    public CompletableFuture<T> toFuture() {
        CompletableFuture<T> f = new CompletableFuture<>();
        
        NbpAsyncObserver<T> s = new NbpAsyncObserver<T>() {
            @Override
            protected void onStart() {
                f.whenComplete((v, e) -> {
                    cancel();
                });
            }
            
            @Override
            public void onNext(T t) {
                f.complete(t);
            }
            
            @Override
            public void onError(Throwable t) {
                f.completeExceptionally(t);
            }
            
            @Override
            public void onComplete() {
                if (!f.isDone()) {
                    f.completeExceptionally(new NoSuchElementException());
                }
            }
        };
        
        o.takeLast(1).subscribe(s);
        
        return f;
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
        LambdaSubscriber<T> ls = new LambdaSubscriber<>(v -> { }, e -> {
            error[0] = e;
            cdl.countDown();
        }, () -> {
            cdl.countDown();
        }, s -> s.request(Long.MAX_VALUE));
        
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
    public void subscribe(NbpSubscriber<? super T> subscriber) {
        final BlockingQueue<Object> queue = new LinkedBlockingQueue<>();
        
        NbpBlockingSubscriber<T> bs = new NbpBlockingSubscriber<>(queue);
        
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
        subscribe(onNext, RxJavaPlugins::onError, () -> { });
    }
    
    /**
     * Subscribes to the source and calls the given actions on the current thread.
     * @param onNext the callback action for each source value
     * @param onError the callback action for an error event
     */
    public void subscribe(final Consumer<? super T> onNext, final Consumer<? super Throwable> onError) {
        subscribe(onNext, onError, () -> { });
    }
    
    /**
     * Subscribes to the source and calls the given actions on the current thread.
     * @param onNext the callback action for each source value
     * @param onError the callback action for an error event
     * @param onComplete the callback action for the completion event.
     */
    public void subscribe(final Consumer<? super T> onNext, final Consumer<? super Throwable> onError, final Runnable onComplete) {
        subscribe(new NbpObserver<T>() {
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
