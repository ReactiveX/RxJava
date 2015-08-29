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

package io.reactivex.observables;

import java.util.*;
import java.util.concurrent.*;
import java.util.function.Consumer;
import java.util.stream.*;

import org.reactivestreams.*;

import io.reactivex.Observable;
import io.reactivex.Observer;
import io.reactivex.disposables.Disposable;
import io.reactivex.internal.operators.*;
import io.reactivex.internal.subscribers.*;
import io.reactivex.internal.util.*;
import io.reactivex.plugins.RxJavaPlugins;

public final class BlockingObservable<T> implements Publisher<T>, Iterable<T> {
    final Publisher<? extends T> o;
    protected BlockingObservable(Publisher<? extends T> source) {
        this.o = source;
    }
    
    @SuppressWarnings("unchecked")
    public static <T> BlockingObservable<T> from(Publisher<? extends T> source) {
        if (source instanceof BlockingObservable) {
            return (BlockingObservable<T>)source;
        }
        return new BlockingObservable<>(source);
    }
    
    @Override
    public Iterator<T> iterator() {
        return iterate(o);
    }
    
    static final <T> BlockingIterator<T> iterate(Publisher<? extends T> p) {
        final BlockingQueue<Object> queue = new LinkedBlockingQueue<>();

        LambdaSubscriber<T> ls = new LambdaSubscriber<>(
            v -> queue.offer(NotificationLite.next(v)),
            e -> queue.offer(NotificationLite.error(e)),
            () -> queue.offer(NotificationLite.complete()),
            s -> s.request(Long.MAX_VALUE)
        );
        
        p.subscribe(ls);
        
        return new BlockingIterator<>(queue, ls);
    }
    
    static final class BlockingIterator<T> implements Iterator<T>, AutoCloseable, Disposable {
        final Queue<Object> queue;
        final Disposable resource;
        public BlockingIterator(Queue<Object> queue, Disposable resource) {
            this.queue = queue;
            this.resource = resource;
        }
        @Override
        public boolean hasNext() {
            Object o = queue.peek();
            if (NotificationLite.isError(o)) {
                Throwable e = NotificationLite.getError(o);
                Exceptions.propagate(e);
            }
            return !NotificationLite.isComplete(o);
        }
        
        @Override
        public T next() {
            if (hasNext()) {
                Object o = queue.poll();
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
        return stream().reduce((a, b) -> b);
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
        Iterator<T> it = iterate(Observable.fromPublisher(o).single());
        Optional<T> o = makeStream(it, false).findFirst();
        if (o.isPresent()) {
            return o.get();
        }
        throw new NoSuchElementException();
    }
    
    public T single(T defaultValue) {
        Iterator<T> it = iterate(Observable.<T>fromPublisher(o).single(defaultValue));
        Optional<T> o = makeStream(it, false).findFirst();
        if (o.isPresent()) {
            return o.get();
        }
        return defaultValue;
    }
    
    public Iterable<T> mostRecent(T initialValue) {
        return BlockingOperatorMostRecent.mostRecent(o, initialValue);
    }
    
    public Iterable<T> next() {
        return BlockingOperatorNext.next(o);
    }
    
    public Iterable<T> latest() {
        return BlockingOperatorLatest.latest(o);
    }
    
    public CompletableFuture<T> toFuture() {
        CompletableFuture<T> f = new CompletableFuture<>();
        Observable<T> source = Observable.fromPublisher(o);
        
        Observer<T> s = new Observer<T>() {
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
        
        source.takeLast(1).subscribe(s);
        
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
    @Override
    public void subscribe(Subscriber<? super T> subscriber) {
        final BlockingQueue<Object> queue = new LinkedBlockingQueue<>();
        
        BlockingSubscriber<T> bs = new BlockingSubscriber<>(queue);
        
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
        subscribe(new Observer<T>() {
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
