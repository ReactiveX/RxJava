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
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.*;
import java.util.function.*;

import org.junit.*;
import org.reactivestreams.*;

import io.reactivex.Observable.Operator;
import io.reactivex.internal.operators.*;
import io.reactivex.schedulers.Schedulers;
import io.reactivex.subscribers.TestSubscriber;

public class ObservableConversionTest {
    
    public static class Cylon {}
    
    public static class Jail {
        Object cylon;
        
        Jail(Object cylon) {
            this.cylon = cylon;
        }
    }
    
    public static class CylonDetectorObservable<T> {
        protected Publisher<T> onSubscribe;
        
        public static <T> CylonDetectorObservable<T> create(Publisher<T> onSubscribe) {
            return new CylonDetectorObservable<>(onSubscribe);
        }

        protected CylonDetectorObservable(Publisher<T> onSubscribe) {
            this.onSubscribe = onSubscribe;
        }

        public void subscribe(Subscriber<T> subscriber) {
            onSubscribe.subscribe(subscriber);
        }

        public <R> CylonDetectorObservable<R> lift(Operator<? extends R, ? super T> operator) {
            return x(new RobotConversionFunc<T, R>(operator));
        }
        
        public <R, O> O x(Function<Publisher<T>, O> operator) {
            return operator.apply(onSubscribe);
        }

        public <R> CylonDetectorObservable<? extends R> compose(Function<CylonDetectorObservable<? super T>, CylonDetectorObservable<? extends R>> transformer) {
            return transformer.apply(this);
        }
        
        public final CylonDetectorObservable<T> beep(Predicate<? super T> predicate) {
            return lift(new OperatorFilter<T>(predicate));
        }
        
        public final <R> CylonDetectorObservable<R> boop(Function<? super T, ? extends R> func) {
            return lift(new OperatorMap<T, R>(func));
        }

        public CylonDetectorObservable<String> DESTROY() {
            return boop(new Function<T, String>() {
                @Override
                public String apply(T t) {
                    Object cylon = ((Jail) t).cylon;
                    throwOutTheAirlock(cylon);
                    if (t instanceof Jail) {
                        String name = cylon.toString();
                        return "Cylon '" + name + "' has been destroyed";
                    }
                    else {
                        return "Cylon 'anonymous' has been destroyed";
                    }
                }});
        }
        
        private static void throwOutTheAirlock(Object cylon) {
            // ...
        }
    }
    
    public static class RobotConversionFunc<T, R> implements Function<Publisher<T>, CylonDetectorObservable<R>> {
        private Operator<? extends R, ? super T> operator;

        public RobotConversionFunc(Operator<? extends R, ? super T> operator) {
            this.operator = operator;
        }

        @Override
        public CylonDetectorObservable<R> apply(final Publisher<T> onSubscribe) {
            return CylonDetectorObservable.create(new Publisher<R>() {
                @Override
                public void subscribe(Subscriber<? super R> o) {
                    try {
                        Subscriber<? super T> st = operator.apply(o);
                        try {
                            onSubscribe.subscribe(st);
                        } catch (Throwable e) {
                            st.onError(e);
                        }
                    } catch (Throwable e) {
                        o.onError(e);
                    }
                
                }});
        }
    }
    
    public static class ConvertToCylonDetector<T> implements Function<Publisher<T>, CylonDetectorObservable<T>> {
        @Override
        public CylonDetectorObservable<T> apply(final Publisher<T> onSubscribe) {
            return CylonDetectorObservable.create(onSubscribe);
        }
    }
    
    public static class ConvertToObservable<T> implements Function<Publisher<T>, Observable<T>> {
        @Override
        public Observable<T> apply(final Publisher<T> onSubscribe) {
            return Observable.create(onSubscribe);
        }
    }
    
    @Test
    public void testConversionBetweenObservableClasses() {
        final TestSubscriber<String> subscriber = new TestSubscriber<>(new Observer<String>() {

            @Override
            public void onComplete() {
                System.out.println("Complete");
            }

            @Override
            public void onError(Throwable e) {
                System.out.println("error: " + e.getMessage());
                e.printStackTrace();
            }

            @Override
            public void onNext(String t) {
                System.out.println(t);
            }
        });
        
        List<Object> crewOfBattlestarGalactica = Arrays.asList(new Object[] {"William Adama", "Laura Roslin", "Lee Adama", new Cylon()});
        
        Observable.fromIterable(crewOfBattlestarGalactica)
            .doOnNext(System.out::println)
            .to(new ConvertToCylonDetector<>())
            .beep(t -> t instanceof Cylon)
            .boop(cylon -> new Jail(cylon))
            .DESTROY()
            .x(new ConvertToObservable<String>())
            .reduce("Cylon Detector finished. Report:\n", (a, n) -> a + n + "\n")
            .subscribe(subscriber);
        
        subscriber.assertNoErrors();
        subscriber.assertComplete();
    }
    
    @Test
    public void testConvertToConcurrentQueue() {
        final AtomicReference<Throwable> thrown = new AtomicReference<>(null);
        final AtomicBoolean isFinished = new AtomicBoolean(false);
        ConcurrentLinkedQueue<? extends Integer> queue = Observable.range(0,5)
                .flatMap(i -> Observable.range(0, 5)
                        .observeOn(Schedulers.io())
                        .map(k -> {
                            try {
                                Thread.sleep(System.currentTimeMillis() % 100);
                            } catch (InterruptedException e) {
                                e.printStackTrace();
                            }
                            return i + k;
                        }))
                    .to(onSubscribe -> {
                        final ConcurrentLinkedQueue<Integer> q = new ConcurrentLinkedQueue<>();
                        onSubscribe.subscribe(new Observer<Integer>(){
                            @Override
                            public void onComplete() {
                                isFinished.set(true);
                            }
      
                            @Override
                            public void onError(Throwable e) {
                                thrown.set(e);
                            }
      
                            @Override
                            public void onNext(Integer t) {
                                q.add(t);
                            }});
                        return q;
                    });
        
        int x = 0;
        while (!isFinished.get()) {
            Integer i = queue.poll();
            if (i != null) {
                x++;
                System.out.println(x + " item: " + i);
            }
        }
        Assert.assertNull(thrown.get());
    }
}