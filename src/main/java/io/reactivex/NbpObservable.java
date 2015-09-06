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
import java.util.concurrent.CountDownLatch;
import java.util.function.*;

import org.reactivestreams.*;

import io.reactivex.disposables.*;
import io.reactivex.internal.disposables.EmptyDisposable;
import io.reactivex.internal.operators.nbp.*;
import io.reactivex.internal.util.Exceptions;

/**
 * Observable for delivering a sequence of values without backpressure. 
 * @param <T>
 */
public class NbpObservable<T> {

    public interface NbpOnSubscribe<T> extends Consumer<NbpSubscriber<? super T>> {
        
    }
    
    public interface NbpSubscriber<T> {
        
        void onSubscribe(Disposable d);
        
        void onNext(T value);
        
        void onError(Throwable e);
        
        void onComplete();
    }
    
    public interface NbpOperator<Downstream, Upstream> extends Function<NbpSubscriber<? super Downstream>, NbpSubscriber<? super Upstream>> {
        
    }
    
    public interface NbpTransformer<Upstream, Downstream> extends Function<NbpObservable<Upstream>, NbpObservable<Downstream>> {
        
    }
    
    protected final NbpOnSubscribe<T> onSubscribe;
    
    protected NbpObservable(NbpOnSubscribe<T> onSubscribe) {
        this.onSubscribe = onSubscribe;
    }
    
    public static <T> NbpObservable<T> create(NbpOnSubscribe<T> onSubscribe) {
        Objects.requireNonNull(onSubscribe);
        // TODO plugin wrapper
        return new NbpObservable<>(onSubscribe);
    }
    
    public final <R> NbpObservable<R> lift(NbpOperator<? extends R, ? super T> onLift) {
        Objects.requireNonNull(onLift);
        return create(s -> {
            NbpSubscriber<? super T> sr = onLift.apply(s);
            // TODO plugin wrapper
            onSubscribe.accept(sr);
        });
    }
    
    public final <R> R to(Function<? super NbpObservable<T>, R> convert) {
        return convert.apply(this);
    }
    
    public final <R> NbpObservable<R> compose(Function<? super NbpObservable<T>, ? extends NbpObservable<R>> convert) {
        return to(convert);
    }
    
    public final void subscribe(NbpSubscriber<? super T> subscriber) {
        Objects.requireNonNull(subscriber);
        onSubscribe.accept(subscriber);
    }

    public final <R> NbpObservable<R> flatMap(Function<? super T, ? extends NbpObservable<? extends R>> mapper) {
        return flatMap(mapper, false);
    }
    
    public final <R> NbpObservable<R> flatMap(Function<? super T, ? extends NbpObservable<? extends R>> mapper, boolean delayError) {
        return lift(new NbpOperatorFlatMap<>(mapper, delayError));
    }
    
    public static <T> NbpObservable<T> merge(NbpObservable<? extends NbpObservable<? extends T>> sources) {
        return sources.flatMap(v -> v);
    }

    public static <T> NbpObservable<T> mergeDelayError(NbpObservable<? extends NbpObservable<? extends T>> sources) {
        return sources.flatMap(v -> v, true);
    }
    
    public final <R> NbpObservable<R> map(Function<? super T, ? extends R> mapper) {
        return lift(new NbpOperatorMap<>(mapper));
    }
    
    public static <T> NbpObservable<T> just(T value) {
        return create(s -> {
            s.onSubscribe(EmptyDisposable.INSTANCE);
            s.onNext(value);
            s.onComplete();
        });
    }
    
    public static NbpObservable<Integer> range(int start, int count) {
        return create(s -> {
            BooleanDisposable d = new BooleanDisposable();
            s.onSubscribe(d);
            
            int end = start - 1 + count;
            for (int i = start; i <= end && !d.isDisposed(); i++) {
                s.onNext(i);
            }
            if (!d.isDisposed()) {
                s.onComplete();
            }
        });
    }
    
    public static <T> NbpObservable<T> fromIterable(Iterable<? extends T> source) {
        return create(s -> {
            BooleanDisposable d = new BooleanDisposable();
            s.onSubscribe(d);
            
            if (d.isDisposed()) {
                return;
            }
            try {
                Iterator<? extends T> it = source.iterator();
                while (it.hasNext()) {
                    if (d.isDisposed()) {
                        return;
                    }
                    s.onNext(it.next());
                }
            } catch (Throwable e) {
                s.onError(e);
                return;
            }
            s.onComplete();
        });
    }
    
    public final List<T> getList() {
        List<T> result = new ArrayList<>();
        Throwable[] error = { null };
        CountDownLatch cdl = new CountDownLatch(1);
        
        subscribe(new NbpSubscriber<T>() {
            @Override
            public void onSubscribe(Disposable d) {
                
            }
            @Override
            public void onNext(T value) {
                result.add(value);
            }
            @Override
            public void onError(Throwable e) {
                error[0] = e;
                cdl.countDown();
            }
            @Override
            public void onComplete() {
                cdl.countDown();
            }
        });
        
        if (cdl.getCount() != 0) {
            try {
                cdl.await();
            } catch (InterruptedException ex) {
                throw Exceptions.propagate(ex);
            }
        }
        
        Throwable e = error[0];
        if (e != null) {
            throw Exceptions.propagate(e);
        }
        
        return result;
    }
    
    public static <T> NbpObservable<T> fromPublisher(Publisher<? extends T> publisher) {
        return create(s -> {
            publisher.subscribe(new Subscriber<T>() {

                @Override
                public void onSubscribe(Subscription inner) {
                    s.onSubscribe(inner::cancel);
                    inner.request(Long.MAX_VALUE);
                }

                @Override
                public void onNext(T t) {
                    s.onNext(t);
                }

                @Override
                public void onError(Throwable t) {
                    s.onError(t);
                }

                @Override
                public void onComplete() {
                    s.onComplete();
                }
                
            });
        });
    }
    
    public final Observable<T> toObservable(BackpressureStrategy strategy) {
        Observable<T> o = Observable.create(s -> {
            subscribe(new NbpSubscriber<T>() {

                @Override
                public void onSubscribe(Disposable d) {
                    s.onSubscribe(new Subscription() {

                        @Override
                        public void request(long n) {
                            // no backpressure so nothing we can do about this
                        }

                        @Override
                        public void cancel() {
                            d.dispose();
                        }
                        
                    });
                }

                @Override
                public void onNext(T value) {
                    s.onNext(value);
                }

                @Override
                public void onError(Throwable e) {
                    s.onError(e);
                }

                @Override
                public void onComplete() {
                    s.onComplete();
                }
                
            });
        });
        
        switch (strategy) {
        case BUFFER:
            return o.onBackpressureBuffer();
        case DROP:
            return o.onBackpressureDrop();
        case LATEST:
            return o.onBackpressureLatest();
        default:
            return o;
        }
    }
 }
