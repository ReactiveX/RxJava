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

package io.reactivex;

import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.*;

import org.reactivestreams.*;

import io.reactivex.disposables.Disposable;
import io.reactivex.internal.disposables.EmptyDisposable;
import io.reactivex.internal.operators.single.*;
import io.reactivex.internal.subscriptions.*;
import io.reactivex.internal.util.Exceptions;

/**
 * Represents a deferred computation and emission of a single value or exception.
 * 
 * @param <T> the value type
 */
public class Single<T> {
    
    public interface SingleCallback<T> {
        
        void onSubscribe(Disposable d);
        
        void onSuccess(T value);
        
        void onFailure(Throwable e);
    }
    
    public interface SingleOnSubscribe<T> extends Consumer<SingleCallback<? super T>> {
        
    }
    
    public interface SingleOperator<Downstream, Upstream> extends Function<SingleCallback<? super Downstream>, SingleCallback<? super Upstream>> {
        
    }
    
    public interface SingleTransformer<Upstream, Downstream> extends Function<Single<Upstream>, Single<Downstream>> {
        
    }
    
    protected final SingleOnSubscribe<T> onSubscribe;
    
    protected Single(SingleOnSubscribe<T> onSubscribe) {
        this.onSubscribe = onSubscribe;
    }
    
    public static <T> Single<T> create(SingleOnSubscribe<T> onSubscribe) {
        Objects.requireNonNull(onSubscribe);
        // TODO plugin wrapper
        return new Single<>(onSubscribe);
    }
    
    public final <R> Single<R> lift(SingleOperator<? extends R, ? super T> onLift) {
        Objects.requireNonNull(onLift);
        return create(s -> {
            SingleCallback<? super T> sr = onLift.apply(s);
            // TODO plugin wrapper
            onSubscribe.accept(sr);
        });
    }
    
    public final <R> R to(Function<? super Single<T>, R> convert) {
        return convert.apply(this);
    }
    
    public final <R> Single<R> compose(Function<? super Single<T>, ? extends Single<R>> convert) {
        return to(convert);
    }
    
    public final void subscribe(SingleCallback<? super T> subscriber) {
        Objects.requireNonNull(subscriber);
        onSubscribe.accept(subscriber);
    }
    
    public final <R> Single<R> map(Function<? super T, ? extends R> mapper) {
        return lift(new SingleOperatorMap<>(mapper));
    }
    
    public static <T> Single<T> fromCallable(Callable<? extends T> callable) {
        Objects.requireNonNull(callable);
        return create(s -> {
            s.onSubscribe(EmptyDisposable.INSTANCE);
            try {
                T v = callable.call();
                if (v != null) {
                    s.onSuccess(v);
                } else {
                    s.onFailure(new NullPointerException());
                }
            } catch (Throwable e) {
                s.onFailure(e);
            }
        });
    }
    
    public static <T> Single<T> just(T value) {
        Objects.requireNonNull(value);
        return create(s -> {
            s.onSubscribe(EmptyDisposable.INSTANCE);
            s.onSuccess(value);
        });
    }
    
    public T get() {
        AtomicReference<T> valueRef = new AtomicReference<>();
        AtomicReference<Throwable> errorRef = new AtomicReference<>();
        CountDownLatch cdl = new CountDownLatch(1);
        
        subscribe(new SingleCallback<T>() {
            @Override
            public void onSubscribe(Disposable d) {
            }

            @Override
            public void onSuccess(T value) {
                valueRef.lazySet(value);
                cdl.countDown();
            }
            @Override
            public void onFailure(Throwable e) {
                errorRef.lazySet(e);
                cdl.countDown();
            }
        });
        
        if (cdl.getCount() != 0L) {
            try {
                cdl.await();
            } catch (InterruptedException ex) {
                throw new RuntimeException(ex);
            }
        }
        Throwable e = errorRef.get();
        if (e != null) {
            throw Exceptions.propagate(e);
        }
        return valueRef.get();
    }
    
    public final Observable<T> toFlowable() {
        return Observable.create(s -> {
            ScalarAsyncSubscription<T> sas = new ScalarAsyncSubscription<>(s);
            AsyncSubscription as = new AsyncSubscription();
            as.setSubscription(sas);
            s.onSubscribe(as);
            
            subscribe(new SingleCallback<T>() {
                @Override
                public void onSubscribe(Disposable d) {
                    as.setResource(d);
                }

                @Override
                public void onSuccess(T value) {
                    sas.setValue(value);
                }

                @Override
                public void onFailure(Throwable e) {
                    s.onError(e);
                }
                
            });
        });
    }
    
    public static <T> Single<T> fromPublisher(Publisher<? extends T> publisher) {
        Objects.requireNonNull(publisher);
        return create(s -> {
            publisher.subscribe(new Subscriber<T>() {
                T value;
                @Override
                public void onSubscribe(Subscription inner) {
                    s.onSubscribe(inner::cancel);
                    inner.request(Long.MAX_VALUE);
                }

                @Override
                public void onNext(T t) {
                    value = t;
                }

                @Override
                public void onError(Throwable t) {
                    value = null;
                    s.onFailure(t);
                }

                @Override
                public void onComplete() {
                    T v = value;
                    value = null;
                    if (v != null) {
                        s.onSuccess(v);
                    } else {
                        s.onFailure(new NoSuchElementException());
                    }
                }
                
            });
        });
    }
    
    public final <R> Single<R> flatMap(Function<? super T, ? extends Single<? extends R>> mapper) {
        return lift(new SingleOperatorFlatMap<>(mapper));   
    }
    
    public static <T> Single<T> merge(Single<? extends Single<? extends T>> source) {
        return source.flatMap(v -> v);
    }
}
