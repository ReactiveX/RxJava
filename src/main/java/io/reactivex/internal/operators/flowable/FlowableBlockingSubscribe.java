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

package io.reactivex.internal.operators.flowable;

import java.util.concurrent.*;

import org.reactivestreams.*;

import io.reactivex.exceptions.Exceptions;
import io.reactivex.functions.*;
import io.reactivex.internal.functions.Functions;
import io.reactivex.internal.subscribers.flowable.*;
import io.reactivex.internal.util.*;
import io.reactivex.plugins.RxJavaPlugins;
import io.reactivex.subscribers.DefaultSubscriber;

/**
 * Utility methods to consume a Publisher in a blocking manner with callbacks or Subscriber.
 */
public enum FlowableBlockingSubscribe {
    ;
    
    /**
     * Subscribes to the source and calls the Subscriber methods on the current thread.
     * <p>
     * @param o the source publisher
     * The unsubscription and backpressure is composed through.
     * @param subscriber the subscriber to forward events and calls to in the current thread
     * @param <T> the value type
     */
    public static <T> void subscribe(Publisher<? extends T> o, Subscriber<? super T> subscriber) {
        final BlockingQueue<Object> queue = new LinkedBlockingQueue<Object>();
        
        BlockingSubscriber<T> bs = new BlockingSubscriber<T>(queue);
        
        o.subscribe(bs);
        
        try {
            for (;;) {
                if (bs.isCancelled()) {
                    break;
                }
                Object v = queue.poll();
                if (v == null) {
                    if (bs.isCancelled()) {
                        break;
                    }
                    v = queue.take();
                }
                if (bs.isCancelled()) {
                    break;
                }
                if (o == BlockingSubscriber.TERMINATED) {
                    break;
                }
                if (NotificationLite.acceptFull(v, subscriber)) {
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
     * @param o the source publisher
     * @param <T> the value type
     */
    public static <T> void subscribe(Publisher<? extends T> o) {
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
        
        BlockingHelper.awaitForComplete(cdl, ls);
        Throwable e = error[0];
        if (e != null) {
            throw ExceptionHelper.wrapOrThrow(e);
        }
    }
    
    /**
     * Subscribes to the source and calls the given actions on the current thread.
     * @param o the source publisher
     * @param onNext the callback action for each source value
     * @param onError the callback action for an error event
     * @param onComplete the callback action for the completion event.
     * @param <T> the value type
     */
    public static <T> void subscribe(Publisher<? extends T> o, final Consumer<? super T> onNext, 
            final Consumer<? super Throwable> onError, final Action onComplete) {
        subscribe(o, new DefaultSubscriber<T>() {
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
